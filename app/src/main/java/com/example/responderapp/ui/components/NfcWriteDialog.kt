package com.example.responderapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.responderapp.data.model.NfcCaseData

/**
 * Dialog for writing case data to NFC tag
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcWriteDialog(
    caseData: NfcCaseData?,
    isWriting: Boolean,
    writeSuccess: Boolean?,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Write to NFC Tag")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when {
                    writeSuccess == true -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "Successfully written to NFC tag!",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        caseData?.let {
                            Text(
                                text = "Case: ${it.patientFullName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    errorMessage != null -> {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    isWriting -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "Tap the NFC tag to write case data...",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        caseData?.let {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Case Information",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "Patient: ${it.patientFullName}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Stage: ${it.pregnancyStage}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Default.Nfc,
                            contentDescription = "NFC",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Ready to write case data to NFC tag",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Tap 'Start Writing' and then tap your device to an NFC tag",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            when {
                writeSuccess == true -> {
                    Button(onClick = onDismiss) {
                        Text("Done")
                    }
                }
                errorMessage != null -> {
                    Button(onClick = onDismiss) {
                        Text("Close")
                    }
                }
                else -> {
                    // No confirm button when waiting
                }
            }
        },
        dismissButton = {
            if (writeSuccess != true && errorMessage == null) {
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
            }
        }
    )
}
