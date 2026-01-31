package com.example.responderapp.data.meshtastic

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Data class representing an SOS signal received via Meshtastic.
 */
@Parcelize
data class MeshtasticSOS(
    val latitude: Double,
    val longitude: Double,
    val userName: String? = null,
    val userId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable
