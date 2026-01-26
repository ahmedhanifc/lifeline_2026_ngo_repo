package com.example.responderapp.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.responderapp.ui.records.StatusBadge
import com.example.responderapp.data.model.PatientRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToAddCase: () -> Unit,
    onNavigateToRecords: () -> Unit,
    onNavigateToDetail: (String) -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val primaryBlue = Color(0xFF3B6EB4)
    val caseCount by viewModel.caseCount.collectAsState()
    val recentCases by viewModel.recentCases.collectAsState()
    val syncState by viewModel.syncState.collectAsState()

    if (syncState is SyncState.Error) {
        AlertDialog(
            onDismissRequest = { viewModel.resetSyncState() },
            title = { Text("Sync Failed") },
            text = { Text((syncState as SyncState.Error).message) },
            confirmButton = {
                TextButton(onClick = { viewModel.resetSyncState() }) {
                    Text("OK")
                }
            }
        )
    }

    if (syncState is SyncState.Success) {
        AlertDialog(
            onDismissRequest = { viewModel.resetSyncState() },
            title = { Text("Sync Complete") },
            text = { Text("All records have been successfully synchronized to the cloud.") },
            confirmButton = {
                TextButton(onClick = { viewModel.resetSyncState() }) {
                    Text("OK")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Welcome back,",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray
                    )
                    Text(
                        text = "Responder",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
                IconButton(onClick = { /* TODO: Notifications */ }) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Notifications",
                        tint = primaryBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Feature Grid
            Text(
                text = "Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val features = listOf(
                DashboardFeature("My Records", "$caseCount Local", Icons.Outlined.Folder, primaryBlue, onNavigateToRecords),
                DashboardFeature("Scan Tag", "NFC Read", Icons.Outlined.Nfc, Color(0xFFE91E63)) {},
                DashboardFeature("New Case", "Create", Icons.Filled.Add, Color(0xFF4CAF50), onNavigateToAddCase),
                if (syncState is SyncState.Syncing) {
                    DashboardFeature("Syncing...", "Please wait", Icons.Filled.CloudSync, Color.Gray) {}
                } else {
                    DashboardFeature("Sync Data", "Upload", Icons.Filled.CloudUpload, Color(0xFFFF9800)) { viewModel.syncData() }
                }
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.height(240.dp) // Limit height so updates aren't pushed off screen
            ) {
                items(features) { feature ->
                    FeatureCard(feature)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Recent Updates
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Updates",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onNavigateToRecords) {
                    Text("See All", color = primaryBlue)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            if (recentCases.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No patient records yet.", color = Color.Gray)
                        TextButton(onClick = onNavigateToAddCase) {
                            Text("Create your first case", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(recentCases) { record ->
                        RecentCaseItem(record, onClick = { onNavigateToDetail(record.master.caseId) })
                    }
                }
            }
        }
    }
}

@Composable
fun RecentCaseItem(record: com.example.responderapp.data.model.PatientRecord, onClick: () -> Unit) {
    val master = record.master
    val latest = record.current
    
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFE3F2FD),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF3B6EB4), modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(master.patientFullName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(latest?.pregnancyStage ?: "New Case", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            StatusBadge(latest?.status ?: "ACTIVE")
        }
    }
}

data class DashboardFeature(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

@Composable
fun FeatureCard(feature: DashboardFeature) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clickable { feature.onClick() }
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = feature.icon,
                contentDescription = feature.title,
                tint = feature.color,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    text = feature.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )
                Text(
                    text = feature.subtitle,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}