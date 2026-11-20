package com.example.tailorscounterpart.ui.screens.auth
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tailorscounterpart.R
import com.example.tailorscounterpart.ui.components.Keypad
import com.example.tailorscounterpart.viewmodel.auth.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun LockScreen(
    vm: AuthViewModel,
    showBiometric: Boolean,
    onBiometric: () -> Unit
) {

    val context = LocalContext.current
//    val hasBiometric = remember { isBiometricAvailable(context) }
    var showRecoveryDialog by remember { mutableStateOf(false) }

    var recoveryQ1 by remember { mutableStateOf("") }
    var recoveryQ2 by remember { mutableStateOf("") }
    var recoveryA1 by remember { mutableStateOf("") }
    var recoveryA2 by remember { mutableStateOf("") }

    LaunchedEffect(showRecoveryDialog) {
        if (showRecoveryDialog) {
            val data = vm.getRecoveryData()
            recoveryQ1 = data["q1"] ?: "Question 1 not set"
            recoveryQ2 = data["q2"] ?: "Question 2 not set"
        }
    }
    LaunchedEffect(vm.mode) {
        if (showRecoveryDialog && vm.mode == "CREATE") {
            showRecoveryDialog = false
        }
    }

//    LaunchedEffect(vm.mode) {
//        if (vm.mode == "VERIFY" && hasBiometric) {
//            showBiometric(context) {
//                vm.unlock()
//            }
//        }
//    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo Section
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(50.dp),
                tint = MaterialTheme.colorScheme.primary
            )


        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Tailor's Counterpart",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(8.dp))

        Text(text = "Developed by: Josephy H. Ng'omba")
        Text(text = "(Osphyncodes Digital Solutions)", fontSize = 12.sp)
        Text(text = "(ngombajosephy@gmail.com/0888078808)", fontSize = 14.sp)

        Spacer(Modifier.height(8.dp))

        Text(
            text = when (vm.mode) {
                "CREATE" -> "Create your PIN"
                "CONFIRM" -> "Confirm your PIN"
                "VERIFY" -> "Enter your PIN"
                else -> "Secure Login"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        PinDots(vm.enteredPin.length)

        Box(Modifier.height(24.dp)) {
            vm.error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Keypad(
            onDigit = vm::onDigit,
            onDelete = vm::delete,
            showBiometric = showBiometric,
            onBiometric = onBiometric
        )

        if (vm.isChangingPin) {
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = { vm.cancelChangePin() }) {
                Text("Cancel")
            }
        }

        if (vm.mode == "VERIFY") {
            TextButton(onClick = { showRecoveryDialog = true }) {
                Text("Forgot PIN?")
            }
        }
    }

    if (showRecoveryDialog) {
        AlertDialog(
            onDismissRequest = { showRecoveryDialog = false },
            title = { Text("Recover PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(recoveryQ1, style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(value = recoveryA1, onValueChange = { recoveryA1 = it }, label = { Text("Answer 1") })
                    Spacer(Modifier.height(8.dp))
                    Text(recoveryQ2, style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(value = recoveryA2, onValueChange = { recoveryA2 = it }, label = { Text("Answer 2") })
                    vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.resetPinWithRecovery(recoveryA1, recoveryA2)
                }) { Text("Reset PIN") }
            },
            dismissButton = {
                TextButton(onClick = { showRecoveryDialog = false }) { Text("Cancel") }
            }
        )
    }
}
