package com.example.responderapp.data.meshtastic

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager class for handling Meshtastic communications and parsing distress signals.
 */
@Singleton
class MeshtasticManager @Inject constructor() {

    /**
     * Parses a raw Meshtastic text message to extract SOS information.
     * 
     * Supports two formats:
     * - Format 1: "SOS - Caretaker device | Location: [LAT], [LONG]"
     * - Format 2: "SOS|lat=[LAT]|lon=[LON]"
     */
    fun parseSOSMessage(message: String): MeshtasticSOS? {
        return try {
            // Check if it's an SOS message
            if (!message.startsWith("SOS", ignoreCase = true)) return null

            if (message.contains("Location:")) {
                val sos = parseLocationFormat(message)
                return sos?.copy(
                    userName = extractValue(message, "User:"),
                    userId = extractValue(message, "ID:")
                )
            }

            // Try format 2: "SOS|lat=...|lon=..."
            if (message.contains("lat=") && message.contains("lon=")) {
                return parsePipeFormat(message)
            }

            null
        } catch (e: Exception) {
            // Log error or handle gracefully
            null
        }
    }

    /**
     * Parse format: "SOS - Caretaker device | Location: lat, lon"
     */
    private fun parseLocationFormat(message: String): MeshtasticSOS? {
        val locationPrefix = "Location:"
        if (!message.contains(locationPrefix)) return null

        val locationString = message.substringAfter(locationPrefix).trim()
        val coordinates = locationString.split(",")

        if (coordinates.size >= 2) {
            val lat = coordinates[0].trim().toDoubleOrNull() ?: return null
            val lon = coordinates[1].trim().toDoubleOrNull() ?: return null
            
            if (!validateCoordinates(lat, lon)) return null
            
            return MeshtasticSOS(lat, lon)
        }
        return null
    }

    /**
     * Parse format: "SOS|lat=25.353|lon=51.486"
     */
    private fun parsePipeFormat(message: String): MeshtasticSOS? {
        val parts = message.split("|")
        var lat: Double? = null
        var lon: Double? = null

        for (part in parts) {
            val trimmed = part.trim()
            when {
                trimmed.startsWith("lat=", ignoreCase = true) -> {
                    lat = trimmed.substringAfter("=").toDoubleOrNull()
                }
                trimmed.startsWith("lon=", ignoreCase = true) -> {
                    lon = trimmed.substringAfter("=").toDoubleOrNull()
                }
            }
        }

        if (lat != null && lon != null && validateCoordinates(lat, lon)) {
            return MeshtasticSOS(lat, lon)
        }
        return null
    }

    /**
     * Validate that coordinates are within valid ranges.
     * Latitude: -90 to 90
     * Longitude: -180 to 180
     */
    private fun validateCoordinates(lat: Double, lon: Double): Boolean {
        return lat in -90.0..90.0 && lon in -180.0..180.0
    }

    /**
     * Extract value for a key like "Key: Value" or "Key=Value"
     * Stops at pipe or end of string.
     */
    private fun extractValue(message: String, key: String): String? {
        val idx = message.indexOf(key)
        if (idx == -1) return null
        
        val start = idx + key.length
        val endPipe = message.indexOf("|", start)
        
        val value = if (endPipe != -1) {
            message.substring(start, endPipe)
        } else {
            message.substring(start)
        }
        
        return value.trim().takeIf { it.isNotEmpty() }
    }
}

