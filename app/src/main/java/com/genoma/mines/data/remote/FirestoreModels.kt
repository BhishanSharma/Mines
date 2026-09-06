package com.genoma.mines.data.remote

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class UserStats(
    val name: String? = null,
    val email: String? = null,
    val photoUrl: String? = null,

    val totalGames: Int = 0,
    val totalWins: Int = 0,
    val totalLosses: Int = 0,
    val totalScore: Int = 0,

    val easyGames: Int = 0,
    val easyWins: Int = 0,
    val easyScore: Int = 0,

    val mediumGames: Int = 0,
    val mediumWins: Int = 0,
    val mediumScore: Int = 0,

    val hardGames: Int = 0,
    val hardWins: Int = 0,
    val hardScore: Int = 0,

    @ServerTimestamp
    val createdAt: Date? = null
) {
    val overallWinRatio: Int
        get() = winRatioOf(totalWins, totalGames)

    val easyStats: DifficultyStats
        get() = DifficultyStats(easyGames, easyWins, easyScore)

    val mediumStats: DifficultyStats
        get() = DifficultyStats(mediumGames, mediumWins, mediumScore)

    val hardStats: DifficultyStats
        get() = DifficultyStats(hardGames, hardWins, hardScore)
}

data class DifficultyStats(
    val games: Int,
    val wins: Int,
    val score: Int
) {
    val winRatio: Int
        get() = winRatioOf(wins, games)
}

private fun winRatioOf(wins: Int, totalGames: Int): Int {
    return if (totalGames == 0) 0 else (wins * 100) / totalGames
}

data class GameHistoryEntry(
    val difficulty: String = "",
    val score: Int = 0,
    val result: String = "",
    val duration: Long = 0,
    @ServerTimestamp
    @get:PropertyName("createdAt")
    @set:PropertyName("createdAt")
    var createdAt: Date? = null
)