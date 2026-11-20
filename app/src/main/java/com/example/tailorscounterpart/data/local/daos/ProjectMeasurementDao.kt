package com.example.tailorscounterpart.data.local.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.tailorscounterpart.data.local.data_classes.ProjectMeasurement
import com.example.tailorscounterpart.data.local.entities.ProjectMeasurementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectMeasurementDao {
    @Insert(onConflict = OnConflictStrategy.Companion.IGNORE)
    suspend fun insertProjectMeasurement(projectMeasurement: ProjectMeasurementEntity)

    @Update
    suspend fun updateProjectMeasurement(projectMeasurement: ProjectMeasurementEntity)

    @Delete
    suspend fun deleteProjectMeasurement(projectMeasurement: ProjectMeasurementEntity)

    @Query("SELECT * FROM project_measurements ORDER BY created_at")
    fun getProjectMeasurements(): Flow<List<ProjectMeasurementEntity>>

    @Query(value = """
            SELECT pm.*, m.name AS measurementName  FROM project_measurements pm 
            LEFT JOIN measurements m 
            ON pm.measurementId = m.id
            WHERE projectId = :id
        """)
        fun getProjectMeasurementByProjectId(id: Long?): Flow<List<ProjectMeasurement>>

}