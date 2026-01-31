package com.example.responderapp.ui.ble

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.responderapp.data.ble.BleConnectionState
import com.example.responderapp.data.ble.BleDevice
import com.example.responderapp.data.local.entity.DistressEventEntity
import com.example.responderapp.data.repository.DistressEventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for BLE connection screen.
 * Manages BLE scanning, connection, and exposes distress events.
 */
@HiltViewModel
class BleConnectionViewModel @Inject constructor(
    private val repository: DistressEventRepository
) : ViewModel() {

    /**
     * Current BLE connection state
     */
    val connectionState: StateFlow<BleConnectionState> = repository.connectionState

    /**
     * List of discovered BLE devices during scanning
     */
    val discoveredDevices: StateFlow<List<BleDevice>> = repository.discoveredDevices

    /**
     * Count of unacknowledged distress events
     */
    val unacknowledgedCount: StateFlow<Int> = repository.getUnacknowledgedCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    /**
     * All distress events
     */
    val distressEvents: StateFlow<List<DistressEventEntity>> = repository.getAllEvents()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Most recent unacknowledged event for alert display
     */
    val latestUnacknowledgedEvent: StateFlow<DistressEventEntity?> = 
        repository.getMostRecentUnacknowledged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )

    private val _showPermissionDialog = MutableStateFlow(false)
    val showPermissionDialog = _showPermissionDialog.asStateFlow()

    /**
     * Check if Bluetooth is enabled
     */
    fun isBluetoothEnabled(): Boolean = repository.isBluetoothEnabled()

    /**
     * Start scanning for BLE devices
     */
    fun startScan() {
        if (!isBluetoothEnabled()) {
            // Could show dialog to enable Bluetooth
            return
        }
        repository.startScan()
    }

    /**
     * Stop scanning for BLE devices
     */
    fun stopScan() = repository.stopScan()

    /**
     * Connect to a specific BLE device
     */
    fun connectToDevice(device: BleDevice) {
        repository.connect(device)
    }

    /**
     * Disconnect from current device
     */
    fun disconnect() = repository.disconnect()

    /**
     * Acknowledge a distress event
     */
    fun acknowledgeEvent(eventId: Long) {
        viewModelScope.launch {
            repository.acknowledgeEvent(eventId)
        }
    }

    fun setShowPermissionDialog(show: Boolean) {
        _showPermissionDialog.value = show
    }
}
