package com.example.responderapp.ui.cases

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.responderapp.data.local.entity.CaseUpdateEntity
import com.example.responderapp.data.repository.PregnancyCaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class EditCaseViewModel @Inject constructor(
    private val repository: PregnancyCaseRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val caseId: String = checkNotNull(savedStateHandle["caseId"])

    private val _uiState = MutableStateFlow(EditCaseUiState())
    val uiState: StateFlow<EditCaseUiState> = _uiState.asStateFlow()

    init {
        loadLastData()
    }

    private fun loadLastData() {
        viewModelScope.launch {
            val record = repository.getPatientRecordById(caseId).first()
            record?.let { data ->
                val latest = data.current
                _uiState.value = _uiState.value.copy(
                    patientName = data.master.patientFullName,
                    stage = latest?.pregnancyStage ?: "",
                    allergies = latest?.allergies ?: "",
                    risks = latest?.keyRisks ?: "",
                    notes = latest?.clinicalNotes ?: "",
                    currentVersion = latest?.version ?: 1
                )
            }
        }
    }

    fun onEvent(event: EditCaseEvent) {
        when (event) {
            is EditCaseEvent.EnteredStage -> _uiState.value = _uiState.value.copy(stage = event.stage)
            is EditCaseEvent.EnteredAllergies -> _uiState.value = _uiState.value.copy(allergies = event.allergies)
            is EditCaseEvent.EnteredRisks -> _uiState.value = _uiState.value.copy(risks = event.risks)
            is EditCaseEvent.EnteredNotes -> _uiState.value = _uiState.value.copy(notes = event.notes)
            EditCaseEvent.SaveCase -> saveUpdate()
        }
    }

    private fun saveUpdate() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val now = System.currentTimeMillis()
            val updateId = UUID.randomUUID().toString()

            val newUpdate = CaseUpdateEntity(
                updateId = updateId,
                caseId = caseId,
                version = currentState.currentVersion + 1,
                capturedAt = now,
                updatedBy = "CURRENT_USER_ID", // TODO: Auth
                isSynced = false,
                pregnancyStage = currentState.stage,
                status = "ACTIVE", // Or keep last status
                allergies = currentState.allergies.ifBlank { null },
                keyRisks = currentState.risks.ifBlank { null },
                clinicalNotes = currentState.notes,
                mediaPath = null,
                latitude = null,
                longitude = null
            )

            repository.addUpdate(newUpdate)
            _uiState.value = _uiState.value.copy(isSaved = true)
        }
    }
}

data class EditCaseUiState(
    val patientName: String = "",
    val stage: String = "",
    val allergies: String = "",
    val risks: String = "",
    val notes: String = "",
    val currentVersion: Int = 1,
    val isSaved: Boolean = false
)

sealed class EditCaseEvent {
    data class EnteredStage(val stage: String) : EditCaseEvent()
    data class EnteredAllergies(val allergies: String) : EditCaseEvent()
    data class EnteredRisks(val risks: String) : EditCaseEvent()
    data class EnteredNotes(val notes: String) : EditCaseEvent()
    object SaveCase : EditCaseEvent()
}
