package com.example.loantracker.ui.screens.customer

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.loantracker.data.local.entities.ProjectEntity
import com.example.loantracker.ui.components.DateInputField
import com.example.loantracker.viewmodel.ProjectViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun CustomerAttemptDialog(
    project: ProjectEntity? = null,
    customerId: Long,
    onDismis: () -> Unit,
    onSave: (Boolean) -> Unit,
    viewModel: ProjectViewModel,
    currency: String
) {
    var dateCreated by remember(project?.id) {
        mutableStateOf(project?.dateCreated?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: LocalDate.now().toString())
    }
    var name by remember(project?.id) { mutableStateOf(project?.name.orEmpty()) }
    var amount by remember(project?.id) {
        mutableStateOf(project?.price?.takeIf { it > 0.0 }?.toString().orEmpty())
    }
    var dueDate by remember(project?.id) {
        mutableStateOf(project?.dueDate?.format(DateTimeFormatter.ISO_LOCAL_DATE).orEmpty())
    }
    var isSaving by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismis() },
        title = { Text(if (project == null) "Add loan" else "Edit loan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DateInputField(
                    label = "Date given *",
                    selectedDate = dateCreated,
                    onDateSelected = { dateCreated = it },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Clothing/item description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { value ->
                        amount = value.filter { it.isDigit() || it == '.' || it == ',' }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    label = { Text("Amount given *") },
                    prefix = { Text("$currency ") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                DateInputField(
                    label = "Expected repayment (optional)",
                    selectedDate = dueDate,
                    onDateSelected = { dueDate = it },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("* Required", style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving,
                onClick = {
                    if (dateCreated.isBlank() || amount.isBlank()) {
                        Toast.makeText(context, "Amount given and date are required.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val parsedAmount = amount.replace(",", ".").toDoubleOrNull()
                    if (parsedAmount == null ||
                        !parsedAmount.isFinite() || parsedAmount <= 0.0
                    ) {
                        Toast.makeText(context, "Enter a valid amount greater than zero.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    try {
                        val created = LocalDate.parse(dateCreated)
                        val due = dueDate.trim().takeIf(String::isNotEmpty)?.let(LocalDate::parse)
                        if (created.isAfter(LocalDate.now())) {
                            Toast.makeText(context, "Date cannot be in the future.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (due != null && due.isBefore(created)) {
                            Toast.makeText(context, "Expected repayment cannot be before the loan date.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSaving = true
                        scope.launch {
                            try {
                                viewModel.saveProject(
                                    existingProject = project,
                                    customerId = customerId,
                                    name = name.trim().ifEmpty { "Loan" },
                                    dateCreated = created.atStartOfDay(),
                                    price = parsedAmount,
                                    dueDate = due?.atStartOfDay()
                                )
                                onSave(true)
                            } catch (error: CancellationException) {
                                throw error
                            } catch (error: Exception) {
                                Toast.makeText(
                                    context,
                                    error.message ?: "Unable to save loan.",
                                    Toast.LENGTH_LONG
                                ).show()
                            } finally {
                                isSaving = false
                            }
                        }
                    } catch (error: Exception) {
                        Toast.makeText(context, "Enter valid dates.", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Text(if (isSaving) "Saving..." else "Save loan")
            }
        },
        dismissButton = {
            OutlinedButton(enabled = !isSaving, onClick = onDismis) {
                Text("Cancel")
            }
        }
    )
}
