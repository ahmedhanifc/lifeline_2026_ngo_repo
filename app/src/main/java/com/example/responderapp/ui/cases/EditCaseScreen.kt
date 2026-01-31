package com.example.responderapp.ui.cases

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.responderapp.MainActivity
import com.example.responderapp.ui.components.NfcWriteDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCaseScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EditCaseViewModel = hiltViewModel()
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
                viewModel.onEvent(EditCaseEvent.OnNfcTagDetected(tag))
            }
        } else if (!state.showNfcWriteDialog && mainActivity != null) {
            mainActivity.setNfcTagCallback(null)
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
            if (!state.isSaved) {
                // Form View
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
            } else {
                // Success View
                Spacer(Modifier.height(32.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Update Saved!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "You can now update the patient's NFC card with these new details.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = { viewModel.onEvent(EditCaseEvent.ShowNfcWriteDialog) },
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
                            Text("Write Update to NFC Tag")
                        }
                        
                        OutlinedButton(
                            onClick = { onSaved() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Finish without Writing")
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
                viewModel.onEvent(EditCaseEvent.DismissNfcWriteDialog)
                if (state.nfcWriteSuccess == true) {
                    onSaved()
                }
            },
            onCancel = {
                viewModel.onEvent(EditCaseEvent.DismissNfcWriteDialog)
            }
        )
        
        // Auto-start NFC write when dialog is shown
        LaunchedEffect(state.showNfcWriteDialog) {
            if (state.showNfcWriteDialog && !state.nfcWriteInProgress && state.nfcWriteSuccess == null) {
                viewModel.onEvent(EditCaseEvent.StartNfcWrite)
            }
        }
    }
}
