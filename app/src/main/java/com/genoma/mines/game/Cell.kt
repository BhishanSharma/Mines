package com.genoma.mines.game

data class Cell(
    val row: Int,
    val column: Int,

    val isMine: Boolean = false,

    val isRevealed: Boolean = false,

    val isFlagged: Boolean = false,

    val adjacentMines: Int = 0
)