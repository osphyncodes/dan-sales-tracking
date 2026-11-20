package com.example.tailorscounterpart.data.local.repository


import com.example.tailorscounterpart.data.local.daos.CustomerDao
import com.example.tailorscounterpart.data.local.entities.CustomerEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CustomerRepository @Inject constructor(private val dao: CustomerDao){
    val customers = dao.getCustomers()
    val customersWithAttempts = dao.getCustomersWithAttempts()
    val customerIdsWithBalance = dao.getCustomerIdsWithBalance()

    fun getOne(id: String): Flow<CustomerEntity> {
        return dao.getCustomerById(id)
    }

    suspend fun insert(customer: CustomerEntity): Long {
        return dao.insertCustomer(customer)
    }

    suspend fun update(customer: CustomerEntity){
        dao.updateCustomer(customer)
    }

    suspend fun delete(customer: CustomerEntity){
        dao.deleteCustomer((customer))
    }
}