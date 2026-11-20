package com.example.tailorscounterpart.data.local.repository


import com.example.tailorscounterpart.data.local.daos.ContactDao
import com.example.tailorscounterpart.data.local.entities.ContactEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ContactRepository @Inject constructor(private val dao: ContactDao){
    val contacts = dao.getContacts()

    fun getCustomerContacts(customerId: String): Flow<List<ContactEntity>> {
        return dao.getContactsByCustomerId(customerId)
    }

    suspend fun insert(contact: ContactEntity){
        dao.insertContact(contact)
    }

    suspend fun insertAll(contacts: List<ContactEntity>){
        dao.insertAll(contacts)
    }

    suspend fun update(contact: ContactEntity){
        dao.updateContact(contact)
    }

    suspend fun delete(contact: ContactEntity){
        dao.deleteContact((contact))
    }

    suspend fun deleteByCustomerId(customerId: Long) {
        dao.deleteContactsByCustomerId(customerId)
    }
}