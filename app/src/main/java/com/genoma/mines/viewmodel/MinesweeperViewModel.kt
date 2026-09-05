package com.genoma.mines.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.genoma.mines.feedback.GameFeedback
import com.genoma.mines.game.Difficulty
import com.genoma.mines.game.GameState
import com.genoma.mines.game.GameStatus
import com.genoma.mines.game.MinesweeperGame
import com.genoma.mines.settings.SettingsDataStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class MinesweeperViewModel(
    application: Application
) : AndroidViewModel(application) {

    private companion object {
        /** A game that runs this long auto-quits back to the home screen. */
        const val MAX_GAME_DURATION_SECONDS = 30 * 60
    }

    private var game: MinesweeperGame? = null
    private var timerJob: Job? = null

    private val feedback = GameFeedback(application)
    private val settings = SettingsDataStore(application)

    private val _gameState = MutableStateFlow<GameState?>(null)
    val gameState: StateFlow<GameState?> = _gameState.asStateFlow()

    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _hapticsEnabled = MutableStateFlow(true)
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled.asStateFlow()


    // Null = no saved preference yet; the UI falls back to the system
    // setting until the user explicitly picks light or dark.
    private val _darkTheme = MutableStateFlow<Boolean?>(null)
    val darkTheme: StateFlow<Boolean?> = _darkTheme.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {

            launch {
                settings.soundEnabled.collect { enabled ->
                    _soundEnabled.value = enabled
                }
            }

            launch {
                settings.hapticsEnabled.collect { enabled ->
                    _hapticsEnabled.value = enabled
                }
            }


            launch {
                settings.darkThemeEnabled.collect { enabled ->
                    _darkTheme.value = enabled
                }
            }
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        _soundEnabled.value = enabled

        viewModelScope.launch {
            settings.setSoundEnabled(enabled)
        }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        _hapticsEnabled.value = enabled

        viewModelScope.launch {
            settings.setHapticsEnabled(enabled)
        }
    }


    fun setDarkTheme(enabled: Boolean) {
        _darkTheme.value = enabled

        viewModelScope.launch {
            settings.setDarkThemeEnabled(enabled)
        }
    }

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
                delay(1.seconds)

                val currentState = _gameState.value

                if (currentState?.status != GameStatus.PLAYING) {
                    break
                }

                val updatedSeconds = currentState.elapsedSeconds + 1

                if (updatedSeconds >= MAX_GAME_DURATION_SECONDS) {
                    goBackToHome()
                    break
                }

                _gameState.value = currentState.copy(
                    elapsedSeconds = updatedSeconds
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

        val tappedCell = currentState.cells.getOrNull(index) ?: return

        val detonatedIndex = if (tappedCell.isRevealed) {
            currentGame.chord(index)
        } else {
            currentGame.reveal(index)
        }

        if (detonatedIndex != null) {
            timerJob?.cancel()

            feedback.explosion(
                soundEnabled = _soundEnabled.value,
                hapticsEnabled = _hapticsEnabled.value
            )

            currentGame.revealAllMines()

            _gameState.value = currentState.copy(
                cells = currentGame.getBoard(),
                flagsPlaced = currentGame.getFlagsPlaced(),
                status = GameStatus.LOST,
                detonatedCellIndex = detonatedIndex
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

            feedback.win(
                soundEnabled = _soundEnabled.value,
                hapticsEnabled = _hapticsEnabled.value
            )
        } else {
            feedback.tap(
                soundEnabled = _soundEnabled.value,
                hapticsEnabled = _hapticsEnabled.value
            )
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

        val changed = currentGame.toggleFlag(index)

        if (!changed) {
            return
        }

        feedback.flag(
            soundEnabled = _soundEnabled.value,
            hapticsEnabled = _hapticsEnabled.value
        )

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

    override fun onCleared() {
        timerJob?.cancel()
        feedback.release()
    }
}