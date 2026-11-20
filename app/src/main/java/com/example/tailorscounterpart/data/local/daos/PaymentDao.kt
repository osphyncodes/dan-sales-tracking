package com.example.tailorscounterpart.data.local.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.tailorscounterpart.data.local.entities.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Insert
    suspend fun insertPayment(payment: PaymentEntity): Long

    @Update
    suspend fun updatePayment(payment: PaymentEntity): Int

    @Delete
    suspend fun deletePayment(payment: PaymentEntity): Int

    @Query("SELECT * FROM payments ORDER BY id")
    fun getPayments(): Flow<List<PaymentEntity>>

    @Query(value = "SELECT * FROM payments WHERE projectId = :projectId ORDER BY datePaid DESC")
    fun getPaymentsByProjectId(projectId: Long): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE id = :id")
    fun getPaymentById(id: String): Flow<PaymentEntity>

    @Query("""
    SELECT COALESCE(SUM(amountPaid), 0.0)
        FROM payments
        WHERE projectId = :projectId
    """)
    fun getTotalPaid(projectId: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(amountPaid), 0.0) FROM payments WHERE projectId = :projectId")
    suspend fun getTotalPaidOnce(projectId: Long): Double
}