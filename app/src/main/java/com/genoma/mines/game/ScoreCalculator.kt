package com.genoma.mines.game

class ScoreCalculator {

    private fun configFor(difficulty: Difficulty): ScoreConfig = when (difficulty) {
        Difficulty.EASY -> ScoreConfig(
            baseScore = 100,
            targetDurationSeconds = 120,
            timeBonusPerSecond = 2,
            pointsPerCell = 5,
            winBonus = 50,
            penaltyPerMistake = 10
        )

        Difficulty.MEDIUM -> ScoreConfig(
            baseScore = 250,
            targetDurationSeconds = 180,
            timeBonusPerSecond = 4,
            pointsPerCell = 10,
            winBonus = 150,
            penaltyPerMistake = 20
        )

        Difficulty.HARD -> ScoreConfig(
            baseScore = 500,
            targetDurationSeconds = 240,
            timeBonusPerSecond = 6,
            pointsPerCell = 20,
            winBonus = 300,
            penaltyPerMistake = 30
        )
    }

    fun calculate(
        difficulty: Difficulty,
        result: GameResultType,
        elapsedSeconds: Long,
        correctlyRevealedCells: Int,
        mistakes: Int
    ): Int {
        val config = configFor(difficulty)

        val timeBonus =
            maxOf(0L, config.targetDurationSeconds - elapsedSeconds) *
                    config.timeBonusPerSecond

        val accuracyBonus = correctlyRevealedCells * config.pointsPerCell

        val mistakePenalty = mistakes * config.penaltyPerMistake

        val winBonus = if (result == GameResultType.WIN) config.winBonus else 0

        return maxOf(
            0,
            config.baseScore + timeBonus.toInt() + accuracyBonus + winBonus - mistakePenalty
        )
    }
}