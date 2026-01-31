package com.example.responderapp.data.repository

import android.util.Log
import com.example.responderapp.data.ble.BleConnectionState
import com.example.responderapp.data.ble.BleDevice
import com.example.responderapp.data.ble.MeshtasticBleClient
import com.example.responderapp.data.local.dao.DistressEventDao
import com.example.responderapp.data.local.entity.DistressEventEntity
import com.example.responderapp.data.meshtastic.MeshtasticManager
import com.example.responderapp.data.meshtastic.MeshtasticSOS
import com.geeksville.mesh.MeshProtos.MeshPacket
import com.geeksville.mesh.Portnums.PortNum
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing Meshtastic BLE connection and distress events.
 * 
 * Bridges the BLE client with the database, processing incoming packets
 * and storing distress events.
 */
@Singleton
class DistressEventRepository @Inject constructor(
    private val bleClient: MeshtasticBleClient,
    private val distressEventDao: DistressEventDao,
    private val meshtasticManager: MeshtasticManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _newDistressEvent = MutableSharedFlow<DistressEventEntity>(extraBufferCapacity = 16)
    val newDistressEvent: SharedFlow<DistressEventEntity> = _newDistressEvent.asSharedFlow()
    
    init {
        // Listen to incoming packets and process them
        Log.d(TAG, "═══════════════════════════════════════════════════════════")
        Log.d(TAG, "🚀 DistressEventRepository INITIALIZED")
        Log.d(TAG, "🔗 Subscribing to bleClient.incomingPackets SharedFlow...")
        Log.d(TAG, "═══════════════════════════════════════════════════════════")
        scope.launch {
            Log.d(TAG, "📡 Collection coroutine started - waiting for packets...")
            bleClient.incomingPackets.collect { packet ->
                Log.d(TAG, "═══════════════════════════════════════════════════════════")
                Log.d(TAG, "📥📥📥 PACKET RECEIVED IN REPOSITORY!")
                Log.d(TAG, "📥 From: ${packet.from}, To: ${packet.to}")
                Log.d(TAG, "📥 Has decoded: ${packet.hasDecoded()}")
                Log.d(TAG, "═══════════════════════════════════════════════════════════")
                processPacket(packet)
            }
        }
    }
    
    /**
     * BLE connection state
     */
    val connectionState: StateFlow<BleConnectionState> = bleClient.connectionState
    
    /**
     * Discovered BLE devices during scan
     */
    val discoveredDevices: StateFlow<List<BleDevice>> = bleClient.discoveredDevices
    
    /**
     * Check if Bluetooth is enabled
     */
    fun isBluetoothEnabled(): Boolean = bleClient.isBluetoothEnabled()
    
    /**
     * Start scanning for BLE devices
     */
    fun startScan() = bleClient.startScan()
    
    /**
     * Stop scanning for BLE devices
     */
    fun stopScan() = bleClient.stopScan()
    
    /**
     * Connect to a BLE device
     */
    fun connect(device: BleDevice) = bleClient.connect(device)
    
    /**
     * Disconnect from current device
     */
    fun disconnect() = bleClient.disconnect()
    
    /**
     * Get all distress events
     */
    fun getAllEvents(): Flow<List<DistressEventEntity>> = distressEventDao.getAllEvents()
    
    /**
     * Get unacknowledged distress events
     */
    fun getUnacknowledgedEvents(): Flow<List<DistressEventEntity>> = 
        distressEventDao.getUnacknowledgedEvents()
    
    /**
     * Get count of unacknowledged events
     */
    fun getUnacknowledgedCount(): Flow<Int> = distressEventDao.getUnacknowledgedCount()
    
    /**
     * Get most recent unacknowledged event
     */
    fun getMostRecentUnacknowledged(): Flow<DistressEventEntity?> = 
        distressEventDao.getMostRecentUnacknowledged()
    
    /**
     * Acknowledge a distress event
     */
    suspend fun acknowledgeEvent(eventId: Long) = distressEventDao.acknowledge(eventId)
    
    /**
     * Process an incoming MeshPacket
     */
    private suspend fun processPacket(packet: MeshPacket) {
        Log.d(TAG, "🔍 Processing packet from node ${packet.from}")
        
        // Check if it's a text message
        if (!packet.hasDecoded()) {
            Log.d(TAG, "⏭️ Packet has no decoded payload - skipping")
            return
        }
        
        val data = packet.decoded
        Log.d(TAG, "📋 Packet portnum: ${data.portnum} (expected: ${PortNum.TEXT_MESSAGE_APP})")
        
        if (data.portnum != PortNum.TEXT_MESSAGE_APP) {
            Log.d(TAG, "⏭️ Not a text message - skipping")
            return
        }
        
        // Try to decode the payload as UTF-8 text
        val payloadText = try {
            data.payload.toStringUtf8()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to decode payload as UTF-8", e)
            return
        }
        
        Log.d(TAG, "💬 Text message received: '$payloadText'")
        
        // Try to parse as SOS message
        val sos = meshtasticManager.parseSOSMessage(payloadText)
        if (sos == null) {
            Log.d(TAG, "⏭️ Not an SOS message - skipping")
            return
        }
        
        Log.d(TAG, "🆘 SOS DETECTED! Lat: ${sos.latitude}, Lon: ${sos.longitude}")
        
        // Create and store distress event
        val event = DistressEventEntity(
            fromNodeId = packet.from.toLong(),
            receivedAt = System.currentTimeMillis(),
            latitude = sos.latitude,
            longitude = sos.longitude,
            userName = sos.userName,
            userId = sos.userId,
            rawText = payloadText,
            rssi = packet.rxRssi.takeIf { it != 0 },
            snr = packet.rxSnr.takeIf { it != 0f }
        )
        
        val insertedId = distressEventDao.insert(event)
        val insertedEvent = event.copy(id = insertedId)
        
        Log.d(TAG, "✅ SOS event saved to database with ID: $insertedId")
        
        // Emit for real-time updates
        _newDistressEvent.emit(insertedEvent)
        Log.d(TAG, "📢 SOS event emitted to UI")
    }
    
    companion object {
        private const val TAG = "DistressRepo"
    }
}
