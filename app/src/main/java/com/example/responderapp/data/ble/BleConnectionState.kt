package com.example.responderapp.data.ble

/**
 * Represents the connection state of the BLE client to a Meshtastic radio.
 */
sealed class BleConnectionState {
    /** Bluetooth is turned off on the device */
    object BluetoothOff : BleConnectionState()
    
    /** Missing required Bluetooth permissions */
    object NeedPermissions : BleConnectionState()
    
    /** Not connected, ready to scan */
    object Idle : BleConnectionState()
    
    /** Scanning for nearby BLE devices */
    object Scanning : BleConnectionState()
    
    /** Connecting to a specific device */
    data class Connecting(val deviceName: String) : BleConnectionState()
    
    /** Android is showing pairing dialog */
    object Pairing : BleConnectionState()
    
    /** Discovering GATT services */
    object DiscoveringServices : BleConnectionState()
    
    /** Performing initial config sync (startConfig + drain) */
    object Syncing : BleConnectionState()
    
    /** Connected and ready to receive packets */
    object Listening : BleConnectionState()
    
    /** Disconnected from device */
    data class Disconnected(val reason: String? = null) : BleConnectionState()
    
    /** An error occurred */
    data class Error(val message: String) : BleConnectionState()
}
