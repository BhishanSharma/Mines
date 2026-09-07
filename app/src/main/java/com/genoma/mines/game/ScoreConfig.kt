package com.genoma.mines.game

data class ScoreConfig(
    val fastTimeSeconds: Long,
    val averageTimeSeconds: Long,
    val fastWinPoints: Int,
    val averageWinPoints: Int,
    val slowWinPoints: Int,
    val earlyLossPenalty: Int,
    val survivalThreshold: Double,
    val survivalPoints: Int
)