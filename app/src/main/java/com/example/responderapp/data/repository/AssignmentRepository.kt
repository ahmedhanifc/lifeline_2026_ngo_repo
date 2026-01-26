package com.example.responderapp.data.repository

import com.example.responderapp.data.local.dao.AssignmentDao
import com.example.responderapp.data.local.entity.AssignmentEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface AssignmentRepository {
    fun getActiveAssignments(): Flow<List<AssignmentEntity>>
    suspend fun syncAssignments(): Result<Unit>
}

@Singleton
class AssignmentRepositoryImpl @Inject constructor(
    private val assignmentDao: AssignmentDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : AssignmentRepository {

    override fun getActiveAssignments(): Flow<List<AssignmentEntity>> {
        val userId = auth.currentUser?.uid ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return assignmentDao.getActiveAssignmentsForResponder(userId)
    }

    override suspend fun syncAssignments(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = auth.currentUser?.uid ?: return@withContext Result.failure(Exception("Not logged in"))

            // Fetch active assignments for this responder
            val snapshot = firestore.collection("assignments")
                .whereEqualTo("responderId", userId)
                .whereEqualTo("isActive", true)
                .get()
                .await()

            val assignments = snapshot.documents.mapNotNull { doc ->
                val caseId = doc.getString("caseId")
                val assignedAt = doc.getLong("assignedAt") ?: System.currentTimeMillis()
                val priority = doc.getString("priority") ?: "MEDIUM"
                
                if (caseId != null) {
                    AssignmentEntity(
                        assignmentId = doc.id,
                        caseId = caseId,
                        responderId = userId,
                        assignedAt = assignedAt,
                        isActive = true,
                        priority = priority
                    )
                } else null
            }

            // Update local DB
            // First clear old assignments for this responder to ensure we don't keep stale ones?
            // Or just insert/replace? If an assignment became inactive, the query wouldn't return it.
            // If we only insert, we might keep assignments that are no longer active/assigned.
            // Best practice: Clear assignments for this responder and re-insert.
            // But we must be careful not to wipe if the fetch failed (handled by try-catch).
            
            assignmentDao.clearAssignmentsForResponder(userId)
            if (assignments.isNotEmpty()) {
                assignmentDao.insertAssignments(assignments)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
