package com.example.responderapp.data.repository

import android.net.Uri
import com.example.responderapp.data.local.dao.UserDao
import com.example.responderapp.data.local.entity.UserAccountEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface UserRepository {
    fun getCurrentUser(): Flow<UserAccountEntity?>
    suspend fun updateProfile(name: String, imageUri: Uri?): Result<Unit>
    suspend fun syncProfile(): Result<Unit>
    suspend fun isFirstTimeLogin(): Boolean
    suspend fun completeOnboarding(): Result<Unit>
}

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val userDao: UserDao
) : UserRepository {

    override fun getCurrentUser(): Flow<UserAccountEntity?> {
        return userDao.getCurrentUser()
    }

    override suspend fun updateProfile(name: String, imageUri: Uri?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
            var finalImageUrl: String? = null

            if (imageUri != null) {
                val storageRef = storage.reference.child("users/$userId/profile.jpg")
                storageRef.putFile(imageUri).await()
                finalImageUrl = storageRef.downloadUrl.await().toString()
            }

            val updates = mutableMapOf<String, Any>(
                "fullName" to name
            )
            if (finalImageUrl != null) {
                updates["profilePictureUrl"] = finalImageUrl
            }

            firestore.collection("users").document(userId)
                .update(updates)
                .await()

            syncProfile()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncProfile(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = auth.currentUser?.uid ?: return@withContext Result.failure(Exception("Not logged in"))
            
            val snapshot = firestore.collection("users").document(userId).get().await()
            
            if (snapshot.exists()) {
                val fullName = snapshot.getString("fullName") ?: "Unknown Responder"
                val email = snapshot.getString("email") ?: auth.currentUser?.email ?: ""
                val role = snapshot.getString("role") ?: "RESPONDER"
                val profilePic = snapshot.getString("profilePictureUrl")
                val phone = snapshot.getString("phone")
                val isActive = snapshot.getBoolean("isActive") ?: true
                val lastLoginAt = snapshot.getLong("lastLoginAt")
                
                val entity = UserAccountEntity(
                    userId = userId,
                    fullName = fullName,
                    email = email,
                    role = role,
                    profilePictureUrl = profilePic,
                    isActive = isActive,
                    phone = phone,
                    lastLoginAt = lastLoginAt
                )
                userDao.insertUser(entity)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isFirstTimeLogin(): Boolean = withContext(Dispatchers.IO) {
        val user = userDao.getCurrentUser().firstOrNull()
        // If user exists locally and has no lastLoginAt (or 0), it is first time.
        // If user doesn't exist locally, we need to sync first.
        if (user == null) {
            // Attempt sync
            syncProfile()
            val syncedUser = userDao.getCurrentUser().firstOrNull()
            return@withContext syncedUser?.lastLoginAt == null || syncedUser.lastLoginAt == 0L
        }
        return@withContext user.lastLoginAt == null || user.lastLoginAt == 0L
    }

    override suspend fun completeOnboarding(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
            val currentTime = System.currentTimeMillis()

            // Update Firestore
            firestore.collection("users").document(userId)
                .update("lastLoginAt", currentTime)
                .await()

            // Update Local
            syncProfile() // Re-fetch to update local state fully
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
