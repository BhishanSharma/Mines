package com.genoma.mines.game

enum class Difficulty(
    val rows: Int,
    val columns: Int,
    val mines: Int
) {
    EASY(rows = 9, columns = 9, mines = 10),
    MEDIUM(rows = 9, columns = 9, mines = 20),
    HARD(rows = 9, columns = 9, mines = 30)
}