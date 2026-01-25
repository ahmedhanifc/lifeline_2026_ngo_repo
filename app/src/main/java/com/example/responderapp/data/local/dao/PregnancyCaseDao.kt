package com.example.responderapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.responderapp.data.local.entity.PregnancyCaseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PregnancyCaseDao {
    @Query("SELECT * FROM pregnancy_cases ORDER BY updatedAt DESC")
    fun getAllCases(): Flow<List<PregnancyCaseEntity>>

    @Query("SELECT COUNT(*) FROM pregnancy_cases")
    fun getCaseCount(): Flow<Int>

    @Query("SELECT * FROM pregnancy_cases WHERE caseId = :id")
    suspend fun getCaseById(id: String): PregnancyCaseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCase(caseEntity: PregnancyCaseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCases(cases: List<PregnancyCaseEntity>)
}