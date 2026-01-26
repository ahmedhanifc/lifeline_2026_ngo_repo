package com.example.responderapp.ui.records

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.responderapp.data.local.entity.CaseUpdateEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit, // Callback for editing
    viewModel: PatientDetailViewModel = hiltViewModel()
) {
    val record by viewModel.patientRecord.collectAsState()
    val primaryBlue = Color(0xFF3B6EB4)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Case Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    record?.let { data ->
                        TextButton(onClick = { onEdit(data.master.caseId) }) {
                            Text("EDIT", color = primaryBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF5F7FA)
    ) { padding ->
        record?.let { data ->
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    PatientHeader(data.master.patientFullName, data.current?.status ?: "Unknown")
                }

                item {
                    ClinicalProfile(data.current)
                }

                item {
                    Text(
                        "Clinical History (Audit Trail)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(data.updates.sortedByDescending { it.version }) { update ->
                    AuditLogItem(update)
                }
            }
        } ?: run {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun PatientHeader(name: String, status: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFE3F2FD),
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(32.dp), tint = Color(0xFF3B6EB4))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                StatusBadge(status)
            }
        }
    }
}

@Composable
fun ClinicalProfile(latest: CaseUpdateEntity?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Latest Vitals", style = MaterialTheme.typography.titleSmall, color = Color.Gray)
            Spacer(Modifier.height(8.dp))
            ProfileInfoRow("Stage", latest?.pregnancyStage ?: "N/A")
            ProfileInfoRow("Allergies", latest?.allergies ?: "None reported")
            ProfileInfoRow("Key Risks", latest?.keyRisks ?: "None identified")
            Divider(Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
            Text("Notes", style = MaterialTheme.typography.titleSmall, color = Color.Gray)
            Text(latest?.clinicalNotes ?: "No notes available", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun ProfileInfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
        Text(value, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun AuditLogItem(update: CaseUpdateEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.History, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Version ${update.version}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(formatFullDate(update.capturedAt), color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                }
                Text("Updated by: ${update.updatedBy}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Spacer(Modifier.height(4.dp))
                Text(update.clinicalNotes ?: "Routine checkup", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}


private fun formatFullDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
