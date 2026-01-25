package com.example.responderapp.data.repository

import com.example.responderapp.data.local.dao.PregnancyCaseDao
import com.example.responderapp.data.local.entity.PregnancyCaseEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface PregnancyCaseRepository {
    fun getAllCases(): Flow<List<PregnancyCaseEntity>>
    fun getCaseCount(): Flow<Int>
    suspend fun getCaseById(caseId: String): PregnancyCaseEntity?
    suspend fun insertCase(case: PregnancyCaseEntity)
}

@Singleton
class PregnancyCaseRepositoryImpl @Inject constructor(
    private val dao: PregnancyCaseDao
) : PregnancyCaseRepository {

    override fun getAllCases(): Flow<List<PregnancyCaseEntity>> {
        return dao.getAllCases()
    }

    override fun getCaseCount(): Flow<Int> {
        return dao.getCaseCount()
    }

    override suspend fun getCaseById(caseId: String): PregnancyCaseEntity? {
        return dao.getCaseById(caseId)
    }

    override suspend fun insertCase(case: PregnancyCaseEntity) {
        dao.insertCase(case)
    }
}