package com.example.tailorscounterpart.data.local.repository


import com.example.tailorscounterpart.data.local.daos.VillageDao
import com.example.tailorscounterpart.data.local.entities.VillageEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class VillageRepository @Inject constructor(private val dao: VillageDao){
    fun getVillages(): Flow<List<VillageEntity>> {
        return dao.getVillages()
    }

    suspend fun insert(village: VillageEntity): Long{
        return dao.insertVillage(village)
    }

    suspend fun update(village: VillageEntity){
        dao.updateVillage(village)
    }

    suspend fun delete(village: VillageEntity){
        dao.deleteVillage(village)
    }
}