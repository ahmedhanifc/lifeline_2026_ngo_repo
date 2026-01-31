package com.example.responderapp.ui.cases

import android.nfc.Tag
import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class EditCaseViewModel @Inject constructor(
    private val repository: PregnancyCaseRepository,
    private val nfcManager: NfcManager,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val caseId: String = checkNotNull(savedStateHandle["caseId"])

    private val _uiState = MutableStateFlow(EditCaseUiState())
    val uiState: StateFlow<EditCaseUiState> = _uiState.asStateFlow()

    private var currentMasterEntity: PregnancyCaseEntity? = null
    private var lastUpdateDate: Long = System.currentTimeMillis()

    init {
        loadLastData()
    }

    private fun loadLastData() {
        viewModelScope.launch {
            // Check for passed NFC data
            val nfcDataJson = savedStateHandle.get<String>("nfcData")
            val nfcData = if (nfcDataJson != null) NfcCaseData.fromJson(nfcDataJson) else null
            
            var record = repository.getPatientRecordById(caseId).first()
            
            // Use NFC data if record doesn't exist or NFC data is newer
            // Note: For now, we prioritize NFC data if passed, as the user explicitly clicked "Update" from the read dialog
            if (nfcData != null) {
                if (record == null) {
                    // New case import! Create it in DB so we can add updates to it.
                    createImportedCase(nfcData)
                    // Fetch the newly created record
                    record = repository.getPatientRecordById(caseId).first()
                } else {
                    // Case exists, check if NFC data is newer/relevant?
                    // For now, let's just use it to pre-fill the form values the user sees
                    // effectively overriding the DB values for the UI session
                    _uiState.value = _uiState.value.copy(
                        stage = nfcData.pregnancyStage,
                        notes = nfcData.lastCheckupSummary ?: "",
                        // Other fields might come from DB record below if we merge, 
                        // but sticking to NFC data for these fields ensures user sees what they scanned.
                    )
                }
            }

            record?.let { data ->
                currentMasterEntity = data.master
                val latest = data.current
                lastUpdateDate = latest?.capturedAt ?: System.currentTimeMillis()
                
                // If we didn't use NFC data, or if we want to fill in gaps (like Allergies) from DB
                val baseState = _uiState.value
                
                _uiState.value = _uiState.value.copy(
                    patientName = data.master.patientFullName,
                    stage = if (nfcData != null) nfcData.pregnancyStage else (latest?.pregnancyStage ?: ""),
                    allergies = latest?.allergies ?: "", // Always from DB
                    risks = latest?.keyRisks ?: "", // Always from DB
                    notes = if (nfcData != null) (nfcData.lastCheckupSummary ?: "") else (latest?.clinicalNotes ?: ""),
                    currentVersion = latest?.version ?: 1
                )
            }
        }
    }

    private suspend fun createImportedCase(nfcData: NfcCaseData) {
        val now = System.currentTimeMillis()
        val master = PregnancyCaseEntity(
            caseId = nfcData.caseId,
            patientFullName = nfcData.patientFullName,
            dateOfBirth = nfcData.dateOfBirth,
            createdAt = now,
            createdBy = "IMPORTED",
            latestUpdateId = null,
            isSynced = false
        )
        
        // Create an initial update representing the state on the tag
        val initialUpdate = CaseUpdateEntity(
            updateId = UUID.randomUUID().toString(),
            caseId = nfcData.caseId,
            version = 1, // Assume 1 for import
            capturedAt = nfcData.lastCheckupAt ?: now,
            updatedBy = "IMPORTED",
            isSynced = false,
            pregnancyStage = nfcData.pregnancyStage,
            status = "ACTIVE",
            allergies = null,
            keyRisks = null,
            clinicalNotes = nfcData.lastCheckupSummary,
            mediaPath = null,
            latitude = null,
            longitude = null
        )
        
        repository.createCase(master, initialUpdate)
    }

    fun onEvent(event: EditCaseEvent) {
        when (event) {
            is EditCaseEvent.EnteredStage -> _uiState.value = _uiState.value.copy(stage = event.stage)
            is EditCaseEvent.EnteredAllergies -> _uiState.value = _uiState.value.copy(allergies = event.allergies)
            is EditCaseEvent.EnteredRisks -> _uiState.value = _uiState.value.copy(risks = event.risks)
            is EditCaseEvent.EnteredNotes -> _uiState.value = _uiState.value.copy(notes = event.notes)
            EditCaseEvent.SaveCase -> saveUpdate()
            EditCaseEvent.ShowNfcWriteDialog -> _uiState.value = _uiState.value.copy(showNfcWriteDialog = true)
            EditCaseEvent.DismissNfcWriteDialog -> _uiState.value = _uiState.value.copy(
                showNfcWriteDialog = false,
                nfcWriteInProgress = false,
                nfcWriteSuccess = null,
                nfcWriteError = null
            )
            EditCaseEvent.StartNfcWrite -> startNfcWrite()
            is EditCaseEvent.OnNfcTagDetected -> handleNfcTagDetected(event.tag)
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
            lastUpdateDate = now // Update local tracking
            _uiState.value = _uiState.value.copy(isSaved = true)
        }
    }

    private fun startNfcWrite() {
        if (currentMasterEntity == null) {
            _uiState.value = _uiState.value.copy(nfcWriteError = "Case master data missing")
            return
        }

        val state = _uiState.value
        val nfcData = NfcCaseData(
            caseId = caseId,
            patientFullName = state.patientName,
            dateOfBirth = currentMasterEntity!!.dateOfBirth,
            pregnancyStage = state.stage,
            lastCheckupSummary = state.notes,
            lastCheckupAt = lastUpdateDate,
            lastUpdatedAt = System.currentTimeMillis()
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

data class EditCaseUiState(
    val patientName: String = "",
    val stage: String = "",
    val allergies: String = "",
    val risks: String = "",
    val notes: String = "",
    val currentVersion: Int = 1,
    val isSaved: Boolean = false,
    // NFC Write State
    val showNfcWriteDialog: Boolean = false,
    val nfcWriteInProgress: Boolean = false,
    val nfcCaseData: NfcCaseData? = null,
    val nfcWriteSuccess: Boolean? = null,
    val nfcWriteError: String? = null
)

sealed class EditCaseEvent {
    data class EnteredStage(val stage: String) : EditCaseEvent()
    data class EnteredAllergies(val allergies: String) : EditCaseEvent()
    data class EnteredRisks(val risks: String) : EditCaseEvent()
    data class EnteredNotes(val notes: String) : EditCaseEvent()
    object SaveCase : EditCaseEvent()
    object ShowNfcWriteDialog : EditCaseEvent()
    object DismissNfcWriteDialog : EditCaseEvent()
    object StartNfcWrite : EditCaseEvent()
    data class OnNfcTagDetected(val tag: Tag) : EditCaseEvent()
}
