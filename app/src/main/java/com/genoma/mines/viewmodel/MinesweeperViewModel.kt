package com.genoma.mines.viewmodel

import androidx.lifecycle.ViewModel
import com.genoma.mines.game.Difficulty
import com.genoma.mines.game.GameState
import com.genoma.mines.game.GameStatus
import com.genoma.mines.game.MinesweeperGame
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MinesweeperViewModel : ViewModel() {

    private var game: MinesweeperGame? = null
    private var timerJob: Job? = null

    private val _gameState = MutableStateFlow<GameState?>(null)

    val gameState: StateFlow<GameState?> = _gameState.asStateFlow()

    fun startGame(difficulty: Difficulty) {
        timerJob?.cancel()

        game = MinesweeperGame(difficulty)

        val newGame = game ?: return

        _gameState.value = GameState(
            difficulty = difficulty,
            cells = newGame.getBoard(),
            flagsPlaced = 0,
            elapsedSeconds = 0,
            status = GameStatus.PLAYING
        )

        startTimer()
    }

    fun togglePause() {
        val currentState = _gameState.value ?: return

        when (currentState.status) {

            GameStatus.PLAYING -> {
                timerJob?.cancel()

                _gameState.value = currentState.copy(
                    status = GameStatus.PAUSED
                )
            }

            GameStatus.PAUSED -> {
                _gameState.value = currentState.copy(
                    status = GameStatus.PLAYING
                )

                startTimer()
            }

            else -> {
                // Cannot pause a game that is ready, won, or lost.
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()

        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)

                val currentState = _gameState.value

                if (currentState?.status != GameStatus.PLAYING) {
                    break
                }

                _gameState.value = currentState.copy(
                    elapsedSeconds = currentState.elapsedSeconds + 1
                )
            }
        }
    }

    fun revealCell(index: Int) {

        val currentGame = game ?: return

        val currentState = _gameState.value ?: return

        if (currentState.status != GameStatus.PLAYING) {
            return
        }

        val hitMine = currentGame.reveal(index)

        if (hitMine) {
            timerJob?.cancel()

            currentGame.revealAllMines()

            _gameState.value = currentState.copy(
                cells = currentGame.getBoard(),
                flagsPlaced = currentGame.getFlagsPlaced(),
                status = GameStatus.LOST,
                detonatedCellIndex = index
            )

            return
        }

        val status = if (currentGame.isWon()) {
            GameStatus.WON
        } else {
            GameStatus.PLAYING
        }

        if (status == GameStatus.WON) {
            timerJob?.cancel()
        }

        _gameState.value = currentState.copy(
            cells = currentGame.getBoard(),
            flagsPlaced = currentGame.getFlagsPlaced(),
            status = status
        )

    }

    fun toggleFlag(index: Int) {

        val currentGame = game ?: return
        val currentState = _gameState.value ?: return

        if (currentState.status != GameStatus.PLAYING) {
            return
        }

        currentGame.toggleFlag(index)

        _gameState.value = currentState.copy(
            cells = currentGame.getBoard(),
            flagsPlaced = currentGame.getFlagsPlaced()
        )
    }

    fun resetGame() {

        val currentState = _gameState.value ?: return

        startGame(currentState.difficulty)
    }

    fun goBackToHome() {
        timerJob?.cancel()
        game = null
        _gameState.value = null
    }
}