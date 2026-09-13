package com.genoma.mines.game.domain
class ScoreCalculator {

    private fun configFor(difficulty: Difficulty): ScoreConfig = when (difficulty) {
        Difficulty.EASY -> ScoreConfig(
            fastTimeSeconds = 60,
            averageTimeSeconds = 120,
            fastWinPoints = 100,
            averageWinPoints = 70,
            slowWinPoints = 40,
            earlyLossPenalty = -30,
            survivalThreshold = 0.5,
            survivalPoints = 20
        )

        Difficulty.MEDIUM -> ScoreConfig(
            fastTimeSeconds = 90,
            averageTimeSeconds = 180,
            fastWinPoints = 200,
            averageWinPoints = 150,
            slowWinPoints = 90,
            earlyLossPenalty = -50,
            survivalThreshold = 0.5,
            survivalPoints = 40
        )

        Difficulty.HARD -> ScoreConfig(
            fastTimeSeconds = 120,
            averageTimeSeconds = 240,
            fastWinPoints = 350,
            averageWinPoints = 250,
            slowWinPoints = 150,
            earlyLossPenalty = -80,
            survivalThreshold = 0.5,
            survivalPoints = 70
        )
    }
    fun calculate(
        difficulty: Difficulty,
        result: GameResultType,
        elapsedSeconds: Long,
        correctlyRevealedCells: Int,
        totalSafeCells: Int
    ): Int {
        val config = configFor(difficulty)

        if (result == GameResultType.WIN) {
            return when {
                elapsedSeconds <= config.fastTimeSeconds -> config.fastWinPoints
                elapsedSeconds <= config.averageTimeSeconds -> config.averageWinPoints
                else -> config.slowWinPoints
            }
        }

        val survivalRatio = if (totalSafeCells > 0) {
            correctlyRevealedCells.toDouble() / totalSafeCells
        } else {
            0.0
        }

        return if (survivalRatio >= config.survivalThreshold) {
            config.survivalPoints
        } else {
            config.earlyLossPenalty
        }
    }
}