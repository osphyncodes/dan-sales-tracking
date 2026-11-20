package com.example.loantracker.domain.finance

import com.example.loantracker.data.local.entities.PaymentEntity
import com.example.loantracker.data.local.entities.ProjectEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class FinancialSummaryTest {
    @Test
    fun summaryFiltersIncomeAndCalculatesUnforgivenBalances() {
        val today = LocalDate.of(2026, 11, 19)
        val projects = listOf(
            project(id = 1, price = 100.0),
            project(id = 2, price = 30.0, forgiven = true),
            project(id = 3, price = 10.0)
        )
        val payments = listOf(
            payment(projectId = 1, amount = 10.0, date = today.minusDays(1)),
            payment(projectId = 1, amount = 50.0, date = today),
            payment(projectId = 2, amount = 0.0, date = today),
            payment(projectId = 3, amount = 20.0, date = today),
            payment(projectId = 1, amount = 5.0, date = today.plusDays(1))
        )

        val summary = calculateFinancialSummary(projects, payments, today, today)

        assertEquals(70.0, summary.periodIncome, 0.0)
        assertEquals(80.0, summary.cumulativeIncome, 0.0)
        assertEquals(35.0, summary.outstandingBalance, 0.0)

        val cashSummary = calculateFinancialSummary(projects, payments, today, today, "Cash")
        assertEquals(70.0, cashSummary.periodIncome, 0.0)
        assertEquals(80.0, cashSummary.cumulativeIncome, 0.0)
    }

    private fun project(id: Long, price: Double, forgiven: Boolean = false) = ProjectEntity(
        id = id,
        customerId = 1,
        dateCreated = LocalDateTime.of(2026, 11, 1, 0, 0),
        name = "Project $id",
        dueDate = LocalDateTime.of(2026, 12, 1, 0, 0),
        price = price,
        isBalanceForgiven = forgiven
    )

    private fun payment(projectId: Long, amount: Double, date: LocalDate) = PaymentEntity(
        projectId = projectId,
        datePaid = date.atStartOfDay(),
        amountPaid = amount,
        method = "Cash"
    )
}
