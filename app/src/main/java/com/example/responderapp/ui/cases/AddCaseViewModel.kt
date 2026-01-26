package com.example.responderapp.ui.cases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.responderapp.data.local.entity.CaseUpdateEntity
import com.example.responderapp.data.local.entity.PregnancyCaseEntity
import com.example.responderapp.data.repository.PregnancyCaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddCaseViewModel @Inject constructor(
    private val repository: PregnancyCaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddCaseUiState())
    val uiState: StateFlow<AddCaseUiState> = _uiState.asStateFlow()

    fun onEvent(event: AddCaseEvent) {
        when (event) {
            is AddCaseEvent.EnteredName -> {
                _uiState.value = _uiState.value.copy(name = event.name)
            }
            is AddCaseEvent.EnteredAge -> {
                _uiState.value = _uiState.value.copy(age = event.age)
            }
            is AddCaseEvent.EnteredStage -> {
                _uiState.value = _uiState.value.copy(stage = event.stage)
            }
            is AddCaseEvent.EnteredAllergies -> {
                _uiState.value = _uiState.value.copy(allergies = event.allergies)
            }
            is AddCaseEvent.EnteredRisks -> {
                _uiState.value = _uiState.value.copy(risks = event.risks)
            }
            AddCaseEvent.SaveCase -> {
                saveCase()
            }
        }
    }

    private fun saveCase() {
        val currentState = _uiState.value
        if (currentState.name.isBlank()) return

        viewModelScope.launch {
            val caseId = UUID.randomUUID().toString()
            val updateId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()

            val master = PregnancyCaseEntity(
                caseId = caseId,
                patientFullName = currentState.name,
                dateOfBirth = now, // Placeholder
                createdAt = now,
                createdBy = "CURRENT_USER_ID", // TODO: Get from Auth
                latestUpdateId = updateId,
                isSynced = false
            )

            val initialUpdate = CaseUpdateEntity(
                updateId = updateId,
                caseId = caseId,
                version = 1,
                capturedAt = now,
                updatedBy = "CURRENT_USER_ID", // TODO: Get from Auth
                isSynced = false,
                pregnancyStage = currentState.stage,
                status = "ACTIVE",
                allergies = currentState.allergies.ifBlank { null },
                keyRisks = currentState.risks.ifBlank { null },
                clinicalNotes = "Initial case creation",
                mediaPath = null,
                latitude = null,
                longitude = null
            )

            repository.createCase(master, initialUpdate)
            _uiState.value = _uiState.value.copy(isSaved = true)
        }
    }
}

data class AddCaseUiState(
    val name: String = "",
    val age: String = "",
    val stage: String = "",
    val allergies: String = "",
    val risks: String = "",
    val isSaved: Boolean = false
)

sealed class AddCaseEvent {
    data class EnteredName(val name: String) : AddCaseEvent()
    data class EnteredAge(val age: String) : AddCaseEvent()
    data class EnteredStage(val stage: String) : AddCaseEvent()
    data class EnteredAllergies(val allergies: String) : AddCaseEvent()
    data class EnteredRisks(val risks: String) : AddCaseEvent()
    object SaveCase : AddCaseEvent()
}