package com.example.responderapp.ui.cases

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCaseScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EditCaseViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            onSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Update Case: ${state.patientName}", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Updating Clinical Info (v${state.currentVersion + 1})",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )

            OutlinedTextField(
                value = state.stage,
                onValueChange = { viewModel.onEvent(EditCaseEvent.EnteredStage(it)) },
                label = { Text("Pregnancy Stage") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.allergies,
                onValueChange = { viewModel.onEvent(EditCaseEvent.EnteredAllergies(it)) },
                label = { Text("Allergies") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.risks,
                onValueChange = { viewModel.onEvent(EditCaseEvent.EnteredRisks(it)) },
                label = { Text("Key Risks") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            OutlinedTextField(
                value = state.notes,
                onValueChange = { viewModel.onEvent(EditCaseEvent.EnteredNotes(it)) },
                label = { Text("Clinical Notes / Observations") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { viewModel.onEvent(EditCaseEvent.SaveCase) },
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                Text("Save New Update Record", fontWeight = FontWeight.Bold)
            }
        }
    }
}
