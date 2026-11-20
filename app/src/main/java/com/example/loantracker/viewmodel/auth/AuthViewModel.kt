package com.example.loantracker.viewmodel.auth

import com.example.loantracker.data.local.PinStore


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val store: PinStore,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    var enteredPin by mutableStateOf("")
    var isAuthenticated by mutableStateOf(
        savedStateHandle.get<Boolean>(HANDOFF_AUTHENTICATED_KEY) == true &&
            (savedStateHandle.get<Long>(HANDOFF_DEADLINE_KEY) ?: 0L) > System.currentTimeMillis()
    )
    var mode by mutableStateOf("LOADING") // LOADING | CREATE | CONFIRM | VERIFY | RECOVERY
    var isChangingPin by mutableStateOf(false)

    var recoveryQuestion1 by mutableStateOf("")
    var recoveryAnswer1 by mutableStateOf("")
    var recoveryQuestion2 by mutableStateOf("")
    var recoveryAnswer2 by mutableStateOf("")

    init {
        checkPinStatus()
    }

    private fun checkPinStatus() {
        viewModelScope.launch {
            val saved = store.getPin()
            if (saved == null) {
                mode = "CREATE"
            } else {
                mode = "VERIFY"
            }
        }
    }

    fun saveRecovery(q1: String, a1: String, q2: String, a2: String) {
        viewModelScope.launch {
            store.saveRecovery(q1, a1, q2, a2)
        }
    }

    suspend fun getRecoveryData() = store.getRecoveryData()

    fun resetPinWithRecovery(a1: String, a2: String) {
        viewModelScope.launch {
            val data = store.getRecoveryData()
            val savedA1 = data["a1"]
            val savedA2 = data["a2"]

            // Note: In PinStore we hash the answers in lowercase
            val inputHash1 = hash(a1.lowercase().trim())
            val inputHash2 = hash(a2.lowercase().trim())

            if (savedA1 == inputHash1 && savedA2 == inputHash2) {
                mode = "CREATE"
                isAuthenticated = false
                enteredPin = ""
                error = null
            } else {
                error = "Incorrect recovery answers"
            }
        }
    }

    private fun hash(input: String): String {
        return java.security.MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    fun changePin() {
        isChangingPin = true
        mode = "CREATE"
        enteredPin = ""
    }

    fun cancelChangePin() {
        isChangingPin = false
        mode = "VERIFY"
        enteredPin = ""
    }

    private var tempPin = ""

    private var lastBackgroundTime = 0L
    private val timeout = 60_000L

    var error by mutableStateOf<String?>(null)

    fun onDigit(d: String) {
        if (enteredPin.length < 4) {
            enteredPin += d
        }

        if (enteredPin.length == 4) handlePin()
    }

    fun delete() {
        enteredPin = enteredPin.dropLast(1)
    }

    private fun handlePin() {

        viewModelScope.launch {

            when (mode) {

                "CREATE" -> {
                    tempPin = enteredPin
                    enteredPin = ""
                    mode = "CONFIRM"
                }

                "CONFIRM" -> {

                    if (tempPin == enteredPin) {
                        store.savePin(enteredPin)
                        isAuthenticated = true
                        isChangingPin = false
                        mode = "VERIFY"
                    } else {
                        error = "PIN mismatch"
                        mode = "CREATE"
                    }

                    enteredPin = ""
                }

                "VERIFY" -> {

                    val saved = store.getPin()

                    if (store.verify(enteredPin, saved)) {
                        isAuthenticated = true
                        error = null
                    } else {
                        error = "Wrong PIN"
                        enteredPin = ""
                    }
                }
            }
        }
    }

    fun lock() {
        isAuthenticated = false
        enteredPin = ""
        error = null
    }

    fun onBackground() {
        lastBackgroundTime = System.currentTimeMillis()
    }

    fun onExternalActivityLaunch() {
        val now = System.currentTimeMillis()
        savedStateHandle[HANDOFF_AUTHENTICATED_KEY] = isAuthenticated
        savedStateHandle[HANDOFF_DEADLINE_KEY] = now + EXTERNAL_ACTIVITY_GRACE_MS
    }

    fun onResumeCheck() {
        val now = System.currentTimeMillis()
        val handoffDeadline = savedStateHandle.get<Long>(HANDOFF_DEADLINE_KEY) ?: 0L
        val authenticatedForHandoff =
            savedStateHandle.get<Boolean>(HANDOFF_AUTHENTICATED_KEY) == true
        if (authenticatedForHandoff && now < handoffDeadline) {
            isAuthenticated = true
            clearExternalActivityHandoff()
            lastBackgroundTime = 0L
            return
        }
        clearExternalActivityHandoff()

        val diff = now - lastBackgroundTime

        if (lastBackgroundTime != 0L && diff > timeout) {
            lock()
        }
        lastBackgroundTime = 0L
    }

    fun unlock() {
        isAuthenticated = true
    }

    private fun clearExternalActivityHandoff() {
        savedStateHandle.remove<Boolean>(HANDOFF_AUTHENTICATED_KEY)
        savedStateHandle.remove<Long>(HANDOFF_DEADLINE_KEY)
    }

    private companion object {
        const val HANDOFF_AUTHENTICATED_KEY = "external_activity_authenticated"
        const val HANDOFF_DEADLINE_KEY = "external_activity_deadline"
        const val EXTERNAL_ACTIVITY_GRACE_MS = 5 * 60 * 1000L
    }
}