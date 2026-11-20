package com.example.tailorscounterpart.data.local.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.tailorscounterpart.data.local.entities.VillageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VillageDao {
    @Insert(onConflict = OnConflictStrategy.Companion.IGNORE)
    suspend fun insertVillage(village: VillageEntity): Long

    @Update
    suspend fun updateVillage(village: VillageEntity)

    @Delete
    suspend fun deleteVillage(village: VillageEntity)

    @Query("SELECT * FROM villages ORDER BY name")
    fun getVillages(): Flow<List<VillageEntity>>

    @Query("SELECT * FROM villages WHERE id = :id")
    fun getVillageById(id: String): Flow<VillageEntity>
}