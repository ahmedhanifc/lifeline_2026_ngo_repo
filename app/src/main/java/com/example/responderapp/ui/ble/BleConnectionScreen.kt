package com.example.responderapp.ui.ble

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.responderapp.data.ble.BleConnectionState
import com.example.responderapp.data.ble.BleDevice
import com.example.responderapp.data.local.entity.DistressEventEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen for BLE device discovery and connection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleConnectionScreen(
    onNavigateBack: () -> Unit,
    viewModel: BleConnectionViewModel = hiltViewModel()
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val unacknowledgedCount by viewModel.unacknowledgedCount.collectAsState()
    val latestEvent by viewModel.latestUnacknowledgedEvent.collectAsState()
    val distressEvents by viewModel.distressEvents.collectAsState()
    val context = LocalContext.current
    
    // State to track if we should show the alert dialog
    var showAlertDialog by remember { mutableStateOf(false) }
    var currentAlertEvent by remember { mutableStateOf<DistressEventEntity?>(null) }
    
    // Show alert when a new unacknowledged event arrives
    LaunchedEffect(latestEvent) {
        if (latestEvent != null && latestEvent?.id != currentAlertEvent?.id) {
            currentAlertEvent = latestEvent
            showAlertDialog = true
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.startScan()
        }
    }

    // Request permissions when screen loads
    val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connect Radio") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Badge for unacknowledged events
                    if (unacknowledgedCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge { Text("$unacknowledgedCount") }
                            }
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "Alerts",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Connection Status Card
            ConnectionStatusCard(
                connectionState = connectionState,
                onDisconnect = { viewModel.disconnect() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Scan/Connect Section
            when (connectionState) {
                is BleConnectionState.Idle,
                is BleConnectionState.BluetoothOff,
                is BleConnectionState.NeedPermissions,
                is BleConnectionState.Disconnected,
                is BleConnectionState.Error -> {
                    ScanSection(
                        isScanning = connectionState is BleConnectionState.Scanning,
                        onStartScan = {
                            permissionLauncher.launch(requiredPermissions)
                        },
                        onStopScan = { viewModel.stopScan() }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Device List
                    if (discoveredDevices.isNotEmpty()) {
                        Text(
                            text = "Discovered Devices",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(discoveredDevices) { device ->
                                DeviceCard(
                                    device = device,
                                    onClick = { viewModel.connectToDevice(device) }
                                )
                            }
                        }
                    } else if (connectionState is BleConnectionState.Scanning) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Scanning for devices...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                is BleConnectionState.Scanning -> {
                    ScanSection(
                        isScanning = true,
                        onStartScan = { },
                        onStopScan = { viewModel.stopScan() }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (discoveredDevices.isNotEmpty()) {
                        Text(
                            text = "Discovered Devices",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(discoveredDevices) { device ->
                                DeviceCard(
                                    device = device,
                                    onClick = { viewModel.connectToDevice(device) }
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Scanning for devices...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                is BleConnectionState.Connecting,
                is BleConnectionState.Pairing,
                is BleConnectionState.DiscoveringServices,
                is BleConnectionState.Syncing -> {
                    ConnectingCard(connectionState)
                }

                is BleConnectionState.Listening -> {
                    ListeningCard(onDisconnect = { viewModel.disconnect() })
                }
            }


        }
    }
    
    // SOS Alert Dialog
    if (showAlertDialog && currentAlertEvent != null) {
        SOSAlertDialog(
            event = currentAlertEvent!!,
            onAcknowledge = {
                viewModel.acknowledgeEvent(currentAlertEvent!!.id)
                showAlertDialog = false
            },
            onDismiss = {
                showAlertDialog = false
            }
        )
    }
}

@Composable
private fun SOSAlertDialog(
    event: DistressEventEntity,
    onAcknowledge: () -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val timeString = remember(event.receivedAt) { dateFormat.format(Date(event.receivedAt)) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFFFEBEE),
        icon = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD32F2F)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        },
        title = {
            Text(
                text = "🚨 DISTRESS SIGNAL RECEIVED",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD32F2F),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                val alertText = if (event.userName != null) {
                    "SOS Signal from ${event.userName}!"
                } else {
                    "A caretaker has sent an SOS signal!"
                }
                
                Text(
                    text = alertText,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Location",
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        
                        if (event.userId != null) {
                            Text(
                                text = "User ID: ${event.userId}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        
                        Text(
                            text = "Lat: ${String.format("%.6f", event.latitude)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                        Text(
                            text = "Lon: ${String.format("%.6f", event.longitude)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "Received at: $timeString",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        
                        if (event.rssi != null) {
                            Text(
                                text = "Signal: ${event.rssi} dBm",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAcknowledge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("✓ Acknowledge & Respond", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Dismiss (Keep Alert)")
            }
        }
    )
}
@Composable
private fun ConnectionStatusCard(
    connectionState: BleConnectionState,
    onDisconnect: () -> Unit
) {
    val (statusText, statusColor, icon) = when (connectionState) {
        is BleConnectionState.BluetoothOff -> Triple("Bluetooth Off", Color.Gray, Icons.Default.Bluetooth)
        is BleConnectionState.NeedPermissions -> Triple("Need Permissions", Color.Gray, Icons.Default.Bluetooth)
        is BleConnectionState.Idle -> Triple("Not Connected", Color.Gray, Icons.Default.Bluetooth)
        is BleConnectionState.Scanning -> Triple("Scanning...", MaterialTheme.colorScheme.primary, Icons.Default.BluetoothSearching)
        is BleConnectionState.Connecting -> Triple("Connecting to ${connectionState.deviceName}...", Color(0xFFFFA500), Icons.Default.BluetoothSearching)
        is BleConnectionState.Pairing -> Triple("Pairing...", Color(0xFFFFA500), Icons.Default.BluetoothSearching)
        is BleConnectionState.DiscoveringServices -> Triple("Setting up...", Color(0xFFFFA500), Icons.Default.BluetoothSearching)
        is BleConnectionState.Syncing -> Triple("Syncing...", Color(0xFFFFA500), Icons.Default.BluetoothSearching)
        is BleConnectionState.Listening -> Triple("Connected & Listening", Color(0xFF4CAF50), Icons.Default.BluetoothConnected)
        is BleConnectionState.Disconnected -> Triple("Disconnected: ${connectionState.reason ?: "Unknown"}", Color.Gray, Icons.Default.Close)
        is BleConnectionState.Error -> Triple("Error: ${connectionState.message}", MaterialTheme.colorScheme.error, Icons.Default.Warning)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = statusColor
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Connection Status",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }

            if (connectionState is BleConnectionState.Listening) {
                IconButton(onClick = onDisconnect) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Disconnect",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun ScanSection(
    isScanning: Boolean,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit
) {
    Button(
        onClick = if (isScanning) onStopScan else onStartScan,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isScanning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(
            imageVector = if (isScanning) Icons.Default.Close else Icons.Default.Refresh,
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(if (isScanning) "Stop Scan" else "Scan for Devices")
    }
}

@Composable
private fun DeviceCard(
    device: BleDevice,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Bluetooth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Signal strength indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SignalCellular4Bar,
                    contentDescription = "Signal",
                    modifier = Modifier.size(16.dp),
                    tint = getSignalColor(device.rssi)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${device.rssi} dBm",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ConnectingCard(connectionState: BleConnectionState) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))

            val statusText = when (connectionState) {
                is BleConnectionState.Connecting -> "Connecting to ${connectionState.deviceName}..."
                is BleConnectionState.Pairing -> "Waiting for pairing..."
                is BleConnectionState.DiscoveringServices -> "Discovering services..."
                is BleConnectionState.Syncing -> "Syncing with radio..."
                else -> "Connecting..."
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ListeningCard(onDisconnect: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.BluetoothConnected,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Connected & Listening",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Ready to receive distress signals",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = onDisconnect,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Close, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Disconnect")
            }
        }
    }
}

@Composable
private fun getSignalColor(rssi: Int): Color {
    return when {
        rssi >= -50 -> Color(0xFF4CAF50)  // Excellent
        rssi >= -60 -> Color(0xFF8BC34A)  // Good
        rssi >= -70 -> Color(0xFFFFC107)  // Fair
        rssi >= -80 -> Color(0xFFFF9800)  // Weak
        else -> Color(0xFFF44336)          // Very weak
    }
}
