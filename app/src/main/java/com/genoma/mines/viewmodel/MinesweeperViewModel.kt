package com.genoma.mines.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.genoma.mines.data.GameRepository
import com.genoma.mines.data.GameRepositoryImpl
import com.genoma.mines.data.GameResult
import com.genoma.mines.data.GameHistoryItem
import com.genoma.mines.data.UserStatistics
import com.genoma.mines.data.AchievementCalculator
import com.genoma.mines.data.local.GuestGameDatabase
import com.genoma.mines.data.local.GuestGameRepository
import com.genoma.mines.data.remote.FirestoreGameRepository
import com.genoma.mines.data.remote.FirestoreWalletRepository
import com.genoma.mines.data.remote.FirestoreFeedbackRepository
import com.genoma.mines.data.remote.FeedbackSubmission
import com.genoma.mines.feedback.CelebrationEvent
import com.genoma.mines.feedback.GameFeedback
import com.genoma.mines.game.Difficulty
import com.genoma.mines.game.GameState
import com.genoma.mines.game.GameStatus
import com.genoma.mines.game.GameResultType
import com.genoma.mines.game.LevelCalculator
import com.genoma.mines.game.MinesweeperGame
import com.genoma.mines.game.ScoreCalculator
import com.genoma.mines.session.SessionManager
import com.genoma.mines.settings.SettingsDataStore
import com.genoma.mines.store.StoreDataStore
import com.genoma.mines.store.StoreItem
import com.genoma.mines.ui.screens.AvatarOption
import com.genoma.mines.ui.screens.FeedbackData
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds
import com.genoma.mines.ui.screens.ThemePreference
import com.genoma.mines.ui.screens.toDarkThemeFlag
import com.genoma.mines.wallet.CoinWalletDataStore
import com.genoma.mines.wallet.RedeemStatus
import com.genoma.mines.wallet.WalletRepository
import com.genoma.mines.wallet.WalletRepositoryImpl

class MinesweeperViewModel(
    application: Application
) : AndroidViewModel(application) {

    private companion object {
        /** A game that runs this long auto-quits back to the home screen. */
        const val MAX_GAME_DURATION_SECONDS = 30 * 60
        const val COIN_REWARD_PER_WIN = 100
    }

    private var game: MinesweeperGame? = null
    private var timerJob: Job? = null

    private val feedback = GameFeedback(application)
    private val settings = SettingsDataStore(application)
    private val storeDataStore = StoreDataStore(application)

    private val sessionManager = SessionManager()
    private val scoreCalculator = ScoreCalculator()

    private val feedbackRepository = FirestoreFeedbackRepository()

    private val gameRepository: GameRepository = GameRepositoryImpl(
        sessionManager = sessionManager,
        guestRepository = GuestGameRepository(
            GuestGameDatabase.getInstance(application).guestGameDao()
        ),
        firestoreRepository = FirestoreGameRepository()
    )

    private var gameResultSaved = false

    private val wallet: WalletRepository = WalletRepositoryImpl(
        sessionManager = sessionManager,
        guestWallet = CoinWalletDataStore(application),
        firestoreWallet = FirestoreWalletRepository()
    )

    private val _coins = MutableStateFlow(0)
    val coins: StateFlow<Int> = _coins.asStateFlow()

    private val _diamonds = MutableStateFlow(0)
    val diamonds: StateFlow<Int> = _diamonds.asStateFlow()

    private val _redeemStatus = MutableStateFlow<RedeemStatus?>(null)
    val redeemStatus: StateFlow<RedeemStatus?> = _redeemStatus.asStateFlow()

    private val _redeemResultMessage = MutableStateFlow<String?>(null)
    val redeemResultMessage: StateFlow<String?> = _redeemResultMessage.asStateFlow()

    private val _gameState = MutableStateFlow<GameState?>(null)
    val gameState: StateFlow<GameState?> = _gameState.asStateFlow()

    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _hapticsEnabled = MutableStateFlow(true)
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled.asStateFlow()

    private val _lastScore = MutableStateFlow<Int?>(null)
    val lastScore: StateFlow<Int?> = _lastScore.asStateFlow()

    private val _isNewBestTime = MutableStateFlow(false)
    val isNewBestTime: StateFlow<Boolean> = _isNewBestTime.asStateFlow()

    // The player's best prior time (seconds) at this difficulty, captured
    // right before the just-finished game is saved — lets the UI show how
    // the current run compares, whether it's a new record or not.
    private val _previousBestSeconds = MutableStateFlow<Long?>(null)
    val previousBestSeconds: StateFlow<Long?> = _previousBestSeconds.asStateFlow()

    // Milestones (level-ups, newly unlocked achievement tiers) earned by the
    // most recently completed game, queued so the UI can acknowledge them
    // one at a time instead of the change only showing up silently the next
    // time the player opens Profile or Achievements.
    private val _celebrationEvents = MutableStateFlow<List<CelebrationEvent>>(emptyList())
    val celebrationEvents: StateFlow<List<CelebrationEvent>> = _celebrationEvents.asStateFlow()

    // Null = no saved preference yet; the UI falls back to the system
    // setting until the user explicitly picks light or dark.
    private val _darkTheme = MutableStateFlow<Boolean?>(null)
    val darkTheme: StateFlow<Boolean?> = _darkTheme.asStateFlow()

    private val _selectedAvatar = MutableStateFlow(AvatarOption.Default)
    val selectedAvatar: StateFlow<AvatarOption> = _selectedAvatar.asStateFlow()

    private val _ownedStoreItemIds = MutableStateFlow<Set<String>>(emptySet())
    val ownedStoreItemIds: StateFlow<Set<String>> = _ownedStoreItemIds.asStateFlow()

    private val _equippedBoardThemeId = MutableStateFlow("board_classic_teal")
    val equippedBoardThemeId: StateFlow<String> = _equippedBoardThemeId.asStateFlow()

    private val _equippedCellSkinId = MutableStateFlow<String?>(null)
    val equippedCellSkinId: StateFlow<String?> = _equippedCellSkinId.asStateFlow()



    private val _isSubmittingFeedback = MutableStateFlow(false)
    val isSubmittingFeedback: StateFlow<Boolean> = _isSubmittingFeedback.asStateFlow()

    private val _feedbackError = MutableStateFlow<String?>(null)
    val feedbackError: StateFlow<String?> = _feedbackError.asStateFlow()

    private val _feedbackSubmitted = MutableStateFlow(false)
    val feedbackSubmitted: StateFlow<Boolean> = _feedbackSubmitted.asStateFlow()

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

            launch {
                settings.selectedAvatarId.collect { avatarId ->
                    _selectedAvatar.value = AvatarOption.fromId(avatarId)
                }
            }

            launch {
                storeDataStore.ownedItemIds.collect { _ownedStoreItemIds.value = it }
            }

            launch {
                storeDataStore.equippedBoardThemeId.collect { _equippedBoardThemeId.value = it }
            }

            launch {
                storeDataStore.equippedCellSkinId.collect { _equippedCellSkinId.value = it }
            }

            launch {
                wallet.coins.collect { _coins.value = it }
            }

            launch {
                wallet.diamonds.collect { _diamonds.value = it }
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

    fun setThemePreference(preference: ThemePreference) {
        val flag = preference.toDarkThemeFlag()
        _darkTheme.value = flag

        viewModelScope.launch {
            if (flag == null) {
                settings.clearDarkThemePreference()
            } else {
                settings.setDarkThemeEnabled(flag)
            }
        }
    }
    fun setAvatar(avatar: AvatarOption) {
        _selectedAvatar.value = avatar

        viewModelScope.launch {
            settings.setSelectedAvatarId(avatar.id)
        }
    }

    fun purchaseOrEquipStoreItem(item: StoreItem) {
        viewModelScope.launch {
            val owned = item.price == 0 || storeDataStore.isOwned(item.id)

            if (!owned) {
                val result = wallet.spendDiamonds(item.price)
                if (!result.success) {
                    _redeemResultMessage.value = result.message
                    return@launch
                }
                storeDataStore.addOwnedItem(item.id)
            }

            when (item) {
                is com.genoma.mines.store.BoardThemeItem -> storeDataStore.equipBoardTheme(item.id)
                is com.genoma.mines.store.CellSkinItem -> storeDataStore.equipCellSkin(item.id)
                is com.genoma.mines.store.AvatarStoreItem -> {
                    // Avatar artwork is not yet part of AvatarOption. Ownership is
                    // still saved, but no fake visual mapping is introduced.
                }
            }
        }
    }

    fun startGame(difficulty: Difficulty) {
        timerJob?.cancel()

        game = MinesweeperGame(difficulty)
        gameResultSaved = false
        _lastScore.value = null
        _isNewBestTime.value = false
        _previousBestSeconds.value = null

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

            val finalState = currentState.copy(
                cells = currentGame.getBoard(),
                flagsPlaced = currentGame.getFlagsPlaced(),
                status = GameStatus.LOST,
                detonatedCellIndex = detonatedIndex
            )

            _gameState.value = finalState
            completeLevel(finalState, GameResultType.LOSS)

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

        val updatedState = currentState.copy(
            cells = currentGame.getBoard(),
            flagsPlaced = currentGame.getFlagsPlaced(),
            status = status
        )

        _gameState.value = updatedState

        if (status == GameStatus.WON) {
            completeLevel(updatedState, GameResultType.WIN)
        }
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

    private fun completeLevel(state: GameState, result: GameResultType) {
        if (gameResultSaved) return
        gameResultSaved = true

        if (result == GameResultType.WIN) {
            viewModelScope.launch {
                wallet.addCoins(COIN_REWARD_PER_WIN)
            }
        }

        val correctlyRevealedCells = state.cells.count { it.isRevealed && !it.isMine }

        val totalSafeCells = state.difficulty.rows * state.difficulty.columns - state.difficulty.mines

        val score = scoreCalculator.calculate(
            difficulty = state.difficulty,
            result = result,
            elapsedSeconds = state.elapsedSeconds.toLong(),
            correctlyRevealedCells = correctlyRevealedCells,
            totalSafeCells = totalSafeCells
        )

        _lastScore.value = score

        val gameResult = GameResult(
            difficulty = state.difficulty,
            score = score,
            result = result,
            durationSeconds = state.elapsedSeconds.toLong()
        )

        viewModelScope.launch {
            // Fetched once up front: doubles as the "before this game"
            // history used both for the best-time comparison below and for
            // the level/achievement diff after saving.
            val historyBeforeThisGame = gameRepository.getGameHistory()

            if (result == GameResultType.WIN) {
                // Compare against past wins at this difficulty *before*
                // saving the current one, so it's judged against previous
                // attempts rather than against itself.
                val previousBestSeconds = historyBeforeThisGame
                    .filter {
                        it.difficulty == state.difficulty &&
                                it.result == GameResultType.WIN
                    }
                    .minOfOrNull { it.durationSeconds }

                val isNewBest = previousBestSeconds == null ||
                        state.elapsedSeconds.toLong() < previousBestSeconds

                _isNewBestTime.value = isNewBest
                _previousBestSeconds.value = previousBestSeconds

                if (isNewBest) {
                    feedback.newBestTime(
                        soundEnabled = _soundEnabled.value,
                        hapticsEnabled = _hapticsEnabled.value
                    )
                }
            } else {
                _isNewBestTime.value = false
                _previousBestSeconds.value = null
            }

            // Snapshot level + achievement progress from *before* this game
            // counts, so afterwards we can tell exactly what just changed.
            val levelBefore = levelFor(historyBeforeThisGame)
            val achievedTiersBefore = AchievementCalculator.calculate(historyBeforeThisGame)
                .associate { it.id to it.achievedTier }

            gameRepository.saveGameResult(gameResult)

            // Rather than re-querying the repository (which, for an
            // authenticated user, could race with server-side write
            // propagation), the just-saved game is appended locally to the
            // same history snapshot used above — cheap, and guaranteed to
            // reflect exactly what was just written.
            val historyAfterThisGame = historyBeforeThisGame + GameHistoryItem(
                difficulty = state.difficulty,
                score = score,
                result = result,
                durationSeconds = state.elapsedSeconds.toLong(),
                createdAtMillis = gameResult.createdAt
            )

            val levelAfter = levelFor(historyAfterThisGame)
            val tracksAfter = AchievementCalculator.calculate(historyAfterThisGame)

            val newCelebrations = mutableListOf<CelebrationEvent>()

            if (levelAfter > levelBefore) {
                newCelebrations += CelebrationEvent.LevelUp(newLevel = levelAfter)
            }

            tracksAfter.forEach { track ->
                val tierAfter = track.achievedTier ?: return@forEach
                val tierBefore = achievedTiersBefore[track.id]

                if (tierBefore == null || tierAfter.ordinal > tierBefore.ordinal) {
                    newCelebrations += CelebrationEvent.AchievementUnlocked(
                        trackId = track.id,
                        trackTitle = track.title,
                        tier = tierAfter
                    )
                }
            }

            if (newCelebrations.isNotEmpty()) {
                if (newCelebrations.any { it is CelebrationEvent.LevelUp }) {
                    feedback.levelUp(
                        soundEnabled = _soundEnabled.value,
                        hapticsEnabled = _hapticsEnabled.value
                    )
                }

                if (newCelebrations.any { it is CelebrationEvent.AchievementUnlocked }) {
                    feedback.achievementUnlocked(
                        soundEnabled = _soundEnabled.value,
                        hapticsEnabled = _hapticsEnabled.value
                    )
                }

                _celebrationEvents.value = _celebrationEvents.value + newCelebrations
            }
        }
    }

    /** Player level implied by a completed history list, via total XP. */
    private fun levelFor(history: List<GameHistoryItem>): Int {
        val wins = history.count { it.result == GameResultType.WIN }
        val losses = history.count { it.result == GameResultType.LOSS }

        return LevelCalculator.calculateProgress(
            LevelCalculator.calculateTotalXp(wins, losses)
        ).level
    }

    /** Pops the front-most queued celebration once the UI has shown it. */
    fun consumeCelebrationEvent() {
        _celebrationEvents.value = _celebrationEvents.value.drop(1)
    }

    suspend fun loadGameHistory(): List<GameHistoryItem> {
        return gameRepository.getGameHistory()
    }

    suspend fun loadStatistics(): UserStatistics {
        return gameRepository.getStatistics()
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

    fun submitFeedback(data: FeedbackData, userId: String?) {
        if (_isSubmittingFeedback.value) return

        _isSubmittingFeedback.value = true
        _feedbackError.value = null

        viewModelScope.launch {
            try {
                feedbackRepository.submitFeedback(
                    FeedbackSubmission(
                        userId = userId,
                        userName = data.userName,
                        userEmail = data.userEmail,
                        description = data.description,
                        screenshotCount = data.screenshots.size
                    )
                )
                _feedbackSubmitted.value = true
            } catch (e: Exception) {
                _feedbackError.value = e.message ?: "Couldn't send feedback. Please try again."
            } finally {
                _isSubmittingFeedback.value = false
            }
        }
    }

    fun resetFeedbackSubmitted() {
        _feedbackSubmitted.value = false
    }

    fun refreshRedeemStatus() {
        viewModelScope.launch {
            _redeemStatus.value = wallet.getRedeemStatus()
        }
    }

    fun redeemDiamond() {
        viewModelScope.launch {
            val result = wallet.redeemDiamond()
            _redeemResultMessage.value = result.message
            _redeemStatus.value = wallet.getRedeemStatus()
        }
    }

    fun consumeRedeemResultMessage() {
        _redeemResultMessage.value = null
    }

    override fun onCleared() {
        timerJob?.cancel()
        feedback.release()
    }
}