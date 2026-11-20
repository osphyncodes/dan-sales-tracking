package com.example.loantracker.ui.screens.project
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// -----------------------------------------------------------------------------
// Temporary UI model.
// Replace this with your MeasurementEntity when connecting Room.
// -----------------------------------------------------------------------------

data class MeasurementUi(
    val id: Int,
    val name: String,
    val createdAt: Long
)

// -----------------------------------------------------------------------------
// Main Screen
// -----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementScreen(
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier
) {
    /*
     * -------------------------------------------------------------------------
     * HARD-CODED DATA
     * -------------------------------------------------------------------------
     *
     * Replace this section with:
     *
     * val measurements by viewModel.measurements.collectAsStateWithLifecycle()
     *
     * The rest of the UI can remain almost identical.
     */

    val measurements = remember {
        mutableStateListOf(
            MeasurementUi(
                id = 1,
                name = "Chest",
                createdAt = System.currentTimeMillis()
            ),
            MeasurementUi(
                id = 2,
                name = "Waist",
                createdAt = System.currentTimeMillis()
            ),
            MeasurementUi(
                id = 3,
                name = "Shoulder",
                createdAt = System.currentTimeMillis()
            ),
            MeasurementUi(
                id = 4,
                name = "Sleeve Length",
                createdAt = System.currentTimeMillis()
            ),
            MeasurementUi(
                id = 5,
                name = "Trouser Length",
                createdAt = System.currentTimeMillis()
            )
        )
    }

    var searchQuery by rememberSaveable {
        mutableStateOf("")
    }

    var showAddDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var measurementBeingEdited by remember {
        mutableStateOf<MeasurementUi?>(null)
    }

    var measurementBeingDeleted by remember {
        mutableStateOf<MeasurementUi?>(null)
    }

    var isLoading by rememberSaveable {
        mutableStateOf(false)
    }

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    val scope = rememberCoroutineScope()

    /*
     * derivedStateOf means the filtered list is recalculated only when
     * measurements or searchQuery actually changes.
     */
    val filteredMeasurements by remember {
        derivedStateOf {
            val query = searchQuery.trim()

            if (query.isBlank()) {
                measurements.toList()
            } else {
                measurements.filter {
                    it.name.contains(
                        query,
                        ignoreCase = true
                    )
                }
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Measurements",
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "${measurements.size} measurement types",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
        ) {

            // -----------------------------------------------------------------
            // Search
            // -----------------------------------------------------------------

            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    ),
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null
                    )
                },
                placeholder = {
                    Text("Search measurements...")
                },
                shape = RoundedCornerShape(14.dp)
            )

            // -----------------------------------------------------------------
            // Content
            // -----------------------------------------------------------------

            when {
                isLoading -> {

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                measurements.isEmpty() -> {

                    EmptyMeasurementsState(
                        onAddMeasurement = {
                            showAddDialog = true
                        }
                    )
                }

                filteredMeasurements.isEmpty() -> {

                    EmptySearchState(
                        query = searchQuery
                    )
                }

                else -> {

                    MeasurementList(
                        measurements = filteredMeasurements,
                        onEdit = {
                            measurementBeingEdited = it
                        },
                        onDelete = {
                            measurementBeingDeleted = it
                        }
                    )
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // ADD
    // -------------------------------------------------------------------------

    if (showAddDialog) {

        MeasurementDialog(
            title = "Add measurement",
            initialName = "",
            confirmText = "Add",
            onDismiss = {
                showAddDialog = false
            },
            onConfirm = { name ->

                /*
                 * HARD-CODED CREATE
                 *
                 * Replace with:
                 *
                 * viewModel.insertMeasurement(
                 *     MeasurementEntity(name = name)
                 * )
                 */

                val nextId = (
                        measurements.maxOfOrNull { it.id } ?: 0
                        ) + 1

                measurements.add(
                    MeasurementUi(
                        id = nextId,
                        name = name,
                        createdAt = System.currentTimeMillis()
                    )
                )

                showAddDialog = false

                scope.launch {
                    snackbarHostState.showSnackbar(
                        "Measurement added"
                    )
                }
            }
        )
    }

    // -------------------------------------------------------------------------
    // EDIT
    // -------------------------------------------------------------------------

    measurementBeingEdited?.let { measurement ->

        MeasurementDialog(
            title = "Edit measurement",
            initialName = measurement.name,
            confirmText = "Save",
            onDismiss = {
                measurementBeingEdited = null
            },
            onConfirm = { newName ->

                /*
                 * HARD-CODED UPDATE
                 *
                 * Replace with:
                 *
                 * viewModel.updateMeasurement(
                 *     measurement.copy(name = newName)
                 * )
                 */

                val index = measurements.indexOfFirst {
                    it.id == measurement.id
                }

                if (index >= 0) {
                    measurements[index] =
                        measurement.copy(
                            name = newName
                        )
                }

                measurementBeingEdited = null

                scope.launch {
                    snackbarHostState.showSnackbar(
                        "Measurement updated"
                    )
                }
            }
        )
    }

    // -------------------------------------------------------------------------
    // DELETE CONFIRMATION
    // -------------------------------------------------------------------------

    measurementBeingDeleted?.let { measurement ->

        AlertDialog(
            onDismissRequest = {
                measurementBeingDeleted = null
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null
                )
            },
            title = {
                Text("Delete measurement?")
            },
            text = {
                Text(
                    "Are you sure you want to delete " +
                            "\"${measurement.name}\"?"
                )
            },
            confirmButton = {

                TextButton(
                    onClick = {

                        /*
                         * HARD-CODED DELETE
                         *
                         * Replace with:
                         *
                         * viewModel.deleteMeasurement(measurement.id)
                         */

                        measurements.removeAll {
                            it.id == measurement.id
                        }

                        measurementBeingDeleted = null

                        scope.launch {
                            snackbarHostState.showSnackbar(
                                "Measurement deleted"
                            )
                        }
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
                        measurementBeingDeleted = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

// -----------------------------------------------------------------------------
// Measurement List
// -----------------------------------------------------------------------------

@Composable
private fun MeasurementList(
    measurements: List<MeasurementUi>,
    onEdit: (MeasurementUi) -> Unit,
    onDelete: (MeasurementUi) -> Unit
) {

    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            bottom = 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        items(
            items = measurements,
            key = {
                it.id
            }
        ) { measurement ->

            MeasurementItem(
                measurement = measurement,
                onEdit = {
                    onEdit(measurement)
                },
                onDelete = {
                    onDelete(measurement)
                }
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Measurement Item
// -----------------------------------------------------------------------------

@Composable
private fun MeasurementItem(
    measurement: MeasurementUi,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
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
                color = MaterialTheme.colorScheme.primaryContainer
            ) {

                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Straighten,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
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
                    text = measurement.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "ID: ${measurement.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onEdit
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit ${measurement.name}"
                )
            }

            IconButton(
                onClick = onDelete
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete ${measurement.name}",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Add/Edit Dialog
// -----------------------------------------------------------------------------

@Composable
private fun MeasurementDialog(
    title: String,
    initialName: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {

    var name by rememberSaveable(initialName) {
        mutableStateOf(initialName)
    }

    val isValid = name.trim().length >= 2

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title)
        },
        text = {

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Text(
                    text = "Enter the name of the measurement.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = {
                        Text("Measurement name")
                    },
                    placeholder = {
                        Text("e.g. Chest")
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {

            TextButton(
                enabled = isValid,
                onClick = {
                    onConfirm(name.trim())
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
}

// -----------------------------------------------------------------------------
// Empty State
// -----------------------------------------------------------------------------

@Composable
private fun EmptyMeasurementsState(
    onAddMeasurement: () -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Icon(
                imageVector = Icons.Default.Straighten,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "No measurements yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Create measurements such as Chest, Waist, " +
                        "Shoulder or Sleeve Length.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            TextButton(
                onClick = onAddMeasurement
            ) {
                Text("Add measurement")
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Empty Search State
// -----------------------------------------------------------------------------

@Composable
private fun EmptySearchState(
    query: String
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(
                modifier = Modifier.size(12.dp)
            )

            Text(
                text = "No measurements found",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Nothing matches \"$query\".",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}