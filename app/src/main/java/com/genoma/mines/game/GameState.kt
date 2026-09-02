package com.genoma.mines.game

enum class GameStatus {
    READY,
    PLAYING,
    PAUSED,
    WON,
    LOST
}

data class GameState(
    val difficulty: Difficulty,
    val cells: List<Cell>,
    val flagsPlaced: Int = 0,
    val elapsedSeconds: Int = 0,
    val status: GameStatus = GameStatus.READY,
    val detonatedCellIndex: Int? = null
)