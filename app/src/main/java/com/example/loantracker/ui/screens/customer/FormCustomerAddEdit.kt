package com.example.loantracker.ui.screens.customer

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.provider.ContactsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.loantracker.ui.components.DateInputField
import com.example.loantracker.viewmodel.CustomerViewModel
import com.example.loantracker.viewmodel.ValViewModel
import com.example.loantracker.viewmodel.auth.AuthViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * A composable screen that provides a form for adding a new customer or editing an existing one.
 *
 * This screen handles:
 * - Fetching customer data if editing (based on [id]).
 * - Picking a phone number from the device's contacts list.
 * - Input validation for name, age, gender, and date.
 * - Saving the customer to the database via the [viewModel].
 *
 * @param id The ID of the customer to edit, or "0" if adding a new customer.
 * @param viewModel The [CustomerViewModel] to handle business logic and data operations.
 * @param paddingValues The padding values provided by the Scaffold.
 * @param navController The [NavHostController] used for navigation.
 * @param v The [ValViewModel] used for managing top bar state, like the title.
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun FormCustomerAddEdit(
    id: String,
    viewModel: CustomerViewModel,
    paddingValues: PaddingValues,
    navController: NavHostController,
    v: ValViewModel,
    authViewModel: AuthViewModel
){
    val tracing by viewModel.getCustomerById(id = id.toLong()).collectAsState(initial = null)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val villages by viewModel.villages.collectAsState(initial = emptyList())
    val clusters by viewModel.clusters.collectAsState(initial = emptyList())
    var phoneNumber by rememberSaveable { mutableStateOf("") }

    val contactPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->

        if (result.resultCode == Activity.RESULT_OK) {

            val uri = result.data?.data

            if (uri != null) {
                try {
                    val phone = context.contentResolver.query(
                        uri,
                        arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                        null,
                        null,
                        null
                    )?.use { cursor ->

                        val numberIndex = cursor.getColumnIndex(
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                        )

                        if (cursor.moveToFirst() && numberIndex >= 0) {
                            cursor.getString(numberIndex)
                        } else null
                    }

                    if (!phone.isNullOrBlank()) {
                        phoneNumber = formatPhoneInput(phone)
                    } else {
                        Toast.makeText(
                            context,
                            "The selected contact has no phone number.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                } catch (error: Exception) {
                    Log.e("ContactPicker", "Error reading contact", error)

                    Toast.makeText(
                        context,
                        "Unable to read the selected phone number.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var name by rememberSaveable { mutableStateOf("") }
    var age by rememberSaveable { mutableStateOf("") }
    var villageId by rememberSaveable { mutableLongStateOf(0L) }
    var clusterId by rememberSaveable { mutableLongStateOf(0L) }
    var villageName by rememberSaveable { mutableStateOf("") }
    var clusterName by rememberSaveable { mutableStateOf("") }
    var enteringVillage by rememberSaveable { mutableStateOf(false) }
    var enteringCluster by rememberSaveable { mutableStateOf(false) }
    var isSaving by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val villageFocusRequester = remember { FocusRequester() }
    val clusterFocusRequester = remember { FocusRequester() }

    LaunchedEffect(tracing) {
        if (id == "0") {
            v.setTitle("Add Customer")
        }
        tracing?.let {
            if (id != "0") {
                v.setTitle("Edit Customer: ${it.name}")
            }
            date = it.dateCreated.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            name = it.name
            age = it.age.toString()
            phoneNumber = it.phoneNumber.orEmpty()
            villageId = it.villageId
            clusterId = it.clusterId
        }
    }

    LaunchedEffect(tracing?.villageId, villages, villageId) {
        tracing?.let { customer ->
            if (villageName.isBlank() && villageId == customer.villageId) {
                villageName = villages.firstOrNull { it.id.toLong() == customer.villageId }?.name.orEmpty()
            }
        }
    }

    LaunchedEffect(tracing?.clusterId, clusters, clusterId) {
        tracing?.let { customer ->
            if (clusterName.isBlank() && clusterId == customer.clusterId) {
                clusterName = clusters.firstOrNull { it.id.toLong() == customer.clusterId }?.name.orEmpty()
            }
        }
    }

    LaunchedEffect(enteringVillage) {
        if (enteringVillage) villageFocusRequester.requestFocus()
    }

    LaunchedEffect(enteringCluster) {
        if (enteringCluster) clusterFocusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ){
            DateInputField(
                label = "Customer added *",
                selectedDate = date,
                onDateSelected = { date = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )

            Spacer(modifier = Modifier.height(12.dp))

            CustomTextField(
                value = name,
                onValueChange = { name = it },
                label = "Name *",
                imeAction = ImeAction.Next
            )
            Spacer(modifier = Modifier.height(12.dp))

            CustomTextField(
                value = age,
                onValueChange = { age = it },
                label = "Age (optional)",
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LocationDropdown(
                    label = "Village",
                    value = villageName,
                    options = villages.map { LocationOption(it.id.toLong(), it.name) },
                    enteringNewLocation = enteringVillage,
                    focusRequester = villageFocusRequester,
                    modifier = Modifier.weight(1f),
                    onValueChange = {
                        villageName = it
                        villageId = 0L
                    },
                    onLocationSelected = { location ->
                        villageId = location.id
                        villageName = location.name
                    }
                )
                IconButton(
                    onClick = {
                        enteringVillage = !enteringVillage
                        villageId = 0L
                        if (enteringVillage) villageName = ""
                    }
                ) {
                    Icon(
                        imageVector = if (enteringVillage) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = if (enteringVillage) "Choose existing village" else "Add village"
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LocationDropdown(
                    label = "Cluster",
                    value = clusterName,
                    options = clusters.map { LocationOption(it.id.toLong(), it.name) },
                    enteringNewLocation = enteringCluster,
                    focusRequester = clusterFocusRequester,
                    modifier = Modifier.weight(1f),
                    onValueChange = {
                        clusterName = it
                        clusterId = 0L
                    },
                    onLocationSelected = { location ->
                        clusterId = location.id
                        clusterName = location.name
                    }
                )
                IconButton(
                    onClick = {
                        enteringCluster = !enteringCluster
                        clusterId = 0L
                        if (enteringCluster) clusterName = ""
                    }
                ) {
                    Icon(
                        imageVector = if (enteringCluster) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = if (enteringCluster) "Choose existing cluster" else "Add cluster"
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Phone number (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                trailingIcon = {
                    IconButton(onClick = {
                        try {
                            authViewModel.onExternalActivityLaunch()
                            contactPicker.launch(
                                Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                            )
                        } catch (_: ActivityNotFoundException) {
                            authViewModel.cancelExternalActivityLaunch()
                            Toast.makeText(context, "No contacts app is available.", Toast.LENGTH_SHORT).show()
                        } catch (_: SecurityException) {
                            authViewModel.cancelExternalActivityLaunch()
                            Toast.makeText(context, "Unable to open the contacts app.", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.ContactPhone, contentDescription = "Choose phone number from contacts")
                    }
                }
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        if (isSaving) return@Button
                        val trimmedDate = date.trim()
                        val trimmedName = name.trim()
                        val trimmedAge = age.trim()
                        val trimmedPhoneNumber = normalizePhoneNumber(phoneNumber)

                        // Validation
                        if (trimmedDate.isEmpty() || trimmedName.isEmpty()) {
                            Toast.makeText(context, "Date and customer name are required.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val newVillageName = villageName.trim().takeIf { enteringVillage }
                        val newClusterName = clusterName.trim().takeIf { enteringCluster }
                        if ((villageId <= 0 && newVillageName.isNullOrEmpty()) ||
                            (clusterId <= 0 && newClusterName.isNullOrEmpty())
                        ) {
                            Toast.makeText(context, "Please select or add a village and cluster", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (id != "0" && tracing == null) {
                            Toast.makeText(context, "Customer details are still loading.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        //validate phoneNumber if set. should be a valid phone number
                        if (trimmedPhoneNumber.isNotEmpty() && !trimmedPhoneNumber.matches(Regex("^\\+[1-9][0-9]{9,14}\$"))) {
                            Toast.makeText(context, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
                            return@Button
                        }


                        val ageInt = trimmedAge.takeIf(String::isNotEmpty)?.toIntOrNull()
                        if (trimmedAge.isNotEmpty() && (ageInt == null || ageInt !in 0..99)) {
                            Toast.makeText(context, "Age must be between 0 and 99", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        try {
                            val selectedLocalDate = LocalDate.parse(trimmedDate)
                            if (selectedLocalDate.isAfter(LocalDate.now())) {
                                Toast.makeText(context, "Date cannot be in the future", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            isSaving = true
                            coroutineScope.launch {
                                try {
                                    viewModel.saveCustomer(
                                        existingCustomer = if (id == "0") null else tracing,
                                        dateCreated = LocalDateTime.of(selectedLocalDate, LocalTime.MIDNIGHT),
                                        name = trimmedName,
                                        age = trimmedAge,
                                        phoneNumber = trimmedPhoneNumber,
                                        villageId = villageId.takeIf { it > 0 },
                                        villageName = newVillageName,
                                        clusterId = clusterId.takeIf { it > 0 },
                                        clusterName = newClusterName,
                                    )
                                    isSaving = false
                                    navController.popBackStack()
                                } catch (error: CancellationException) {
                                    throw error
                                } catch (error: Exception) {
                                    isSaving = false
                                    Toast.makeText(
                                        context,
                                        "Unable to save customer: ${error.message ?: "Please try again."}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Invalid date format", Toast.LENGTH_SHORT).show()
                        }
                    },
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    enabled = !isSaving
                ) {
                    Text(text = "Save")
                }
                Spacer(modifier = Modifier.width(12.dp))
                OutlinedButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(text = "Cancel")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationDropdown(
    label: String,
    value: String,
    options: List<LocationOption>,
    enteringNewLocation: Boolean,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
    onLocationSelected: (LocationOption) -> Unit
) {
    var expanded by remember(label) { mutableStateOf(false) }

    if (enteringNewLocation) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = modifier
                .focusRequester(focusRequester)
                .fillMaxWidth(),
            singleLine = true,
        )
    } else {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = modifier
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = {
                    onValueChange(it)
                    expanded = true
                },
                label = { Text(label) },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                singleLine = true,
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.filter { it.name.contains(value, ignoreCase = true) }
                    .forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.name) },
                            onClick = {
                                onLocationSelected(option)
                                expanded = false
                            }
                        )
                    }
            }
        }
    }
}

private data class LocationOption(val id: Long, val name: String)

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next
){
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        singleLine = true,
    )
}

private fun formatPhoneInput(input: String): String {
    val digits = input.filter(Char::isDigit).take(15)
    if (digits.isEmpty()) return ""
    val international = input.trimStart().startsWith("+") || digits.startsWith("265") && digits.length > 10
    val national = digits.startsWith("0")
    val prefix = when {
        international && digits.startsWith("265") -> "+265 "
        international -> "+"
        else -> ""
    }
    val body = if (international && digits.startsWith("265")) digits.drop(3) else digits
    val groups = when {
        national -> listOf(4, 3, 3)
        international && digits.startsWith("265") -> listOf(3, 3, 3, 3)
        else -> listOf(3, 3, 3, 3, 3)
    }
    var remaining = body
    val formatted = mutableListOf<String>()
    for (size in groups) {
        if (remaining.isEmpty()) break
        formatted += remaining.take(size)
        remaining = remaining.drop(size)
    }
    if (remaining.isNotEmpty()) formatted += remaining
    return prefix + formatted.joinToString(" ")
}

private fun normalizePhoneNumber(input: String): String {
    val trimmed = input.trim()
    val digits = trimmed.filter(Char::isDigit)
    return when {
        digits.length == 10 && digits.startsWith("0") -> "+265${digits.drop(1)}"
        digits.length == 12 && digits.startsWith("265") -> "+$digits"
        trimmed.startsWith("+") -> "+$digits"
        else -> digits
    }
}
