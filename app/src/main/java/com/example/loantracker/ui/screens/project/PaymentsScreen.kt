package com.example.loantracker.ui.screens.project

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.loantracker.data.local.entities.PaymentEntity
import com.example.loantracker.data.local.entities.ProjectEntity
import com.example.loantracker.functions.formatMoney
import com.example.loantracker.viewmodel.ProjectViewModel
import com.example.loantracker.viewmodel.ValViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// =============================================================================
// UI MODEL
// =============================================================================

data class PaymentUi(
    val id: Long,
    val projectId: Long,
    val datePaid: LocalDateTime,
    val amountPaid: Double,
    val method: String?
)

// =============================================================================
// PAYMENT SCREEN
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectPaymentsScreen(
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
    projectViewModel: ProjectViewModel,
    valViewModel: ValViewModel,
    currency: String
) {
        val projectId = valViewModel.activeProjectId
        val payments by projectViewModel
            .getPaymentsByProjectId(projectId)
            .collectAsState(initial = emptyList())

        val project by projectViewModel
            .getProjectById(projectId)
            .collectAsState(initial = null)

        var showAddDialog by rememberSaveable {
            mutableStateOf(false)
        }

        var editingPayment by remember {
            mutableStateOf<PaymentEntity?>(null)
        }

        var deletingPayment by remember {
            mutableStateOf<PaymentEntity?>(null)
        }
        var isSaving by remember { mutableStateOf(false) }

        val snackbarHostState = remember {
            SnackbarHostState()
        }

        val scope = rememberCoroutineScope()

        val totalPaid = remember(payments.toList()) {
            payments.sumOf { it.amountPaid }
        }
        val projectBalance = ((project?.price ?: 0.0) - totalPaid).coerceAtLeast(0.0)

        Scaffold(

            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),

            topBar = {

                TopAppBar(
                    title = {

                        project?.let {project ->
                            Column {

                                Text(
                                    text = "Payments",
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "Project Price - ${formatMoney(project.price, currency)}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall
                                )

                                Text(
                                    text = "${payments.size} payment${if (payments.size == 1) "" else "s"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },

                    actions = {
                        IconButton(
                            onClick = { showAddDialog = true },
                            enabled = projectBalance > 0.0 && !isSaving
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add payment"
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
                if (projectBalance > 0.0) {
                    FloatingActionButton(onClick = { if (!isSaving) showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add payment")
                    }
                }
            }

        ) { innerPadding ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .navigationBarsPadding()
            ) {

                // =================================================================
                // TOTAL PAID CARD
                // =================================================================

                TotalPaidCard(
                    totalPaid = totalPaid,
                    currency = currency
                )
                project?.let {
                    Text(
                        text = "Remaining balance: ${formatMoney(projectBalance, currency)}",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (projectBalance > 0.0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                }
                Button(
                    onClick = { showAddDialog = true },
                    enabled = projectBalance > 0.0 && !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Add payment")
                }

                // =================================================================
                // PAYMENT LIST
                // =================================================================

                if (payments.isEmpty()) {

                    Box(modifier = Modifier.weight(1f)) {
                        EmptyPaymentsState(
                            onAddPayment = {
                                showAddDialog = true
                            },
                            project = project,
                            currency = currency
                        )
                    }

                } else {

                    LazyColumn(
                        modifier = Modifier.weight(1f),

//                    contentPadding = PaddingValues(
//                        horizontal = 16.dp,
//                        vertical = 12.dp,
//                        bottom = 100.dp
//                    ),

                        verticalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {

                        items(
                            items = payments,
                            key = {
                                it.id
                            }
                        ) { payment ->

                            PaymentItem(

                                payment = payment,
                                currency = currency,

                                onEdit = {
                                    editingPayment = payment
                                },

                                onDelete = {
                                    deletingPayment = payment
                                }
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // CREATE PAYMENT
        // =========================================================================

        if (showAddDialog) {

            PaymentDialog(

                title = "Add payment",

                initialAmount = "",

                initialMethod = "Cash",

                initialDate = LocalDate.now(),

                currency = currency,
                projectAmount = project?.price ?: 0.0,
                availableBalance = projectBalance,

                confirmText = "Add payment",
                isSaving = isSaving,

                onDismiss = {
                    showAddDialog = false
                },

                onConfirm = { amount, method, date ->
                    scope.launch {
                        isSaving = true
                        try {
                            val selectedProject = project
                                ?: error("Project details are still loading. Try again.")
                            projectViewModel.insertPayment(
                                projectId = selectedProject.id,
                                datePaid = date.atStartOfDay(),
                                amountPaid = amount,
                                method = method.orEmpty()
                            )
                            showAddDialog = false
                            snackbarHostState.showSnackbar("Payment added")
                        } catch (error: CancellationException) {
                            throw error
                        } catch (error: Exception) {
                            snackbarHostState.showSnackbar(error.message ?: "Unable to save payment.")
                        } finally {
                            isSaving = false
                        }
                    }
                }
            )
        }

        // =========================================================================
        // EDIT PAYMENT
        // =========================================================================

        editingPayment?.let { payment ->

            PaymentDialog(

                title = "Edit payment",

                initialAmount = payment.amountPaid
                    .toString(),

                initialMethod = payment.method
                    ?: "Cash",

                initialDate = payment.datePaid.toLocalDate(),

                currency = currency,
                projectAmount = project?.price ?: 0.0,
                availableBalance = ((project?.price ?: 0.0) - totalPaid + payment.amountPaid)
                    .coerceAtLeast(0.0),

                confirmText = "Save changes",
                isSaving = isSaving,

                onDismiss = {
                    editingPayment = null
                },

                onConfirm = { amount, method, date ->
                    scope.launch {
                        isSaving = true
                        try {
                            projectViewModel.updatePayment(
                                payment.copy(
                                    datePaid = date.atStartOfDay(),
                                    amountPaid = amount,
                                    method = method
                                )
                            )
                            editingPayment = null
                            snackbarHostState.showSnackbar("Payment updated")
                        } catch (error: CancellationException) {
                            throw error
                        } catch (error: Exception) {
                            snackbarHostState.showSnackbar(error.message ?: "Unable to update payment.")
                        } finally {
                            isSaving = false
                        }
                    }
                }
            )
        }

        // =========================================================================
        // DELETE PAYMENT
        // =========================================================================

        deletingPayment?.let { payment ->
            AlertDialog(

                onDismissRequest = {
                    deletingPayment = null
                },

                icon = {

                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null
                    )
                },

                title = {
                    Text("Delete payment?")
                },

                text = {

                    Text(
                        "Delete the payment of " +
                                "${formatMoney(payment.amountPaid, currency)} " +
                                "from ${formatPaymentDate(payment.datePaid)}?"
                    )
                },

                confirmButton = {

                    TextButton(
                        enabled = !isSaving,

                        onClick = {
                            scope.launch {
                                isSaving = true
                                try {
                                    projectViewModel.deletePayment(payment)
                                    deletingPayment = null
                                    snackbarHostState.showSnackbar("Payment deleted")
                                } catch (error: CancellationException) {
                                    throw error
                                } catch (error: Exception) {
                                    snackbarHostState.showSnackbar(error.message ?: "Unable to delete payment.")
                                } finally {
                                    isSaving = false
                                }
                            }
                        }
                    ) {

                        Text(
                            text = "Delete",
                            color =
                                MaterialTheme.colorScheme.error
                        )
                    }
                },

                dismissButton = {

                    TextButton(
                        onClick = {
                            deletingPayment = null
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
}

// =============================================================================
// TOTAL PAID CARD
// =============================================================================

@Composable
private fun TotalPaidCard(
    totalPaid: Double,
    currency: String
) {

    Card(

        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 12.dp
            ),

        shape = RoundedCornerShape(20.dp),

        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.primaryContainer
        )
    ) {

        Row(

            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Surface(

                modifier =
                    Modifier.size(52.dp),

                shape =
                    RoundedCornerShape(15.dp),

                color =
                    MaterialTheme.colorScheme.primary
            ) {

                Box(
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Payments,

                        contentDescription = null,

                        tint =
                            MaterialTheme.colorScheme.onPrimary
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
                    text = "Total paid",

                    style =
                        MaterialTheme.typography.labelLarge,

                    color =
                        MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(
                    modifier = Modifier.size(2.dp)
                )

                Text(
                    text = formatMoney(totalPaid, currency),

                    style =
                        MaterialTheme.typography.headlineSmall,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

// =============================================================================
// PAYMENT ITEM
// =============================================================================

@Composable
private fun PaymentItem(
    payment: PaymentEntity,
    currency: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(16.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surfaceContainer
            )
    ) {

        Row(

            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 14.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            PaymentMethodIcon(
                method = payment.method
            )

            Spacer(
                modifier = Modifier.size(14.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text =
                        formatMoney(
                            payment.amountPaid,
                            currency
                        ),

                    style =
                        MaterialTheme.typography.titleMedium,

                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.size(2.dp)
                )

                Text(
                    text =
                        formatPaymentDate(
                            payment.datePaid
                        ),

                    style =
                        MaterialTheme.typography.bodySmall,

                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )

                payment.method
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?.let { method ->

                        Spacer(
                            modifier =
                                Modifier.size(3.dp)
                        )

                        Text(
                            text = method,

                            style =
                                MaterialTheme.typography.labelMedium,

                            color =
                                MaterialTheme.colorScheme.primary
                        )
                    }
            }

            IconButton(
                onClick = onEdit
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Edit,

                    contentDescription =
                        "Edit payment"
                )
            }

            IconButton(
                onClick = onDelete
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Delete,

                    contentDescription =
                        "Delete payment",

                    tint =
                        MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// =============================================================================
// PAYMENT METHOD ICON
// =============================================================================

@Composable
private fun PaymentMethodIcon(
    method: String?
) {

    val icon = when {

        method.equals(
            "Mpamba",
            ignoreCase = true
        ) -> Icons.Default.PhoneAndroid

        method.equals(
            "Airtel Money",
            ignoreCase = true
        ) -> Icons.Default.PhoneAndroid

        method.equals(
            "Bank",
            ignoreCase = true
        ) -> Icons.Default.AccountBalance

        method.equals(
            "Item Exchange",
            ignoreCase = true
        ) -> Icons.Default.ReceiptLong

        else -> Icons.Default.AttachMoney
    }

    Surface(

        modifier =
            Modifier.size(46.dp),

        shape =
            RoundedCornerShape(13.dp),

        color =
            MaterialTheme.colorScheme.secondaryContainer
    ) {

        Box(
            contentAlignment =
                Alignment.Center
        ) {

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint =
                    MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

// =============================================================================
// PAYMENT DIALOG
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PaymentDialog(
    title: String,
    initialAmount: String,
    initialMethod: String,
    initialDate: LocalDate,
    currency: String,
    projectAmount: Double,
    availableBalance: Double,
    confirmText: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (
        amount: Double,
        method: String?,
        date: LocalDate
    ) -> Unit
) {

    var amountText by rememberSaveable(
        initialAmount
    ) {
        mutableStateOf(initialAmount)
    }

    var selectedMethod by rememberSaveable(
        initialMethod
    ) {
        mutableStateOf(initialMethod)
    }

    var selectedDate by rememberSaveable(
        initialDate.toString()
    ) {
        mutableStateOf(initialDate.toString())
    }

    var showDatePicker by rememberSaveable {
        mutableStateOf(false)
    }

    var showMethodMenu by rememberSaveable {
        mutableStateOf(false)
    }

    val parsedAmount =
        amountText
            .replace(",", ".")
            .toDoubleOrNull()

    val date =
        runCatching {
            LocalDate.parse(selectedDate)
        }.getOrElse {
            LocalDate.now()
        }

    val isValid =
        parsedAmount != null &&
                parsedAmount > 0.0 &&
                parsedAmount.isFinite() &&
                availableBalance > 0.0 &&
                parsedAmount <= availableBalance

    val paymentMethods = listOf(
        "Cash",
        "Mpamba",
        "Airtel Money",
        "Bank",
        "Item Exchange"
    )

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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Project amount: ${formatMoney(projectAmount, currency)}")
                        Text(
                            "Available balance: ${formatMoney(availableBalance, currency)}",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                if (availableBalance <= 0.0) {
                    Text(
                        "No balance remains, so another payment cannot be recorded.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // =============================================================
                // AMOUNT
                // =============================================================

                OutlinedTextField(

                    value = amountText,

                    onValueChange = {
                        amountText = it.filter { character ->
                            character.isDigit() || character == '.' || character == ','
                        }
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    singleLine = true,

                    label = {
                        Text("Amount ($currency)")
                    },

                    placeholder = {
                        Text("e.g. 25000")
                    },

                    leadingIcon = {

                        Icon(
                            imageVector =
                                Icons.Default.AttachMoney,

                            contentDescription = null
                        )
                    },

                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType =
                                KeyboardType.Decimal
                        ),

                    shape =
                        RoundedCornerShape(12.dp)
                )
                if (parsedAmount != null && parsedAmount > availableBalance) {
                    Text(
                        "Amount cannot exceed ${formatMoney(availableBalance, currency)}.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // =============================================================
                // PAYMENT METHOD
                // =============================================================

                Box {

                    OutlinedTextField(

                        value =
                            selectedMethod,

                        onValueChange = {},

                        modifier =
                            Modifier.fillMaxWidth(),

                        readOnly = true,

                        label = {
                            Text("Payment method")
                        },

                        trailingIcon = {

                            Icon(
                                imageVector =
                                    Icons.Default.ChevronRight,

                                contentDescription = null
                            )
                        },

                        shape =
                            RoundedCornerShape(12.dp)
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showMethodMenu = true }
                    )
                    androidx.compose.material3.DropdownMenu(

                        expanded =
                            showMethodMenu,

                        onDismissRequest = {
                            showMethodMenu = false
                        }
                    ) {

                        paymentMethods.forEach { method ->

                            androidx.compose.material3.DropdownMenuItem(

                                text = {
                                    Text(method)
                                },

                                onClick = {

                                    selectedMethod =
                                        method

                                    showMethodMenu =
                                        false
                                }
                            )
                        }
                    }
                }

                // =============================================================
                // DATE
                // =============================================================

                OutlinedTextField(

                    value =
                        formatLocalDate(date),

                    onValueChange = {},

                    modifier =
                        Modifier.fillMaxWidth(),

                    readOnly = true,

                    label = {
                        Text("Date paid")
                    },

                    trailingIcon = {

                        Icon(
                            imageVector =
                                Icons.Default.ChevronRight,

                            contentDescription =
                                "Select date"
                        )
                    },

                    shape =
                        RoundedCornerShape(12.dp)
                )

                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    FilledTonalButton(
                        onClick = {
                            showDatePicker = true
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Change payment date"
                        )
                    }
                }
            }
        },

        confirmButton = {

            TextButton(

                enabled = isValid && !isSaving,

                onClick = {

                    onConfirm(
                        parsedAmount!!,
                        selectedMethod,
                        date
                    )
                }
            ) {

                Text(if (isSaving) "Saving..." else confirmText)
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

    // =========================================================================
    // DATE PICKER
    // =========================================================================

    if (showDatePicker) {

        val datePickerState =
            rememberDatePickerState(
                initialSelectedDateMillis =
                    date
                        .atStartOfDay(
                            ZoneId.systemDefault()
                        )
                        .toInstant()
                        .toEpochMilli()
            )

        DatePickerDialog(

            onDismissRequest = {
                showDatePicker = false
            },

            confirmButton = {

                TextButton(

                    onClick = {

                        datePickerState
                            .selectedDateMillis
                            ?.let { millis ->

                                val selected =
                                    Instant
                                        .ofEpochMilli(
                                            millis
                                        )
                                        .atZone(
                                            ZoneId
                                                .systemDefault()
                                        )
                                        .toLocalDate()

                                selectedDate =
                                    selected.toString()
                            }

                        showDatePicker = false
                    }
                ) {
                    Text("Select")
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showDatePicker = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        ) {

            DatePicker(
                state = datePickerState
            )
        }
    }
}

// =============================================================================
// EMPTY STATE
// =============================================================================

@Composable
private fun EmptyPaymentsState(
    onAddPayment: () -> Unit,
    project: ProjectEntity?,
    currency: String
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
                    Icons.Default.Payments,

                contentDescription = null,

                modifier =
                    Modifier.size(56.dp),

                tint =
                    MaterialTheme.colorScheme.primary
            )

            project?.let { project ->
                Text(
                    text = "No payments yet",
                    style =
                        MaterialTheme.typography.titleLarge,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text = "Record the customer's first payment for this project.",
                    style =
                        MaterialTheme.typography.bodyMedium,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onAddPayment) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Add payment")
                }


            }


        }
    }
}

// =============================================================================
// FORMATTERS
// =============================================================================

private fun formatPaymentDate(
    dateTime: LocalDateTime
): String {

    val formatter =
        DateTimeFormatter.ofPattern(
            "dd MMM yyyy",
            Locale.getDefault()
        )

    return dateTime.format(formatter)
}

private fun formatLocalDate(
    date: LocalDate
): String {

    val formatter =
        DateTimeFormatter.ofPattern(
            "dd MMM yyyy",
            Locale.getDefault()
        )

    return date.format(formatter)
}