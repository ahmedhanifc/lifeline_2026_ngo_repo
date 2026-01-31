package com.example.responderapp.data.model

import org.json.JSONObject

/**
 * Data class representing the NFC payload structure for pregnancy case records.
 * This matches the structure specified in database.md for NFC tags.
 */
data class NfcCaseData(
    val caseId: String,
    val patientFullName: String,
    val dateOfBirth: Long,
    val pregnancyStage: String,
    val lastCheckupSummary: String?,
    val lastCheckupAt: Long?,
    val lastUpdatedAt: Long
) {
    /**
     * Serialize to JSON string for NFC storage
     */
    fun toJson(): String {
        val json = JSONObject().apply {
            put("caseId", caseId)
            put("patientFullName", patientFullName)
            put("dateOfBirth", dateOfBirth)
            put("pregnancyStage", pregnancyStage)
            put("lastCheckupSummary", lastCheckupSummary ?: JSONObject.NULL)
            put("lastCheckupAt", lastCheckupAt ?: JSONObject.NULL)
            put("lastUpdatedAt", lastUpdatedAt)
            put("version", 1) // Format version for future compatibility
        }
        return json.toString()
    }

    companion object {
        /**
         * Deserialize from JSON string read from NFC tag
         */
        fun fromJson(jsonString: String): NfcCaseData? {
            return try {
                val json = JSONObject(jsonString)
                NfcCaseData(
                    caseId = json.getString("caseId"),
                    patientFullName = json.getString("patientFullName"),
                    dateOfBirth = json.getLong("dateOfBirth"),
                    pregnancyStage = json.getString("pregnancyStage"),
                    lastCheckupSummary = if (json.isNull("lastCheckupSummary")) null else json.getString("lastCheckupSummary"),
                    lastCheckupAt = if (json.isNull("lastCheckupAt")) null else json.getLong("lastCheckupAt"),
                    lastUpdatedAt = json.getLong("lastUpdatedAt")
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
