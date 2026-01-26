package com.example.responderapp.data.repository

import com.example.responderapp.data.local.dao.PregnancyCaseDao
import com.example.responderapp.data.local.entity.CaseUpdateEntity
import com.example.responderapp.data.local.entity.PregnancyCaseEntity
import com.example.responderapp.data.model.PatientRecord
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

interface PregnancyCaseRepository {
    fun getAllPatientRecords(): Flow<List<PatientRecord>>
    fun getCaseCount(): Flow<Int>
    fun getPatientRecordById(caseId: String): Flow<PatientRecord?>
    suspend fun createCase(master: PregnancyCaseEntity, initialUpdate: CaseUpdateEntity)
    suspend fun addUpdate(update: CaseUpdateEntity)
    suspend fun syncAll(): Result<Unit>
}

@Singleton
class PregnancyCaseRepositoryImpl @Inject constructor(
    private val dao: PregnancyCaseDao,
    private val firestore: FirebaseFirestore
) : PregnancyCaseRepository {
    override fun getAllPatientRecords(): Flow<List<PatientRecord>> {
        return dao.getAllPatientRecords()
    }
    
    override fun getCaseCount(): Flow<Int> {
        return dao.getCaseCount()
    }

    override fun getPatientRecordById(caseId: String): Flow<PatientRecord?> {
        return dao.getPatientRecordById(caseId)
    }

    override suspend fun createCase(master: PregnancyCaseEntity, initialUpdate: CaseUpdateEntity) {
        dao.createFullCase(master, initialUpdate)
    }
    override suspend fun addUpdate(update: CaseUpdateEntity) {
        dao.insertUpdate(update)
    }

    override suspend fun syncAll(): Result<Unit> {
        return try {
            val unsyncedCases = dao.getUnsyncedCases()
            val unsyncedUpdates = dao.getUnsyncedUpdates()

            if (unsyncedCases.isEmpty() && unsyncedUpdates.isEmpty()) {
                return Result.success(Unit)
            }

            val batch = firestore.batch()

            // 1. Queue Cases
            unsyncedCases.forEach { case ->
                val docRef = firestore.collection("cases").document(case.caseId)
                val data = hashMapOf(
                    "caseId" to case.caseId,
                    "patientFullName" to case.patientFullName,
                    "dateOfBirth" to case.dateOfBirth,
                    "createdAt" to case.createdAt,
                    "createdBy" to case.createdBy,
                    "latestUpdateId" to case.latestUpdateId,
                    "lastSyncedAt" to System.currentTimeMillis()
                )
                batch.set(docRef, data)
            }

            // 2. Queue Updates (Sub-collection)
            unsyncedUpdates.forEach { update ->
                val docRef = firestore.collection("cases")
                    .document(update.caseId)
                    .collection("updates")
                    .document(update.updateId)
                
                val data = hashMapOf(
                    "updateId" to update.updateId,
                    "caseId" to update.caseId,
                    "version" to update.version,
                    "capturedAt" to update.capturedAt,
                    "updatedBy" to update.updatedBy,
                    "pregnancyStage" to update.pregnancyStage,
                    "status" to update.status,
                    "allergies" to update.allergies,
                    "keyRisks" to update.keyRisks,
                    "clinicalNotes" to update.clinicalNotes,
                    "mediaPath" to update.mediaPath,
                    "latitude" to update.latitude,
                    "longitude" to update.longitude
                )
                batch.set(docRef, data)
            }

            // 3. atomic commit
            batch.commit().await()

            // 4. Update local state
            if (unsyncedCases.isNotEmpty()) {
                dao.markCasesAsSynced(unsyncedCases.map { it.caseId })
            }
            if (unsyncedUpdates.isNotEmpty()) {
                dao.markUpdatesAsSynced(unsyncedUpdates.map { it.updateId })
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}