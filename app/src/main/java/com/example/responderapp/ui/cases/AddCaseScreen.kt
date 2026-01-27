package com.example.responderapp.ui.cases

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.responderapp.MainActivity
import com.example.responderapp.ui.components.NfcWriteDialog
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
    val context = LocalContext.current
    val mainActivity = remember(context) { 
        if (context is MainActivity) context else null
    }

    // Register NFC callback when dialog is shown
    LaunchedEffect(state.showNfcWriteDialog, state.nfcWriteInProgress) {
        if (state.showNfcWriteDialog && state.nfcWriteInProgress && mainActivity != null) {
            mainActivity.setNfcTagCallback { tag ->
                viewModel.onEvent(AddCaseEvent.OnNfcTagDetected(tag))
            }
        } else if (!state.showNfcWriteDialog && mainActivity != null) {
            mainActivity.setNfcTagCallback(null)
        }
    }

    // Auto-dismiss and navigate after successful save (if user doesn't want to write to NFC)
    LaunchedEffect(state.isSaved) {
        if (state.isSaved && !state.showNfcWriteDialog) {
            // Don't auto-navigate, let user choose to write to NFC
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
            if (!state.isSaved) {
                // Show form fields when not saved
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
            } else {
                // Show success message and NFC write option
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Case saved successfully!",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Patient: ${state.name}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = { viewModel.onEvent(AddCaseEvent.ShowNfcWriteDialog) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Nfc,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Write to NFC Tag")
                        }
                        
                        OutlinedButton(
                            onClick = { onCaseSaved() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Done")
                        }
                    }
                }
            }
        }
    }

    // Show NFC write dialog
    if (state.showNfcWriteDialog) {
        NfcWriteDialog(
            caseData = state.nfcCaseData,
            isWriting = state.nfcWriteInProgress,
            writeSuccess = state.nfcWriteSuccess,
            errorMessage = state.nfcWriteError,
            onDismiss = {
                viewModel.onEvent(AddCaseEvent.DismissNfcWriteDialog)
                if (state.nfcWriteSuccess == true) {
                    onCaseSaved()
                }
            },
            onCancel = {
                viewModel.onEvent(AddCaseEvent.DismissNfcWriteDialog)
            }
        )
        
        // Auto-start NFC write when dialog is shown
        LaunchedEffect(state.showNfcWriteDialog) {
            if (state.showNfcWriteDialog && !state.nfcWriteInProgress && state.nfcWriteSuccess == null) {
                viewModel.onEvent(AddCaseEvent.StartNfcWrite)
            }
        }
    }
}