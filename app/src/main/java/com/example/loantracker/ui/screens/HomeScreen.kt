package com.example.loantracker.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.loantracker.navigation.MainNavScreen
import com.example.loantracker.data.local.entities.CustomerEntity
import com.example.loantracker.ui.components.CustomerCard
import com.example.loantracker.viewmodel.CustomerViewModel
import com.example.loantracker.viewmodel.ValViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    customerViewModel: CustomerViewModel,
    navController: NavHostController,
    paddingValues: PaddingValues,
    v: ValViewModel
) {
    val customers by customerViewModel.customers.collectAsState()
    val searchQuery by customerViewModel.searchQuery.collectAsState()
    val selectedFilter by customerViewModel.selectedFilter.collectAsState()
    val startDate by customerViewModel.startDate.collectAsState()
    val endDate by customerViewModel.endDate.collectAsState()
    var customerToDelete by remember { mutableStateOf<CustomerEntity?>(null) }

    LaunchedEffect(Unit) {
        v.setTitle("Home")
    }

    val hasActiveFilters =
        searchQuery.isNotBlank() ||
                (selectedFilter != "All" && selectedFilter != "Date Range") ||
                startDate != null ||
                endDate != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp)
    ) {

        // ---------------------------------------------------------------------
        // ACTIVE FILTERS
        // ---------------------------------------------------------------------

        if (hasActiveFilters) {

            ActiveFilters(
                searchQuery = searchQuery,
                selectedFilter = selectedFilter,
                startDate = startDate,
                endDate = endDate,
                onClearSearch = {
                    customerViewModel.onSearchQueryChange("")
                },
                onClearStatus = {
                    customerViewModel.onFilterChange("All")
                },
                onClearDate = {
                    customerViewModel.clearDateFilter()
                }
            )

            Spacer(
                modifier = Modifier.size(4.dp)
            )
        }

        // ---------------------------------------------------------------------
        // CUSTOMER LIST
        // ---------------------------------------------------------------------

        if (customers.isEmpty()) {

            EmptyCustomerState(
                hasFilters = hasActiveFilters
            )

        } else {

            LazyColumn(
                modifier = Modifier.fillMaxSize(),

                contentPadding = PaddingValues(
                    top = 8.dp,
                    bottom = 100.dp
                ),

                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                items(
                    items = customers,
                    key = { customerWithProjects ->
                        customerWithProjects.customer.id
                    }
                ) { customerWithProjects ->

                    CustomerCard(
                        customerWithProjects = customerWithProjects,

                        onEdit = {
                            navController.navigate(
                                MainNavScreen.CustomerAddEdit
                                    .goToCustomerAddEdit(
                                        customerWithProjects.customer.id
                                    )
                            )
                        },

                        onDelete = {
                            customerToDelete = customerWithProjects.customer
                        },

                        modifier = Modifier.fillMaxWidth(),

                        navController = navController,

                        customerViewModel = customerViewModel
                    )
                }

            }
        }
    }

    customerToDelete?.let { customer ->
        AlertDialog(
            onDismissRequest = { customerToDelete = null },
            title = { Text("Delete customer?") },
            text = {
                Text("Delete ${customer.name} and all of their projects, payments, measurements, and contacts?")
            },
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

// =============================================================================
// ACTIVE FILTERS
// =============================================================================

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun ActiveFilters(
    searchQuery: String,
    selectedFilter: String,
    startDate: LocalDateTime?,
    endDate: LocalDateTime?,
    onClearSearch: () -> Unit,
    onClearStatus: () -> Unit,
    onClearDate: () -> Unit
) {

    val dateFormatter =
        DateTimeFormatter.ofPattern("dd MMM")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(
                rememberScrollState()
            )
            .padding(
                top = 8.dp,
                bottom = 4.dp
            ),

        horizontalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {

        // ---------------------------------------------------------------------
        // SEARCH FILTER
        // ---------------------------------------------------------------------

        if (searchQuery.isNotBlank()) {

            AssistChip(

                onClick = onClearSearch,

                label = {
                    Text(
                        text = "Search: $searchQuery",
                        maxLines = 1
                    )
                },

                trailingIcon = {

                    Icon(
                        imageVector =
                            Icons.Default.Close,

                        contentDescription =
                            "Clear search",

                        modifier =
                            Modifier.size(16.dp)
                    )
                }
            )
        }

        // ---------------------------------------------------------------------
        // STATUS FILTER
        // ---------------------------------------------------------------------

        if (
            selectedFilter != "All" &&
            selectedFilter != "Date Range"
        ) {

            AssistChip(

                onClick = onClearStatus,

                label = {
                    Text(
                        text = "Status: $selectedFilter"
                    )
                },

                trailingIcon = {

                    Icon(
                        imageVector =
                            Icons.Default.Close,

                        contentDescription =
                            "Clear status filter",

                        modifier =
                            Modifier.size(16.dp)
                    )
                }
            )
        }

        // ---------------------------------------------------------------------
        // DATE FILTER
        // ---------------------------------------------------------------------

        if (startDate != null || endDate != null) {

            val start =
                startDate
                    ?.format(dateFormatter)
                    ?: "..."

            val end =
                endDate
                    ?.format(dateFormatter)
                    ?: "..."

            AssistChip(

                onClick = onClearDate,

                label = {
                    Text(
                        text = "Date: $start - $end"
                    )
                },

                trailingIcon = {

                    Icon(
                        imageVector =
                            Icons.Default.Close,

                        contentDescription =
                            "Clear date filter",

                        modifier =
                            Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}

// =============================================================================
// EMPTY STATE
// =============================================================================

@Composable
private fun EmptyCustomerState(
    hasFilters: Boolean
) {

    Box(
        modifier = Modifier.fillMaxSize(),

        contentAlignment =
            Alignment.Center
    ) {

        Column(

            modifier =
                Modifier.padding(32.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally,

            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            Icon(
                imageVector =
                    Icons.Default.SearchOff,

                contentDescription = null,

                modifier =
                    Modifier.size(52.dp),

                tint =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(

                text =
                    if (hasFilters)
                        "No customers found"
                    else
                        "No customers yet",

                style =
                    MaterialTheme.typography.titleLarge
            )

            Text(

                text =
                    if (hasFilters)
                        "Try changing or clearing your filters."
                    else
                        "Customers you add will appear here.",

                style =
                    MaterialTheme.typography.bodyMedium,

                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}