package com.example.tailorscounterpart.viewmodel

import android.net.Uri
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.tailorscounterpart.data.local.AutomaticBackupSettings
import com.example.tailorscounterpart.data.local.EncryptedBackupManager
import com.example.tailorscounterpart.data.local.DriveBackupFile
import com.example.tailorscounterpart.data.local.GoogleDriveBackupWorker
import com.example.tailorscounterpart.data.local.GoogleDriveBackupManager
import com.example.tailorscounterpart.data.local.UserPreferences
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class BackupProgress(
    val label: String,
    val fraction: Float?
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backups: EncryptedBackupManager,
    private val googleDriveBackups: GoogleDriveBackupManager,
    private val preferences: UserPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    private val _isBusy = MutableStateFlow(false)
    val isBusy = _isBusy.asStateFlow()
    private val _progress = MutableStateFlow<BackupProgress?>(null)
    val progress = _progress.asStateFlow()
    private val _driveBackups = MutableStateFlow<List<DriveBackupFile>>(emptyList())
    val driveBackups = _driveBackups.asStateFlow()
    val automaticBackupSettings = preferences.automaticBackupSettings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AutomaticBackupSettings()
    )

    fun export(destination: Uri, passphrase: String) = viewModelScope.launch {
        runOperation(
            label = "Creating encrypted backup...",
            operation = { onProgress -> backups.export(destination, passphrase, onProgress) },
            successMessage = "Encrypted backup created.",
            failureMessage = "Backup failed."
        )
    }

    fun exportToFolder(folder: Uri, passphrase: String) = viewModelScope.launch {
        runOperation(
            label = "Uploading encrypted backup to Google Drive...",
            operation = { onProgress -> backups.exportToTree(folder, passphrase, onProgress) },
            successMessage = "Encrypted backup saved to TailorCounterpart/backup.",
            failureMessage = "Backup failed."
        )
    }

    fun restore(source: Uri, passphrase: String) = viewModelScope.launch {
        runOperation(
            label = "Downloading and restoring backup from Google Drive...",
            operation = { onProgress -> backups.restore(source, passphrase, onProgress) },
            successMessage = "Backup restored. Restart the app to use it.",
            failureMessage = "Restore failed."
        )
    }

    fun createGoogleDriveBackup(account: GoogleSignInAccount) = viewModelScope.launch {
        runOperation(
            label = "Encrypting and uploading backup to Google Drive...",
            operation = { onProgress -> googleDriveBackups.createBackup(account, onProgress) },
            successMessage = "Backup uploaded to Google Drive.",
            failureMessage = "Google Drive backup failed."
        )
    }

    fun loadGoogleDriveBackups(account: GoogleSignInAccount) = viewModelScope.launch {
        if (_isBusy.value) return@launch
        _isBusy.value = true
        _driveBackups.value = emptyList()
        _progress.value = BackupProgress("Loading backups from Google Drive...", null)
        try {
            _driveBackups.value = googleDriveBackups.listBackups(account)
            _progress.value = null
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            _progress.value = null
            _message.value = error.message ?: "Unable to load backups from Google Drive."
        } finally {
            _isBusy.value = false
        }
    }

    fun restoreGoogleDriveBackup(
        account: GoogleSignInAccount,
        backup: DriveBackupFile
    ) = viewModelScope.launch {
        runOperation(
            label = "Downloading and restoring ${backup.name}...",
            operation = { onProgress ->
                googleDriveBackups.restoreBackup(account, backup, onProgress)
            },
            successMessage = "Backup restored. Restart the app to use it.",
            failureMessage = "Google Drive restore failed."
        )
    }

    fun configureAutomaticBackup(enabled: Boolean, frequency: String) = viewModelScope.launch {
        preferences.configureAutomaticBackup(enabled, frequency)
        val workManager = WorkManager.getInstance(context)
        if (!enabled) {
            workManager.cancelUniqueWork(AUTOMATIC_BACKUP_WORK)
            _message.value = "Automatic Google Drive backup disabled."
            return@launch
        }

        val intervalDays = when (frequency) {
            "weekly" -> 7L
            "monthly" -> 30L
            else -> 1L
        }
        val request = PeriodicWorkRequestBuilder<GoogleDriveBackupWorker>(
            intervalDays,
            TimeUnit.DAYS
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        workManager.enqueueUniquePeriodicWork(
            AUTOMATIC_BACKUP_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
        _message.value = "Automatic Google Drive backup enabled (${frequency})."
    }

    private suspend fun <T> runOperation(
        label: String,
        operation: suspend ((Float?) -> Unit) -> T,
        successMessage: String,
        failureMessage: String
    ) {
        if (_isBusy.value) return
        _isBusy.value = true
        _progress.value = BackupProgress(label, 0f)
        try {
            operation { fraction ->
                _progress.value = BackupProgress(label, fraction?.coerceIn(0f, 1f))
            }
            _message.value = successMessage
            _progress.value = BackupProgress(successMessage, 1f)
        } catch (error: CancellationException) {
            _progress.value = null
            throw error
        } catch (error: Exception) {
            _message.value = error.message ?: failureMessage
            _progress.value = null
        } finally {
            _isBusy.value = false
        }
    }

    fun clearMessage() { _message.value = null }

    fun showMessage(message: String) {
        _message.value = message
    }

    private companion object {
        const val AUTOMATIC_BACKUP_WORK = "automatic_google_drive_backup"
    }
}
