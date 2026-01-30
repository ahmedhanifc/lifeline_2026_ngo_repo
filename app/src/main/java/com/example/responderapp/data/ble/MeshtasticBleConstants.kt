package com.example.responderapp.data.ble

import java.util.UUID

/**
 * BLE UUIDs for the Meshtastic service and characteristics.
 * 
 * Meshtastic uses a custom BLE GATT service with three characteristics:
 * - ToRadio: Write packets to send to the mesh
 * - FromRadio: Read packets received from the mesh (mailbox-style)
 * - FromNum: Notify when new packets are available to read
 */
object MeshtasticBleConstants {
    
    /**
     * Meshtastic BLE Service UUID
     */
    val SERVICE_UUID: UUID = UUID.fromString("6ba1b218-15a8-461f-9fa8-5dcae273eafd")
    
    /**
     * ToRadio characteristic - WRITE
     * Used to send packets to the radio for transmission on the mesh.
     */
    val TORADIO_UUID: UUID = UUID.fromString("f75c76d2-129e-4dad-a1dd-7866124401e7")
    
    /**
     * FromRadio characteristic - READ
     * Used to read packets from the radio's receive mailbox.
     * Must be read repeatedly until empty after each FromNum notification.
     */
    val FROMRADIO_UUID: UUID = UUID.fromString("2c55e69e-4993-11ed-b878-0242ac120002")
    
    /**
     * FromNum characteristic - NOTIFY
     * Notifies when there are new packets available to read from FromRadio.
     * When notified, drain FromRadio until it returns empty.
     */
    val FROMNUM_UUID: UUID = UUID.fromString("ed9da18c-a800-4f66-a670-aa7547e34453")
    
    /**
     * Client Characteristic Configuration Descriptor UUID
     * Used to enable/disable notifications on FromNum.
     */
    val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    
    /**
     * Recommended MTU size for Meshtastic BLE communication.
     * Larger MTU allows bigger packets to be transferred in a single operation.
     */
    const val RECOMMENDED_MTU: Int = 512
    
    /**
     * Default reconnection delay in milliseconds
     */
    const val RECONNECT_DELAY_MS: Long = 3000
    
    /**
     * Maximum reconnection attempts before giving up
     */
    const val MAX_RECONNECT_ATTEMPTS: Int = 5
}
