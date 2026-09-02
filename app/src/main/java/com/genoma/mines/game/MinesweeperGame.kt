package com.genoma.mines.game

import kotlin.random.Random

class MinesweeperGame(
    val difficulty: Difficulty
) {

    private val rows = difficulty.rows
    private val columns = difficulty.columns
    private val mineCount = difficulty.mines

    private var board: MutableList<Cell> = mutableListOf()

    init {
        createBoard()
    }

    private fun createBoard() {

        board = MutableList(rows * columns) { index ->
            Cell(
                row = index / columns,
                column = index % columns
            )
        }

        placeMines()
        calculateAdjacentMines()
    }

    private fun placeMines() {

        val minePositions = mutableSetOf<Int>()

        while (minePositions.size < mineCount) {
            minePositions.add(
                Random.nextInt(board.size)
            )
        }

        minePositions.forEach { position ->

            board[position] = board[position].copy(
                isMine = true
            )
        }
    }

    private fun calculateAdjacentMines() {

        for (index in board.indices) {

            val cell = board[index]

            if (cell.isMine) {
                continue
            }

            val adjacentMines = getNeighbors(cell)
                .count { it.isMine }

            board[index] = cell.copy(
                adjacentMines = adjacentMines
            )
        }
    }

    private fun getNeighbors(cell: Cell): List<Cell> {

        val neighbors = mutableListOf<Cell>()

        for (rowOffset in -1..1) {
            for (columnOffset in -1..1) {

                if (rowOffset == 0 && columnOffset == 0) {
                    continue
                }

                val neighborRow = cell.row + rowOffset
                val neighborColumn = cell.column + columnOffset

                if (
                    neighborRow in 0 until rows &&
                    neighborColumn in 0 until columns
                ) {

                    val neighborIndex =
                        neighborRow * columns + neighborColumn

                    neighbors.add(board[neighborIndex])
                }
            }
        }

        return neighbors
    }

    fun getBoard(): List<Cell> {
        return board.toList()
    }

    fun reveal(index: Int): Boolean {
        if (index !in board.indices) {
            return false
        }

        val cell = board[index]

        if (cell.isRevealed || cell.isFlagged) {
            return false
        }

        // If it's a mine, reveal only that mine.
        if (cell.isMine) {
            board[index] = cell.copy(
                isRevealed = true
            )
            return true
        }

        // Reveal the selected cell.
        board[index] = cell.copy(
            isRevealed = true
        )

        // If there are no adjacent mines,
        // reveal the connected empty area.
        if (cell.adjacentMines == 0) {
            revealEmptyArea(index)
        }

        return false
    }

    private fun revealEmptyArea(startIndex: Int) {
        val queue = ArrayDeque<Int>()
        val visited = mutableSetOf<Int>()

        queue.addLast(startIndex)

        while (queue.isNotEmpty()) {
            val currentIndex = queue.removeFirst()

            if (!visited.add(currentIndex)) {
                continue
            }

            val currentCell = board[currentIndex]

            if (
                currentCell.isMine ||
                currentCell.isFlagged
            ) {
                continue
            }

            if (!currentCell.isRevealed) {
                board[currentIndex] = currentCell.copy(
                    isRevealed = true
                )
            }

            // Only continue expanding from empty cells.
            if (currentCell.adjacentMines != 0) {
                continue
            }

            val neighbors = getNeighbors(board[currentIndex])

            neighbors.forEach { neighbor ->

                if (
                    !neighbor.isMine &&
                    !neighbor.isFlagged &&
                    !neighbor.isRevealed
                ) {
                    val neighborIndex =
                        neighbor.row * columns + neighbor.column

                    queue.addLast(neighborIndex)
                }
            }
        }
    }

    fun toggleFlag(index: Int): Boolean {

        if (index !in board.indices) {
            return false
        }

        val cell = board[index]

        if (cell.isRevealed) {
            return false
        }

        board[index] = cell.copy(
            isFlagged = !cell.isFlagged
        )

        return true
    }

    fun revealAllMines() {

        board = board.map { cell ->
            if (cell.isMine) {
                cell.copy(isRevealed = true)
            } else {
                cell
            }
        }.toMutableList()
    }

    fun isWon(): Boolean {

        return board.none {
            !it.isMine && !it.isRevealed
        }
    }

    fun getFlagsPlaced(): Int {
        return board.count { it.isFlagged }
    }

    fun getMineCount(): Int {
        return board.count { it.isMine }
    }
}