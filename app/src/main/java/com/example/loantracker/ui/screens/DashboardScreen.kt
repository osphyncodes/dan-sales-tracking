package com.example.loantracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.loantracker.data.local.database.AppDatabase
import com.example.loantracker.data.local.entities.isProjectInProgress
import com.example.loantracker.domain.finance.calculateFinancialSummary
import com.example.loantracker.functions.formatMoney
import java.time.LocalDate

@Composable
fun DashboardScreen(
    database: AppDatabase,
    paddingValues: PaddingValues,
    currency: String
) {
    val payments by database.paymentDao().getPayments().collectAsState(initial = emptyList())
    val projects by database.projectDao().getProjects().collectAsState(initial = emptyList())
    val customers by database.customerDao().getCustomers().collectAsState(initial = emptyList())
    var period by remember { mutableStateOf("30 days") }
    var paymentMethod by remember { mutableStateOf("All") }
    val today = LocalDate.now()
    val startDate = when (period) {
        "Today" -> today
        "7 days" -> today.minusDays(6)
        "30 days" -> today.minusDays(29)
        "This year" -> today.withDayOfYear(1)
        else -> null
    }
    val summary = calculateFinancialSummary(projects, payments, startDate, today, paymentMethod)
//    val projectsInProgress = projects.count { it.status.isProjectInProgress() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Business overview", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Track money received and customer balances.", style = MaterialTheme.typography.bodyMedium)
        }
        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Today", "7 days", "30 days", "This year", "All time").forEach { value ->
                    FilterChip(
                        selected = period == value,
                        onClick = { period = value },
                        label = { Text(value) }
                    )
                }
            }
        }
        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Cash", "Mpamba", "Airtel Money", "Bank", "Item Exchange").forEach { method ->
                    FilterChip(
                        selected = paymentMethod == method,
                        onClick = { paymentMethod = method },
                        label = { Text(method) }
                    )
                }
            }
        }
        item {
            MetricCard("Income received • $period • $paymentMethod", formatMoney(summary.periodIncome, currency))
        }
        item {
            MetricCard("Cumulative income", formatMoney(summary.cumulativeIncome, currency))
        }
        item {
            MetricCard("Outstanding balances", formatMoney(summary.outstandingBalance, currency))
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    "Total customers",
                    customers.size.toString(),
                    Modifier.weight(1f)
                )
                MetricCard(
                    "Cumulative projects",
                    projects.size.toString(),
                    Modifier.weight(1f)
                )
            }
        }
//        item {
//            MetricCard("Projects in progress", projectsInProgress.toString())
//        }
        item {
            Text("Latest payments", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        val visiblePayments = payments
            .filter { paymentMethod == "All" || it.method.equals(paymentMethod, ignoreCase = true) }
            .filter { startDate == null || !it.datePaid.toLocalDate().isBefore(startDate) }
            .filter { it.datePaid.toLocalDate() <= today }
            .sortedByDescending { it.datePaid }
            .take(20)
        if (visiblePayments.isEmpty()) {
            item { Text("No payments in this period.", style = MaterialTheme.typography.bodyMedium) }
        } else {
            items(visiblePayments.size) { index ->
                val payment = visiblePayments[index]
                val project = projects.firstOrNull { it.id == payment.projectId }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(project?.name ?: "Project #${payment.projectId}", fontWeight = FontWeight.SemiBold)
                        Text("${payment.method ?: "Payment"} • ${payment.datePaid.toLocalDate()}")
                        Text(formatMoney(payment.amountPaid, currency), color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    Card(modifier = modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}
