package com.example.tailorscounterpart.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tailorscounterpart.data.local.data_classes.ContactPhonebook
import com.example.tailorscounterpart.data.local.entities.ContactEntity
import com.example.tailorscounterpart.data.local.entities.CustomerEntity
import com.example.tailorscounterpart.data.local.entities.isProjectCompleted
import com.example.tailorscounterpart.data.local.entities.CustomerWithProjects
import com.example.tailorscounterpart.data.local.repository.ContactRepository
import com.example.tailorscounterpart.data.local.repository.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class CustomerViewModel @Inject constructor(
    private val repository: CustomerRepository,
    private val contactRepository: ContactRepository
): ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery

    private val _contacts = mutableStateListOf<NewContact>()

    val contacts = _contacts

    fun addContacts(contact: NewContact){
        _contacts.add(contact)
    }

    fun removeContacts(index: Int){
        if (index in _contacts.indices){
            _contacts.removeAt(index)
        }
    }
    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter = _selectedFilter

    private val _startDate = MutableStateFlow<LocalDateTime?>(null)
    val startDate = _startDate

    private val _endDate = MutableStateFlow<LocalDateTime?>(null)
    val endDate = _endDate

    private val _customerIdsWithBalance = repository.customerIdsWithBalance

    private val _insertedCustomerId = MutableStateFlow<Long?>(null)
    val insertedCustomerId = _insertedCustomerId.asStateFlow()

    val customers = repository.customersWithAttempts
        .combine(_searchQuery) { customers, query ->
            if (query.isBlank()) customers
            else customers.filter {
                it.customer.name.contains(query, ignoreCase = true) ||
                it.customer.gender.contains(query, ignoreCase = true)
            }
        }
        .combine(_selectedFilter) { customers, filter ->
            when (filter) {
                "Unfinished" -> customers.filter { unfinished(it) }
                "Finished" -> customers.filter { finished(it) }
                else -> customers
            }
        }
        .combine(_customerIdsWithBalance) { customers, customerIdsWithBalance ->
            if (_selectedFilter.value == "Balances") {
                customers.filter { it.customer.id in customerIdsWithBalance }
            } else customers
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

    private fun isTraced(customer: CustomerWithProjects): Boolean {
        return customer.projects.isNotEmpty()
    }

    private fun complete(customer: CustomerWithProjects): Boolean {
        return customer.projects.any { it.status.isProjectCompleted() }
    }

    private fun finished(customer: CustomerWithProjects): Boolean {
        return customer.projects.isNotEmpty() &&
                customer.projects.all { it.status.isProjectCompleted() }
    }

    private fun unfinished(customer: CustomerWithProjects): Boolean {
        return customer.projects.any { !it.status.isProjectCompleted() }
    }

    fun getCustomerById(id: Long): Flow<CustomerEntity> {
        return repository.getOne(id = id.toString())
    }

    fun getContactsByCustomerId(customerId: String): Flow<List<ContactEntity>> {
        return  contactRepository.getCustomerContacts(customerId = customerId)
    }
    
    fun insertCustomer(
        dateCreated: LocalDateTime,
        name: String,
        age: String,
        gender: String
    ){
        viewModelScope.launch {
            val id = repository.insert(
                CustomerEntity(
                    dateCreated = dateCreated,
                    name = name,
                    age = age,
                    gender = gender
                )
            )

            _insertedCustomerId.value = id
        }
    }

    data class NewContact(
        val type: String,
        val value: String
    )

    fun insertCustomerWithContacts(
        dateCreated: LocalDateTime,
        name: String,
        age: String,
        gender: String,
        contacts: List<NewContact>
    ) {
        viewModelScope.launch {

            val customerId = repository.insert(
                CustomerEntity(
                    dateCreated = dateCreated,
                    name = name,
                    age = age,
                    gender = gender
                )
            )

            val contactEntities = contacts.map { contact ->
                ContactEntity(
                    customerId = customerId,
                    type = contact.type,
                    value = contact.value
                )
            }

            contactRepository.insertAll(contactEntities)

            _insertedCustomerId.value = customerId


        }
    }

    fun updateCustomer(customer: CustomerEntity){
        viewModelScope.launch {
            repository.update(customer)
        }
    }

    fun updateCustomerWithContacts(customer: CustomerEntity, contacts: List<NewContact>) {
        viewModelScope.launch {
            repository.update(customer)
            contactRepository.deleteByCustomerId(customer.id)
            contactRepository.insertAll(
                contacts.map {
                    ContactEntity(customerId = customer.id, type = it.type, value = it.value)
                }
            )
        }
    }

    fun deleteCustomer(customer: CustomerEntity){
        viewModelScope.launch {
            repository.delete(customer)
        }
    }

    //Crud operations of Contacts =================================================
    fun insertContact(
        customerId: Long,
        type: String,
        value: String
    ){
        viewModelScope.launch {
            contactRepository.insert(
                ContactEntity(
                    customerId = customerId,
                    type = type,
                    value = value
                )
            )
        }
    }

    fun updateContact(contact: ContactEntity){
        viewModelScope.launch {
            contactRepository.update(
                contact
            )
        }
    }

    fun deleteContact(contact: ContactEntity){
        viewModelScope.launch {
            contactRepository.delete(
                contact
            )
        }
    }
}