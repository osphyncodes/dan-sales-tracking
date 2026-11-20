package com.example.tailorscounterpart.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tailorscounterpart.data.local.data_classes.ProjectMeasurement
import com.example.tailorscounterpart.data.local.entities.NoteEntity
import com.example.tailorscounterpart.data.local.entities.PaymentEntity
import com.example.tailorscounterpart.data.local.entities.ProjectEntity
import com.example.tailorscounterpart.data.local.repository.NoteRepository
import com.example.tailorscounterpart.data.local.repository.PaymentRepository
import com.example.tailorscounterpart.data.local.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class ProjectViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val paymentRepository: PaymentRepository,
    private val noteRepository: NoteRepository,
): ViewModel() {
    //Project CRUD Operations ====================================================
    fun insertProject(
        customerId: Long,
        name: String,
        dateCreated: LocalDateTime,
        dueDate: LocalDateTime,
        price: Double
    ){
        viewModelScope.launch {
            projectRepository.insert(
                ProjectEntity(
                    customerId = customerId,
                    name = name,
                    dateCreated = dateCreated,
                    dueDate = dueDate,
                    price = price,
                )
            )
        }
    }

    fun updateProject(project: ProjectEntity){
        viewModelScope.launch {
            projectRepository.update(project)
        }
    }

    suspend fun updateProjectNow(project: ProjectEntity) {
        projectRepository.update(project)
    }

    fun getProjectById(projectId: Long): Flow<ProjectEntity>{
        return projectRepository.getOne(projectId)
    }

    fun deleteProject(project: ProjectEntity){
        viewModelScope.launch {
            projectRepository.delete(project)
        }
    }

    suspend fun deleteProjectNow(project: ProjectEntity) {
        projectRepository.delete(project)
    }

    fun getProjectsByCustomerId(customerID: Long): Flow<List<ProjectEntity>> {
        return projectRepository.getByCustomerId(customerID)
    }

    //PAYMENT CRUD Operations =======================================================
    suspend fun insertPayment(
        projectId: Long,
        datePaid: LocalDateTime,
        amountPaid: Double,
        method: String
    ): Long {
        require(amountPaid > 0.0 && amountPaid.isFinite()) { "Payment amount must be greater than zero." }
        val project = projectRepository.findOne(projectId)
            ?: error("This project no longer exists. Reopen the project and try again.")
        val alreadyPaid = paymentRepository.getTotalPaymentsOnce(projectId)
        val balance = (project.price - alreadyPaid).coerceAtLeast(0.0)
        check(balance > 0.0) { "This project has no outstanding balance." }
        require(amountPaid <= balance) {
            "Payment cannot exceed the remaining balance of $balance."
        }
        val insertedId = paymentRepository.insert(
            PaymentEntity(
                projectId = projectId,
                datePaid = datePaid,
                amountPaid = amountPaid,
                method = method
            )
        )
        check(insertedId > 0) { "Payment could not be saved." }
        return insertedId
    }

    suspend fun updatePayment(payment: PaymentEntity) {
        require(payment.amountPaid > 0.0 && payment.amountPaid.isFinite()) {
            "Payment amount must be greater than zero."
        }
        val project = projectRepository.findOne(payment.projectId)
            ?: error("This project no longer exists. Reopen the project and try again.")
        val alreadyPaid = paymentRepository.getTotalPaymentsOnce(payment.projectId)
        val availableBalance = (project.price - (alreadyPaid - payment.amountPaid)).coerceAtLeast(0.0)
        check(availableBalance > 0.0) { "This project has no outstanding balance." }
        require(payment.amountPaid <= availableBalance) {
            "Payment cannot exceed the remaining balance of $availableBalance."
        }
        check(paymentRepository.update(payment) == 1) { "Payment no longer exists." }
    }

    suspend fun deletePayment(payment: PaymentEntity) {
        check(paymentRepository.delete(payment) == 1) { "Payment no longer exists." }
    }

    fun getPaymentsByProjectId(projectId: Long): Flow<List<PaymentEntity>>{
        return paymentRepository.getPaymentsByProjectId(projectId)
    }

    fun getTotalPayment(projectId: Long): Flow<Double>{
        return paymentRepository.getTotalPayments(projectId)
    }
    //NOTE CRUD Operations ====================================================
    fun insertNote(
        projectId: Long,
        dateCreated: LocalDateTime,
        note: String,
    ){
        viewModelScope.launch {
            noteRepository.insert(
                NoteEntity(
                    projectId = projectId,
                    dateCreated = dateCreated,
                    note = note
                )
            )
        }
    }

    fun updateNote(note: NoteEntity){
        viewModelScope.launch {
            noteRepository.update(note)
        }
    }

    fun deleteNote(note: NoteEntity){
        viewModelScope.launch {
            noteRepository.delete(note)
        }
    }

    fun getNotesByProjectId(projectId: Long): Flow<List<NoteEntity>> {
        return noteRepository.getByProjectId(projectId)
    }

}