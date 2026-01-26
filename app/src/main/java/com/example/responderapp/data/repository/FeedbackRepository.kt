package com.example.responderapp.data.repository

import com.example.responderapp.data.model.Feedback
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface FeedbackRepository {
    suspend fun sendFeedback(feedback: Feedback): Result<Unit>
}

@Singleton
class FeedbackRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : FeedbackRepository {
    override suspend fun sendFeedback(feedback: Feedback): Result<Unit> {
        return try {
            val feedbackData = hashMapOf(
                "type" to feedback.type.name,
                "message" to feedback.message,
                "userId" to feedback.userId,
                "timestamp" to feedback.timestamp
            )
            firestore.collection("feedback")
                .add(feedbackData)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
