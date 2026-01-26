package com.example.responderapp.ui.records
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.responderapp.data.model.PatientRecord
import com.example.responderapp.data.repository.PregnancyCaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
@HiltViewModel
class RecordsViewModel @Inject constructor(
    repository: PregnancyCaseRepository
) : ViewModel() {
    val allCases: StateFlow<List<PatientRecord>> = repository.getAllPatientRecords()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}