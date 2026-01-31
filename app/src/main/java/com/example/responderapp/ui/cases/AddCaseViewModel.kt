package com.example.responderapp.ui.cases

import android.nfc.Tag
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.responderapp.data.local.entity.CaseUpdateEntity
import com.example.responderapp.data.local.entity.PregnancyCaseEntity
import com.example.responderapp.data.model.NfcCaseData
import com.example.responderapp.data.nfc.NfcManager
import com.example.responderapp.data.repository.PregnancyCaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddCaseViewModel @Inject constructor(
    private val repository: PregnancyCaseRepository,
    private val nfcManager: NfcManager
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
            AddCaseEvent.ShowNfcWriteDialog -> {
                _uiState.value = _uiState.value.copy(showNfcWriteDialog = true)
            }
            AddCaseEvent.DismissNfcWriteDialog -> {
                _uiState.value = _uiState.value.copy(
                    showNfcWriteDialog = false,
                    nfcWriteInProgress = false,
                    nfcWriteSuccess = null,
                    nfcWriteError = null
                )
            }
            AddCaseEvent.StartNfcWrite -> {
                startNfcWrite()
            }
            is AddCaseEvent.OnNfcTagDetected -> {
                handleNfcTagDetected(event.tag)
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
            _uiState.value = _uiState.value.copy(
                isSaved = true,
                savedCaseId = caseId,
                savedMaster = master,
                savedUpdate = initialUpdate
            )
        }
    }

    private fun startNfcWrite() {
        val state = _uiState.value
        val master = state.savedMaster
        val update = state.savedUpdate

        if (master == null || update == null) {
            _uiState.value = _uiState.value.copy(
                nfcWriteError = "Case data not available"
            )
            return
        }

        // Convert to NfcCaseData
        val nfcData = NfcCaseData(
            caseId = master.caseId,
            patientFullName = master.patientFullName,
            dateOfBirth = master.dateOfBirth,
            pregnancyStage = update.pregnancyStage,
            lastCheckupSummary = update.clinicalNotes,
            lastCheckupAt = update.capturedAt,
            lastUpdatedAt = update.capturedAt
        )

        _uiState.value = _uiState.value.copy(
            nfcWriteInProgress = true,
            nfcCaseData = nfcData,
            nfcWriteSuccess = null,
            nfcWriteError = null
        )
    }

    private fun handleNfcTagDetected(tag: Tag) {
        val state = _uiState.value
        if (!state.nfcWriteInProgress || state.nfcCaseData == null) {
            return
        }

        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                nfcManager.writeToTag(tag, state.nfcCaseData!!)
            }

            _uiState.value = _uiState.value.copy(
                nfcWriteInProgress = false,
                nfcWriteSuccess = success,
                nfcWriteError = if (!success) "Failed to write to NFC tag. Please try again." else null
            )
        }
    }
}

data class AddCaseUiState(
    val name: String = "",
    val age: String = "",
    val stage: String = "",
    val allergies: String = "",
    val risks: String = "",
    val isSaved: Boolean = false,
    val savedCaseId: String? = null,
    val savedMaster: PregnancyCaseEntity? = null,
    val savedUpdate: CaseUpdateEntity? = null,
    val showNfcWriteDialog: Boolean = false,
    val nfcWriteInProgress: Boolean = false,
    val nfcCaseData: NfcCaseData? = null,
    val nfcWriteSuccess: Boolean? = null,
    val nfcWriteError: String? = null
)

sealed class AddCaseEvent {
    data class EnteredName(val name: String) : AddCaseEvent()
    data class EnteredAge(val age: String) : AddCaseEvent()
    data class EnteredStage(val stage: String) : AddCaseEvent()
    data class EnteredAllergies(val allergies: String) : AddCaseEvent()
    data class EnteredRisks(val risks: String) : AddCaseEvent()
    object SaveCase : AddCaseEvent()
    object ShowNfcWriteDialog : AddCaseEvent()
    object DismissNfcWriteDialog : AddCaseEvent()
    object StartNfcWrite : AddCaseEvent()
    data class OnNfcTagDetected(val tag: Tag) : AddCaseEvent()
}