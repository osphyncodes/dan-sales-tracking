package com.example.tailorscounterpart.ui.screens.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.tailorscounterpart.data.local.data_classes.ProjectMeasurement
import com.example.tailorscounterpart.data.local.entities.MeasurementEntity
import com.example.tailorscounterpart.data.local.entities.ProjectMeasurementEntity
import com.example.tailorscounterpart.viewmodel.MeasurementViewModel
import com.example.tailorscounterpart.viewmodel.ProjectViewModel
import com.example.tailorscounterpart.viewmodel.ValViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectMeasurementsScreen(
    valViewModel: ValViewModel,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
    projectViewModel: ProjectViewModel,
    measurementViewModel: MeasurementViewModel
)
{
    run {

        // ========================================================================
        // DATABASE DATA
        // ========================================================================
        val projectId = valViewModel.activeProjectId
        val availableMeasurements by measurementViewModel
            .measurements
            .collectAsState(initial = emptyList())

        val projectMeasurements by projectViewModel
            .getProjectMeasurementsByProjectId(projectId)
            .collectAsState(initial = emptyList())

        // ========================================================================
        // UI STATE
        // ========================================================================

        var showAddDialog by rememberSaveable {
            mutableStateOf(false)
        }

        var editingMeasurement by remember {
            mutableStateOf<ProjectMeasurement?>(null)
        }

        var deletingMeasurement by remember {
            mutableStateOf<ProjectMeasurement?>(null)
        }

        val snackbarHostState = remember {
            SnackbarHostState()
        }

        val scope = rememberCoroutineScope()

        // ========================================================================
        // SCREEN
        // ========================================================================

        Scaffold(

            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),

            topBar = {

                androidx.compose.material3.TopAppBar(

                    title = {

                        Column {

                            Text(
                                text = "Measurements",
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "${projectMeasurements.size} measurements",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            },

            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState
                )
            },

            floatingActionButton = {

                FloatingActionButton(
                    onClick = {
                        showAddDialog = true
                    }
                ) {

                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add measurement"
                    )
                }
            }

        ) { innerPadding ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .navigationBarsPadding()
            ) {

                if (projectMeasurements.isEmpty()) {

                    EmptyProjectMeasurements(
                        onAdd = {
                            showAddDialog = true
                        }
                    )

                } else {

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),

    //                    contentPadding = PaddingValues(
    //                        horizontal = 16.dp,
    //                        vertical = 12.dp,
    //                        bottom = 100.dp
    //                    ),

                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {

                        items(
                            items = projectMeasurements,

                            key = {
                                "${it.projectId}_${it.measurementId}"
                            }

                        ) { measurement ->

                            ProjectMeasurementItem(

                                measurement = measurement,

                                onEdit = {
                                    editingMeasurement = measurement
                                },

                                onDelete = {
                                    deletingMeasurement = measurement
                                }
                            )
                        }
                    }
                }
            }
        }

        // ========================================================================
        // ADD
        // ========================================================================

        if (showAddDialog) {

            ProjectMeasurementDialog(

                title = "Add measurement",

                availableMeasurements = availableMeasurements,

                existingProjectMeasurements = projectMeasurements,

                initialMeasurementId = null,

                initialValue = "",

                confirmText = "Add",

                onDismiss = {
                    showAddDialog = false
                },

                onCreateMeasurement = { name ->

                    /*
                     * This assumes insertMeasurement() returns the
                     * newly generated ID.
                     */
                    scope.launch {

                        val newId =
                            measurementViewModel.insertMeasurement(
                                name.trim()
                            )

                        /*
                         * The database Flow will automatically update
                         * availableMeasurements.
                         *
                         * The dialog itself will be recreated with the
                         * updated list.
                         */
                    }
                },

                onConfirm = { measurementId, value ->

                    val measurement =
                        availableMeasurements.firstOrNull {
                            it.id == measurementId
                        }

                    if (measurement == null) {

                        scope.launch {
                            snackbarHostState.showSnackbar(
                                "Measurement type not found"
                            )
                        }

                        return@ProjectMeasurementDialog
                    }

                    val alreadyExists =
                        projectMeasurements.any {
                            it.measurementId == measurementId
                        }

                    if (alreadyExists) {

                        scope.launch {
                            snackbarHostState.showSnackbar(
                                "${measurement.name} already exists in this project"
                            )
                        }

                    } else {
                        projectViewModel.insertProjectMeasurement(

                            ProjectMeasurementEntity(

                                projectId = projectId,

                                measurementId = measurementId,

                                value = value,

                                createdAt = System.currentTimeMillis()
                            )
                        )


                        showAddDialog = false

                        scope.launch {
                            snackbarHostState.showSnackbar(
                                "${measurement.name} added"
                            )
                        }
                    }
                }
            )
        }

        // ========================================================================
        // EDIT
        // ========================================================================

        editingMeasurement?.let { measurement ->

            ProjectMeasurementDialog(

                title = "Edit measurement",

                availableMeasurements = availableMeasurements,

                existingProjectMeasurements = projectMeasurements,

                initialMeasurementId = measurement.measurementId,

                initialValue = measurement.value.toString(),

                confirmText = "Save",

                onDismiss = {
                    editingMeasurement = null
                },

                onCreateMeasurement = { name ->

                    scope.launch {

                        measurementViewModel
                            .insertMeasurement(
                                name.trim()
                            )
                    }
                },

                onConfirm = { measurementId, value ->

                    val selectedMeasurement =
                        availableMeasurements.firstOrNull {
                            it.id == measurementId
                        }

                    if (selectedMeasurement == null) {

                        scope.launch {
                            snackbarHostState.showSnackbar(
                                "Measurement type not found"
                            )
                        }

                        return@ProjectMeasurementDialog
                    }

                    /*
                     * Check whether the newly selected measurement
                     * already belongs to the project.
                     *
                     * The currently edited measurement is excluded.
                     */
                    val duplicate =
                        projectMeasurements.any {

                            it.measurementId == measurementId &&
                                    it.measurementId !=
                                    measurement.measurementId
                        }

                    if (duplicate) {

                        scope.launch {
                            snackbarHostState.showSnackbar(
                                "${selectedMeasurement.name} already exists"
                            )
                        }

                    } else {

                        projectViewModel.updateProjectMeasurement(

                            ProjectMeasurementEntity(

                                projectId =
                                    measurement.projectId,

                                measurementId =
                                    measurementId,

                                value = value,

                                createdAt =
                                    measurement.created_at
                            )
                        )

                        editingMeasurement = null

                        scope.launch {
                            snackbarHostState.showSnackbar(
                                "Measurement updated"
                            )
                        }
                    }
                }
            )
        }

        // ========================================================================
        // DELETE
        // ========================================================================

        deletingMeasurement?.let { measurement ->

            AlertDialog(

                onDismissRequest = {
                    deletingMeasurement = null
                },

                icon = {

                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null
                    )
                },

                title = {
                    Text("Remove measurement?")
                },

                text = {

                    Text(
                        "Remove this measurement from the project?"
                    )
                },

                confirmButton = {

                    TextButton(

                        onClick = {

                            /*
                             * Convert the UI model back into the
                             * entity required by the repository.
                             */
                            projectViewModel.deleteProjectMeasurement(

                                ProjectMeasurementEntity(

                                    projectId =
                                        measurement.projectId,

                                    measurementId =
                                        measurement.measurementId,

                                    value =
                                        measurement.value,

                                    createdAt =
                                        measurement.created_at
                                )
                            )

                            deletingMeasurement = null

                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "Measurement removed"
                                )
                            }
                        }
                    ) {

                        Text(
                            text = "Remove",
                            color =
                                MaterialTheme.colorScheme.error
                        )
                    }
                },

                dismissButton = {

                    TextButton(
                        onClick = {
                            deletingMeasurement = null
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}


// =============================================================================
// ITEM
// =============================================================================

@Composable
private fun ProjectMeasurementItem(
    measurement: ProjectMeasurement,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(16.dp),

        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surfaceContainer
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                ),

            verticalAlignment = Alignment.CenterVertically
        ) {

            Surface(
                modifier = Modifier.size(46.dp),

                shape = RoundedCornerShape(13.dp),

                color =
                    MaterialTheme.colorScheme.primaryContainer
            ) {

                Box(
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Straighten,

                        contentDescription = null,

                        tint =
                            MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(
                modifier = Modifier.size(14.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = measurement.measurementName,

                    style =
                        MaterialTheme.typography.titleMedium,

                    fontWeight =
                        FontWeight.SemiBold
                )

                Text(
                    text = "Measurement",

                    style =
                        MaterialTheme.typography.bodySmall,

                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = formatMeasurementValue(
                    measurement.value
                ),

                style =
                    MaterialTheme.typography.titleLarge,

                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.size(6.dp)
            )

            IconButton(
                onClick = onEdit
            ) {

                Icon(
                    imageVector = Icons.Default.Edit,

                    contentDescription =
                        "Edit ${measurement.measurementName}"
                )
            }

            IconButton(
                onClick = onDelete
            ) {

                Icon(
                    imageVector = Icons.Default.Delete,

                    contentDescription =
                        "Delete ${measurement.measurementName}",

                    tint =
                        MaterialTheme.colorScheme.error
                )
            }
        }
    }
}


// =============================================================================
// ADD / EDIT DIALOG
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProjectMeasurementDialog(
    title: String,
    availableMeasurements: List<MeasurementEntity>,
    existingProjectMeasurements: List<ProjectMeasurement>,
    initialMeasurementId: Int?,
    initialValue: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onCreateMeasurement: (String) -> Unit,
    onConfirm: (Int, Double) -> Unit
) {

    var selectedMeasurementId
            by rememberSaveable(initialMeasurementId) {
                mutableStateOf(initialMeasurementId)
            }

    var valueText
            by rememberSaveable(initialValue) {
                mutableStateOf(initialValue)
            }

    var measurementMenuExpanded
            by remember {
                mutableStateOf(false)
            }

    var searchText
            by rememberSaveable {
                mutableStateOf("")
            }

    var showQuickCreate
            by rememberSaveable {
                mutableStateOf(false)
            }

    val selectedMeasurement =
        availableMeasurements.firstOrNull {
            it.id == selectedMeasurementId
        }

    val availableForSelection by remember(
        availableMeasurements,
        existingProjectMeasurements,
        selectedMeasurementId,
        searchText
    ) {

        derivedStateOf {

            val query =
                searchText.trim()

            availableMeasurements.filter { measurement ->

                val belongsToProject =
                    existingProjectMeasurements.any {
                        it.measurementId ==
                                measurement.id
                    }

                val isCurrent =
                    measurement.id ==
                            selectedMeasurementId

                val matchesSearch =
                    query.isBlank() ||
                            measurement.name.contains(
                                query,
                                ignoreCase = true
                            )

                (isCurrent || !belongsToProject) &&
                        matchesSearch
            }
        }
    }

    val parsedValue =
        valueText
            .replace(',', '.')
            .toDoubleOrNull()

    val isValid =
        selectedMeasurementId != null &&
                parsedValue != null &&
                parsedValue > 0.0

    AlertDialog(

        onDismissRequest = onDismiss,

        title = {
            Text(title)
        },

        text = {

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(14.dp)
            ) {

                Text(
                    text =
                        "Select a measurement and enter its value.",

                    style =
                        MaterialTheme.typography.bodyMedium,

                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )

                // ============================================================
                // MEASUREMENT DROPDOWN
                // ============================================================

                ExposedDropdownMenuBox(

                    expanded =
                        measurementMenuExpanded,

                    onExpandedChange = {

                        measurementMenuExpanded =
                            !measurementMenuExpanded
                    }
                ) {

                    OutlinedTextField(

                        value =
                            selectedMeasurement?.name ?: "",

                        onValueChange = {},

                        readOnly = true,

                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),

                        label = {
                            Text("Measurement")
                        },

                        placeholder = {
                            Text("Select measurement")
                        },

                        trailingIcon = {

                            ExposedDropdownMenuDefaults
                                .TrailingIcon(
                                    expanded =
                                        measurementMenuExpanded
                                )
                        },

                        leadingIcon = {

                            Icon(
                                imageVector =
                                    Icons.Default.Straighten,

                                contentDescription = null
                            )
                        },

                        shape =
                            RoundedCornerShape(12.dp)
                    )

                    androidx.compose.material3.DropdownMenu(

                        expanded =
                            measurementMenuExpanded,

                        onDismissRequest = {

                            measurementMenuExpanded =
                                false
                        },

                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        // SEARCH
                        OutlinedTextField(

                            value =
                                searchText,

                            onValueChange = {
                                searchText = it
                            },

                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),

                            singleLine = true,

                            leadingIcon = {

                                Icon(
                                    imageVector =
                                        Icons.Default.Search,

                                    contentDescription =
                                        null
                                )
                            },

                            placeholder = {
                                Text("Search...")
                            }
                        )

                        availableForSelection.forEach { measurement ->

                            DropdownMenuItem(

                                text = {
                                    Text(measurement.name)
                                },

                                onClick = {

                                    selectedMeasurementId =
                                        measurement.id

                                    measurementMenuExpanded =
                                        false

                                    searchText = ""
                                }
                            )
                        }

                        DropdownMenuItem(

                            text = {

                                Text(
                                    text =
                                        "+ Create new measurement",

                                    color =
                                        MaterialTheme.colorScheme.primary,

                                    fontWeight =
                                        FontWeight.SemiBold
                                )
                            },

                            onClick = {

                                measurementMenuExpanded =
                                    false

                                showQuickCreate = true
                            }
                        )
                    }
                }

                // ============================================================
                // VALUE
                // ============================================================

                OutlinedTextField(

                    value = valueText,

                    onValueChange = {
                        valueText = it
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    singleLine = true,

                    label = {
                        Text("Value")
                    },

                    placeholder = {
                        Text("e.g. 42.5")
                    },

                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType =
                                KeyboardType.Decimal
                        ),

                    suffix = {
                        Text("cm")
                    },

                    shape =
                        RoundedCornerShape(12.dp)
                )
            }
        },

        confirmButton = {

            TextButton(

                enabled = isValid,

                onClick = {

                    onConfirm(
                        selectedMeasurementId!!,
                        parsedValue!!
                    )
                }
            ) {

                Text(confirmText)
            }
        },

        dismissButton = {

            TextButton(
                onClick = onDismiss
            ) {

                Text("Cancel")
            }
        }
    )

    // ========================================================================
    // QUICK CREATE
    // ========================================================================

    if (showQuickCreate) {

        QuickCreateMeasurementDialog(

            onDismiss = {
                showQuickCreate = false
            },

            onCreate = { name ->

                onCreateMeasurement(name)

                showQuickCreate = false
            }
        )
    }
}


// =============================================================================
// QUICK CREATE
// =============================================================================

@Composable
private fun QuickCreateMeasurementDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {

    var name by rememberSaveable {
        mutableStateOf("")
    }

    val isValid =
        name.trim().length >= 2

    AlertDialog(

        onDismissRequest = onDismiss,

        title = {
            Text("New measurement")
        },

        text = {

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                Text(
                    text =
                        "Create a measurement type without leaving this project.",

                    style =
                        MaterialTheme.typography.bodyMedium,

                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(

                    value = name,

                    onValueChange = {
                        name = it
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    singleLine = true,

                    label = {
                        Text("Measurement name")
                    },

                    placeholder = {
                        Text("e.g. Neck")
                    },

                    shape =
                        RoundedCornerShape(12.dp)
                )
            }
        },

        confirmButton = {

            TextButton(

                enabled = isValid,

                onClick = {
                    onCreate(name.trim())
                }
            ) {

                Text("Create")
            }
        },

        dismissButton = {

            TextButton(
                onClick = onDismiss
            ) {

                Text("Cancel")
            }
        }
    )
}


// =============================================================================
// EMPTY STATE
// =============================================================================

@Composable
private fun EmptyProjectMeasurements(
    onAdd: () -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),

        contentAlignment =
            Alignment.Center
    ) {

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally,

            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            Icon(
                imageVector =
                    Icons.Default.Straighten,

                contentDescription = null,

                modifier =
                    Modifier.size(56.dp),

                tint =
                    MaterialTheme.colorScheme.primary
            )

            Text(
                text =
                    "No measurements added",

                style =
                    MaterialTheme.typography.titleLarge,

                fontWeight =
                    FontWeight.Bold
            )

            Text(
                text =
                    "Add the customer's measurements to this project.",

                style =
                    MaterialTheme.typography.bodyMedium,

                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onAdd
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Add,

                    contentDescription = null
                )

                Spacer(
                    modifier =
                        Modifier.size(8.dp)
                )

                Text("Add measurement")
            }
        }
    }
}


// =============================================================================
// FORMATTER
// =============================================================================

private fun formatMeasurementValue(
    value: Double
): String {

    return if (value % 1.0 == 0.0) {

        value.toInt().toString()

    } else {

        value.toString()
    }
}