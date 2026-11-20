package com.example.loantracker.data.local.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.loantracker.data.local.entities.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Insert(onConflict = OnConflictStrategy.Companion.IGNORE)
    suspend fun insertProject(project: ProjectEntity)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Query("SELECT * FROM projects ORDER BY dateCreated DESC")
    fun getProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE customerId = :id ORDER BY dateCreated DESC")
    fun getProjectsCustomerId(id: Long): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun getProjectById(id: Long): Flow<ProjectEntity>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun findProjectById(id: Long): ProjectEntity?
}