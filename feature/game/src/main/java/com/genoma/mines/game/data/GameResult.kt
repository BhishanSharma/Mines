package com.genoma.mines.game.data
import com.genoma.mines.game.domain.Difficulty
import com.genoma.mines.game.domain.GameResultType

data class GameResult(
    val difficulty: Difficulty,
    val score: Int,
    val result: GameResultType,
    val durationSeconds: Long,
    val createdAt: Long = System.currentTimeMillis()
)