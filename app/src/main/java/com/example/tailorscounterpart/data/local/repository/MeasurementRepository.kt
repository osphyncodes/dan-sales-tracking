package com.example.tailorscounterpart.data.local.repository


import com.example.tailorscounterpart.data.local.daos.MeasurementDao
import com.example.tailorscounterpart.data.local.entities.MeasurementEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class MeasurementRepository @Inject constructor(private val dao: MeasurementDao){
    val measurements = dao.getMeasurements()

    suspend fun insert(measurement: MeasurementEntity): Long{
        return dao.insertMeasurement(measurement)
    }

    suspend fun update(measurement: MeasurementEntity){
        dao.updateMeasurement(measurement)
    }

    suspend fun delete(measurement: MeasurementEntity){
        dao.deleteMeasurement((measurement))
    }
}