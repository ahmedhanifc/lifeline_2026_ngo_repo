package com.example.responderapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.responderapp.data.local.entity.CaseUpdateEntity
import com.example.responderapp.data.local.entity.PregnancyCaseEntity
import com.example.responderapp.data.model.PatientRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface PregnancyCaseDao {
    
    // Transaction ensures we fetch the master and its relations safely
    @Transaction
    @Query("SELECT * FROM pregnancy_cases ORDER BY createdAt DESC")
    fun getAllPatientRecords(): Flow<List<PatientRecord>>
    
    @Transaction
    @Query("SELECT * FROM pregnancy_cases WHERE caseId = :caseId")
    fun getPatientRecordById(caseId: String): Flow<PatientRecord?>

    @Query("SELECT COUNT(*) FROM pregnancy_cases")
    fun getCaseCount(): Flow<Int>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaster(caseEntity: PregnancyCaseEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUpdate(update: CaseUpdateEntity)
    // Helper transaction to create a brand new case + initial screening
    @Transaction
    suspend fun createFullCase(master: PregnancyCaseEntity, initialUpdate: CaseUpdateEntity) {
        insertMaster(master)
        insertUpdate(initialUpdate)
    }

    // Sync Helpers
    @Query("SELECT * FROM pregnancy_cases WHERE isSynced = 0")
    suspend fun getUnsyncedCases(): List<PregnancyCaseEntity>

    @Query("SELECT * FROM case_updates WHERE isSynced = 0")
    suspend fun getUnsyncedUpdates(): List<CaseUpdateEntity>

    @Query("UPDATE pregnancy_cases SET isSynced = 1 WHERE caseId IN (:ids)")
    suspend fun markCasesAsSynced(ids: List<String>)

    @Query("UPDATE case_updates SET isSynced = 1 WHERE updateId IN (:ids)")
    suspend fun markUpdatesAsSynced(ids: List<String>)
}