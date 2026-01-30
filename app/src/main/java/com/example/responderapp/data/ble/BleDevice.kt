package com.example.responderapp.data.ble

import android.bluetooth.BluetoothDevice

/**
 * Represents a discovered BLE device during scanning.
 */
data class BleDevice(
    val name: String?,
    val address: String,
    val rssi: Int,
    val device: BluetoothDevice
) {
    /**
     * Display name for the device (or address if name is unknown)
     */
    val displayName: String
        get() = name?.takeIf { it.isNotBlank() } ?: address
}
