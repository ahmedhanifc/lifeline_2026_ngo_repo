package com.example.responderapp.ui.dashboard

import android.nfc.Tag
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.responderapp.data.model.NfcCaseData
import com.example.responderapp.data.nfc.NfcManager
import com.example.responderapp.data.repository.PregnancyCaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.responderapp.data.model.PatientRecord
import com.example.responderapp.data.meshtastic.MeshtasticManager

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: PregnancyCaseRepository,
    private val nfcManager: NfcManager,
    private val meshtasticManager: MeshtasticManager

) : ViewModel() {

    // Real count from the database
    val caseCount: StateFlow<Int> = repository.getCaseCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // Latest 3 patient records for the "Recent Updates" section
    val recentCases: StateFlow<List<PatientRecord>> = repository.getAllPatientRecords()
        .map { records -> records.take(3) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
    // FLAG In a real scenario, you might map this list to a UI model
    // For now, we just want to see if data flows.

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState = _syncState.asStateFlow()

    // NFC Read state
    private val _nfcReadState = MutableStateFlow(NfcReadState())
    val nfcReadState = _nfcReadState.asStateFlow()

    fun syncData() {
        viewModelScope.launch {
            _syncState.value = SyncState.Syncing
            try {
                val result = repository.syncAll()
                if (result.isSuccess) {
                    _syncState.value = SyncState.Success
                } else {
                    _syncState.value = SyncState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
                }
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetSyncState() {
        _syncState.value = SyncState.Idle
    }

    fun onNfcReadEvent(event: NfcReadEvent) {
        when (event) {
            is NfcReadEvent.StartNfcRead -> {
                _nfcReadState.value = NfcReadState(
                    showNfcReadDialog = true,
                    nfcReadInProgress = true,
                    nfcReadData = null,
                    nfcReadError = null
                )
            }
            is NfcReadEvent.DismissNfcReadDialog -> {
                _nfcReadState.value = NfcReadState()
            }
            is NfcReadEvent.OnNfcTagDetected -> {
                if (event.tag != null) {
                    handleNfcTagDetected(event.tag)
                } else {
                    // Handle NFC not available/enabled error
                    _nfcReadState.value = _nfcReadState.value.copy(
                        nfcReadInProgress = false,
                        nfcReadError = "NFC is not available or not enabled on this device. Please enable NFC in settings."
                    )
                }
            }
        }
    }

    /**
     * Ensures the case exists in the local database. If not, imports it from the NFC data.
     */
    suspend fun ensureCaseExists(nfcData: NfcCaseData) {
        val record = repository.getPatientRecordById(nfcData.caseId).first()
            
        if (record == null) {
            val now = System.currentTimeMillis()
            val master = com.example.responderapp.data.local.entity.PregnancyCaseEntity(
                caseId = nfcData.caseId,
                patientFullName = nfcData.patientFullName,
                dateOfBirth = nfcData.dateOfBirth,
                createdAt = now,
                createdBy = "IMPORTED",
                latestUpdateId = null,
                isSynced = false
            )
            
            val initialUpdate = com.example.responderapp.data.local.entity.CaseUpdateEntity(
                updateId = java.util.UUID.randomUUID().toString(),
                caseId = nfcData.caseId,
                version = 1,
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
    }

    private fun handleNfcTagDetected(tag: Tag) {
        val state = _nfcReadState.value
        if (!state.nfcReadInProgress) {
            return
        }

        viewModelScope.launch {
            val readData = withContext(Dispatchers.IO) {
                nfcManager.readFromTag(tag)
            }

            _nfcReadState.value = _nfcReadState.value.copy(
                nfcReadInProgress = false,
                nfcReadData = readData,
                nfcReadError = if (readData == null) {
                    "Failed to read medical record from NFC tag. The tag may not contain valid data or may be corrupted."
                } else null
            )
        }
    }
    
    fun getNfcManager(): NfcManager = nfcManager
}

data class NfcReadState(
    val showNfcReadDialog: Boolean = false,
    val nfcReadInProgress: Boolean = false,
    val nfcReadData: NfcCaseData? = null,
    val nfcReadError: String? = null
)

sealed class NfcReadEvent {
    object StartNfcRead : NfcReadEvent()
    object DismissNfcReadDialog : NfcReadEvent()
    data class OnNfcTagDetected(val tag: Tag?) : NfcReadEvent()
}

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    object Success : SyncState()
    data class Error(val message: String) : SyncState()
}