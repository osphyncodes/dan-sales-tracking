package com.example.loantracker.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.security.MessageDigest
import java.util.Objects.hash
import javax.inject.Inject
import javax.inject.Singleton

private val Context.securePreferencesDataStore by preferencesDataStore("secure_prefs")

@Singleton
class PinStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val PIN_KEY = stringPreferencesKey("pin_hash")
    private val QUESTION1_KEY = stringPreferencesKey("q1")
    private val ANSWER1_KEY = stringPreferencesKey("a1")
    private val QUESTION2_KEY = stringPreferencesKey("q2")
    private val ANSWER2_KEY = stringPreferencesKey("a2")

    suspend fun savePin(pin: String){
        context.securePreferencesDataStore.edit {
            it[PIN_KEY] = hash(pin)
        }
    }

    suspend fun saveRecovery(q1: String, a1: String, q2: String, a2: String) {
        context.securePreferencesDataStore.edit {
            it[QUESTION1_KEY] = q1
            it[ANSWER1_KEY] = hash(a1.lowercase().trim())
            it[QUESTION2_KEY] = q2
            it[ANSWER2_KEY] = hash(a2.lowercase().trim())
        }
    }

    suspend fun getRecoveryData(): Map<String, String?> {
        val prefs = context.securePreferencesDataStore.data.first()
        return mapOf(
            "q1" to prefs[QUESTION1_KEY],
            "a1" to prefs[ANSWER1_KEY],
            "q2" to prefs[QUESTION2_KEY],
            "a2" to prefs[ANSWER2_KEY]
        )
    }

    suspend fun getPin(): String?{
        return context.securePreferencesDataStore.data.first()[PIN_KEY]
    }

    fun verify(pin: String, savedHash: String?): Boolean {
        return savedHash == hash(pin)
    }

    private fun hash(input: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}