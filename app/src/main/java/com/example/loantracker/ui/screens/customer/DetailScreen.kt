package com.example.loantracker.ui.screens.customer

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.House
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.loantracker.data.local.entities.CustomerEntity
import com.example.loantracker.data.local.entities.ProjectEntity
import com.example.loantracker.navigation.ProjectNavScreen
import com.example.loantracker.viewmodel.CustomerViewModel
import com.example.loantracker.viewmodel.ProjectViewModel
import com.example.loantracker.viewmodel.ValViewModel
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DetailScreen(
    paddingValues: PaddingValues,
    projectViewModel: ProjectViewModel,
    customerViewModel: CustomerViewModel,
    v: ValViewModel,
    navController: NavHostController,
    currency: String,
    customerId: Long = 1L
) {

    val projects by projectViewModel
        .getProjectsByCustomerId(customerId)
        .collectAsState(initial = emptyList())

    val customer by customerViewModel
        .getCustomerById(customerId)
        .collectAsState(initial = null)
    val villages by customerViewModel.villages.collectAsState()
    val clusters by customerViewModel.clusters.collectAsState()

    var showAttemptDialog by remember {
        mutableStateOf(false)
    }

    var selectedProject by remember {
        mutableStateOf<ProjectEntity?>(null)
    }

    var projectToDelete by remember {
        mutableStateOf<ProjectEntity?>(null)
    }

    val currentProjectId = v.activeProjectId

    LaunchedEffect(customer?.id) {
        customer?.let {
            v.setTracing(it.name)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {

            // =================================================================
            // CUSTOMER DETAILS
            // =================================================================

            customer?.let {
                CustomerDetailsSection(
                    customer = it,
                    village = villages.firstOrNull { village -> village.id.toLong() == it.villageId }?.name,
                    cluster = clusters.firstOrNull { cluster -> cluster.id.toLong() == it.clusterId }?.name
                )
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            // =================================================================
            // PROJECT SECTION HEADER
            // =================================================================

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "Loans",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "${projects.size} loan${if (projects.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            // =================================================================
            // PROJECT LIST
            // =================================================================

            if (projects.isEmpty()) {

                EmptyProjectsState()

            } else {

                LazyColumn(
                    modifier = Modifier.weight(1f),

                    contentPadding = PaddingValues(
                        top = 4.dp,
                        bottom = 120.dp
                    ),

                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    items(
                        items = projects,
                        key = { it.id }
                    ) { project ->

                        ProjectCard(

                            project = project,

                            onOpen = {
                                navController.navigate(
                                    ProjectNavScreen.Home
                                        .goToProjectHome(project.id)
                                )

                                v.setActiveProjectId(project.id)
                                v.setActiveProject(project)
                            },

                            onEdit = {
                                selectedProject = project
                                showAttemptDialog = true
                            },

                            onDelete = {
                                projectToDelete = project
                            }
                        )
                    }
                }
            }
        }

        // =====================================================================
        // FLOATING ACTION BUTTONS
        // =====================================================================

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(16.dp),

            verticalArrangement =
                Arrangement.spacedBy(12.dp),

            horizontalAlignment =
                Alignment.End
        ) {
            FloatingActionButton(
                onClick = {
                    selectedProject = null
                    showAttemptDialog = true
                }
            ) {

                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add loan"
                )
            }
        }
    }

    // =========================================================================
    // ATTEMPT DIALOG
    // =========================================================================

    if (showAttemptDialog) {

        CustomerAttemptDialog(
            project = selectedProject,
            customerId = customerId,

            onDismis = {
                showAttemptDialog = false
            },

            onSave = { saved ->
                if (saved) {
                    showAttemptDialog = false
                }
            },

            viewModel = projectViewModel,
            currency = currency
        )
    }

    // =========================================================================
    // DELETE PROJECT CONFIRMATION
    // =========================================================================

    projectToDelete?.let { project ->

        AlertDialog(

            onDismissRequest = {
                projectToDelete = null
            },

            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },

            title = {
                Text("Delete loan?")
            },

            text = {
                Text(
                    "Are you sure you want to delete " +
                            "\"${project.name}\" and its repayments? This action cannot be undone."
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        projectViewModel.deleteProject(
                            project
                        )

                        projectToDelete = null
                    }
                ) {

                    Text(
                        text = "Delete",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        projectToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

// =============================================================================
// CUSTOMER DETAILS
// =============================================================================

@Composable
fun CustomerDetailsSection(
    customer: CustomerEntity,
    village: String?,
    cluster: String?
) {

    ElevatedCard(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(20.dp),

        colors =
            CardDefaults.elevatedCardColors(
                containerColor =
                    MaterialTheme.colorScheme.surfaceContainerLow
            ),

        elevation =
            CardDefaults.elevatedCardElevation(
                defaultElevation = 1.dp
            )
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            // -----------------------------------------------------------------
            // CUSTOMER HEADER
            // -----------------------------------------------------------------

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                CustomerAvatar(
                    name = customer.name
                )

                Spacer(
                    modifier = Modifier.width(14.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = customer.name,

                        style =
                            MaterialTheme.typography.titleLarge,

                        fontWeight =
                            FontWeight.Bold,

                        maxLines = 1,

                        overflow =
                            TextOverflow.Ellipsis
                    )

                    Text(
                        text = "Customer",

                        style =
                            MaterialTheme.typography.bodySmall,

                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            // -----------------------------------------------------------------
            // CUSTOMER INFO
            // -----------------------------------------------------------------

            DetailItem(
                label = "Village",
                value = village ?: "Not set"
            )

            DetailItem(label = "Cluster", value = cluster ?: "Not set")

            DetailItem(
                label = "Age",
                value = customer.age?.takeIf(String::isNotBlank) ?: "Not set"
            )

            DetailItem(
                label = "Added",
                value = customer.dateCreated.format(
                    DateTimeFormatter.ofPattern(
                        "dd MMM yyyy"
                    )
                )
            )


            // -----------------------------------------------------------------
            // CONTACTS
            // -----------------------------------------------------------------

            customer.phoneNumber?.let { phone ->
                if (phone.isNotBlank()) {

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    ContactSection(
                        contacts = listOf(phone)
                    )
                }
            }

        }
    }
}

// =============================================================================
// CUSTOMER AVATAR
// =============================================================================

@Composable
private fun CustomerAvatar(
    name: String
) {

    val initials = name
        .trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") {
            it.first().uppercase()
        }

    Surface(
        modifier = Modifier.size(52.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {

        Box(
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = initials.ifBlank { "?" },

                style =
                    MaterialTheme.typography.titleMedium,

                fontWeight =
                    FontWeight.Bold,

                color =
                    MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

// =============================================================================
// CONTACT SECTION
// =============================================================================

@Composable
private fun ContactSection(
    contacts: List<String>
) {

    Column(
        verticalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {

        Text(
            text = "Contact",
            style =
                MaterialTheme.typography.titleSmall,
            fontWeight =
                FontWeight.Bold
        )

        contacts.forEach { contact ->

            ContactRow(
                icon = Icons.Default.Phone,
                value = contact
            )
        }

    }
}

// =============================================================================
// CONTACT ROW
// =============================================================================

@Composable
private fun ContactRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String
) {

    Row(
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(
            modifier = Modifier.width(10.dp)
        )

        Text(
            text = value,

            style =
                MaterialTheme.typography.bodyMedium,

            color =
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// =============================================================================
// DETAIL ITEM
// =============================================================================

@Composable
fun DetailItem(
    label: String,
    value: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),

        horizontalArrangement =
            Arrangement.SpaceBetween,

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(
            text = label,

            style =
                MaterialTheme.typography.bodyMedium,

            color =
                MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,

            style =
                MaterialTheme.typography.bodyMedium,

            fontWeight =
                FontWeight.SemiBold
        )
    }
}

// =============================================================================
// PROJECT CARD
// =============================================================================

@Composable
fun ProjectCard(
    project: ProjectEntity,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {

    val formatter =
        DateTimeFormatter.ofPattern(
            "dd MMM yyyy"
        )

    ElevatedCard(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(18.dp),

        colors =
            CardDefaults.elevatedCardColors(
                containerColor =
                    MaterialTheme.colorScheme.surfaceContainer
            ),

        elevation =
            CardDefaults.elevatedCardElevation(
                defaultElevation = 1.dp
            )
    ) {

        Column {

            // =================================================================
            // MAIN CLICKABLE AREA
            // =================================================================

            Row(

                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClick = onOpen
                    )
                    .padding(
                        start = 16.dp,
                        top = 16.dp,
                        end = 12.dp,
                        bottom = 12.dp
                    ),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                // -------------------------------------------------------------
                // PROJECT ICON
                // -------------------------------------------------------------

                Surface(

                    modifier =
                        Modifier.size(48.dp),

                    shape =
                        RoundedCornerShape(14.dp),

                    color =
                        MaterialTheme.colorScheme.primaryContainer
                ) {

                    Box(
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Work,

                            contentDescription = null,

                            tint =
                                MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.width(14.dp)
                )

                // -------------------------------------------------------------
                // PROJECT INFORMATION
                // -------------------------------------------------------------

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(

                        text = project.name,

                        style =
                            MaterialTheme.typography.titleMedium,

                        fontWeight =
                            FontWeight.Bold,

                        maxLines = 1,

                        overflow =
                            TextOverflow.Ellipsis
                    )

                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.CalendarToday,

                            contentDescription = null,

                            modifier =
                                Modifier.size(14.dp),

                            tint =
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(
                            modifier =
                                Modifier.width(5.dp)
                        )

                        Text(

                            text =
                                project.dateCreated
                                    .format(formatter),

                            style =
                                MaterialTheme.typography.bodySmall,

                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // -------------------------------------------------------------
                // OPEN INDICATOR
                // -------------------------------------------------------------

                Icon(
                    imageVector =
                        Icons.Default.ChevronRight,

                    contentDescription =
                        "Open loan",

                    tint =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // =================================================================
            // ACTION BAR
            // =================================================================

            Row(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 12.dp,
                            end = 8.dp,
                            bottom = 8.dp
                        ),

                horizontalArrangement =
                    Arrangement.End,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                TextButton(
                    onClick = onEdit
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Edit,

                        contentDescription = null,

                        modifier =
                            Modifier.size(18.dp)
                    )

                    Spacer(
                        modifier =
                            Modifier.width(6.dp)
                    )

                    Text("Edit")
                }

                TextButton(
                    onClick = onDelete
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Delete,

                        contentDescription = null,

                        modifier =
                            Modifier.size(18.dp),

                        tint =
                            MaterialTheme.colorScheme.error
                    )

                    Spacer(
                        modifier =
                            Modifier.width(6.dp)
                    )

                    Text(
                        text = "Delete",

                        color =
                            MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// =============================================================================
// SMALL ACTION BUTTON
// =============================================================================

@Composable
private fun SmallActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {

    FilledTonalButton(
        onClick = onClick
    ) {

        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )

        Spacer(
            modifier = Modifier.width(6.dp)
        )

        Text(text)
    }
}

// =============================================================================
// EMPTY PROJECT STATE
// =============================================================================

@Composable
private fun EmptyProjectsState() {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 48.dp,
                bottom = 48.dp
            ),

        contentAlignment =
            Alignment.Center
    ) {

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Icon(
                imageVector =
                    Icons.Default.Work,

                contentDescription = null,

                modifier =
                    Modifier.size(48.dp),

                tint =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            Text(
                text =
                    "No loans yet",

                style =
                    MaterialTheme.typography.titleMedium,

                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )

            Text(
                text =
                    "Add a loan to start tracking this customer's balance.",

                style =
                    MaterialTheme.typography.bodyMedium,

                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}