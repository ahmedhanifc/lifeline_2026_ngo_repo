package com.example.responderapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.responderapp.data.local.entity.AssignmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssignmentDao {
    @Query("SELECT * FROM assignments WHERE responderId = :responderId AND isActive = 1 ORDER BY priority DESC")
    fun getActiveAssignmentsForResponder(responderId: String): Flow<List<AssignmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignments(assignments: List<AssignmentEntity>)

    @Query("DELETE FROM assignments WHERE responderId = :responderId")
    suspend fun clearAssignmentsForResponder(responderId: String)
}
