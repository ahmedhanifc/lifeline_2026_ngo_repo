package com.example.responderapp.data.model

data class Feedback(
    val type: FeedbackType,
    val message: String,
    val userId: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class FeedbackType {
    BUG, SUGGESTION, OTHER
}
