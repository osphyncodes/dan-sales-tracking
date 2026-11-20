package com.example.loantracker.ui.screens

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.loantracker.data.local.entities.CustomerEntity
import com.example.loantracker.data.local.entities.CustomerWithProjects
import com.example.loantracker.data.local.entities.ProjectEntity
import com.example.loantracker.functions.formatMoney
import com.example.loantracker.navigation.MainNavScreen
import com.example.loantracker.ui.components.CustomerCard
import com.example.loantracker.ui.screens.customer.CustomerAttemptDialog
import com.example.loantracker.ui.screens.project.PaymentDialog
import com.example.loantracker.viewmodel.CustomerViewModel
import com.example.loantracker.viewmodel.ProjectViewModel
import com.example.loantracker.viewmodel.ValViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate

private data class QuickPayment(val project: ProjectEntity, val balance: Double)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    customerViewModel: CustomerViewModel,
    projectViewModel: ProjectViewModel,
    navController: NavHostController,
    paddingValues: PaddingValues,
    v: ValViewModel,
    currency: String
) {
    val customers by customerViewModel.customers.collectAsState()
    val payments by projectViewModel.getAllPayments().collectAsState(initial = emptyList())
    val villages by customerViewModel.villages.collectAsState()
    val clusters by customerViewModel.clusters.collectAsState()
    val searchQuery by customerViewModel.searchQuery.collectAsState()
    val selectedFilter by customerViewModel.selectedFilter.collectAsState()
    var customerToDelete by remember { mutableStateOf<CustomerEntity?>(null) }
    var selectedCustomerId by remember { mutableStateOf<Long?>(null) }
    var showCustomerLoans by remember { mutableStateOf(false) }
    var loanCustomerId by remember { mutableStateOf<Long?>(null) }
    var quickPayment by remember { mutableStateOf<QuickPayment?>(null) }
    var isSavingPayment by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    androidx.compose.runtime.LaunchedEffect(Unit) { v.setTitle("Customers") }

    val paidByProject = remember(payments) { payments.groupBy { it.projectId }.mapValues { (_, rows) -> rows.sumOf { it.amountPaid } } }
    fun outstandingFor(customer: CustomerWithProjects): Double = customer.projects
        .asSequence()
        .filterNot { it.isBalanceForgiven }
        .sumOf { (it.price - (paidByProject[it.id] ?: 0.0)).coerceAtLeast(0.0) }
    fun collectedFor(customer: CustomerWithProjects): Double = customer.projects.sumOf { project ->
        (paidByProject[project.id] ?: 0.0).coerceAtMost(project.price.coerceAtLeast(0.0))
    }

    val visibleCustomers = customers.filter { customer ->
        when (selectedFilter) {
            "Outstanding" -> outstandingFor(customer) > 0.0
            "Completed" -> customer.projects.isNotEmpty() && outstandingFor(customer) <= 0.0
            else -> true
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Outstanding", "Completed").forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { customerViewModel.onFilterChange(filter) },
                    label = { Text(filter) }
                )
            }
        }
        if (searchQuery.isNotBlank()) {
            Text(
                "${visibleCustomers.size} customer${if (visibleCustomers.size == 1) "" else "s"} found",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        if (visibleCustomers.isEmpty()) {
            EmptyCustomerState(hasFilters = searchQuery.isNotBlank() || selectedFilter != "All")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(visibleCustomers, key = { it.customer.id }) { customer ->
                    CustomerCard(
                        customerWithProjects = customer,
                        outstanding = outstandingFor(customer),
                        collected = collectedFor(customer),
                        currency = currency,
                        village = villages.firstOrNull { it.id.toLong() == customer.customer.villageId }?.name,
                        cluster = clusters.firstOrNull { it.id.toLong() == customer.customer.clusterId }?.name,
                        onClick = {
                            selectedCustomerId = customer.customer.id
                            showCustomerLoans = true
                        },
                        onEdit = {
                            navController.navigate(MainNavScreen.CustomerAddEdit.goToCustomerAddEdit(customer.customer.id))
                        },
                        onDelete = { customerToDelete = customer.customer },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showCustomerLoans) {
        val customer = customers.firstOrNull { it.customer.id == selectedCustomerId }
        if (customer != null) {
            val unpaidLoans = customer.projects
                .filterNot { it.isBalanceForgiven }
                .mapNotNull { project ->
                    val remaining = (project.price - (paidByProject[project.id] ?: 0.0)).coerceAtLeast(0.0)
                    if (remaining > 0.0) QuickPayment(project, remaining) else null
                }
            AlertDialog(
                onDismissRequest = { showCustomerLoans = false },
                title = {
                    Column {
                        Text(customer.customer.name)
                        Text(
                            "Unpaid loans · ${formatMoney(unpaidLoans.sumOf { it.balance }, currency)} remaining",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (unpaidLoans.isEmpty()) {
                            Text("No unpaid loans for this customer.")
                        } else {
                            unpaidLoans.forEach { loan ->
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(loan.project.name, fontWeight = FontWeight.SemiBold)
                                    Text("Remaining: ${formatMoney(loan.balance, currency)}")
                                    loan.project.dueDate?.let {
                                        Text("Expected by ${it.toLocalDate()}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    OutlinedButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            showCustomerLoans = false
                                            quickPayment = loan
                                        }
                                    ) {
                                        Text("Add payment")
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        loanCustomerId = customer.customer.id
                        showCustomerLoans = false
                    }) { Text("Add loan") }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = {
                            showCustomerLoans = false
                            navController.navigate(MainNavScreen.CustomerDetail.goToCustomerDetail(customer.customer.id))
                        }) { Text("Details") }
                        TextButton(onClick = { showCustomerLoans = false }) { Text("Close") }
                    }
                }
            )
        }
    }

    loanCustomerId?.let { customerId ->
        CustomerAttemptDialog(
            customerId = customerId,
            onDismis = { loanCustomerId = null },
            onSave = { saved -> if (saved) loanCustomerId = null },
            viewModel = projectViewModel,
            currency = currency
        )
    }

    quickPayment?.let { payment ->
        PaymentDialog(
            title = "Record loan payment",
            initialAmount = "",
            initialMethod = "Cash",
            initialDate = LocalDate.now(),
            currency = currency,
            projectAmount = payment.project.price,
            availableBalance = payment.balance,
            confirmText = "Save payment",
            isSaving = isSavingPayment,
            onDismiss = { if (!isSavingPayment) quickPayment = null },
            onConfirm = { amount, method, date ->
                isSavingPayment = true
                scope.launch {
                    try {
                        projectViewModel.insertPayment(
                            payment.project.id,
                            date.atStartOfDay(),
                            amount,
                            method.orEmpty()
                        )
                        quickPayment = null
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        Toast.makeText(context, error.message ?: "Unable to save payment.", Toast.LENGTH_LONG).show()
                    } finally {
                        isSavingPayment = false
                    }
                }
            }
        )
    }

    customerToDelete?.let { customer ->
        AlertDialog(
            onDismissRequest = { customerToDelete = null },
            title = { Text("Delete customer?") },
            text = { Text("Delete ${customer.name} and all of their loans and payments?") },
            confirmButton = {
                TextButton(onClick = {
                    customerViewModel.deleteCustomer(customer)
                    customerToDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { customerToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun EmptyCustomerState(hasFilters: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            if (hasFilters) "No customers found" else "No customers yet",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (hasFilters) "Try another search or filter." else "Customers you add will appear here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
