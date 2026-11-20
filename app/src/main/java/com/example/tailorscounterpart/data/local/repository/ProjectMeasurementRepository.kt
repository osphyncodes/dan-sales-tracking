package com.example.tailorscounterpart.data.local.repository


import com.example.tailorscounterpart.data.local.daos.ProjectMeasurementDao
import com.example.tailorscounterpart.data.local.data_classes.ProjectMeasurement
import com.example.tailorscounterpart.data.local.entities.ProjectMeasurementEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ProjectMeasurementRepository @Inject constructor(private val dao: ProjectMeasurementDao){
    val projectMeasurements = dao.getProjectMeasurements()

    suspend fun insert(projectMeasurement: ProjectMeasurementEntity){
        dao.insertProjectMeasurement(projectMeasurement)
    }

    suspend fun update(projectMeasurement: ProjectMeasurementEntity){
        dao.updateProjectMeasurement(projectMeasurement)
    }

    suspend fun delete(projectMeasurement: ProjectMeasurementEntity){
        dao.deleteProjectMeasurement((projectMeasurement))
    }

    fun getProjectMeasurementById(projectId: Long?): Flow<List<ProjectMeasurement>>{
        return dao.getProjectMeasurementByProjectId(projectId)
    }
}