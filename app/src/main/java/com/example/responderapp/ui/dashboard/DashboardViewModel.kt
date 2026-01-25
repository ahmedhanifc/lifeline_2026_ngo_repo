package com.example.responderapp.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.responderapp.data.repository.PregnancyCaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    repository: PregnancyCaseRepository
) : ViewModel() {

    // Real count from the database
    val caseCount: StateFlow<Int> = repository.getCaseCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
        
    // FLAG In a real scenario, you might map this list to a UI model
    // For now, we just want to see if data flows.
}