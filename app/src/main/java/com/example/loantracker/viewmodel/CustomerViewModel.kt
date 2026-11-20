package com.example.loantracker.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loantracker.data.local.data_classes.ContactPhonebook
import com.example.loantracker.data.local.entities.ClusterEntity
import com.example.loantracker.data.local.entities.CustomerEntity
import com.example.loantracker.data.local.entities.isProjectCompleted
import com.example.loantracker.data.local.entities.CustomerWithProjects
import com.example.loantracker.data.local.entities.VillageEntity
import com.example.loantracker.data.local.repository.ClusterRepository
import com.example.loantracker.data.local.repository.CustomerRepository
import com.example.loantracker.data.local.repository.VillageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class CustomerViewModel @Inject constructor(
    private val repository: CustomerRepository,
    private val villageRepository: VillageRepository,
    private val clusterRepository: ClusterRepository
): ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery

    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter = _selectedFilter

    private val _startDate = MutableStateFlow<LocalDateTime?>(null)
    val startDate = _startDate

    private val _endDate = MutableStateFlow<LocalDateTime?>(null)
    val endDate = _endDate

    private val _insertedCustomerId = MutableStateFlow<Long?>(null)
    val insertedCustomerId = _insertedCustomerId.asStateFlow()

    val customers = repository.customersWithAttempts
        .combine(_searchQuery) { customers, query ->
            if (query.isBlank()) customers
            else customers.filter {
                it.customer.name.contains(query, ignoreCase = true)
            }
        }
        .combine(_startDate) { customers, start ->
            if (start == null) customers
            else customers.filter { it.customer.dateCreated.isAfter(start) || it.customer.dateCreated.toLocalDate().isEqual(start.toLocalDate()) }
        }
        .combine(_endDate) { customers, end ->
            if (end == null) customers
            else customers.filter { it.customer.dateCreated.isBefore(end) || it.customer.dateCreated.toLocalDate().isEqual(end.toLocalDate()) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = emptyList()
        )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterChange(filter: String) {
        _selectedFilter.value = filter
        if (filter == "All") {
            _startDate.value = null
            _endDate.value = null
        }
    }

    fun onDateRangeChange(start: LocalDateTime?, end: LocalDateTime?) {
        _startDate.value = start
        _endDate.value = end
        if (start != null || end != null) {
            _selectedFilter.value = "Date Range"
        }
    }

    fun clearDateFilter() {
        _startDate.value = null
        _endDate.value = null
        if (_selectedFilter.value == "Date Range") {
            _selectedFilter.value = "All"
        }
    }

    fun getCustomerById(id: Long): Flow<CustomerEntity> {
        return repository.getOne(id = id.toString())
    }

    suspend fun saveCustomer(
        existingCustomer: CustomerEntity?,
        dateCreated: LocalDateTime,
        name: String,
        age: String,
        phoneNumber: String,
        villageId: Long?,
        villageName: String?,
        clusterId: Long?,
        clusterName: String?
    ) {
        val resolvedVillageId = villageId ?: villageRepository.insert(
            VillageEntity(name = requireNotNull(villageName) { "Village name is required." })
        )
        val resolvedClusterId = clusterId ?: clusterRepository.insert(
            ClusterEntity(name = requireNotNull(clusterName) { "Cluster name is required." })
        )

        val customer = existingCustomer?.copy(
            dateCreated = dateCreated,
            name = name,
            age = age,
            phoneNumber = phoneNumber,
            villageId = resolvedVillageId,
            clusterId = resolvedClusterId
        ) ?: CustomerEntity(
            dateCreated = dateCreated,
            name = name,
            age = age,
            phoneNumber = phoneNumber,
            villageId = resolvedVillageId,
            clusterId = resolvedClusterId
        )

        if (existingCustomer == null) {
            val id = repository.insert(customer)
            _insertedCustomerId.value = id
        } else {
            repository.update(customer)
        }
    }

    fun insertCustomer(
        dateCreated: LocalDateTime,
        name: String,
        age: String,
        phoneNumber: String,
        villageId: Long,
        clusterId: Long
    ){
        viewModelScope.launch {
            val id = repository.insert(
                CustomerEntity(
                    dateCreated = dateCreated,
                    name = name,
                    age = age,
                    phoneNumber = phoneNumber,
                    villageId = villageId,
                    clusterId = clusterId
                )
            )

            _insertedCustomerId.value = id
        }
    }

    data class NewContact(
        val type: String,
        val value: String
    )

    fun updateCustomer(customer: CustomerEntity){
        viewModelScope.launch {
            repository.update(customer)
        }
    }

    fun deleteCustomer(customer: CustomerEntity){
        viewModelScope.launch {
            repository.delete(customer)
        }
    }

    //Create crud for villages
    fun insertVillage(village: VillageEntity){
        viewModelScope.launch {
            villageRepository.insert(village)
        }
    }

    fun updateVillage(village: VillageEntity){
        viewModelScope.launch {
            villageRepository.update(village)
        }
    }

    fun deleteVillage(village: VillageEntity){
        viewModelScope.launch {
            villageRepository.delete(village)
        }
    }

    val villages: StateFlow<List<VillageEntity>> =
        villageRepository.getVillages()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    //Create crud for clusters
    fun insertCluster(cluster: ClusterEntity){
        viewModelScope.launch {
            clusterRepository.insert(cluster)
        }
    }

    fun updateCluster(cluster: ClusterEntity){
        viewModelScope.launch {
            clusterRepository.update(cluster)
        }
    }

    fun deleteCluster(cluster: ClusterEntity){
        viewModelScope.launch {
            clusterRepository.delete(cluster)
        }
    }

    val clusters: StateFlow<List<ClusterEntity>> =
        clusterRepository.getClusters()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
}