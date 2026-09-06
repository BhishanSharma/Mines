package com.genoma.mines.data

import com.genoma.mines.game.Difficulty
import com.genoma.mines.game.GameResultType

/** One row in a history list — the same shape whether it came from Room or Firestore. */
data class GameHistoryItem(
    val difficulty: Difficulty,
    val score: Int,
    val result: GameResultType,
    val durationSeconds: Long,
    val createdAtMillis: Long
)

/** Overall + per-difficulty totals, shown on the authenticated Profile screen only. */
data class UserStatistics(
    val totalGames: Int,
    val totalWins: Int,
    val totalLosses: Int,
    val totalScore: Int,
    val easy: DifficultyStatistics,
    val medium: DifficultyStatistics,
    val hard: DifficultyStatistics
) {
    val winRatio: Int
        get() = if (totalGames == 0) 0 else (totalWins * 100) / totalGames

    companion object {
        val EMPTY = UserStatistics(
            totalGames = 0,
            totalWins = 0,
            totalLosses = 0,
            totalScore = 0,
            easy = DifficultyStatistics(0, 0, 0),
            medium = DifficultyStatistics(0, 0, 0),
            hard = DifficultyStatistics(0, 0, 0)
        )
    }
}

data class DifficultyStatistics(
    val games: Int,
    val wins: Int,
    val score: Int
) {
    val winRatio: Int
        get() = if (games == 0) 0 else (wins * 100) / games
}