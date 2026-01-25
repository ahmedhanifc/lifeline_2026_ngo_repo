package com.example.responderapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pregnancy_cases")
data class PregnancyCaseEntity(
    @PrimaryKey val caseId: String, // Likely a UUID
    val patientFullName: String,
    val dateOfBirth: Long, // Epoch timestamp
    val pregnancyStage: String, // e.g., "Trimester 1"
    val allergies: String?,
    val keyRisks: String?,
    val lastCheckupSummary: String?,
    val lastCheckupAt: Long?,
    val status: String, // "ACTIVE", "CLOSED", "LOST_FOLLOWUP"
    val createdAt: Long,
    val updatedAt: Long
)