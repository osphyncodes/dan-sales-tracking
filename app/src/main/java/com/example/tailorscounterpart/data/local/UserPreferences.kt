package com.example.tailorscounterpart.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userPreferencesDataStore by preferencesDataStore("user_preferences")

data class AutomaticBackupSettings(
    val enabled: Boolean = false,
    val frequency: String = "daily",
    val lastSuccessAt: Long? = null,
    val lastError: String? = null
)

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val currencyKey = stringPreferencesKey("currency_symbol")
    private val backupFolderKey = stringPreferencesKey("backup_folder_uri")
    private val automaticBackupEnabledKey = booleanPreferencesKey("automatic_drive_backup_enabled")
    private val automaticBackupFrequencyKey = stringPreferencesKey("automatic_drive_backup_frequency")
    private val lastAutomaticBackupSuccessKey = longPreferencesKey("automatic_drive_backup_last_success")
    private val lastAutomaticBackupErrorKey = stringPreferencesKey("automatic_drive_backup_last_error")

    val currency: Flow<String> = context.userPreferencesDataStore.data.map {
        it[currencyKey] ?: "MK"
    }

    val backupFolderUri: Flow<String?> = context.userPreferencesDataStore.data.map {
        it[backupFolderKey]
    }

    val automaticBackupSettings: Flow<AutomaticBackupSettings> =
        context.userPreferencesDataStore.data.map { preferences ->
            AutomaticBackupSettings(
                enabled = preferences[automaticBackupEnabledKey] ?: false,
                frequency = preferences[automaticBackupFrequencyKey] ?: "daily",
                lastSuccessAt = preferences[lastAutomaticBackupSuccessKey],
                lastError = preferences[lastAutomaticBackupErrorKey]
            )
        }

    suspend fun setCurrency(currency: String) {
        context.userPreferencesDataStore.edit { it[currencyKey] = currency }
    }

    suspend fun setBackupFolderUri(uri: String) {
        context.userPreferencesDataStore.edit { it[backupFolderKey] = uri }
    }

    suspend fun configureAutomaticBackup(enabled: Boolean, frequency: String) {
        require(frequency in setOf("daily", "weekly", "monthly"))
        context.userPreferencesDataStore.edit {
            it[automaticBackupEnabledKey] = enabled
            it[automaticBackupFrequencyKey] = frequency
        }
    }

    suspend fun recordAutomaticBackupSuccess(timestamp: Long) {
        context.userPreferencesDataStore.edit {
            it[lastAutomaticBackupSuccessKey] = timestamp
            it.remove(lastAutomaticBackupErrorKey)
        }
    }

    suspend fun recordAutomaticBackupError(message: String) {
        context.userPreferencesDataStore.edit {
            it[lastAutomaticBackupErrorKey] = message
        }
    }
}
