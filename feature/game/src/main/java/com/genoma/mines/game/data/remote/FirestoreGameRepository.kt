package com.genoma.mines.game.data.remote
import com.genoma.mines.game.data.GameResult
import com.genoma.mines.game.data.local.GuestGame
import com.genoma.mines.game.domain.Difficulty
import com.genoma.mines.game.domain.GameResultType
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import java.util.Date

class FirestoreGameRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun userDoc(uid: String) = firestore.collection("users").document(uid)

    /**
     * Creates this user's Firestore document the first time they ever
     * sign in, and does nothing on every later sign-in.
     *
     * @return `true` if the document was just created — i.e. this
     * account has never been used with the app before — `false` if it
     * already existed.
     */
    suspend fun ensureUserDocument(
        uid: String,
        name: String?,
        email: String?,
        photoUrl: String?
    ): Boolean {
        val doc = userDoc(uid).get().await()

        if (doc.exists()) {
            return false
        }

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
            "coins" to 0,
            "diamonds" to 0,
            "redeemTimestamps" to emptyList<Long>(),
            "createdAt" to FieldValue.serverTimestamp()
        )

        userDoc(uid).set(initial).await()
        return true
    }

    suspend fun saveGameResult(
        uid: String,
        gameResult: GameResult,
        createdAtOverride: Any = FieldValue.serverTimestamp()
    ) {
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
            // Read the current totals first — Firestore transactions
            // require all reads before any writes.
            val snapshot = transaction.get(userDocRef)

            val currentTotalScore = snapshot.getLong("totalScore") ?: 0L
            val currentDifficultyScore = snapshot.getLong(scoreField) ?: 0L

            // Score is floored at zero as it's written, rather than only
            // when displayed. Otherwise a losing streak leaves a negative
            // balance stored server-side, and the next win has to "pay
            // off" that debt before the total starts climbing again —
            // instead it should just start counting up from zero.
            val newTotalScore = (currentTotalScore + gameResult.score).coerceAtLeast(0L)
            val newDifficultyScore = (currentDifficultyScore + gameResult.score).coerceAtLeast(0L)

            val historyDoc = hashMapOf(
                "difficulty" to gameResult.difficulty.name,
                "score" to gameResult.score,
                "result" to gameResult.result.name,
                "duration" to gameResult.durationSeconds,
                "createdAt" to createdAtOverride
            )
            transaction.set(gameDocRef, historyDoc)

            val updates = mutableMapOf<String, Any>(
                "totalGames" to FieldValue.increment(1),
                "totalScore" to newTotalScore,
                gamesField to FieldValue.increment(1),
                scoreField to newDifficultyScore
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

    /**
     * Permanently deletes everything stored for this user: every document
     * in their `games` subcollection, then the user document itself
     * (stats, profile fields, etc).
     *
     * The Firestore mobile SDKs have no recursive delete, so the
     * subcollection is fetched and removed in batches first — a single
     * batch is capped at 500 writes, so large histories are chunked
     * rather than risking an oversized commit.
     */
    suspend fun deleteAllUserData(uid: String) {
        val gamesSnapshot = userDoc(uid)
            .collection("games")
            .get()
            .await()

        gamesSnapshot.documents.chunked(400).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { doc -> batch.delete(doc.reference) }
            batch.commit().await()
        }

        userDoc(uid).delete().await()
    }

    /**
     * Uploads games that were played locally as a guest into this
     * (brand new) account, replaying them in the order they were played.
     *
     * Each game goes through the same [saveGameResult] path a live game
     * would — so totals and per-difficulty aggregates end up exactly as
     * if the games had been played while already signed in, with the
     * same zero-floor behaviour on a losing streak — except the history
     * entry keeps the guest game's original timestamp instead of getting
     * a fresh server timestamp, so game history still shows when it was
     * actually played.
     *
     * Only call this right after [ensureUserDocument] reports the
     * account as brand new — merging guest progress into an account that
     * already has its own history isn't handled here.
     */
    suspend fun migrateGuestGames(uid: String, guestGames: List<GuestGame>) {
        guestGames.sortedBy { it.createdAt }.forEach { game ->
            saveGameResult(
                uid = uid,
                gameResult = GameResult(
                    difficulty = game.difficulty,
                    score = game.score,
                    result = game.result,
                    durationSeconds = game.duration,
                    createdAt = game.createdAt
                ),
                createdAtOverride = Date(game.createdAt)
            )
        }
    }

    private fun fieldNamesFor(difficulty: Difficulty): Triple<String, String, String> {
        return when (difficulty) {
            Difficulty.EASY -> Triple("easyGames", "easyWins", "easyScore")
            Difficulty.MEDIUM -> Triple("mediumGames", "mediumWins", "mediumScore")
            Difficulty.HARD -> Triple("hardGames", "hardWins", "hardScore")
        }
    }
}