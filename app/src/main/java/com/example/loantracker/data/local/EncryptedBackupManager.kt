package com.example.loantracker.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.example.loantracker.data.local.database.AppDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/** Creates portable, passphrase-encrypted copies of the complete Room database. */
@Singleton
class EncryptedBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase
) {
    private val databaseName = "app_database"
    private val magic = byteArrayOf('T'.code.toByte(), 'C'.code.toByte(), 'B'.code.toByte(), '1'.code.toByte())

    suspend fun export(
        destination: Uri,
        passphrase: String,
        onProgress: (Float?) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        val output = context.contentResolver.openOutputStream(destination, "wt")
            ?: error("Unable to write the selected backup file.")
        output.use {
            writeEncryptedBackup(passphrase, it, onProgress)
        }
    }

    suspend fun exportToFile(
        destination: File,
        passphrase: String,
        onProgress: (Float?) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        destination.outputStream().use {
            writeEncryptedBackup(passphrase, it, onProgress)
        }
    }

    private fun writeEncryptedBackup(
        passphrase: String,
        output: java.io.OutputStream,
        onProgress: (Float?) -> Unit
    ) {
        require(passphrase.length >= 8) { "Use a backup passphrase with at least 8 characters." }
        onProgress(0.02f)
        val source = prepareDatabaseForBackup()
        onProgress(0.05f)
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val iv = ByteArray(12).also(SecureRandom()::nextBytes)
        val cipher = cipher(Cipher.ENCRYPT_MODE, passphrase, salt, iv)

        output.use {
            it.write(magic)
            it.write(salt)
            it.write(iv)
            CipherOutputStream(it, cipher).use { encrypted ->
                source.inputStream().use { input ->
                    copyWithProgress(
                        input = input,
                        output = encrypted,
                        totalBytes = source.length(),
                        startFraction = 0.05f,
                        endFraction = 0.98f,
                        onProgress = onProgress
                    )
                }
            }
        }
        onProgress(1f)
    }

    suspend fun exportToTree(
        treeUri: Uri,
        passphrase: String,
        onProgress: (Float?) -> Unit = {}
    ): Uri = withContext(Dispatchers.IO) {
        onProgress(0.01f)
        val root = DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri)
        )
        val loanTrackerFolder = findOrCreateDirectory(treeUri, root, "LoanTracker")
        val backupFolder = findOrCreateDirectory(treeUri, loanTrackerFolder, "backup")
        val fileName = "loan-tracker-${System.currentTimeMillis()}.tcb"
        val destination = DocumentsContract.createDocument(
            context.contentResolver,
            backupFolder,
            "application/octet-stream",
            fileName
        ) ?: error("Unable to create the backup file in the selected folder.")
        try {
            export(destination, passphrase, onProgress)
        } catch (error: Exception) {
            context.contentResolver.delete(destination, null, null)
            throw error
        }
        destination
    }

    /** Restores a backup and closes Room; the caller must restart the application afterwards. */
    suspend fun restore(
        source: Uri,
        passphrase: String,
        onProgress: (Float?) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        require(passphrase.length >= 8) { "Enter the passphrase used to create this backup." }
        val input = context.contentResolver.openInputStream(source)
            ?: error("Unable to read the selected backup file.")
        restoreInput(input, getDocumentSize(source), passphrase, onProgress)
    }

    suspend fun restoreFromFile(
        source: File,
        passphrase: String,
        onProgress: (Float?) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        require(source.exists() && source.isFile) { "The downloaded backup file is unavailable." }
        restoreInput(source.inputStream(), source.length(), passphrase, onProgress)
    }

    private fun restoreInput(
        input: java.io.InputStream,
        sourceSize: Long?,
        passphrase: String,
        onProgress: (Float?) -> Unit
    ) {
        onProgress(0.02f)
        val encryptedBackup = File(context.cacheDir, "tailors-counterpart-backup-import.bin")
        val restored = File(context.cacheDir, "tailors-counterpart-restored.db")
        try {
            input.use { sourceStream ->
                encryptedBackup.outputStream().use { output ->
                    copyWithProgress(
                        input = sourceStream,
                        output = output,
                        totalBytes = sourceSize,
                        startFraction = 0.02f,
                        endFraction = 0.68f,
                        onProgress = onProgress
                    )
                }
            }
            onProgress(0.7f)
            encryptedBackup.inputStream().buffered().use { backupInput ->
                backupInput.mark(magic.size)
                val prefix = ByteArray(magic.size)
                val prefixSize = backupInput.read(prefix)
                backupInput.reset()
                if (prefixSize == magic.size && prefix.contentEquals(magic)) {
                    val header = ByteArray(magic.size + 16 + 12)
                    DataInputStream(backupInput).readFully(header)
                    val salt = header.copyOfRange(magic.size, magic.size + 16)
                    val iv = header.copyOfRange(magic.size + 16, header.size)
                    val cipher = cipher(Cipher.DECRYPT_MODE, passphrase, salt, iv)
                    CipherInputStream(backupInput, cipher).use { decrypted ->
                        restored.outputStream().use { output ->
                            copyWithProgress(
                                input = decrypted,
                                output = output,
                                totalBytes = (encryptedBackup.length() - magic.size - 16 - 12 - 16)
                                    .takeIf { it > 0L },
                                startFraction = 0.7f,
                                endFraction = 0.88f,
                                onProgress = onProgress
                            )
                        }
                    }
                } else {
                    restoreLegacyDeviceBackup(encryptedBackup, restored)
                    onProgress(0.88f)
                }
            }
            validateDatabase(restored)
            onProgress(0.94f)

            database.close()
            val destination = context.getDatabasePath(databaseName)
            destination.parentFile?.mkdirs()
            File(destination.path + "-wal").delete()
            File(destination.path + "-shm").delete()
            restored.copyTo(destination, overwrite = true)
            onProgress(1f)
        } finally {
            encryptedBackup.delete()
            restored.delete()
        }
    }

    private fun getDocumentSize(uri: Uri): Long? =
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val size = cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE))
                size.takeIf { it > 0L }
            } else {
                null
            }
        }

    private fun copyWithProgress(
        input: java.io.InputStream,
        output: java.io.OutputStream,
        totalBytes: Long?,
        startFraction: Float,
        endFraction: Float,
        onProgress: (Float?) -> Unit
    ) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var copiedBytes = 0L
        var lastReportedProgress = startFraction
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            output.write(buffer, 0, read)
            copiedBytes += read
            if (totalBytes != null) {
                val ratio = (copiedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
                val progress = startFraction + (endFraction - startFraction) * ratio
                if (progress - lastReportedProgress >= 0.01f || ratio >= 1f) {
                    onProgress(progress)
                    lastReportedProgress = progress
                }
            } else {
                onProgress(null)
            }
        }
        onProgress(endFraction)
    }

    private fun restoreLegacyDeviceBackup(encrypted: File, destination: File) {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val legacyFile = EncryptedFile.Builder(
            context,
            encrypted,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
        legacyFile.openFileInput().use { input ->
            destination.outputStream().use { output -> input.copyTo(output) }
        }
    }

    private fun prepareDatabaseForBackup(): File {
        database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { cursor ->
            if (cursor.moveToFirst() && cursor.getInt(0) != 0) {
                error("The database is busy; try creating the backup again.")
            }
        }
        val source = context.getDatabasePath(databaseName)
        require(source.exists()) { "There is no database to back up yet." }
        return source
    }

    private fun cipher(mode: Int, passphrase: String, salt: ByteArray, iv: ByteArray): Cipher {
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, 210_000, 256)
        val keyBytes = try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
        return try {
            Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(mode, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(128, iv))
            }
        } finally {
            keyBytes.fill(0)
        }
    }

    private fun validateDatabase(file: File) {
        require(file.exists() && file.length() > 0) { "The backup did not contain a database." }
        val restoredDb = SQLiteDatabase.openDatabase(
            file.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY
        )
        try {
            require(restoredDb.version in 1..3) { "Unsupported backup database version." }
            restoredDb.rawQuery("PRAGMA quick_check", null).use { cursor ->
                require(cursor.moveToFirst() && cursor.getString(0) == "ok") {
                    "The backup database is damaged."
                }
            }
            val tables = mutableSetOf<String>()
            restoredDb.rawQuery(
                "SELECT name FROM sqlite_master WHERE type = 'table'",
                null
            ).use { cursor ->
                val nameColumn = cursor.getColumnIndexOrThrow("name")
                while (cursor.moveToNext()) tables += cursor.getString(nameColumn)
            }
            require(
                setOf("customers", "contacts", "projects", "measurements", "project_measurements", "payments", "notes")
                    .all { it in tables }
            ) { "The backup does not contain a complete Loan Tracker database." }
        } finally {
            restoredDb.close()
        }
    }

    private fun findOrCreateDirectory(treeUri: Uri, parent: Uri, name: String): Uri {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            DocumentsContract.getDocumentId(parent)
        )
        context.contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ),
            null,
            null,
            null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            while (cursor.moveToNext()) {
                if (cursor.getString(nameColumn) == name &&
                    cursor.getString(mimeColumn) == DocumentsContract.Document.MIME_TYPE_DIR
                ) {
                    return DocumentsContract.buildDocumentUriUsingTree(treeUri, cursor.getString(idColumn))
                }
            }
        } ?: error("Unable to list folders in the selected backup location.")

        return DocumentsContract.createDocument(
            context.contentResolver,
            parent,
            DocumentsContract.Document.MIME_TYPE_DIR,
            name
        ) ?: error("Unable to create the $name folder.")
    }
}
