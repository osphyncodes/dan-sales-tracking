package com.example.loantracker.data.local

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File as DriveFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FilterOutputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

data class DriveBackupFile(
    val id: String,
    val name: String,
    val modifiedTime: Date?
)

@Singleton
class GoogleDriveBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptedBackups: EncryptedBackupManager
) {
    suspend fun createBackup(
        account: GoogleSignInAccount,
        onProgress: (Float?) -> Unit
    ): DriveBackupFile = withContext(Dispatchers.IO) {
        val drive = createDriveClient(account)
        val folderId = findOrCreateBackupFolder(drive)
        val localBackup = java.io.File(
            context.cacheDir,
            "loan-tracker-drive-backup-${System.currentTimeMillis()}.tcb"
        )
        try {
            val email = account.email?.takeIf { it.isNotBlank() }
                ?: error("The connected Google account does not have an email address.")
            encryptedBackups.exportToFile(localBackup, email) { progress ->
                onProgress(progress?.times(0.25f))
            }
            val metadata = DriveFile().apply {
                name = "loan-tracker-${System.currentTimeMillis()}.tcb"
                mimeType = "application/octet-stream"
                parents = listOf(folderId)
            }
            val request = drive.files()
                .create(metadata, FileContent("application/octet-stream", localBackup))
                .setFields("id,name,modifiedTime")
            request.mediaHttpUploader
                .setDirectUploadEnabled(false)
                .setProgressListener { uploader ->
                    onProgress(0.25f + uploader.progress.toFloat().coerceIn(0f, 1f) * 0.75f)
                }
            val uploaded = request.execute()
            onProgress(1f)
            DriveBackupFile(
                id = uploaded.id,
                name = uploaded.name,
                modifiedTime = uploaded.modifiedTime?.value?.let(::Date)
            )
        } finally {
            localBackup.delete()
        }
    }

    suspend fun listBackups(account: GoogleSignInAccount): List<DriveBackupFile> =
        withContext(Dispatchers.IO) {
            val drive = createDriveClient(account)
            val folders = findBackupFolders(drive)
            folders.flatMap { folderId ->
                drive.files().list()
                    .setQ("'$folderId' in parents and name contains '.tcb' and trashed = false")
                    .setSpaces("drive")
                    .setPageSize(1)
                    .setOrderBy("modifiedTime desc")
                    .setFields("files(id,name,modifiedTime)")
                    .execute()
                    .files
                    .orEmpty()
                    .map { file ->
                        DriveBackupFile(
                            id = file.id,
                            name = file.name,
                            modifiedTime = file.modifiedTime?.value?.let(::Date)
                        )
                    }
            }.sortedByDescending { it.modifiedTime }.take(1)
        }

    suspend fun restoreBackup(
        account: GoogleSignInAccount,
        backup: DriveBackupFile,
        onProgress: (Float?) -> Unit
    ) = withContext(Dispatchers.IO) {
        val drive = createDriveClient(account)
        val metadata = drive.files().get(backup.id).setFields("id,name,size").execute()
        val downloaded = java.io.File(
            context.cacheDir,
            "loan-tracker-drive-restore-${System.currentTimeMillis()}.tcb"
        )
        try {
            val totalBytes = metadata.size?.takeIf { it > 0L }
            var copied = 0L
            var lastProgress = 0f
            val output = object : FilterOutputStream(FileOutputStream(downloaded)) {
                override fun write(buffer: ByteArray, offset: Int, length: Int) {
                    out.write(buffer, offset, length)
                    copied += length
                    if (totalBytes != null) {
                        val fraction = (copied.toFloat() / totalBytes).coerceIn(0f, 1f)
                        if (fraction - lastProgress >= 0.01f || fraction >= 1f) {
                            onProgress(fraction * 0.65f)
                            lastProgress = fraction
                        }
                    } else {
                        onProgress(null)
                    }
                }

                override fun write(value: Int) {
                    out.write(value)
                    copied++
                }
            }
            output.use {
                drive.files().get(backup.id).executeMediaAndDownloadTo(it)
            }
            onProgress(0.65f)
            val email = account.email?.takeIf { it.isNotBlank() }
                ?: error("The connected Google account does not have an email address.")
            encryptedBackups.restoreFromFile(downloaded, email) { progress ->
                onProgress(progress?.let { 0.65f + it * 0.35f })
            }
            onProgress(1f)
        } finally {
            downloaded.delete()
        }
    }

    private fun createDriveClient(account: GoogleSignInAccount): Drive {
        val selectedAccount = account.account
            ?: error("The selected Google account is unavailable. Connect Google Drive again.")
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_FILE)
        ).apply {
            this.selectedAccount = selectedAccount
        }
        return Drive.Builder(
            com.google.api.client.extensions.android.http.AndroidHttp.newCompatibleTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("Loan Tracker")
            .build()
    }

    private fun findOrCreateBackupFolder(drive: Drive): String {
        val rootFolder = findOrCreateFolder(drive, "root", "LoanTracker")
        return findOrCreateFolder(drive, rootFolder, "backup")
    }

    private fun findBackupFolders(drive: Drive): List<String> {
        return listOf("LoanTracker", "TailorCounterpart")
            .mapNotNull { name -> findFolder(drive, "root", name) }
            .mapNotNull { folderId -> findFolder(drive, folderId, "backup") }
            .distinct()
    }

    private fun findOrCreateFolder(drive: Drive, parentId: String, name: String): String =
        findFolder(drive, parentId, name) ?: drive.files().create(
            DriveFile().apply {
                this.name = name
                mimeType = "application/vnd.google-apps.folder"
                parents = listOf(parentId)
            }
        ).setFields("id").execute().id

    private fun findFolder(drive: Drive, parentId: String, name: String): String? =
        drive.files().list()
            .setQ("'$parentId' in parents and name = '$name' and mimeType = 'application/vnd.google-apps.folder' and trashed = false")
            .setSpaces("drive")
            .setPageSize(10)
            .setFields("files(id,name)")
            .execute()
            .files
            ?.firstOrNull()
            ?.id
}
