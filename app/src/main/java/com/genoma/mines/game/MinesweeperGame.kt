package com.genoma.mines.game

import kotlin.random.Random

class MinesweeperGame(
    difficulty: Difficulty
) {

    private val rows = difficulty.rows
    private val columns = difficulty.columns
    private val mineCount = difficulty.mines

    private var board: MutableList<Cell> = mutableListOf()

    /**
     * Mines aren't placed until the first reveal. This guarantees the
     * opening move can never be a mine, and — since the first click's
     * whole neighborhood is excluded from mine placement — it also
     * guarantees the first reveal opens up a small area rather than
     * landing on an isolated numbered cell.
     */
    private var minesPlaced = false

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
    }

    private fun placeMines(excludingIndex: Int) {

        val excludedCell = board[excludingIndex]
        val safeZone = (getNeighbors(excludedCell).map { it.row * columns + it.column } + excludingIndex)
            .toSet()

        var candidates = board.indices.filterNot { it in safeZone }

        // Fallback for boards where mines can't all fit outside the safe
        // zone (e.g. a very small custom difficulty) — fall back to only
        // excluding the tapped cell itself, so the game can still start.
        if (candidates.size < mineCount) {
            candidates = board.indices.filterNot { it == excludingIndex }
        }

        val minePositions = candidates.shuffled(Random).take(mineCount).toSet()

        minePositions.forEach { position ->
            board[position] = board[position].copy(isMine = true)
        }

        calculateAdjacentMines()
        minesPlaced = true
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

    /**
     * Reveals [index]. Returns the index of the mine that was detonated,
     * or null if the reveal was safe (including no-ops on cells that are
     * already revealed or flagged).
     */
    fun reveal(index: Int): Int? {
        if (index !in board.indices) {
            return null
        }

        if (!minesPlaced) {
            placeMines(excludingIndex = index)
        }

        val cell = board[index]

        if (cell.isRevealed || cell.isFlagged) {
            return null
        }

        if (cell.isMine) {
            board[index] = cell.copy(isRevealed = true)
            return index
        }

        board[index] = cell.copy(isRevealed = true)

        if (cell.adjacentMines == 0) {
            revealEmptyArea(index)
        }

        return null
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

    /**
     * Chording: tapping an already-revealed numbered cell whose flagged
     * neighbor count matches its own number auto-reveals all remaining
     * unflagged neighbors at once. Standard Minesweeper shortcut for
     * clearing cells you're confident about without tapping each one.
     *
     * Returns the index of a mine if chording detonates one (i.e. a
     * neighbor was wrongly left unflagged), or null if all revealed
     * neighbors were safe. No-ops (returns null) if [index] isn't a
     * revealed, non-mine, numbered cell, or if the flag count doesn't
     * match yet.
     */
    fun chord(index: Int): Int? {
        if (index !in board.indices) {
            return null
        }

        val cell = board[index]

        if (!cell.isRevealed || cell.isMine) {
            return null
        }

        val neighbors = getNeighbors(cell)
        val flaggedCount = neighbors.count { it.isFlagged }

        if (flaggedCount != cell.adjacentMines) {
            return null
        }

        var detonatedIndex: Int? = null

        neighbors.forEach { neighbor ->
            if (!neighbor.isFlagged && !neighbor.isRevealed) {
                val neighborIndex = neighbor.row * columns + neighbor.column
                val result = reveal(neighborIndex)

                if (result != null && detonatedIndex == null) {
                    detonatedIndex = result
                }
            }
        }

        return detonatedIndex
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
}