package com.example.tailorscounterpart.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun Keypad(
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    showBiometric: Boolean? = true,
    onBiometric: () -> Unit
) {

    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9")

    Column(Modifier.fillMaxWidth()) {

        keys.chunked(3).forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                row.forEach {
                    KeyButton(it) { onDigit(it) }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            // Left slot: Biometric OR Empty Box
            Box(
                Modifier.size(64.dp).padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (showBiometric == true) {
                    IconButton(onClick = onBiometric) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Biometric",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Middle slot: Zero
            KeyButton("0") { onDigit("0") }

            // Right slot: Delete
            Box(
                Modifier.size(64.dp).padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Backspace,
                        contentDescription = "Delete",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun KeyButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .size(64.dp),
        shape = androidx.compose.foundation.shape.CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge
        )
    }
}
