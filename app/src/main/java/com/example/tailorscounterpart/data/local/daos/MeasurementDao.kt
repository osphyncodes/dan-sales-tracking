package com.example.tailorscounterpart.data.local.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.tailorscounterpart.data.local.entities.MeasurementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementDao {
    @Insert(onConflict = OnConflictStrategy.Companion.IGNORE)
    suspend fun insertMeasurement(measurement: MeasurementEntity): Long

    @Update
    suspend fun updateMeasurement(measurement: MeasurementEntity)

    @Delete
    suspend fun deleteMeasurement(measurement: MeasurementEntity)

    @Query("SELECT * FROM measurements ORDER BY name")
    fun getMeasurements(): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements WHERE id = :id")
    fun getMeasurementById(id: String): Flow<MeasurementEntity>
}