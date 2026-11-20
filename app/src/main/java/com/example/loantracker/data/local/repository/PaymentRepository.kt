package com.example.loantracker.data.local.repository


import com.example.loantracker.data.local.daos.PaymentDao
import com.example.loantracker.data.local.entities.PaymentEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class PaymentRepository @Inject constructor(private val dao: PaymentDao){
    fun getAllPayments() = dao.getPayments()

    val payments = dao.getPayments()

    suspend fun insert(payment: PaymentEntity): Long {
        return dao.insertPayment(payment)
    }

    suspend fun update(payment: PaymentEntity): Int {
        return dao.updatePayment(payment)
    }

    suspend fun delete(payment: PaymentEntity): Int {
        return dao.deletePayment(payment)
    }

    fun getTotalPayments(projectId: Long): Flow<Double> {
        return dao.getTotalPaid(projectId)
    }

    suspend fun getTotalPaymentsOnce(projectId: Long): Double =
        dao.getTotalPaidOnce(projectId)

    fun getPaymentsByProjectId(projectId: Long): Flow<List<PaymentEntity>> {
        return dao.getPaymentsByProjectId(projectId)
    }
}