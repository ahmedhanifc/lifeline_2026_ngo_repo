package com.example.responderapp.ui.cases

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.responderapp.ui.cases.AddCaseViewModel
import com.example.responderapp.ui.cases.AddCaseEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCaseScreen(
    onCaseSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddCaseViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            onCaseSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Pregnancy Case") },
                navigationIcon = {
                    // You can add a back button icon here if needed
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.onEvent(AddCaseEvent.EnteredName(it)) },
                label = { Text("Patient Full Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.age,
                onValueChange = { viewModel.onEvent(AddCaseEvent.EnteredAge(it)) },
                label = { Text("Age (Years)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.stage,
                onValueChange = { viewModel.onEvent(AddCaseEvent.EnteredStage(it)) },
                label = { Text("Pregnancy Stage (e.g. Trimester 1)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.allergies,
                onValueChange = { viewModel.onEvent(AddCaseEvent.EnteredAllergies(it)) },
                label = { Text("Allergies (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.risks,
                onValueChange = { viewModel.onEvent(AddCaseEvent.EnteredRisks(it)) },
                label = { Text("Key Risks (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Button(
                onClick = { viewModel.onEvent(AddCaseEvent.SaveCase) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Save Case Record")
            }
        }
    }
}