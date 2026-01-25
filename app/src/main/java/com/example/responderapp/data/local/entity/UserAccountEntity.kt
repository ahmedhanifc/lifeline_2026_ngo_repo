package com.example.responderapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_accounts")
data class UserAccountEntity(
    @PrimaryKey val userId: String,
    val fullName: String,
    val email: String,
    val role: String, // "RESPONDER", "SUPERVISOR"
    val isActive: Boolean
)