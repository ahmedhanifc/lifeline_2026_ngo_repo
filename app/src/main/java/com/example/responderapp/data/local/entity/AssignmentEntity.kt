package com.example.responderapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assignments")
data class AssignmentEntity(
    @PrimaryKey val assignmentId: String,
    val caseId: String,
    val responderId: String,
    val assignedAt: Long,
    val isActive: Boolean,
    val priority: String
)
