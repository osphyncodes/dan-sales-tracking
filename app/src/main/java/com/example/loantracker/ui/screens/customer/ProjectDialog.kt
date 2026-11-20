package com.example.loantracker.ui.screens.customer

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.loantracker.data.local.entities.ProjectEntity
import com.example.loantracker.ui.components.DateInputField
import com.example.loantracker.ui.components.SelectDropdown
import com.example.loantracker.viewmodel.ProjectViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun CustomerAttemptDialog(
    project: ProjectEntity? = null,
    customerId: Long = 0,
    onDismis: () -> Unit,
    onSave: (Boolean) -> Unit,
    viewModel: ProjectViewModel,
    type: String,
    currency: String
) {
    var dateCreated by remember { mutableStateOf(project?.dateCreated?.format(
        DateTimeFormatter.ofPattern("yyyy-MM-dd")
    ) ?: "") }
    var name by remember { mutableStateOf(project?.name ?: "") }
    var price by remember { mutableStateOf(project?.price?.toString() ?: "") }
    var dueDate by remember { mutableStateOf(project?.dueDate?.format(
        DateTimeFormatter.ofPattern("yyyy-MM-dd")
    ) ?: "") }
    var saved by remember { mutableStateOf(false) }

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismis,
        title = {
            Text(
                text = if (project == null) {
                    "Add Project"
                } else {
                    "Edit Project"
                }
            )
        },
        text = {
            Column {
                DateInputField(
                    label = "Date of Creation",
                    selectedDate = dateCreated,
                    onDateSelected = {
                        dateCreated = it
                    }
                )
                
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                    },
                    label = {
                        Text(text = "Project Name")
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = price.toString(),
                    onValueChange = {
                        price = it
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    label = {
                        Text(text = "Price")
                    },
                    prefix = {
                        Text(text = currency)
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                DateInputField(
                    label = "Due Date",
                    selectedDate = dueDate,
                    onDateSelected = {
                        dueDate = it
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {

                    //validation
                    if(
                        dateCreated.trim().isEmpty() ||
                        name.trim().isEmpty() ||
                        price.trim().isEmpty() ||
                        dueDate.trim().isEmpty()
                    ){
                        Toast.makeText(
                            context, "All fields are required ${dateCreated}, ${type}", Toast.LENGTH_SHORT
                        ).show()

                        return@Button
                    }

                    try {
                        val parsedPrice = price.toDoubleOrNull()
                        if (parsedPrice == null || parsedPrice < 0) {
                            Toast.makeText(context, "Enter a valid non-negative price", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val selectedLocalDate = LocalDate.parse(dateCreated.trim())
                        val selectedDueDate = LocalDate.parse(dueDate.trim())

                        if (selectedLocalDate.isAfter(LocalDate.now())) {
                            Toast.makeText(context, "Date cannot be in the future", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        // validating due date less than Creation date
                        if (selectedLocalDate.isAfter(selectedDueDate)){
                            Toast.makeText(
                                context,
                                "Due date cannot be before the creation date",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }

                        //vadidating price less than 0
                        if (project == null) {
                            viewModel.insertProject(
                                dateCreated = LocalDateTime.of(selectedLocalDate, LocalTime.MIDNIGHT),
                                name = name.trim(),
                                customerId = customerId,
                                price = parsedPrice,
                                dueDate = LocalDateTime.of(selectedDueDate, LocalTime.MIDNIGHT)
                            )
                        } else {
                            project.let {
                                viewModel.updateProject(it.copy(
                                    dateCreated = LocalDateTime.of(selectedLocalDate, LocalTime.MIDNIGHT),
                                    name = name.trim(),
                                    customerId = customerId,
                                    price = parsedPrice,
                                    dueDate = LocalDateTime.of(selectedDueDate, LocalTime.MIDNIGHT)
                                ))
                            }
                        }

                        saved = !saved
                    } catch (e: Exception) {
                        Toast.makeText(context, "Invalid date format", Toast.LENGTH_SHORT).show()
                    }

                    onSave(
                        saved
                    )
                }
            ) {
                Text(text = "Save")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismis
            ) {
                Text(text = "Cancel")
            }
        }
    )
}

@Composable
@Preview(showSystemUi = true, showBackground = true)
fun PersonsdFormPreview(){
//    TracingAttemptDialog(
//        ta = TracingAttemptEntity(
//            dateAttempted = LocalDateTime.parse("2024-03-01"),
//            tracingId = 1,
//            type = "Phone",
//            outcome = "Found"
//        ),
//        onDismis = {},
//        onSave = {a, b ->
//
//        }
//    )
}