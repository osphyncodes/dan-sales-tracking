package com.example.tailorscounterpart.data.local.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.tailorscounterpart.data.local.entities.CustomerEntity
import com.example.tailorscounterpart.data.local.entities.CustomerWithProjects
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Insert(onConflict = OnConflictStrategy.Companion.IGNORE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)

    @Query("SELECT * FROM customers ORDER BY dateCreated")
    fun getCustomers(): Flow<List<CustomerEntity>>

    @androidx.room.Transaction
    @Query("SELECT * FROM customers")
    fun getCustomersWithAttempts(): Flow<List<CustomerWithProjects>>

    @Query("""
        SELECT DISTINCT c.id FROM customers c
        JOIN projects p ON p.customerId = c.id
        LEFT JOIN payments pay ON pay.projectId = p.id
        GROUP BY c.id, p.id, p.price
        HAVING p.price > COALESCE(SUM(pay.amountPaid), 0)
    """)
    fun getCustomerIdsWithBalance(): Flow<List<Long>>

    @Query("SELECT * FROM customers WHERE id = :id")
    fun getCustomerById(id: String): Flow<CustomerEntity>
}