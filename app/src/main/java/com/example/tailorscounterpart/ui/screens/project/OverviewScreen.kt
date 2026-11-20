package com.example.tailorscounterpart.ui.screens.project

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.tailorscounterpart.functions.formatMoney
import com.example.tailorscounterpart.functions.getTopAppBarColors
import com.example.tailorscounterpart.data.local.entities.NoteEntity
import com.example.tailorscounterpart.data.local.entities.ProjectMeasurementEntity
import com.example.tailorscounterpart.data.local.entities.ProjectEntity
import com.example.tailorscounterpart.data.local.entities.normalizedProjectStatus
import com.example.tailorscounterpart.ui.screens.customer.CustomerAttemptDialog
import com.example.tailorscounterpart.viewmodel.MeasurementViewModel
import com.example.tailorscounterpart.viewmodel.CustomerViewModel
import com.example.tailorscounterpart.viewmodel.ProjectViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.time.LocalDateTime
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectOverviewScreen(
    paddingValues: PaddingValues,
    projectId: Long,
    projectViewModel: ProjectViewModel,
    customerViewModel: CustomerViewModel,
    currency: String,
    measurementViewModel: MeasurementViewModel,
    onPaymentsClick: () -> Unit = {},
    onMeasurementsClick: () -> Unit = {},
    onProjectDeleted: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    // ----------------------------------------------------------------
    // Hard-coded values for now.
    // Replace these later with values from ProjectEntity/ViewModel.
    // ----------------------------------------------------------------
    val project by projectViewModel
        .getProjectById(projectId)
        .collectAsState(initial = null)

    val totalPayment = projectViewModel
        .getTotalPayment(projectId)
        .collectAsState(initial = 0.0)

    val amountPaid = totalPayment.value

    var selectedTab by remember { mutableIntStateOf(0) }
    var noteText by remember(projectId) { mutableStateOf("") }
    var noteBeingEdited by remember(projectId) { mutableStateOf<NoteEntity?>(null) }
    var noteBeingDeleted by remember(projectId) { mutableStateOf<NoteEntity?>(null) }
    var showNoteEditor by remember(projectId) { mutableStateOf(false) }
    var showEditProject by remember(projectId) { mutableStateOf(false) }
    var showDeleteProject by remember(projectId) { mutableStateOf(false) }
    var showStatusDialog by remember(projectId) { mutableStateOf(false) }
    var showAddPayment by remember(projectId) { mutableStateOf(false) }
    var showAddMeasurement by remember(projectId) { mutableStateOf(false) }
    var isSavingPayment by remember(projectId) { mutableStateOf(false) }
    var selectedStatus by remember(projectId) { mutableStateOf("In Progress") }
    val scope = rememberCoroutineScope()

    val projectGreen = Color(0xFF16805C)
    val lightGreen = Color(0xFFE8F5EF)

    project?.let { currentProject ->
        val customer by customerViewModel.getCustomerById(currentProject.customerId)
            .collectAsState(initial = null)
        val customerName = customer?.name ?: "Customer"
        val notes by projectViewModel.getNotesByProjectId(projectId)
            .collectAsState(initial = emptyList())
        val availableMeasurements by measurementViewModel.measurements.collectAsState(initial = emptyList())
        val projectMeasurements by projectViewModel
            .getProjectMeasurementsByProjectId(projectId)
            .collectAsState(initial = emptyList())
        val balance = (currentProject.price - amountPaid).coerceAtLeast(0.0)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 20.dp,
                vertical = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        )
        {

            // --------------------------------------------------------
            // PROJECT HEADER
            // --------------------------------------------------------

            item {
                ProjectHeaderCard(
                    projectName = currentProject.name,
                    customerName = customerName,
                    status = currentProject.status.normalizedProjectStatus(),
                    projectGreen = projectGreen,
                    lightGreen = lightGreen,
                    onEdit = { showEditProject = true },
                    onDelete = { showDeleteProject = true },
                    onUpdateStatus = {
                        selectedStatus = currentProject.status.normalizedProjectStatus()
                        showStatusDialog = true
                    },
                    onAddPayment = { showAddPayment = true },
                    onAddMeasurement = { showAddMeasurement = true }
                )
            }

            // --------------------------------------------------------
            // FINANCIAL SUMMARY
            // --------------------------------------------------------

            item {
                SectionTitle(
                    title = "Financial summary"
                )
            }

            item {
                FinancialSummaryCard(
                    price = currentProject.price,
                    amountPaid = amountPaid,
                    balance = balance,
                    isBalanceForgiven = currentProject.isBalanceForgiven,
                    projectGreen = projectGreen,
                    currency = currency
                )
            }

            // --------------------------------------------------------
            // PROJECT INFORMATION
            // --------------------------------------------------------

            item {
                SectionTitle(
                    title = "Project information"
                )
            }

            item {
                ProjectInformationCard(
                    customerName = customerName,
                    dateCreated = currentProject.dateCreated
                        .format(
                            DateTimeFormatter.ofPattern("MMMM dd, yyyy")
                        ),
                    dueDate = currentProject.dueDate.format(
                        DateTimeFormatter.ofPattern(
                        "MMMM dd, yyyy"
                    )),
                    status = currentProject.status.normalizedProjectStatus(),
                    projectGreen = projectGreen
                )
            }

            // --------------------------------------------------------
            // QUICK ACTIONS
            // --------------------------------------------------------

            item {
                SectionTitle(
                    title = "Quick actions"
                )
            }

            item {
                QuickActionsCard(
                    onMeasurementsClick = onMeasurementsClick,
                    onPaymentsClick = onPaymentsClick
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionTitle(title = "Project notes")
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = {
                        noteBeingEdited = null
                        noteText = ""
                        showNoteEditor = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add project note")
                    }
                }
            }
            if (notes.isEmpty()) {
                item { Text("No notes for this project yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(notes.size, key = { notes[it].id }) { index ->
                    val note = notes[index]
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(note.note, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    note.dateCreated.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = {
                                noteBeingEdited = note
                                noteText = note.note
                                showNoteEditor = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit note")
                            }
                            IconButton(onClick = { noteBeingDeleted = note }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete note")
                            }
                        }
                    }
                }
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (showNoteEditor) {
            AlertDialog(
                onDismissRequest = { showNoteEditor = false },
                title = { Text(if (noteBeingEdited == null) "Add project note" else "Edit project note") },
                text = {
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Note") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                },
                confirmButton = {
                    TextButton(
                        enabled = noteText.isNotBlank(),
                        onClick = {
                            val text = noteText.trim()
                            noteBeingEdited?.let { projectViewModel.updateNote(it.copy(note = text)) }
                                ?: projectViewModel.insertNote(projectId, LocalDateTime.now(), text)
                            showNoteEditor = false
                        }
                    ) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { showNoteEditor = false }) { Text("Cancel") }
                }
            )
        }

        if (showEditProject) {
            CustomerAttemptDialog(
                project = currentProject,
                customerId = currentProject.customerId,
                onDismis = { showEditProject = false },
                onSave = { saved -> if (saved) showEditProject = false },
                viewModel = projectViewModel,
                type = "project",
                currency = currency
            )
        }

        if (showDeleteProject) {
            AlertDialog(
                onDismissRequest = { showDeleteProject = false },
                title = { Text("Delete project?") },
                text = {
                    Text("Delete \"${currentProject.name}\" and all its payments, measurements, and notes? This cannot be undone.")
                },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            try {
                                projectViewModel.deleteProjectNow(currentProject)
                                showDeleteProject = false
                                onProjectDeleted(currentProject.customerId)
                            } catch (error: CancellationException) {
                                throw error
                            } catch (error: Exception) {
                                android.widget.Toast.makeText(
                                    context,
                                    error.message ?: "Unable to delete project.",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteProject = false }) { Text("Cancel") }
                }
            )
        }

        if (showStatusDialog) {
            AlertDialog(
                onDismissRequest = { showStatusDialog = false },
                title = { Text("Update project status") },
                text = {
                    Column {
                        listOf("In Progress", "Cancelled", "Completed").forEach { status ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedStatus = status },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedStatus == status,
                                    onClick = { selectedStatus = status }
                                )
                                Text(status)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            try {
                                projectViewModel.updateProjectNow(currentProject.copy(status = selectedStatus))
                                showStatusDialog = false
                            } catch (error: CancellationException) {
                                throw error
                            } catch (error: Exception) {
                                android.widget.Toast.makeText(
                                    context,
                                    error.message ?: "Unable to update project status.",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { showStatusDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (showAddPayment) {
            PaymentDialog(
                title = "Add payment",
                initialAmount = "",
                initialMethod = "Cash",
                initialDate = LocalDate.now(),
                currency = currency,
                projectAmount = currentProject.price,
                availableBalance = balance,
                confirmText = "Add payment",
                isSaving = isSavingPayment,
                onDismiss = { if (!isSavingPayment) showAddPayment = false },
                onConfirm = { amount, method, date ->
                    scope.launch {
                        isSavingPayment = true
                        try {
                            projectViewModel.insertPayment(
                                currentProject.id,
                                date.atStartOfDay(),
                                amount,
                                method.orEmpty()
                            )
                            showAddPayment = false
                        } catch (error: CancellationException) {
                            throw error
                        } catch (error: Exception) {
                            android.widget.Toast.makeText(
                                context,
                                error.message ?: "Unable to add payment.",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        } finally {
                            isSavingPayment = false
                        }
                    }
                }
            )
        }

        if (showAddMeasurement) {
            ProjectMeasurementDialog(
                title = "Add measurement",
                availableMeasurements = availableMeasurements,
                existingProjectMeasurements = projectMeasurements,
                initialMeasurementId = null,
                initialValue = "",
                confirmText = "Add measurement",
                onDismiss = { showAddMeasurement = false },
                onCreateMeasurement = { name -> measurementViewModel.insertMeasurement(name.trim()) },
                onConfirm = { measurementId, value ->
                    scope.launch {
                        try {
                            projectViewModel.insertProjectMeasurementNow(
                                ProjectMeasurementEntity(
                                    projectId = projectId,
                                    measurementId = measurementId,
                                    value = value
                                )
                            )
                            showAddMeasurement = false
                        } catch (error: CancellationException) {
                            throw error
                        } catch (error: Exception) {
                            android.widget.Toast.makeText(
                                context,
                                error.message ?: "Unable to add measurement.",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            )
        }

        noteBeingDeleted?.let { note ->
                AlertDialog(
                    onDismissRequest = { noteBeingDeleted = null },
                    title = { Text("Delete note?") },
                    text = { Text("This project note will be removed.") },
                    confirmButton = {
                        TextButton(onClick = {
                            projectViewModel.deleteNote(note)
                            noteBeingDeleted = null
                        }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    },
                    dismissButton = {
                        TextButton(onClick = { noteBeingDeleted = null }) { Text("Cancel") }
                    }
                )
        }
    }

}


// ====================================================================
// PROJECT HEADER
// ====================================================================

@Composable
private fun ProjectHeaderCard(
    projectName: String,
    customerName: String,
    status: String,
    projectGreen: Color,
    lightGreen: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onUpdateStatus: () -> Unit,
    onAddPayment: () -> Unit,
    onAddMeasurement: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(lightGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        tint = projectGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = projectName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.width(5.dp))

                        Text(
                            text = customerName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Project actions")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        listOf(
                            "Edit" to onEdit,
                            "Delete" to onDelete,
                            "Update status" to onUpdateStatus,
                            "Add Payment" to onAddPayment,
                            "Add measurement" to onAddMeasurement
                        ).forEach { (label, action) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    menuExpanded = false
                                    action()
                                }
                            )
                        }
                    }
                    StatusBadge(
                        status = status,
                        color = projectGreen,
                        backgroundColor = lightGreen
                    )
                }
            }

        }
    }
}


// ====================================================================
// FINANCIAL SUMMARY
// ====================================================================

@Composable
private fun FinancialSummaryCard(
    price: Double,
    amountPaid: Double,
    balance: Double,
    isBalanceForgiven: Boolean,
    projectGreen: Color,
    currency: String
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {

        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            Text(
                text = "Total project price",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = formatMoney(price, currency),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                FinancialItem(
                    modifier = Modifier.weight(1f),
                    label = "Paid",
                    value = formatMoney(amountPaid, currency),
                    valueColor = projectGreen
                )

                FinancialItem(
                    modifier = Modifier.weight(1f),
                    label = "Balance",
                    value = if (isBalanceForgiven) {
                        "Forgiven"
                    } else {
                        formatMoney(balance, currency)
                    },
                    valueColor = if (isBalanceForgiven) {
                        projectGreen
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Payment progress

            Text(
                text = "Payment progress",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(10.dp))

            val progress = if (price > 0) {
                (amountPaid / price).coerceIn(0.0, 1.0).toFloat()
            } else {
                0f
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant
                    )
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(9.dp)
                        .clip(CircleShape)
                        .background(projectGreen)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = "${(progress * 100).toInt()}% paid",
                    style = MaterialTheme.typography.labelMedium,
                    color = projectGreen,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = if (isBalanceForgiven) {
                        "Balance forgiven"
                    } else {
                        "Remaining: ${formatMoney(balance, currency)}"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


// ====================================================================
// FINANCIAL ITEM
// ====================================================================

@Composable
private fun FinancialItem(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: Color
) {

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {

        Column(
            modifier = Modifier.padding(14.dp)
        ) {

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
    }
}


// ====================================================================
// PROJECT INFORMATION
// ====================================================================

@Composable
private fun ProjectInformationCard(
    customerName: String,
    dateCreated: String,
    dueDate: String,
    status: String,
    projectGreen: Color
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {

        Column(
            modifier = Modifier.padding(
                horizontal = 20.dp,
                vertical = 6.dp
            )
        ) {

            InformationRow(
                icon = Icons.Default.Person,
                title = "Customer",
                value = customerName,
                iconColor = projectGreen
            )

            HorizontalDivider()

            InformationRow(
                icon = Icons.Default.CalendarToday,
                title = "Date created",
                value = dateCreated,
                iconColor = projectGreen
            )

            HorizontalDivider()

            InformationRow(
                icon = Icons.Default.Event,
                title = "Due date",
                value = dueDate,
                iconColor = MaterialTheme.colorScheme.error
            )

            HorizontalDivider()

            InformationRow(
                icon = Icons.Default.CheckCircle,
                title = "Status",
                value = status,
                iconColor = projectGreen
            )
        }
    }
}


// ====================================================================
// INFORMATION ROW
// ====================================================================

@Composable
private fun InformationRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    iconColor: Color
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}


// ====================================================================
// QUICK ACTIONS
// ====================================================================

@Composable
private fun QuickActionsCard(
    onMeasurementsClick: () -> Unit,
    onPaymentsClick: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {

        Column {

            QuickActionRow(
                icon = Icons.Default.Straighten,
                title = "Measurements",
                subtitle = "View and manage customer measurements",
                onClick = onMeasurementsClick
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            QuickActionRow(
                icon = Icons.Default.Payments,
                title = "Payments",
                subtitle = "View payment history and balance",
                onClick = onPaymentsClick
            )
        }
    }
}


// ====================================================================
// QUICK ACTION ROW
// ====================================================================

@Composable
private fun QuickActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(
                    MaterialTheme.colorScheme.primaryContainer
                ),
            contentAlignment = Alignment.Center
        ) {

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(23.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


// ====================================================================
// STATUS BADGE
// ====================================================================

@Composable
private fun StatusBadge(
    status: String,
    color: Color,
    backgroundColor: Color
) {

    Surface(
        shape = RoundedCornerShape(50),
        color = backgroundColor
    ) {

        Row(
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 6.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = status,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}


// ====================================================================
// SECTION TITLE
// ====================================================================

@Composable
private fun SectionTitle(
    title: String
) {

    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(
            start = 2.dp,
            top = 4.dp
        )
    )
}


// ====================================================================
// DIVIDER
// ====================================================================

@Composable
private fun HorizontalDivider(modifier: Modifier? = null) {

    androidx.compose.material3.HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    )
}


// ====================================================================
// MONEY FORMATTER
// ====================================================================
