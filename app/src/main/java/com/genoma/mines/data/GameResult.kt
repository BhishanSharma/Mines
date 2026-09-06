package com.genoma.mines.data

import com.genoma.mines.game.Difficulty
import com.genoma.mines.game.GameResultType

data class GameResult(
    val difficulty: Difficulty,
    val score: Int,
    val result: GameResultType,
    val durationSeconds: Long,
    val createdAt: Long = System.currentTimeMillis()
)