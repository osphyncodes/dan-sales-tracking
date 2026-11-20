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
import androidx.compose.material3.LinearProgressIndicator
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
    val totalLoaned = projects.sumOf { it.price.coerceAtLeast(0.0) }
    val totalCollected = projects.sumOf { project ->
        payments.asSequence()
            .filter { it.projectId == project.id && !it.datePaid.toLocalDate().isAfter(today) }
            .sumOf { it.amountPaid }
            .coerceAtMost(project.price.coerceAtLeast(0.0))
    }
    val stillOwedProgress = if (totalLoaned > 0.0) {
        (summary.outstandingBalance / totalLoaned).coerceIn(0.0, 1.0).toFloat()
    } else 0f
    val customersWithOutstanding = projects.asSequence()
        .filterNot { it.isBalanceForgiven }
        .filter { project ->
            project.price - payments.asSequence()
                .filter { it.projectId == project.id }
                .sumOf { it.amountPaid } > 0.0
        }
        .map { it.customerId }
        .distinct()
        .count()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Loan overview", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Track clothing loans, collections, and remaining balances.", style = MaterialTheme.typography.bodyMedium)
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
                listOf("All", "Cash", "Mpamba", "Airtel Money", "Bank", "Other").forEach { method ->
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
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Money still with customers", style = MaterialTheme.typography.titleMedium)
                    Text(formatMoney(summary.outstandingBalance, currency), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(progress = { stillOwedProgress }, modifier = Modifier.fillMaxWidth())
                    Text(
                        "${(stillOwedProgress * 100).toInt()}% of all loaned money is still outstanding",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "${formatMoney(totalCollected, currency)} collected of ${formatMoney(totalLoaned, currency)} loaned",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    "Total customers",
                    customers.size.toString(),
                    Modifier.weight(1f)
                )
                MetricCard("Customers owing", customersWithOutstanding.toString(), Modifier.weight(1f))
            }
        }
        item {
            Text("Latest payments", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        val visiblePayments = payments
            .filter {
                paymentMethod == "All" ||
                    it.method.equals(paymentMethod, ignoreCase = true) ||
                    (paymentMethod == "Other" && it.method.equals("Item Exchange", ignoreCase = true))
            }
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
                        Text(project?.name ?: "Loan #${payment.projectId}", fontWeight = FontWeight.SemiBold)
                        val method = if (payment.method.equals("Item Exchange", ignoreCase = true)) {
                            "Other"
                        } else {
                            payment.method ?: "Payment"
                        }
                        Text("$method • ${payment.datePaid.toLocalDate()}")
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
