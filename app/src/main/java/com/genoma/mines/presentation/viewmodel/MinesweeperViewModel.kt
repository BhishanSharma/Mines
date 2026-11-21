package com.genoma.mines.presentation.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.genoma.mines.MinesApplication
import com.genoma.mines.achievements.domain.AchievementCalculator
import com.genoma.mines.ads.AdUsageStore
import com.genoma.mines.celebration.domain.CelebrationEvent
import com.genoma.mines.core.economy.AdEligibility
import com.genoma.mines.core.economy.AdFrequencyRules
import com.genoma.mines.core.economy.LuckPrize
import com.genoma.mines.core.economy.LuckPrizeTable
import com.genoma.mines.core.economy.LuckPrizeType
import com.genoma.mines.core.economy.RewardedAdPlacement
import com.genoma.mines.core.economy.SettlementPlan
import com.genoma.mines.core.remoteconfig.RemoteConfigSnapshot
import com.genoma.mines.economy.toAdCapConfig
import com.genoma.mines.home.ui.LuckSpinResultUi
import com.genoma.mines.life.domain.LifeCalculator
import com.genoma.mines.core.economy.ConversionDirection
import com.genoma.mines.core.economy.EconomyRules
import com.genoma.mines.core.economy.GameOutcome
import com.genoma.mines.core.economy.GameSettlementRules
import com.genoma.mines.core.economy.PendingSettlement
import com.genoma.mines.core.economy.WalletTransaction
import com.genoma.mines.core.economy.WalletTransactionType
import com.genoma.mines.economy.FirestoreSettlementRepository
import com.genoma.mines.economy.HeartRefillResult
import com.genoma.mines.economy.HeartRefillService
import com.genoma.mines.economy.StorePurchaseResult
import com.genoma.mines.economy.StorePurchaseService
import com.genoma.mines.economy.PendingSettlementStore
import com.genoma.mines.economy.SettlementProcessor
import com.genoma.mines.economy.SettlementResult
import com.genoma.mines.economy.WinBonusTicket
import com.genoma.mines.economy.toEconomyRules
import com.genoma.mines.economy.winRewardFor
import com.genoma.mines.game.data.GameHistoryItem
import com.genoma.mines.game.data.GameRepository
import com.genoma.mines.game.data.GameRepositoryImpl
import com.genoma.mines.game.data.GameResult
import com.genoma.mines.game.data.UserStatistics
import com.genoma.mines.game.data.local.GuestGameDatabase
import com.genoma.mines.game.data.local.GuestGameRepository
import com.genoma.mines.game.data.remote.FirestoreGameRepository
import com.genoma.mines.game.domain.Difficulty
import com.genoma.mines.game.domain.GameFeedback
import com.genoma.mines.game.domain.GameResultType
import com.genoma.mines.game.domain.GameState
import com.genoma.mines.game.domain.GameStatus
import com.genoma.mines.game.domain.LevelCalculator
import com.genoma.mines.game.domain.MinesweeperGame
import com.genoma.mines.game.domain.ScoreCalculator
import com.genoma.mines.life.data.LifeDataStore
import com.genoma.mines.life.data.LifeRepository
import com.genoma.mines.life.data.LifeRepositoryImpl
import com.genoma.mines.life.data.remote.FirestoreLifeRepository
import com.genoma.mines.life.domain.LifeRules
import com.genoma.mines.life.domain.LifeSnapshot
import com.genoma.mines.profile.domain.AvatarOption
import com.genoma.mines.session.SessionManager
import com.genoma.mines.settings.data.SettingsDataStore
import com.genoma.mines.settings.ui.ThemePreference
import com.genoma.mines.settings.ui.toDarkThemeFlag
import com.genoma.mines.store.data.StoreDataStore
import com.genoma.mines.store.data.StoreRepository
import com.genoma.mines.store.data.StoreRepositoryImpl
import com.genoma.mines.store.data.remote.FirestoreStoreRepository
import com.genoma.mines.store.domain.AvatarStoreItem
import com.genoma.mines.store.domain.BoardThemeItem
import com.genoma.mines.store.domain.CellSkinItem
import com.genoma.mines.store.domain.StoreItem
import com.genoma.mines.store.data.StoreCatalog
import com.genoma.mines.session.UserSession
import com.genoma.mines.userfeedback.data.FeedbackSubmission
import com.genoma.mines.userfeedback.data.FirestoreFeedbackRepository
import com.genoma.mines.userfeedback.ui.FeedbackData
import com.genoma.mines.wallet.data.CoinWalletDataStore
import com.genoma.mines.wallet.data.RedeemStatus
import com.genoma.mines.wallet.data.WalletRepository
import com.genoma.mines.wallet.data.WalletRepositoryImpl
import com.genoma.mines.wallet.data.remote.FirestoreWalletRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class MinesweeperViewModel(
    application: Application
) : AndroidViewModel(application) {

    private companion object {
        const val TAG = "MinesweeperViewModel"
        const val MAX_GAME_DURATION_SECONDS = 30 * 60
    }

    private var game: MinesweeperGame? = null
    private var timerJob: Job? = null

    private val feedback = GameFeedback(application)

    private val remoteConfig
        get() = (getApplication<Application>() as MinesApplication)
            .remoteConfigManager.snapshot.value

    private val settings = SettingsDataStore(application)

    private val sessionManager = SessionManager()
    private val scoreCalculator = ScoreCalculator()

    private val guestStore = StoreDataStore(application)

    private val storeDataStore: StoreRepository = StoreRepositoryImpl(
        sessionManager = sessionManager,
        guestStore = guestStore,
        firestoreStore = FirestoreStoreRepository()
    )

    private val feedbackRepository = FirestoreFeedbackRepository()

    private val gameRepository: GameRepository = GameRepositoryImpl(
        sessionManager = sessionManager,
        guestRepository = GuestGameRepository(
            GuestGameDatabase.getInstance(application).guestGameDao()
        ),
        firestoreRepository = FirestoreGameRepository()
    )

    private var gameResultSaved = false

    private val guestWallet = CoinWalletDataStore(application)
    private val guestLife = LifeDataStore(application)

    private val wallet: WalletRepository = WalletRepositoryImpl(
        sessionManager = sessionManager,
        guestWallet = guestWallet,
        firestoreWallet = FirestoreWalletRepository()
    )

    private val lives: LifeRepository = LifeRepositoryImpl(
        sessionManager = sessionManager,
        guestLife = guestLife,
        firestoreLife = FirestoreLifeRepository()
    )

    /** Applies game results and rewards exactly once, retrying when offline. */
    private val settlements = SettlementProcessor(
        sessionManager = sessionManager,
        queue = PendingSettlementStore(application),
        guestWallet = guestWallet,
        guestLife = guestLife,
        firestoreSettlements = FirestoreSettlementRepository()
    )

    /** Charges and grants cosmetics together, exactly once. */
    private val storePurchases = StorePurchaseService(
        sessionManager = sessionManager,
        guestWallet = guestWallet,
        guestStore = guestStore
    )

    /** Charges Gems and refills hearts together. */
    private val heartRefills = HeartRefillService(
        sessionManager = sessionManager,
        guestWallet = guestWallet,
        guestLife = guestLife
    )

    private val _isPurchasing = MutableStateFlow(false)
    val isPurchasing: StateFlow<Boolean> = _isPurchasing.asStateFlow()

    /** Identifies the current game; settlement idempotency keys derive from it. */
    private var gameId: String = UUID.randomUUID().toString()

    /** Economy rules captured when the current game started. */
    private var gameRules: EconomyRules = EconomyRules.DEFAULT

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private var startGameJob: Job? = null

    private val _lifeSnapshot = MutableStateFlow(LifeSnapshot.FULL)
    val lifeSnapshot: StateFlow<LifeSnapshot> = _lifeSnapshot.asStateFlow()

    private val _showHeartsDialog = MutableStateFlow(false)
    val showHeartsDialog: StateFlow<Boolean> = _showHeartsDialog.asStateFlow()

    private val _isRefillingHearts = MutableStateFlow(false)
    val isRefillingHearts: StateFlow<Boolean> = _isRefillingHearts.asStateFlow()

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

    private val _winAdBonusClaimed = MutableStateFlow(false)
    val winAdBonusClaimed: StateFlow<Boolean> = _winAdBonusClaimed.asStateFlow()

    /** Bonus amount offered for the current game, or null when no bonus applies. */
    private val _winAdBonusOffer = MutableStateFlow<Int?>(null)
    val winAdBonusOffer: StateFlow<Int?> = _winAdBonusOffer.asStateFlow()

    private val _isConverting = MutableStateFlow(false)
    val isConverting: StateFlow<Boolean> = _isConverting.asStateFlow()

    /** Recent wallet changes, newest first, for the active session. */
    val walletHistory: StateFlow<List<WalletTransaction>> by lazy {
        wallet.history.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    }

    private val _winAdBonusClaimInProgress = MutableStateFlow(false)
    val winAdBonusClaimInProgress: StateFlow<Boolean> =
        _winAdBonusClaimInProgress.asStateFlow()

    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _hapticsEnabled = MutableStateFlow(true)
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled.asStateFlow()

    private val _lastScore = MutableStateFlow<Int?>(null)
    val lastScore: StateFlow<Int?> = _lastScore.asStateFlow()

    private val _isNewBestTime = MutableStateFlow(false)
    val isNewBestTime: StateFlow<Boolean> = _isNewBestTime.asStateFlow()

    private val _previousBestSeconds = MutableStateFlow<Long?>(null)
    val previousBestSeconds: StateFlow<Long?> = _previousBestSeconds.asStateFlow()

    private val _celebrationEvents =
        MutableStateFlow<List<CelebrationEvent>>(emptyList())
    val celebrationEvents: StateFlow<List<CelebrationEvent>> =
        _celebrationEvents.asStateFlow()

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

    private val remoteConfigFlow: StateFlow<RemoteConfigSnapshot> =
        (application as MinesApplication).remoteConfigManager.snapshot

    private val adUsageStore = AdUsageStore(application)

    /** Re-evaluates time-based rules (cooldowns, regeneration) as time passes. */
    private val clockTicks = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(15.seconds)
        }
    }

    /** Rewarded-ad caps and cooldowns per placement (Stage 3G). */
    val adEligibility: StateFlow<Map<RewardedAdPlacement, AdEligibility>> =
        combine(adUsageStore.usage, remoteConfigFlow, clockTicks) { usage, config, now ->
            val caps = config.toAdCapConfig()
            RewardedAdPlacement.entries.associateWith { placement ->
                AdFrequencyRules.check(placement, caps, usage, now)
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    /** True when the post-win bonus ad is within its caps. */
    val winBonusAdEligible: StateFlow<Boolean> =
        adEligibility.map { it[RewardedAdPlacement.WIN_BONUS] == AdEligibility.Eligible }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /**
     * Life recovery ad (Stage 3D): null hides the option (flag off or hearts
     * full); otherwise its current cap/cooldown eligibility.
     */
    val lifeRecoveryAd: StateFlow<AdEligibility?> =
        combine(adEligibility, remoteConfigFlow, _lifeSnapshot, clockTicks) { eligibility, config, life, now ->
            when {
                !config.lifeRecoveryAdEnabled -> null
                LifeCalculator.status(life, now).isFull -> null
                else -> eligibility[RewardedAdPlacement.LIFE_RECOVERY] ?: AdEligibility.Eligible
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _lifeRecoveryInProgress = MutableStateFlow(false)
    val lifeRecoveryInProgress: StateFlow<Boolean> = _lifeRecoveryInProgress.asStateFlow()

    /** Prize table offered by Luck Spin; empty when the feature is off or misconfigured. */
    val luckSpinPrizes: StateFlow<List<LuckPrize>> =
        remoteConfigFlow.map { config ->
            if (!config.isLuckAdsAvailable) {
                emptyList()
            } else {
                LuckPrizeTable.build(
                    prizesJson = config.luckAdPrizesJson,
                    legacyCoinReward = config.luckAdCoinReward,
                    legacyGemReward = config.luckAdGemReward,
                    legacyCoinWeight = config.luckAdCoinProbability,
                    legacyGemWeight = config.luckAdGemProbability,
                    legacyNoRewardWeight = config.luckAdNoRewardProbability
                )
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Luck Spin eligibility, or null when Luck Spin is not offered at all. */
    val luckSpinEligibility: StateFlow<AdEligibility?> =
        combine(luckSpinPrizes, adEligibility) { prizes, eligibility ->
            if (prizes.isEmpty()) null else eligibility[RewardedAdPlacement.LUCK_SPIN] ?: AdEligibility.Eligible
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Short availability text for the Home entry and the dialog, or null to hide Luck Spin. */
    val luckSpinStatus: StateFlow<String?> =
        combine(luckSpinEligibility, clockTicks) { eligibility, now ->
            when (eligibility) {
                null, AdEligibility.Disabled -> null
                AdEligibility.Eligible -> "Watch an ad for a chance to win Coins or Gems."
                is AdEligibility.CoolingDown ->
                    "Next spin in ${formatWait(eligibility.availableAtMillis - now)}."

                AdEligibility.DailyCapReached, AdEligibility.TotalDailyCapReached ->
                    "You've used today's spins. Come back tomorrow."
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _luckSpinOpen = MutableStateFlow(false)
    val luckSpinOpen: StateFlow<Boolean> = _luckSpinOpen.asStateFlow()

    private val _luckSpinBusy = MutableStateFlow(false)
    val luckSpinBusy: StateFlow<Boolean> = _luckSpinBusy.asStateFlow()

    private val _luckSpinResult = MutableStateFlow<LuckSpinResultUi?>(null)
    val luckSpinResult: StateFlow<LuckSpinResultUi?> = _luckSpinResult.asStateFlow()

    private var luckSpinNonce = 0

    init {
        loadSettings()
        watchForSettlementRetries()

        viewModelScope.launch {
            try {
                storePurchases.reconcileGuestPurchases()
            } catch (exception: Exception) {
                Log.w(TAG, "Couldn't reconcile guest purchases", exception)
            }
        }
    }

    /**
     * Retries queued settlements on start, on every sign-in/sign-out and
     * whenever the network comes back.
     */
    private fun watchForSettlementRetries() {
        viewModelScope.launch {
            sessionManager.sessionFlow.collect { retryPendingSettlements() }
        }

        val connectivity = getApplication<Application>()
            .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                retryPendingSettlements()
            }
        }

        try {
            connectivity.registerDefaultNetworkCallback(callback)
            networkCallback = callback
        } catch (exception: Exception) {
            Log.w(TAG, "Couldn't watch connectivity for settlement retries", exception)
        }
    }

    /** Applies any results or rewards that couldn't be applied earlier. */
    fun retryPendingSettlements() {
        viewModelScope.launch {
            try {
                settlements.processPending()
            } catch (exception: Exception) {
                Log.w(TAG, "Pending settlement retry failed", exception)
            }
        }
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
                // The equipped avatar is stored with the other cosmetics so it
                // follows the account across devices; the older device-only
                // setting is the fallback. Premium avatars need ownership.
                combine(
                    storeDataStore.equippedAvatarId,
                    settings.selectedAvatarId,
                    storeDataStore.ownedItemIds
                ) { equipped, legacy, owned ->
                    AvatarOption.resolve(equipped ?: legacy, owned)
                }.collect { avatar ->
                    _selectedAvatar.value = avatar
                }
            }

            launch {
                storeDataStore.ownedItemIds.collect {
                    _ownedStoreItemIds.value = it
                }
            }

            launch {
                storeDataStore.equippedBoardThemeId.collect {
                    _equippedBoardThemeId.value = it
                }
            }

            launch {
                storeDataStore.equippedCellSkinId.collect {
                    _equippedCellSkinId.value = it
                }
            }

            launch {
                wallet.coins.collect { _coins.value = it }
            }

            launch {
                wallet.diamonds.collect { _diamonds.value = it }
            }

            launch {
                lives.lifeSnapshot.collect { _lifeSnapshot.value = it }
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
        if (!avatar.isUsableWith(_ownedStoreItemIds.value)) {
            _redeemResultMessage.value = "Buy this avatar in the Store first."
            return
        }

        _selectedAvatar.value = avatar
        viewModelScope.launch {
            settings.setSelectedAvatarId(avatar.id)
            try {
                storeDataStore.equipAvatar(avatar.id)
            } catch (exception: Exception) {
                Log.w(TAG, "Couldn't save the avatar to the account", exception)
            }
        }
    }

    /**
     * Buys [item] if needed (charge and ownership together, exactly once),
     * then applies it. Owned items are applied for free.
     */
    fun purchaseOrEquipStoreItem(item: StoreItem) {
        if (_isPurchasing.value) return
        _isPurchasing.value = true

        viewModelScope.launch {
            try {
                val owned = item.price <= 0 || item.id in _ownedStoreItemIds.value

                if (!owned) {
                    when (val result = storePurchases.purchase(item.id, item.price)) {
                        StorePurchaseResult.Purchased ->
                            _redeemResultMessage.value = "${item.name} is yours!"

                        StorePurchaseResult.AlreadyOwned -> Unit

                        is StorePurchaseResult.InsufficientGems -> {
                            _redeemResultMessage.value =
                                "You need ${result.gemsShort} more Gems to buy ${item.name}."
                            return@launch
                        }
                    }
                }

                try {
                    equip(item)
                } catch (exception: Exception) {
                    Log.w(TAG, "Couldn't apply ${item.id}", exception)
                    _redeemResultMessage.value =
                        "${item.name} is yours, but couldn't be applied. Tap it again."
                }
            } catch (exception: Exception) {
                Log.w(TAG, "Purchase of ${item.id} failed", exception)
                _redeemResultMessage.value =
                    "Couldn't complete the purchase. Check your connection and try again."
            } finally {
                _isPurchasing.value = false
            }
        }
    }

    private suspend fun equip(item: StoreItem) {
        when (item) {
            is BoardThemeItem -> storeDataStore.equipBoardTheme(item.id)
            is CellSkinItem -> storeDataStore.equipCellSkin(item.id)
            is AvatarStoreItem -> AvatarOption.forStoreItem(item.id)?.let { avatar ->
                _selectedAvatar.value = avatar
                settings.setSelectedAvatarId(avatar.id)
                storeDataStore.equipAvatar(avatar.id)
            }
        }
    }

    /**
     * Unequips [item], returning to the default for its slot. Ownership is
     * never removed and nothing is charged (plan §1.6).
     */
    fun unequipStoreItem(item: StoreItem) {
        viewModelScope.launch {
            try {
                when (item) {
                    is BoardThemeItem ->
                        storeDataStore.equipBoardTheme(StoreCatalog.DEFAULT_BOARD_THEME_ID)

                    is CellSkinItem -> storeDataStore.unequipCellSkin()

                    is AvatarStoreItem -> resetAvatar()
                }
                _redeemResultMessage.value = "${item.name} unequipped. You still own it."
            } catch (exception: Exception) {
                Log.w(TAG, "Couldn't unequip ${item.id}", exception)
                _redeemResultMessage.value = "Couldn't unequip ${item.name}. Please try again."
            }
        }
    }

    /** Resets the board theme, cell skin and avatar to the defaults. Owned items stay owned. */
    fun resetCosmeticsToDefault() {
        viewModelScope.launch {
            try {
                storeDataStore.equipBoardTheme(StoreCatalog.DEFAULT_BOARD_THEME_ID)
                storeDataStore.unequipCellSkin()
                resetAvatar()
                _redeemResultMessage.value = "Default look restored. You keep everything you own."
            } catch (exception: Exception) {
                Log.w(TAG, "Couldn't reset cosmetics", exception)
                _redeemResultMessage.value = "Couldn't reset your look. Please try again."
            }
        }
    }

    private suspend fun resetAvatar() {
        _selectedAvatar.value = AvatarOption.Default
        settings.setSelectedAvatarId(AvatarOption.Default.id)
        storeDataStore.equipAvatar(AvatarOption.Default.id)
    }

    /**
     * Starts a game only when the player has the configured minimum Coins
     * and at least one Life available.
     *
     * No Coins or Lives are deducted at game start.
     */
    fun startGame(difficulty: Difficulty) {
        if (startGameJob?.isActive == true) return

        // Maintenance blocks new games only; a game in progress is never interrupted.
        if (remoteConfig.maintenanceMode) {
            _redeemResultMessage.value = remoteConfig.maintenanceMessage
            return
        }

        startGameJob = viewModelScope.launch {
            try {
                val minimumCoins =
                    remoteConfig.minimumStartCoins.coerceAtLeast(0)
                val currentCoins = wallet.coins.first()

                if (currentCoins < minimumCoins) {
                    _redeemResultMessage.value =
                        "You need at least $minimumCoins Coins to start a game."
                    return@launch
                }

                if (lives.getStatus().hearts <= 0) {
                    _showHeartsDialog.value = true
                    return@launch
                }

                beginGame(difficulty)
            } catch (_: Exception) {
                _redeemResultMessage.value =
                    "Couldn't start the game. Check your connection and try again."
            }
        }
    }

    private fun beginGame(difficulty: Difficulty) {
        timerJob?.cancel()

        game = MinesweeperGame(difficulty)
        gameId = UUID.randomUUID().toString()
        gameRules = remoteConfig.toEconomyRules()
        gameResultSaved = false
        _winAdBonusClaimed.value = false
        _winAdBonusClaimInProgress.value = false
        // Stage 3E: the optional ad can at most double the base win reward, so
        // with defaults a win pays at most 50 + 50 Coins (80 net of the entry fee).
        _winAdBonusOffer.value = minOf(gameRules.winAdBonus, gameRules.winRewardFor(difficulty))
            .takeIf { gameRules.isWinAdBonusAvailable && it > 0 }
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

    fun pauseIfPlaying() {
        if (_gameState.value?.status == GameStatus.PLAYING) {
            togglePause()
        }
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

            else -> Unit
        }
    }

    private fun startTimer() {
        timerJob?.cancel()

        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1.seconds)

                val currentState = _gameState.value
                if (currentState?.status != GameStatus.PLAYING) break

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

        if (currentState.status != GameStatus.PLAYING) return

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

        if (currentState.status != GameStatus.PLAYING) return

        val changed = currentGame.toggleFlag(index)
        if (!changed) return

        feedback.flag(
            soundEnabled = _soundEnabled.value,
            hapticsEnabled = _hapticsEnabled.value
        )

        _gameState.value = currentState.copy(
            cells = currentGame.getBoard(),
            flagsPlaced = currentGame.getFlagsPlaced()
        )
    }

    /**
     * Processes a completed result once.
     *
     * The entry fee is charged on every finished game, win or loss:
     * - Win: entry fee, then the win reward (defaults: net +30 Coins). No Life lost.
     * - Loss: entry fee and exactly one Life (defaults: -20 Coins, -1 Life).
     *
     * The values come from the rules captured when the game started, so a
     * Remote Config change mid-game never alters this game's payout. The
     * wallet change is a settlement with the idempotency key `game-<id>`,
     * applied exactly once and retried later if offline. Saving the game
     * record never depends on the wallet step succeeding.
     */
    private fun completeLevel(
        state: GameState,
        result: GameResultType
    ) {
        if (gameResultSaved) return
        gameResultSaved = true

        val correctlyRevealedCells =
            state.cells.count { it.isRevealed && !it.isMine }

        val totalSafeCells =
            state.difficulty.rows * state.difficulty.columns -
                    state.difficulty.mines

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

        val isWin = result == GameResultType.WIN
        val settlement = PendingSettlement(
            id = "game-$gameId",
            owner = settlements.currentOwner(),
            type = if (isWin) WalletTransactionType.GAME_WIN else WalletTransactionType.GAME_LOSS,
            plan = GameSettlementRules.forGame(
                outcome = if (isWin) GameOutcome.WIN else GameOutcome.LOSS,
                winReward = gameRules.winRewardFor(state.difficulty),
                entryFee = gameRules.entryFee
            ),
            createdAtMillis = gameResult.createdAt
        )

        viewModelScope.launch {
            val outcome = try {
                settlements.submit(settlement)
            } catch (exception: Exception) {
                Log.w(TAG, "Couldn't queue settlement ${settlement.id}", exception)
                null
            }

            when (outcome) {
                SettlementResult.Queued -> _redeemResultMessage.value =
                    if (isWin) {
                        "You're offline. Your win reward will be added when you reconnect."
                    } else {
                        "You're offline. Your Coins and Lives will update when you reconnect."
                    }

                null -> _redeemResultMessage.value =
                    "Couldn't update your wallet. Please try again."

                else -> Unit
            }
        }

        viewModelScope.launch {
            try {
                recordResultAndCelebrate(state, result, score, gameResult)
            } catch (exception: Exception) {
                Log.w(TAG, "Couldn't record game history", exception)
            }
        }
    }

    private suspend fun recordResultAndCelebrate(
        state: GameState,
        result: GameResultType,
        score: Int,
        gameResult: GameResult
    ) {
        run {
            val historyBeforeThisGame = gameRepository.getGameHistory()

            if (result == GameResultType.WIN) {
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

            val levelBefore = levelFor(historyBeforeThisGame)
            val achievedTiersBefore =
                AchievementCalculator.calculate(historyBeforeThisGame)
                    .associate { it.id to it.achievedTier }

            val historyAfterThisGame =
                historyBeforeThisGame + GameHistoryItem(
                    difficulty = state.difficulty,
                    score = score,
                    result = result,
                    durationSeconds = state.elapsedSeconds.toLong(),
                    createdAtMillis = gameResult.createdAt
                )

            val levelAfter = levelFor(historyAfterThisGame)
            val tracksAfter =
                AchievementCalculator.calculate(historyAfterThisGame)

            val newCelebrations = mutableListOf<CelebrationEvent>()

            if (levelAfter > levelBefore) {
                newCelebrations +=
                    CelebrationEvent.LevelUp(newLevel = levelAfter)
            }

            tracksAfter.forEach { track ->
                val tierAfter = track.achievedTier ?: return@forEach
                val tierBefore = achievedTiersBefore[track.id]

                if (tierBefore == null ||
                    tierAfter.ordinal > tierBefore.ordinal
                ) {
                    newCelebrations +=
                        CelebrationEvent.AchievementUnlocked(
                            trackId = track.id,
                            trackTitle = track.title,
                            tier = tierAfter
                        )
                }
            }

            if (newCelebrations.isNotEmpty()) {
                if (newCelebrations.any {
                        it is CelebrationEvent.LevelUp
                    }
                ) {
                    feedback.levelUp(
                        soundEnabled = _soundEnabled.value,
                        hapticsEnabled = _hapticsEnabled.value
                    )
                }

                if (newCelebrations.any {
                        it is CelebrationEvent.AchievementUnlocked
                    }
                ) {
                    feedback.achievementUnlocked(
                        soundEnabled = _soundEnabled.value,
                        hapticsEnabled = _hapticsEnabled.value
                    )
                }

                _celebrationEvents.value =
                    _celebrationEvents.value + newCelebrations
            }

            gameRepository.saveGameResult(gameResult)
        }
    }

    private fun levelFor(history: List<GameHistoryItem>): Int {
        val wins = history.count {
            it.result == GameResultType.WIN
        }
        val losses = history.count {
            it.result == GameResultType.LOSS
        }

        return LevelCalculator.calculateProgress(
            LevelCalculator.calculateTotalXp(wins, losses)
        ).level
    }

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
                _feedbackError.value =
                    e.message ?: "Couldn't send feedback. Please try again."
            } finally {
                _isSubmittingFeedback.value = false
            }
        }
    }

    fun resetFeedbackSubmitted() {
        _feedbackSubmitted.value = false
    }

    private val conversionLimit: Int
        get() = remoteConfig.conversionLimitDaily

    fun refreshRedeemStatus() {
        viewModelScope.launch { loadRedeemStatus() }
    }

    private suspend fun loadRedeemStatus() {
        try {
            _redeemStatus.value = wallet.getRedeemStatus(conversionLimit)
        } catch (exception: Exception) {
            // Keep the last known status; the Store stays usable offline.
            Log.w(TAG, "Couldn't load conversion status", exception)
        }
    }

    /** Converts 500 Coins into 1 Gem. */
    fun redeemDiamond() = convert(ConversionDirection.COINS_TO_GEMS)

    /** Converts 1 Gem into 500 Coins. */
    fun convertGemToCoins() = convert(ConversionDirection.GEMS_TO_COINS)

    private fun convert(direction: ConversionDirection) {
        if (_isConverting.value) return
        _isConverting.value = true

        // One id per tap: a retried or duplicated request can't convert twice.
        val requestId = "convert-${UUID.randomUUID()}"

        viewModelScope.launch {
            try {
                val result = wallet.convert(direction, conversionLimit, requestId)
                _redeemResultMessage.value = result.message
            } catch (exception: Exception) {
                Log.w(TAG, "Conversion failed", exception)
                _redeemResultMessage.value =
                    "Couldn't convert right now. Check your connection and try again."
            } finally {
                _isConverting.value = false
            }

            loadRedeemStatus()
        }
    }

    fun openHeartsDialog() {
        _showHeartsDialog.value = true
    }

    fun dismissHeartsDialog() {
        if (_isRefillingHearts.value) return
        _showHeartsDialog.value = false
    }

    fun refillHearts() {
        if (_isRefillingHearts.value) return
        _isRefillingHearts.value = true

        viewModelScope.launch {
            try {
                when (val result = heartRefills.refill()) {
                    HeartRefillResult.AlreadyFull ->
                        _redeemResultMessage.value = "Your hearts are already full."

                    is HeartRefillResult.InsufficientGems ->
                        _redeemResultMessage.value =
                            "You need ${result.cost} Gems to refill ${result.missingHearts} " +
                                    if (result.missingHearts == 1) "heart." else "hearts."

                    is HeartRefillResult.Refilled -> {
                        _redeemResultMessage.value =
                            if (result.heartsAdded == 1) {
                                "1 heart refilled!"
                            } else {
                                "${result.heartsAdded} hearts refilled!"
                            }
                        _showHeartsDialog.value = false
                    }
                }
            } catch (exception: Exception) {
                Log.w(TAG, "Heart refill failed", exception)
                _redeemResultMessage.value =
                    "Couldn't refill hearts. Check your connection and try again."
            } finally {
                _isRefillingHearts.value = false
            }
        }
    }

    /**
     * Returns the bonus offer for the game that was just won, or null when
     * no bonus applies. Take the ticket before showing the ad and pass it to
     * [claimWinAdBonus], so the claim is tied to that game and amount even if
     * Remote Config changes while the ad plays.
     */
    fun winAdBonusTicket(): WinBonusTicket? {
        val state = _gameState.value ?: return null
        val amount = _winAdBonusOffer.value ?: return null

        if (state.status != GameStatus.WON) return null
        if (_winAdBonusClaimed.value || _winAdBonusClaimInProgress.value) return null
        if (!winBonusAdEligible.value) {
            _redeemResultMessage.value = "You've reached today's bonus ad limit."
            return null
        }

        return WinBonusTicket(gameId = gameId, amount = amount)
    }

    /**
     * Grants the optional post-win ad bonus. Call only from the ad SDK's
     * earned-reward callback.
     *
     * The settlement key `winbonus-<gameId>` makes the claim idempotent:
     * duplicate callbacks or retries never pay twice. Production wallet
     * integrity is enforced server-side when the server economy is enabled.
     */
    fun claimWinAdBonus(ticket: WinBonusTicket) {
        if (ticket.amount <= 0) return
        if (_winAdBonusClaimInProgress.value) return

        _winAdBonusClaimInProgress.value = true

        val settlement = PendingSettlement(
            id = "winbonus-${ticket.gameId}",
            owner = settlements.currentOwner(),
            type = WalletTransactionType.WIN_AD_BONUS,
            plan = GameSettlementRules.forWinAdBonus(ticket.amount),
            createdAtMillis = System.currentTimeMillis()
        )

        viewModelScope.launch {
            try {
                adUsageStore.recordRewarded(RewardedAdPlacement.WIN_BONUS, settlement.createdAtMillis)
                val outcome = settlements.submit(settlement)
                if (ticket.gameId == gameId) _winAdBonusClaimed.value = true

                _redeemResultMessage.value = when (outcome) {
                    SettlementResult.Applied -> "+${ticket.amount} bonus Coins added!"
                    SettlementResult.AlreadyApplied -> "Bonus already added."
                    SettlementResult.Queued ->
                        "Bonus saved. +${ticket.amount} Coins will be added when you reconnect."
                }
            } catch (exception: Exception) {
                Log.w(TAG, "Couldn't save the win bonus", exception)
                _redeemResultMessage.value =
                    "Couldn't add the bonus Coins. Please try again."
            } finally {
                _winAdBonusClaimInProgress.value = false
            }
        }
    }

    /**
     * Returns a claim id when the Life recovery ad may be shown now, or null.
     * Pass the id to [claimLifeRecovery] from the earned-reward callback.
     */
    fun lifeRecoveryClaimId(): String? {
        if (_lifeRecoveryInProgress.value) return null
        if (lifeRecoveryAd.value != AdEligibility.Eligible) return null
        return "liferecovery-${UUID.randomUUID()}"
    }

    /**
     * Recovers exactly one Life (capped at the maximum). Call only from the
     * ad SDK's earned-reward callback. The claim id makes it idempotent.
     */
    fun claimLifeRecovery(claimId: String) {
        if (_lifeRecoveryInProgress.value) return
        _lifeRecoveryInProgress.value = true

        val settlement = PendingSettlement(
            id = claimId,
            owner = settlements.currentOwner(),
            type = WalletTransactionType.LIFE_RECOVERY_AD,
            plan = SettlementPlan(lifeCredit = 1),
            createdAtMillis = System.currentTimeMillis()
        )

        viewModelScope.launch {
            try {
                adUsageStore.recordRewarded(RewardedAdPlacement.LIFE_RECOVERY, settlement.createdAtMillis)
                _redeemResultMessage.value = when (settlements.submit(settlement)) {
                    SettlementResult.Applied -> "1 Life recovered!"
                    SettlementResult.AlreadyApplied -> "Life already recovered."
                    SettlementResult.Queued -> "Life saved. It will be added when you reconnect."
                }
            } catch (exception: Exception) {
                Log.w(TAG, "Couldn't recover a Life", exception)
                _redeemResultMessage.value = "Couldn't recover a Life. Please try again."
            } finally {
                _lifeRecoveryInProgress.value = false
            }
        }
    }

    fun openLuckSpin() {
        if (luckSpinEligibility.value == null) return
        _luckSpinResult.value = null
        _luckSpinOpen.value = true
    }

    fun closeLuckSpin() {
        if (_luckSpinBusy.value) return
        _luckSpinOpen.value = false
        _luckSpinResult.value = null
    }

    /** Returns a claim id when a spin may start now, or null. */
    fun luckSpinClaimId(): String? {
        if (_luckSpinBusy.value) return null
        if (luckSpinEligibility.value != AdEligibility.Eligible) return null
        return "luckspin-${UUID.randomUUID()}"
    }

    /**
     * Selects and credits one prize after the ad's earned-reward callback.
     *
     * The same slot index drives the wheel animation and the credited
     * amount, so what the player sees is what they get. With the server
     * economy enabled, the server chooses and credits the prize.
     */
    fun claimLuckSpin(claimId: String) {
        if (_luckSpinBusy.value) return
        _luckSpinBusy.value = true

        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                adUsageStore.recordRewarded(RewardedAdPlacement.LUCK_SPIN, now)

                val table = luckSpinPrizes.value
                val index = LuckPrizeTable.selectIndex(table)
                    ?: throw IllegalStateException("Luck Spin has no prizes")
                val plan = table[index].toSettlementPlan()

                if (!plan.isEmpty) {
                    settlements.submit(
                        PendingSettlement(
                            id = claimId,
                            owner = settlements.currentOwner(),
                            type = WalletTransactionType.LUCK_SPIN,
                            plan = plan,
                            createdAtMillis = now
                        )
                    )
                }

                val prize = table[index]
                luckSpinNonce++
                _luckSpinResult.value = LuckSpinResultUi(
                    spinNonce = luckSpinNonce,
                    prizes = table,
                    prizeIndex = index,
                    message = when (prize.type) {
                        LuckPrizeType.COINS -> "You won ${prize.amount} Coins!"
                        LuckPrizeType.GEMS ->
                            if (prize.amount == 1) "You won 1 Gem!" else "You won ${prize.amount} Gems!"

                        LuckPrizeType.NONE -> "No reward this time. Better luck on your next spin!"
                    }
                )
            } catch (exception: Exception) {
                Log.w(TAG, "Luck Spin failed", exception)
                _redeemResultMessage.value =
                    "Couldn't complete the spin. Check your connection and try again."
            } finally {
                _luckSpinBusy.value = false
            }
        }
    }

    private fun formatWait(millis: Long): String {
        val minutes = ((millis.coerceAtLeast(0) + 59_999) / 60_000).toInt()
        return if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m"
    }

    fun consumeRedeemResultMessage() {
        _redeemResultMessage.value = null
    }

    override fun onCleared() {
        timerJob?.cancel()
        feedback.release()

        networkCallback?.let { callback ->
            val connectivity = getApplication<Application>()
                .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            runCatching { connectivity?.unregisterNetworkCallback(callback) }
        }
    }
}