package com.genoma.mines.data

import com.genoma.mines.game.BadgeTier
import com.genoma.mines.game.Difficulty
import com.genoma.mines.game.GameResultType

data class AchievementTier(
    val tier: BadgeTier,
    val threshold: Int
)


data class AchievementTrack(
    val id: String,
    val title: String,
    val description: String,
    val currentValue: Int,
    val tiers: List<AchievementTier>
) {
    val achievedTier: BadgeTier?
        get() = tiers.lastOrNull { currentValue >= it.threshold }?.tier
    val nextTier: AchievementTier?
        get() = tiers.firstOrNull { currentValue < it.threshold }
    val progress: Float
        get() {
            val next = nextTier ?: return 1f
            val previousThreshold = tiers
                .lastOrNull { it.threshold <= currentValue }
                ?.threshold ?: 0
            val span = (next.threshold - previousThreshold).coerceAtLeast(1)
            return ((currentValue - previousThreshold).toFloat() / span).coerceIn(0f, 1f)
        }
}

object AchievementCalculator {

    fun calculate(history: List<GameHistoryItem>): List<AchievementTrack> {
        val gamesPlayed = history.size
        val gamesWon = history.count { it.result == GameResultType.WIN }
        // Floored chronologically, same as the Profile screen's Total
        // Score: once a losing streak drags the running total to zero,
        // the next win counts up from zero rather than paying off debt.
        val totalScore = run {
            var running = 0
            for (item in history.sortedBy { it.createdAtMillis }) {
                running = (running + item.score).coerceAtLeast(0)
            }
            running
        }
        val hardWins = history.count {
            it.difficulty == Difficulty.HARD && it.result == GameResultType.WIN
        }
        val bestWinStreak = longestWinStreak(history)

        return listOf(
            AchievementTrack(
                id = "games_played",
                title = "Games Played",
                description = "Play games of Mines, any difficulty, any result.",
                currentValue = gamesPlayed,
                tiers = listOf(
                    AchievementTier(BadgeTier.BRONZE, 5),
                    AchievementTier(BadgeTier.SILVER, 20),
                    AchievementTier(BadgeTier.GOLD, 50),
                    AchievementTier(BadgeTier.PLATINUM, 100),
                    AchievementTier(BadgeTier.DIAMOND, 200)
                )
            ),
            AchievementTrack(
                id = "games_won",
                title = "Games Won",
                description = "Clear the board without hitting a mine.",
                currentValue = gamesWon,
                tiers = listOf(
                    AchievementTier(BadgeTier.BRONZE, 3),
                    AchievementTier(BadgeTier.SILVER, 15),
                    AchievementTier(BadgeTier.GOLD, 40),
                    AchievementTier(BadgeTier.PLATINUM, 80),
                    AchievementTier(BadgeTier.DIAMOND, 150)
                )
            ),
            AchievementTrack(
                id = "win_streak",
                title = "Win Streak",
                description = "String together consecutive wins.",
                currentValue = bestWinStreak,
                tiers = listOf(
                    AchievementTier(BadgeTier.BRONZE, 2),
                    AchievementTier(BadgeTier.SILVER, 4),
                    AchievementTier(BadgeTier.GOLD, 7),
                    AchievementTier(BadgeTier.PLATINUM, 12),
                    AchievementTier(BadgeTier.DIAMOND, 20)
                )
            ),
            AchievementTrack(
                id = "total_score",
                title = "Total Score",
                description = "Rack up points across every game you play.",
                currentValue = totalScore,
                tiers = listOf(
                    AchievementTier(BadgeTier.BRONZE, 500),
                    AchievementTier(BadgeTier.SILVER, 2_500),
                    AchievementTier(BadgeTier.GOLD, 7_500),
                    AchievementTier(BadgeTier.PLATINUM, 20_000),
                    AchievementTier(BadgeTier.DIAMOND, 50_000)
                )
            ),
            AchievementTrack(
                id = "hard_wins",
                title = "Hard Mode Wins",
                description = "Win a game on Hard difficulty.",
                currentValue = hardWins,
                tiers = listOf(
                    AchievementTier(BadgeTier.BRONZE, 1),
                    AchievementTier(BadgeTier.SILVER, 5),
                    AchievementTier(BadgeTier.GOLD, 10),
                    AchievementTier(BadgeTier.PLATINUM, 20),
                    AchievementTier(BadgeTier.DIAMOND, 40)
                )
            )
        )
    }

    private fun longestWinStreak(history: List<GameHistoryItem>): Int {
        val chronological = history.sortedBy { it.createdAtMillis }

        var longest = 0
        var current = 0

        chronological.forEach { item ->
            if (item.result == GameResultType.WIN) {
                current += 1
                longest = maxOf(longest, current)
            } else {
                current = 0
            }
        }

        return longest
    }
}