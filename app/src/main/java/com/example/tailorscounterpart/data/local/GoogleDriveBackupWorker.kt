package com.example.tailorscounterpart.data.local

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.util.Date

@EntryPoint
@InstallIn(SingletonComponent::class)
interface GoogleDriveBackupWorkerDependencies {
    fun googleDriveBackupManager(): GoogleDriveBackupManager
    fun userPreferences(): UserPreferences
}

class GoogleDriveBackupWorker(
    appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        val dependencies = EntryPointAccessors.fromApplication(
            applicationContext,
            GoogleDriveBackupWorkerDependencies::class.java
        )
        val preferences = dependencies.userPreferences()
        if (!preferences.automaticBackupSettings.first().enabled) return Result.success()

        val account = GoogleSignIn.getLastSignedInAccount(applicationContext)
        if (account == null || !GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_FILE))) {
            preferences.recordAutomaticBackupError(
                "Automatic backup needs a connected Google Drive account. Reconnect in Settings."
            )
            return Result.failure()
        }

        return try {
            dependencies.googleDriveBackupManager().createBackup(account) {}
            preferences.recordAutomaticBackupSuccess(Date().time)
            Result.success()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            preferences.recordAutomaticBackupError(
                error.message ?: "Automatic Google Drive backup failed."
            )
            Result.retry()
        }
    }
}
