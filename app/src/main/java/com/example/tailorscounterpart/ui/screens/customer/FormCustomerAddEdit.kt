package com.example.tailorscounterpart.ui.screens.customer

import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.tailorscounterpart.functions.getContact
import com.example.tailorscounterpart.ui.components.DateInputField
import com.example.tailorscounterpart.ui.components.SelectDropdown
import com.example.tailorscounterpart.viewmodel.CustomerViewModel
import com.example.tailorscounterpart.viewmodel.ValViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.jar.Manifest

/**
 * A composable screen that provides a form for adding a new customer or editing an existing one.
 *
 * This screen handles:
 * - Fetching customer data if editing (based on [id]).
 * - Picking a contact from the device's contacts list.
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
    v: ValViewModel
){
    val tracing by viewModel.getCustomerById(id = id.toLong()).collectAsState(initial = null)
    val savedContacts by viewModel.getContactsByCustomerId(id).collectAsState(initial = null)
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var contactDraft by remember(id) {
        mutableStateOf<List<CustomerViewModel.NewContact>?>(null)
    }

    LaunchedEffect(savedContacts) {
        if (contactDraft == null && savedContacts != null) {
            contactDraft = savedContacts.orEmpty()
                .map { CustomerViewModel.NewContact(it.type, it.value) }
        }
    }

    val contactPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri ->
        if (uri != null) {
            try {
                val phone = getContact(context, uri)?.phoneNumber
                if (!phone.isNullOrBlank()) {
                    contactDraft = contactDraft.orEmpty() +
                        CustomerViewModel.NewContact("Phone", phone)
                } else {
                    Toast.makeText(context, "The selected contact has no phone number.", Toast.LENGTH_SHORT).show()
                }
            } catch (error: SecurityException) {
                Toast.makeText(
                    context,
                    "Allow contacts access to import a phone number.",
                    Toast.LENGTH_LONG
                ).show()
            } catch (error: IllegalArgumentException) {
                Toast.makeText(context, "Unable to read the selected contact.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            contactPicker.launch(null)
        } else {
            Toast.makeText(context, "Contacts permission is required to import a phone number.", Toast.LENGTH_LONG).show()
        }
    }

    var date by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf("") }
    var age by rememberSaveable { mutableStateOf("") }
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

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
            gender = it.gender
            age = it.age.toString()
        }
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
                label = "Date",
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
                label = "Name",
                imeAction = ImeAction.Next
            )
            Spacer(modifier = Modifier.height(12.dp))

            CustomTextField(
                value = age,
                onValueChange = { age = it },
                label = "Age",
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            )
            Spacer(modifier = Modifier.height(12.dp))

            SelectDropdown(
                label = "Gender",
                options = listOf("Male", "FNP", "FP"),
                selectedOption = gender,
                onOptionSelected = { gender = it },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (showDialog) {
                ContactDialog(
                    onSave = { type, value ->
                        contactDraft = contactDraft.orEmpty() +
                                CustomerViewModel.NewContact(type, value)
                        showDialog = false
                    },
                    onDismis = {
                        showDialog = false
                    }
                )
            }

            contactDraft.orEmpty().forEachIndexed { index, contact ->
                ContactCard(
                    contact = contact,
                    onDelete = {
                        contactDraft = contactDraft.orEmpty().toMutableList().also { it.removeAt(index) }
                    },
                    imageVector = if (contact.type == "Phone"){
                        Icons.Default.Phone
                    } else {
                        Icons.Default.Email
                    }
                )

                Spacer(modifier = Modifier.height(11.dp))
            }

            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    showDialog = true
                }
            ){
                Text("Add Contact")
            }
            OutlinedButton(
                onClick = {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.READ_CONTACTS
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        contactPicker.launch(null)
                    } else {
                        permissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                    }
                }
            ) {
                Text("Import Contact")
            }
            }

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        val trimmedDate = date.trim()
                        val trimmedName = name.trim()
                        val trimmedAge = age.trim()
                        val trimmedGender = gender.trim()

                        // Validation
                        if (trimmedDate.isEmpty()  || trimmedName.isEmpty() ||
                            trimmedAge.isEmpty() || trimmedGender.isEmpty()) {
                            Toast.makeText(context, "All fields are required", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val ageInt = trimmedAge.toIntOrNull()
                        if (ageInt == null || ageInt !in 0..99) {
                            Toast.makeText(context, "Age must be between 0 and 99", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        try {
                            val selectedLocalDate = LocalDate.parse(trimmedDate)
                            if (selectedLocalDate.isAfter(LocalDate.now())) {
                                Toast.makeText(context, "Date cannot be in the future", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            if (id == "0" || tracing == null) {
                                viewModel.insertCustomerWithContacts(
                                    dateCreated = LocalDateTime.of(selectedLocalDate, LocalTime.MIDNIGHT),
                                    name = trimmedName,
                                    age = trimmedAge,
                                    gender = trimmedGender,
                                    contacts = contactDraft.orEmpty()
                                )
                            } else {
                                tracing?.let {
                                    viewModel.updateCustomerWithContacts(it.copy(
                                        dateCreated = LocalDateTime.of(selectedLocalDate, LocalTime.MIDNIGHT),
                                        name = trimmedName,
                                        age = trimmedAge,
                                        gender = trimmedGender,
                                    ), contactDraft.orEmpty())
                                }
                            }
                            navController.popBackStack()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Invalid date format", Toast.LENGTH_SHORT).show()
                        }
                    },
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
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

/**
 * A custom wrapper around [OutlinedTextField] to provide a consistent style and behavior.
 *
 * @param value The input text to be shown in the text field.
 * @param onValueChange The callback that is triggered when the input text changes.
 * @param label The label to be displayed inside the text field.
 * @param keyboardType The type of keyboard to be shown (e.g., Text, Number).
 * @param imeAction The IME action to be performed (e.g., Next, Done).
 */
@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next
){
    val focusManager = LocalFocusManager.current

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

@Composable
fun ContactCard(
    contact: CustomerViewModel.NewContact,
    onDelete: ()-> Unit,
    imageVector: ImageVector
){
    val lightBlue = Color(0xFFE3F2FD)
    val darkBlue = Color(0xFF1565C0)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(contentColor = lightBlue),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(darkBlue.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = imageVector,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = darkBlue
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.type,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Text(
                    text = contact.value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color(0xFFD32F2F)
                )
            }
        }
    }
}
