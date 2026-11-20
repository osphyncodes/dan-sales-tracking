package com.example.tailorscounterpart.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tailorscounterpart.data.local.entities.MeasurementEntity
import com.example.tailorscounterpart.data.local.repository.MeasurementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MeasurementViewModel @Inject constructor(
    private val repository: MeasurementRepository
): ViewModel() {
    fun insertMeasurement(
        name: String
    ){
        viewModelScope.launch {
            repository.insert(
                MeasurementEntity(
                    name = name
                )
            )
        }
    }

    fun updateMeasurement(measurement: MeasurementEntity){
        viewModelScope.launch {
            repository.update(measurement)
        }
    }

    fun deleteMeasurement(measurement: MeasurementEntity){
        viewModelScope.launch {
            repository.delete(measurement)
        }
    }

    val measurements = repository.measurements.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = emptyList()
    )
}