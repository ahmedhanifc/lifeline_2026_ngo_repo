package com.example.responderapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.responderapp.data.local.entity.UserAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM user_accounts LIMIT 1")
    fun getCurrentUser(): Flow<UserAccountEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserAccountEntity)
    
    @Query("DELETE FROM user_accounts")
    suspend fun clearUser()
}