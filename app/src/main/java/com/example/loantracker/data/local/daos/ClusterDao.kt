package com.example.loantracker.data.local.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.loantracker.data.local.entities.ClusterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClusterDao {
    @Insert(onConflict = OnConflictStrategy.Companion.IGNORE)
    suspend fun insertCluster(cluster: ClusterEntity): Long

    @Update
    suspend fun updateCluster(cluster: ClusterEntity)

    @Delete
    suspend fun deleteCluster(cluster: ClusterEntity)

    @Query("SELECT * FROM clusters ORDER BY name")
    fun getClusters(): Flow<List<ClusterEntity>>

    @Query("SELECT * FROM clusters WHERE id = :id")
    fun getClusterById(id: String): Flow<ClusterEntity>
}