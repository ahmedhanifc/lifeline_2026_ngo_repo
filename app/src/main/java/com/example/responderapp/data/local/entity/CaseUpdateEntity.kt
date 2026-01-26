package com.example.responderapp.data.local.entity
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
@Entity(
    tableName = "case_updates",
    foreignKeys = [
        ForeignKey(
            entity = PregnancyCaseEntity::class,
            parentColumns = ["caseId"],
            childColumns = ["caseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["caseId"])]
)
data class CaseUpdateEntity(
    @PrimaryKey
    val updateId: String, // UUID
    val caseId: String,   // Links to Master
    // Version Control
    val version: Int,
    val capturedAt: Long,
    val updatedBy: String, // Responder ID
    val isSynced: Boolean = false,
    // Clinical Diffs
    val pregnancyStage: String, // e.g., "Trimester 1"
    val status: String,         // "ACTIVE", "RISK", "CLOSED", etc.
    val allergies: String?,     // Explicit field
    val keyRisks: String?,      // Explicit field
    val clinicalNotes: String?, // General observation summary
    
    // Metadata
    val mediaPath: String?,     // Local path to photos
    val latitude: Double?,
    val longitude: Double?
)