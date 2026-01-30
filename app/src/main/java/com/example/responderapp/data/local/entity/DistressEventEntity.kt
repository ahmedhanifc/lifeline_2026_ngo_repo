package com.example.responderapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a distress event received via Meshtastic.
 * 
 * Stores SOS signals with location data for tracking and response.
 */
@Entity(tableName = "distress_events")
data class DistressEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /**
     * Meshtastic node ID that sent the distress signal
     */
    val fromNodeId: Long,
    
    /**
     * Timestamp when the event was received (epoch millis)
     */
    val receivedAt: Long,
    
    /**
     * Latitude of the distress location
     */
    val latitude: Double,
    
    /**
     * Longitude of the distress location
     */
    val longitude: Double,
    
    /**
     * Raw text of the SOS message
     */
    val rawText: String,
    
    /**
     * RSSI (signal strength) of received packet, if available
     */
    val rssi: Int? = null,
    
    /**
     * Signal-to-noise ratio of received packet, if available
     */
    val snr: Float? = null,
    
    /**
     * Whether this distress event has been acknowledged by the responder
     */
    val acknowledged: Boolean = false
)
