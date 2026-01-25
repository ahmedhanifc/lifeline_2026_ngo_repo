package com.example.responderapp.ui.cases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
        if (currentState.name.isBlank()) return // Basic validation

        viewModelScope.launch {
            val newCase = PregnancyCaseEntity(
                caseId = UUID.randomUUID().toString(),
                patientFullName = currentState.name,
                dateOfBirth = System.currentTimeMillis(), // Placeholder: In real app, calculate from age/datepicker
                pregnancyStage = currentState.stage,
                allergies = currentState.allergies.ifBlank { null },
                keyRisks = currentState.risks.ifBlank { null },
                lastCheckupSummary = null,
                lastCheckupAt = null,
                status = "ACTIVE",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repository.insertCase(newCase)
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