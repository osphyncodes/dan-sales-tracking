package com.example.loantracker.ui.screens.settings

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.loantracker.data.local.DriveBackupFile
import com.example.loantracker.viewmodel.BackupViewModel
import com.example.loantracker.viewmodel.auth.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import java.text.DateFormat
import java.util.Date

private fun restartApplication(context: android.content.Context) {
    val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        ?: error("Unable to find the app launch activity.")
    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    val flags = PendingIntent.FLAG_CANCEL_CURRENT or
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
    val restart = PendingIntent.getActivity(context, 0, launchIntent, flags)
    val alarmManager = context.getSystemService(AlarmManager::class.java)
        ?: error("Unable to schedule the app restart.")
    val restartAt = SystemClock.elapsedRealtime() + 1_500L
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME, restartAt, restart)
    } else {
        alarmManager.set(AlarmManager.ELAPSED_REALTIME, restartAt, restart)
    }
    (context as? Activity)?.finishAffinity()
    Handler(Looper.getMainLooper()).postDelayed(
        { Process.killProcess(Process.myPid()) },
        600L
    )
}

private fun handleGoogleSignInResult(
    result: ActivityResult,
    onSuccess: (GoogleSignInAccount) -> Unit,
    onFailure: (Exception) -> Unit
) {
    if (result.resultCode != Activity.RESULT_OK) {
        onFailure(IllegalStateException("Google Drive sign-in was cancelled."))
        return
    }
    try {
        val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            .getResult(ApiException::class.java)
        onSuccess(account)
    } catch (error: ApiException) {
        onFailure(error)
    }
}

@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    paddingValues: PaddingValues,
    currency: String,
    onCurrencyChange: (String) -> Unit
) {
    val context = LocalContext.current
    val backupViewModel: BackupViewModel = hiltViewModel()
    val snackbar = remember { SnackbarHostState() }
    val isBusy by backupViewModel.isBusy.collectAsStateWithLifecycle()
    val backupProgress by backupViewModel.progress.collectAsStateWithLifecycle()
    val driveBackups by backupViewModel.driveBackups.collectAsStateWithLifecycle()
    val automaticBackupSettings by backupViewModel.automaticBackupSettings.collectAsStateWithLifecycle()
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var currencyText by remember(currency) { mutableStateOf(currency) }
    var recoveryQuestion1 by rememberSaveable { mutableStateOf("") }
    var recoveryAnswer1 by rememberSaveable { mutableStateOf("") }
    var recoveryQuestion2 by rememberSaveable { mutableStateOf("") }
    var recoveryAnswer2 by rememberSaveable { mutableStateOf("") }
    var selectedRestore by remember { mutableStateOf<DriveBackupFile?>(null) }
    var showRestoreConfirmation by rememberSaveable { mutableStateOf(false) }
    var showBackupPicker by rememberSaveable { mutableStateOf(false) }
    val driveScope = remember { Scope(DriveScopes.DRIVE_FILE) }
    val signInOptions = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(driveScope)
            .build()
    }
    val googleSignInClient = remember(signInOptions) {
        GoogleSignIn.getClient(context, signInOptions)
    }
    var driveAccount by remember {
        mutableStateOf(
            GoogleSignIn.getLastSignedInAccount(context)?.takeIf {
                GoogleSignIn.hasPermissions(it, driveScope)
            }
        )
    }
    val googleSignInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleGoogleSignInResult(
            result = result,
            onSuccess = { account ->
                driveAccount = account
                backupViewModel.loadGoogleDriveBackups(account)
            },
            onFailure = { error ->
                backupViewModel.showMessage(error.message ?: "Google Drive sign-in failed.")
            }
        )
    }

    LaunchedEffect(Unit) {
        backupViewModel.message.collect { message ->
            if (message != null) {
                statusMessage = message
                snackbar.showSnackbar(message)
                backupViewModel.clearMessage()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().padding(paddingValues),
        snackbarHost = { SnackbarHost(snackbar) }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
            Text("Manage security, currency, and portable backups.", style = MaterialTheme.typography.bodyMedium)

            Card(modifier = Modifier.fillMaxWidth(), onClick = authViewModel::changePin) {
                ListItem(
                    headlineContent = { Text("Change PIN") },
                    supportingContent = { Text("Create a new four-digit app PIN.") }
                )
            }

            Text("PIN recovery questions", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = recoveryQuestion1,
                onValueChange = { recoveryQuestion1 = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Question 1") },
                singleLine = true
            )
            OutlinedTextField(
                value = recoveryAnswer1,
                onValueChange = { recoveryAnswer1 = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Answer 1") },
                singleLine = true
            )
            OutlinedTextField(
                value = recoveryQuestion2,
                onValueChange = { recoveryQuestion2 = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Question 2") },
                singleLine = true
            )
            OutlinedTextField(
                value = recoveryAnswer2,
                onValueChange = { recoveryAnswer2 = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Answer 2") },
                singleLine = true
            )
            Button(
                onClick = {
                    authViewModel.saveRecovery(
                        recoveryQuestion1.trim(),
                        recoveryAnswer1.trim(),
                        recoveryQuestion2.trim(),
                        recoveryAnswer2.trim()
                    )
                    backupViewModel.showMessage("PIN recovery questions saved.")
                },
                enabled = recoveryQuestion1.isNotBlank() && recoveryAnswer1.isNotBlank() &&
                    recoveryQuestion2.isNotBlank() && recoveryAnswer2.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save recovery questions") }

            Text("Working currency", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = currencyText,
                onValueChange = {
                    currencyText = it.take(5)
                    if (currencyText.isNotBlank()) onCurrencyChange(currencyText.trim())
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Currency symbol or code") },
                supportingText = { Text("Used for prices, payments, and dashboard totals.") },
                singleLine = true
            )

            Text("Backups", style = MaterialTheme.typography.titleMedium)
            Text(
                "Google Drive backups are encrypted using the connected account's email address. Use the same Google account to restore.",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = {
                    if (driveAccount == null) {
                        authViewModel.onExternalActivityLaunch()
                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                    } else {
                        googleSignInClient.signOut().addOnCompleteListener {
                            driveAccount = null
                        }
                    }
                },
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(driveAccount?.let { "Disconnect ${it.email ?: "Google Drive"}" } ?: "Connect Google Drive")
            }
            driveAccount?.let { account ->
                Text(
                    "Connected: ${account.email ?: "Google account"} • Backups are stored in Drive/LoanTracker/backup.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Automatic Google Drive backup", style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (driveAccount == null) "Connect Google Drive to enable scheduled backups."
                                else "Runs while the device has network access.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked = automaticBackupSettings.enabled,
                            enabled = (driveAccount != null || automaticBackupSettings.enabled) && !isBusy,
                            onCheckedChange = {
                                backupViewModel.configureAutomaticBackup(
                                    it,
                                    automaticBackupSettings.frequency
                                )
                            }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("daily", "weekly", "monthly").forEach { frequency ->
                            FilterChip(
                                selected = automaticBackupSettings.frequency == frequency,
                                enabled = driveAccount != null && !isBusy,
                                onClick = {
                                    backupViewModel.configureAutomaticBackup(
                                        automaticBackupSettings.enabled,
                                        frequency
                                    )
                                },
                                label = { Text(frequency.replaceFirstChar(Char::uppercase)) }
                            )
                        }
                    }
                    automaticBackupSettings.lastSuccessAt?.let { time ->
                        Text(
                            "Last automatic backup: ${DateFormat.getDateTimeInstance().format(Date(time))}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    automaticBackupSettings.lastError?.let { error ->
                        Text(
                            "Last automatic backup issue: $error",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        "The email is the encryption key and is not a strong secret. Anyone with access to the Drive account and backup may be able to restore the data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        "Scheduled backups run approximately daily, every 7 days, or every 30 days when Android permits background work.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Button(
                onClick = {
                    val account = driveAccount
                    if (account == null) {
                        authViewModel.onExternalActivityLaunch()
                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                    } else {
                        statusMessage = null
                        backupViewModel.createGoogleDriveBackup(account)
                    }
                },
                enabled = !isBusy && driveAccount != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isBusy) "Backing up to Drive..." else "Back up to Google Drive")
            }
            Button(
                onClick = {
                    val account = driveAccount
                    if (account == null) {
                        authViewModel.onExternalActivityLaunch()
                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                    } else {
                        statusMessage = null
                        showBackupPicker = true
                        backupViewModel.loadGoogleDriveBackups(account)
                    }
                },
                enabled = !isBusy && driveAccount != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Restore latest Google Drive backup")
            }
            if (selectedRestore != null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text("Selected backup") },
                        supportingContent = { Text(selectedRestore?.name ?: "Backup selected") },
                        trailingContent = {
                            TextButton(onClick = { selectedRestore = null }) { Text("Clear") }
                        }
                    )
                }
            }
            backupProgress?.let { progress ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            progress.label,
                            style = MaterialTheme.typography.titleSmall
                        )
                        val fraction = progress.fraction
                        if (fraction == null) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        } else {
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                "${(fraction * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            statusMessage?.let { message ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(message, color = MaterialTheme.colorScheme.primary)
                        if (message.startsWith("Backup restored.")) {
                            Button(
                                onClick = {
                                    runCatching { restartApplication(context) }
                                        .onFailure {
                                            statusMessage =
                                                "Restart failed. Close and reopen the app to use the restored data."
                                        }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Restart app now") }
                        }
                    }
                }
            }
            Text(
                "The app stores backups in Drive/LoanTracker/backup. The latest backup is offered for restore. Existing backups in the former TailorCounterpart folder remain available.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showBackupPicker) {
        AlertDialog(
            onDismissRequest = { showBackupPicker = false },
            title = { Text("Google Drive backups") },
            text = {
                if (driveBackups.isEmpty()) {
                    Text(
                        if (isBusy) "Loading Drive backups..." else
                            "No backup found in Google Drive/LoanTracker/backup or its former TailorCounterpart folder."
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        driveBackups.forEach { backup ->
                            TextButton(
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isBusy,
                                onClick = {
                                    selectedRestore = backup
                                    showBackupPicker = false
                                    showRestoreConfirmation = true
                                }
                            ) {
                                Text(
                                    "Latest backup: ${backup.name}",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBackupPicker = false }) { Text("Close") }
            }
        )
    }

    if (showRestoreConfirmation && selectedRestore != null) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmation = false },
            title = { Text("Replace local data?") },
            text = {
                Text(
                    "Restore the latest backup (${selectedRestore?.name ?: "backup"})? This replaces all current customer, loan, payment, and note data."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isBusy,
                    onClick = {
                        val account = driveAccount
                        val backup = selectedRestore
                        if (account != null && backup != null) {
                            backupViewModel.restoreGoogleDriveBackup(account, backup)
                        }
                        showRestoreConfirmation = false
                    }
                ) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmation = false }) { Text("Cancel") }
            }
        )
    }
}
