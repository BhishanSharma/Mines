package com.genoma.mines.game

data class ScoreConfig(
    val baseScore: Int,
    val targetDurationSeconds: Long,
    val timeBonusPerSecond: Int,
    val pointsPerCell: Int,
    val winBonus: Int,
    val penaltyPerMistake: Int
)