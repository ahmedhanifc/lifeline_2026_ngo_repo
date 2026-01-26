package com.example.responderapp.data.repository

import com.example.responderapp.data.local.dao.PregnancyCaseDao
import com.example.responderapp.data.local.entity.CaseUpdateEntity
import com.example.responderapp.data.local.entity.PregnancyCaseEntity
import com.example.responderapp.data.model.PatientRecord
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface PregnancyCaseRepository {
    fun getAllPatientRecords(): Flow<List<PatientRecord>>
    fun getCaseCount(): Flow<Int>
    fun getPatientRecordById(caseId: String): Flow<PatientRecord?>
    suspend fun createCase(master: PregnancyCaseEntity, initialUpdate: CaseUpdateEntity)
    suspend fun addUpdate(update: CaseUpdateEntity)
}

@Singleton
class PregnancyCaseRepositoryImpl @Inject constructor(
    private val dao: PregnancyCaseDao
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
}