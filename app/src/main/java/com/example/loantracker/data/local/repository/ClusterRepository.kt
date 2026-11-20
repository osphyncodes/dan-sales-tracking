package com.example.loantracker.data.local.repository


import com.example.loantracker.data.local.daos.ClusterDao
import com.example.loantracker.data.local.entities.ClusterEntity
import com.example.loantracker.data.local.entities.VillageEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ClusterRepository @Inject constructor(private val dao: ClusterDao){

    fun getClusters(): Flow<List<ClusterEntity>> {
        return dao.getClusters()
    }

    suspend fun insert(cluster: ClusterEntity): Long{
        return dao.insertCluster(cluster)
    }

    suspend fun update(cluster: ClusterEntity){
        dao.updateCluster(cluster)
    }

    suspend fun delete(cluster: ClusterEntity){
        dao.deleteCluster(cluster)
    }
}