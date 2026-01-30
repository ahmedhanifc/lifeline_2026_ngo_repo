package com.example.responderapp.data.local.dao

import androidx.room.*
import com.example.responderapp.data.local.entity.DistressEventEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for distress events.
 */
@Dao
interface DistressEventDao {
    
    /**
     * Insert a new distress event
     */
    @Insert
    suspend fun insert(event: DistressEventEntity): Long
    
    /**
     * Get all distress events, ordered by most recent first
     */
    @Query("SELECT * FROM distress_events ORDER BY receivedAt DESC")
    fun getAllEvents(): Flow<List<DistressEventEntity>>
    
    /**
     * Get only unacknowledged distress events
     */
    @Query("SELECT * FROM distress_events WHERE acknowledged = 0 ORDER BY receivedAt DESC")
    fun getUnacknowledgedEvents(): Flow<List<DistressEventEntity>>
    
    /**
     * Get the most recent unacknowledged event (for alert display)
     */
    @Query("SELECT * FROM distress_events WHERE acknowledged = 0 ORDER BY receivedAt DESC LIMIT 1")
    fun getMostRecentUnacknowledged(): Flow<DistressEventEntity?>
    
    /**
     * Mark an event as acknowledged
     */
    @Query("UPDATE distress_events SET acknowledged = 1 WHERE id = :eventId")
    suspend fun acknowledge(eventId: Long)
    
    /**
     * Get count of unacknowledged events (for badge display)
     */
    @Query("SELECT COUNT(*) FROM distress_events WHERE acknowledged = 0")
    fun getUnacknowledgedCount(): Flow<Int>
    
    /**
     * Get a specific event by ID
     */
    @Query("SELECT * FROM distress_events WHERE id = :eventId")
    suspend fun getById(eventId: Long): DistressEventEntity?
    
    /**
     * Delete old acknowledged events (cleanup)
     */
    @Query("DELETE FROM distress_events WHERE acknowledged = 1 AND receivedAt < :beforeTimestamp")
    suspend fun deleteOldAcknowledged(beforeTimestamp: Long): Int
}
