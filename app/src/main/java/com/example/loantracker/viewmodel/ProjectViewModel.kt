package com.example.loantracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loantracker.data.local.entities.NoteEntity
import com.example.loantracker.data.local.entities.PaymentEntity
import com.example.loantracker.data.local.entities.ProjectEntity
import com.example.loantracker.data.local.repository.NoteRepository
import com.example.loantracker.data.local.repository.PaymentRepository
import com.example.loantracker.data.local.repository.ProjectRepository
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
    suspend fun saveProject(
        existingProject: ProjectEntity?,
        customerId: Long,
        name: String,
        dateCreated: LocalDateTime,
        price: Double,
        dueDate: LocalDateTime?
    ) {
        require(price >= 0.0 && price.isFinite()) { "Loan amount must be a valid non-negative number." }
        if (existingProject != null) {
            val payments = paymentRepository.getTotalPaymentsOnce(existingProject.id)
            require(price >= payments) {
                "The loan amount cannot be less than the ${payments} already collected."
            }
        }
        val project = existingProject?.copy(
            customerId = customerId,
            name = name,
            dateCreated = dateCreated,
            price = price,
            dueDate = dueDate
        ) ?: ProjectEntity(
            customerId = customerId,
            name = name,
            dateCreated = dateCreated,
            price = price,
            dueDate = dueDate
        )

        if (existingProject == null) {
            projectRepository.insert(project)
        } else {
            projectRepository.update(project)
        }
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
            ?: error("This loan no longer exists. Reopen it and try again.")
        val alreadyPaid = paymentRepository.getTotalPaymentsOnce(projectId)
        val balance = (project.price - alreadyPaid).coerceAtLeast(0.0)
        check(balance > 0.0) { "This loan has no outstanding balance." }
        check(!project.isBalanceForgiven) { "This loan has been marked as settled." }
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
            ?: error("This loan no longer exists. Reopen it and try again.")
        val alreadyPaid = paymentRepository.getTotalPaymentsOnce(payment.projectId)
        val availableBalance = (project.price - (alreadyPaid - payment.amountPaid)).coerceAtLeast(0.0)
        check(availableBalance > 0.0) { "This loan has no outstanding balance." }
        check(!project.isBalanceForgiven) { "This loan has been marked as settled." }
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

    fun getAllPayments(): Flow<List<PaymentEntity>> = paymentRepository.getAllPayments()

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