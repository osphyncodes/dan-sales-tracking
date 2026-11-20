package com.example.tailorscounterpart.ui.screens.customer

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tailorscounterpart.data.local.data_classes.ContactPhonebook
import com.example.tailorscounterpart.data.local.entities.ContactEntity
import com.example.tailorscounterpart.data.local.entities.CustomerEntity
import com.example.tailorscounterpart.data.local.entities.ProjectEntity
import com.example.tailorscounterpart.ui.components.DateInputField
import com.example.tailorscounterpart.ui.components.SelectDropdown
import com.example.tailorscounterpart.viewmodel.ProjectViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun ContactDialog(
    contact: ContactEntity? = null,
    customer: CustomerEntity? = null,
    index: Int? = 0,
    onDismis: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var type by remember { mutableStateOf(contact?.type ?: "") }
    var value by remember { mutableStateOf(contact?.value ?: "") }
    var saved by remember { mutableStateOf(false) }

    val typeOptions = listOf<String>(
        "Phone",
        "Email"
    )

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismis,
        title = {
            Text(
                text = "Add Contact"
            )
        },
        text = {
            Column {
                SelectDropdown(
                    label = "Type",
                    options = typeOptions,
                    selectedOption = type,
                    onOptionSelected = {
                        type = it
                    }
                )

                Spacer(modifier = Modifier.height(11.dp))

                OutlinedTextField(
                    value = value,
                    onValueChange = {
                        value = it
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {

                    //validation
                    if( type.isEmpty() || value.isEmpty()){
                        Toast.makeText(
                            context, "All fields are required", Toast.LENGTH_SHORT
                        ).show()

                        return@Button
                    }

                    saved = !saved

                    onSave(type, value)
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
