package com.example.responderapp.data.local.entity
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "pregnancy_cases")
data class PregnancyCaseEntity(
    @PrimaryKey val caseId: String, // UUID
    val patientFullName: String,
    val dateOfBirth: Long,
    val createdAt: Long,
    val createdBy: String,
    
    // Performance Optimization: Cache these so we don't always need a JOIN
    val latestUpdateId: String?, 
    val isSynced: Boolean = false
)