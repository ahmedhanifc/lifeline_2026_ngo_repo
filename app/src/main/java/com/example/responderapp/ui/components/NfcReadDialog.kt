package com.example.responderapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.responderapp.data.model.NfcCaseData
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dialog for reading and displaying medical record data from NFC tag
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcReadDialog(
    isReading: Boolean,
    readData: NfcCaseData?,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Read NFC Tag")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when {
                    readData != null -> {
                        // Success - Display medical record
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "Medical Record Read Successfully",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MedicalRecordField(
                                    label = "Patient Name",
                                    value = readData.patientFullName
                                )
                                
                                MedicalRecordField(
                                    label = "Date of Birth",
                                    value = formatDate(readData.dateOfBirth)
                                )
                                
                                MedicalRecordField(
                                    label = "Pregnancy Stage",
                                    value = readData.pregnancyStage
                                )
                                
                                if (!readData.allergies.isNullOrBlank()) {
                                    MedicalRecordField(
                                        label = "Allergies",
                                        value = readData.allergies
                                    )
                                }
                                
                                if (!readData.keyRisks.isNullOrBlank()) {
                                    MedicalRecordField(
                                        label = "Key Risks",
                                        value = readData.keyRisks
                                    )
                                }
                                
                                if (!readData.lastCheckupSummary.isNullOrBlank()) {
                                    MedicalRecordField(
                                        label = "Last Checkup Summary",
                                        value = readData.lastCheckupSummary
                                    )
                                }
                                
                                if (readData.lastCheckupAt != null) {
                                    MedicalRecordField(
                                        label = "Last Checkup Date",
                                        value = formatDate(readData.lastCheckupAt)
                                    )
                                }
                                
                                MedicalRecordField(
                                    label = "Last Updated",
                                    value = formatDate(readData.lastUpdatedAt)
                                )
                            }
                        }
                    }
                    errorMessage != null -> {
                        // Error state
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
                    isReading -> {
                        // Reading state
                        CircularProgressIndicator(
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "Tap the NFC tag to read medical record...",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Hold your device close to the NFC tag",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        // Initial state
                        Icon(
                            imageVector = Icons.Default.Nfc,
                            contentDescription = "NFC",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Ready to read NFC tag",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        },
        confirmButton = {
            when {
                readData != null || errorMessage != null -> {
                    Button(onClick = onDismiss) {
                        Text(if (readData != null) "Close" else "OK")
                    }
                }
                else -> {
                    // No confirm button when waiting
                }
            }
        },
        dismissButton = {
            if (readData == null && errorMessage == null) {
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
private fun MedicalRecordField(
    label: String,
    value: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Format timestamp to readable date string
 */
private fun formatDate(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}
