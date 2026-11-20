package com.example.tailorscounterpart.data.local.repository


import com.example.tailorscounterpart.data.local.daos.ProjectDao
import com.example.tailorscounterpart.data.local.entities.ProjectEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ProjectRepository @Inject constructor (private val dao: ProjectDao){
    val projects = dao.getProjects()

    fun getOne(id: Long): Flow<ProjectEntity> {
        return dao.getProjectById(id)
    }

    suspend fun findOne(id: Long): ProjectEntity? = dao.findProjectById(id)

    fun getByCustomerId(customerId: Long): Flow<List<ProjectEntity>> {
        return dao.getProjectsCustomerId(customerId)
    }

    suspend fun insert(project: ProjectEntity){
        dao.insertProject(project)
    }

    suspend fun update(project: ProjectEntity){
        dao.updateProject(project)
    }

    suspend fun delete(project: ProjectEntity){
        dao.deleteProject((project))
    }
}