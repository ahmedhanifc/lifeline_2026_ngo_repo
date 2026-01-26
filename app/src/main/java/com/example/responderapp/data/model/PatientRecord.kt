package com.example.responderapp.data.model
import androidx.room.Embedded
import androidx.room.Relation
import com.example.responderapp.data.local.entity.CaseUpdateEntity
import com.example.responderapp.data.local.entity.PregnancyCaseEntity
// This class represents a fully hydrated view for the UI list
data class PatientRecord(
    @Embedded val master: PregnancyCaseEntity,
    @Relation(
        parentColumn = "caseId",
        entityColumn = "caseId"
    )
    val updates: List<CaseUpdateEntity>
) {
    // Helper to get the latest update easily
    val current: CaseUpdateEntity?
        get() = updates.maxByOrNull { it.version }
}