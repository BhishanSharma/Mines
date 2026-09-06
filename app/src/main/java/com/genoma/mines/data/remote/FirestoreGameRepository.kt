package com.genoma.mines.data.remote

import com.genoma.mines.data.GameResult
import com.genoma.mines.game.Difficulty
import com.genoma.mines.game.GameResultType
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

class FirestoreGameRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun userDoc(uid: String) = firestore.collection("users").document(uid)

    suspend fun ensureUserDocument(
        uid: String,
        name: String?,
        email: String?,
        photoUrl: String?
    ) {
        val doc = userDoc(uid).get().await()

        if (!doc.exists()) {
            val initial = hashMapOf(
                "name" to name,
                "email" to email,
                "photoUrl" to photoUrl,
                "totalGames" to 0,
                "totalWins" to 0,
                "totalLosses" to 0,
                "totalScore" to 0,
                "easyGames" to 0,
                "easyWins" to 0,
                "easyScore" to 0,
                "mediumGames" to 0,
                "mediumWins" to 0,
                "mediumScore" to 0,
                "hardGames" to 0,
                "hardWins" to 0,
                "hardScore" to 0,
                "createdAt" to FieldValue.serverTimestamp()
            )

            userDoc(uid).set(initial).await()
        }
    }

    suspend fun saveGameResult(uid: String, gameResult: GameResult) {
        val gameId = firestore.collection("users")
            .document(uid)
            .collection("games")
            .document()
            .id

        val gameDocRef = userDoc(uid).collection("games").document(gameId)
        val userDocRef = userDoc(uid)

        val (gamesField, winsField, scoreField) = fieldNamesFor(gameResult.difficulty)
        val isWin = gameResult.result == GameResultType.WIN

        firestore.runTransaction { transaction ->
            val historyDoc = hashMapOf(
                "difficulty" to gameResult.difficulty.name,
                "score" to gameResult.score,
                "result" to gameResult.result.name,
                "duration" to gameResult.durationSeconds,
                "createdAt" to FieldValue.serverTimestamp()
            )
            transaction.set(gameDocRef, historyDoc)

            val updates = mutableMapOf<String, Any>(
                "totalGames" to FieldValue.increment(1),
                "totalScore" to FieldValue.increment(gameResult.score.toLong()),
                gamesField to FieldValue.increment(1),
                scoreField to FieldValue.increment(gameResult.score.toLong())
            )

            if (isWin) {
                updates["totalWins"] = FieldValue.increment(1)
                updates[winsField] = FieldValue.increment(1)
            } else {
                updates["totalLosses"] = FieldValue.increment(1)
            }

            transaction.set(userDocRef, updates, com.google.firebase.firestore.SetOptions.merge())
        }.await()
    }

    suspend fun getStatistics(uid: String): UserStats {
        val snapshot = userDoc(uid).get(Source.SERVER).await()
        return snapshot.toObject(UserStats::class.java) ?: UserStats()
    }

    suspend fun getGameHistory(uid: String, limit: Long = 100): List<GameHistoryEntry> {
        val snapshot = userDoc(uid)
            .collection("games")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()

        return snapshot.documents.mapNotNull { it.toObject(GameHistoryEntry::class.java) }
    }

    private fun fieldNamesFor(difficulty: Difficulty): Triple<String, String, String> {
        return when (difficulty) {
            Difficulty.EASY -> Triple("easyGames", "easyWins", "easyScore")
            Difficulty.MEDIUM -> Triple("mediumGames", "mediumWins", "mediumScore")
            Difficulty.HARD -> Triple("hardGames", "hardWins", "hardScore")
        }
    }
}