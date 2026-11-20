package com.example.loantracker.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.loantracker.data.local.entities.ProjectEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

enum class LoanDetailAction {
    EDIT,
    DELETE,
    ADD_PAYMENT,
    ADD_NOTE
}

@HiltViewModel
class ValViewModel @Inject constructor(): ViewModel() {

    private var _activeProjectId by mutableStateOf<Long>(value = 0)
    val activeProjectId: Long get() = _activeProjectId
    fun setActiveProjectId(projectId: Long){
        this._activeProjectId = projectId
    }

    private var _activeProject by mutableStateOf<ProjectEntity?>(value = null)
    val activeProject: ProjectEntity? get() = _activeProject
    fun setActiveProject(project: ProjectEntity){
        this._activeProject = project
    }
    private var _title by mutableStateOf("Home")
    val title: State<String> = derivedStateOf { _title }
    fun setTitle(title: String){
        this._title = title
    }

    private var _name by mutableStateOf("Customer details")
    val name: String
        get() = _name

    fun setTracing(name: String){
        this._name = name
    }

    var pendingLoanDetailAction by mutableStateOf<LoanDetailAction?>(null)
        private set

    fun requestLoanDetailAction(action: LoanDetailAction) {
        pendingLoanDetailAction = action
    }

    fun consumeLoanDetailAction() {
        pendingLoanDetailAction = null
    }
}