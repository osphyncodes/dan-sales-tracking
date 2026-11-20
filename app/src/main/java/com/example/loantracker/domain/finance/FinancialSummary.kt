package com.example.loantracker.domain.finance

import com.example.loantracker.data.local.entities.PaymentEntity
import com.example.loantracker.data.local.entities.ProjectEntity
import java.time.LocalDate

data class FinancialSummary(
    val periodIncome: Double,
    val cumulativeIncome: Double,
    val outstandingBalance: Double
)

fun calculateFinancialSummary(
    projects: List<ProjectEntity>,
    payments: List<PaymentEntity>,
    startDate: LocalDate?,
    endDate: LocalDate,
    paymentMethod: String? = null
) : FinancialSummary {
    val selectedPayments = payments.filter {
        paymentMethod == null || paymentMethod == "All" ||
            it.method.equals(paymentMethod, ignoreCase = true) ||
            (paymentMethod.equals("Other", ignoreCase = true) &&
                it.method.equals("Item Exchange", ignoreCase = true))
    }
    val inRangePayments = selectedPayments.filter {
        val date = it.datePaid.toLocalDate()
        (startDate == null || !date.isBefore(startDate)) && !date.isAfter(endDate)
    }
    val paymentsByProject = payments.groupBy { it.projectId }
        .mapValues { (_, rows) -> rows.sumOf { it.amountPaid } }
    val outstanding = projects.asSequence()
        .filterNot { it.isBalanceForgiven }
        .sumOf { project ->
            (project.price - (paymentsByProject[project.id] ?: 0.0)).coerceAtLeast(0.0)
        }

    return FinancialSummary(
        periodIncome = inRangePayments.sumOf { it.amountPaid },
        cumulativeIncome = selectedPayments.filter { !it.datePaid.toLocalDate().isAfter(endDate) }
            .sumOf { it.amountPaid },
        outstandingBalance = outstanding
    )
}
