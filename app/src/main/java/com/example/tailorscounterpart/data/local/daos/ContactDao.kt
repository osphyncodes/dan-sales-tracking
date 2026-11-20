package com.example.tailorscounterpart.data.local.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.tailorscounterpart.data.local.entities.ContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Insert(onConflict = OnConflictStrategy.Companion.IGNORE)
    suspend fun insertContact(contact: ContactEntity)

    @Insert(onConflict = OnConflictStrategy.Companion.IGNORE)
    suspend fun insertAll(contacts: List<ContactEntity>)
    @Update
    suspend fun updateContact(contact: ContactEntity)

    @Delete
    suspend fun deleteContact(contact: ContactEntity)

    @Query("SELECT * FROM contacts ORDER BY created_at")
    fun getContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE customerId = :id")
    fun getContactsByCustomerId(id: String): Flow<List<ContactEntity>>

    @Query("DELETE FROM contacts WHERE customerId = :id")
    suspend fun deleteContactsByCustomerId(id: Long)
}