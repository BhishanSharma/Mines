package com.genoma.mines.data.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class FeedbackSubmission(
    val userId: String?,
    val userName: String,
    val userEmail: String,
    val description: String,
    val screenshotCount: Int
)

class FirestoreFeedbackRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun submitFeedback(feedback: FeedbackSubmission) {
        val doc = hashMapOf(
            "userId" to feedback.userId,
            "userName" to feedback.userName,
            "userEmail" to feedback.userEmail,
            "description" to feedback.description,
            "screenshotCount" to feedback.screenshotCount,
            "createdAt" to FieldValue.serverTimestamp()
        )

        firestore.collection("feedback")
            .document()
            .set(doc)
            .await()
    }
}