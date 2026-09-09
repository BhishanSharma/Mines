package com.genoma.mines

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.genoma.mines.auth.AccountDeletionResult
import com.genoma.mines.auth.GoogleAuthManager
import com.genoma.mines.auth.GoogleSignInResult
import com.genoma.mines.auth.UserSessionStore
import com.genoma.mines.data.AchievementCalculator
import com.genoma.mines.data.GameHistoryItem
import com.genoma.mines.data.UserStatistics
import com.genoma.mines.data.local.GuestGameDatabase
import com.genoma.mines.data.local.GuestGameRepository
import com.genoma.mines.data.remote.FirestoreGameRepository
import com.google.firebase.auth.FirebaseAuth
import com.genoma.mines.game.Difficulty
import com.genoma.mines.game.GameResultType
import com.genoma.mines.game.LevelCalculator
import com.genoma.mines.ui.components.BottomNavItem
import com.genoma.mines.ui.components.BottomNavbar
import com.genoma.mines.ui.screens.AchievementScreen
import com.genoma.mines.ui.screens.CelebrationScreen
import com.genoma.mines.ui.screens.CellUiState
import com.genoma.mines.ui.screens.FeedbackScreen
import com.genoma.mines.ui.screens.GameScreen
import com.genoma.mines.ui.screens.HistoryScreen
import com.genoma.mines.ui.screens.HomeScreen
import com.genoma.mines.ui.screens.displayName
import com.genoma.mines.ui.screens.HowToPlayScreen
import com.genoma.mines.ui.screens.LoginScreen
import com.genoma.mines.ui.screens.ProfileScreen
import com.genoma.mines.ui.screens.SettingsScreen
import com.genoma.mines.ui.screens.StoreScreen
import com.genoma.mines.ui.screens.ThemePreference
import com.genoma.mines.ui.theme.MinesTheme
import com.genoma.mines.viewmodel.MinesweeperViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Formats a duration in seconds as mm:ss, for best-time display on Profile. */
private fun formatBestTime(totalSeconds: Long): String {
    val clamped = totalSeconds.coerceIn(0, 99 * 60 + 59)
    val minutes = clamped / 60
    val seconds = clamped % 60
    return "%02d:%02d".format(minutes, seconds)
}

private sealed class Screen {
    object Login : Screen()
    object Home : Screen()
    object Settings : Screen()
    object HowToPlay : Screen()
    object Profile : Screen()
    object History : Screen()
    object Achievements : Screen()
    object Feedback : Screen()
    object Celebration : Screen()
    object Store : Screen()
    data class Game(val difficulty: Difficulty) : Screen()
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(
                WindowInsetsCompat.Type.statusBars() or
                        WindowInsetsCompat.Type.navigationBars()
            )

            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        enableEdgeToEdge()

        setContent {
            val viewModel: MinesweeperViewModel = viewModel()

            val darkThemePreference by viewModel.darkTheme.collectAsState()
            val systemInDarkTheme = isSystemInDarkTheme()
            val darkTheme = darkThemePreference ?: systemInDarkTheme

            MinesTheme(darkTheme = darkTheme) {
                MinesweeperApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MinesweeperApp(
    viewModel: MinesweeperViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val scope = rememberCoroutineScope()

    val authManager = remember {
        GoogleAuthManager(context)
    }

    val sessionStore = remember {
        UserSessionStore(context)
    }

    val firestoreRepository = remember {
        FirestoreGameRepository()
    }

    val guestGameRepository = remember {
        GuestGameRepository(
            GuestGameDatabase.getInstance(context).guestGameDao()
        )
    }

    var isDeletingAccount by remember {
        mutableStateOf(false)
    }

    val userProfile by sessionStore.userProfile.collectAsState(
        initial = null
    )

    val webClientId = androidx.compose.ui.res.stringResource(
        R.string.google_web_client_id
    )

    var screen by remember {
        mutableStateOf<Screen>(Screen.Login)
    }

    var selectedBottomNavItem by remember {
        mutableStateOf(BottomNavItem.HOME)
    }

    var selectedDifficulty by remember {
        mutableStateOf(Difficulty.EASY)
    }

    var sessionLoaded by remember {
        mutableStateOf(false)
    }

    var gameHistory by remember {
        mutableStateOf<List<GameHistoryItem>>(emptyList())
    }

    var historyLoading by remember {
        mutableStateOf(true)
    }

    var userStatistics by remember {
        mutableStateOf(UserStatistics.EMPTY)
    }

    var statisticsLoading by remember {
        mutableStateOf(true)
    }

    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsState()
    val gameState by viewModel.gameState.collectAsState()
    val isNewBestTime by viewModel.isNewBestTime.collectAsState()
    val previousBestSeconds by viewModel.previousBestSeconds.collectAsState()
    val celebrationEvents by viewModel.celebrationEvents.collectAsState()
    val selectedAvatar by viewModel.selectedAvatar.collectAsState()

    val darkThemePreference by viewModel.darkTheme.collectAsState()
    val themePreference =
        ThemePreference.fromDarkThemeFlag(darkThemePreference)

    // Whenever the game just finished earns a level-up and/or an
    // achievement unlock, take the player to a dedicated celebration
    // screen instead of layering a dialog on top of the board. Once every
    // queued milestone has been acknowledged, send them back Home.
    LaunchedEffect(celebrationEvents, screen) {
        if (screen is Screen.Game && celebrationEvents.isNotEmpty()) {
            screen = Screen.Celebration
        } else if (screen is Screen.Celebration && celebrationEvents.isEmpty()) {
            viewModel.goBackToHome()
            selectedBottomNavItem = BottomNavItem.HOME
            screen = Screen.Home
        }
    }

    LaunchedEffect(Unit) {
        val savedProfile = sessionStore.userProfile.first()

        if (savedProfile != null) {
            screen = Screen.Home
        }

        sessionLoaded = true
    }

    LaunchedEffect(screen) {
        when (screen) {

            is Screen.History,
            is Screen.Achievements -> {
                historyLoading = true
                gameHistory = viewModel.loadGameHistory()
                historyLoading = false
            }

            is Screen.Store,

            is Screen.Profile -> {
                historyLoading = true
                statisticsLoading = true
                gameHistory = viewModel.loadGameHistory()
                userStatistics = viewModel.loadStatistics()
                historyLoading = false
                statisticsLoading = false
            }

            is Screen.Home -> {
                statisticsLoading = true
                userStatistics = viewModel.loadStatistics()
                statisticsLoading = false
            }

            else -> Unit
        }
    }

    val levelProgress = remember(gameHistory) {
        val wins = gameHistory.count { it.result == GameResultType.WIN }
        val losses = gameHistory.size - wins
        LevelCalculator.calculateProgress(
            LevelCalculator.calculateTotalXp(wins, losses)
        )
    }

    // Fastest win per difficulty, derived straight from game history — the
    // Profile screen already accepts this shape, it just wasn't being fed
    // real data before.
    val bestSecondsByDifficulty = remember(gameHistory) {
        gameHistory
            .filter { it.result == GameResultType.WIN }
            .groupBy { it.difficulty }
            .mapValues { (_, wins) -> wins.minOf { it.durationSeconds } }
    }

    val bestTimesByDifficultyLabel = remember(bestSecondsByDifficulty) {
        bestSecondsByDifficulty.mapKeys { (difficulty, _) ->
            difficulty.displayName()
        }.mapValues { (_, seconds) ->
            formatBestTime(seconds)
        }
    }

    val overallBestDifficulty = remember(bestSecondsByDifficulty) {
        bestSecondsByDifficulty.entries.minByOrNull { it.value }?.key
    }

    val bestTimeOverall = remember(overallBestDifficulty, bestSecondsByDifficulty) {
        overallBestDifficulty?.let { bestSecondsByDifficulty[it] }?.let(::formatBestTime)
    }

    val bestTimeDifficultyLabel = overallBestDifficulty?.displayName() ?: "Easy"

    val keepGoingMessage = remember(levelProgress) {
        val xpRemaining = (levelProgress.xpForNextLevel - levelProgress.currentXp)
            .coerceAtLeast(0)
        "Play more — $xpRemaining XP to level ${levelProgress.level + 1}."
    }

    val achievementTracks = remember(gameHistory) {
        AchievementCalculator.calculate(gameHistory)
    }

    if (!sessionLoaded) {
        return
    }

    BackHandler(
        enabled = screen !is Screen.Home && screen !is Screen.Login
    ) {
        when (screen) {

            is Screen.Game -> {
                viewModel.goBackToHome()
                screen = Screen.Home
            }

            is Screen.Settings,
            is Screen.HowToPlay,
            is Screen.Profile,
            is Screen.History,
            is Screen.Achievements -> {
                screen = Screen.Home
            }

            is Screen.Store -> {
                screen = Screen.Home
            }

            is Screen.Feedback -> {
                screen = Screen.Settings
            }

            else -> {
                // Home/Login are excluded above.
            }
        }
    }

    Scaffold(
        bottomBar = {


            if (screen !is Screen.Login && screen !is Screen.Game && screen !is Screen.Celebration) {

                BottomNavbar(
                    selectedItem = selectedBottomNavItem,

                    /*
                     * HOME
                     */
                    onHome = {
                        selectedBottomNavItem = BottomNavItem.HOME
                        screen = Screen.Home
                    },

                    /*
                     * HOW TO PLAY
                     */
                    onHowToPlay = {
                        selectedBottomNavItem = BottomNavItem.HOW_TO_PLAY
                        screen = Screen.HowToPlay
                    },

                    /*
                     * STATISTICS
                     */
                    onOpenSTORE = {
                        selectedBottomNavItem = BottomNavItem.STORE
                        screen = Screen.Store
                    },


                    onOpenAchievements = {
                        selectedBottomNavItem = BottomNavItem.ACHIEVEMENTS
                        screen = Screen.Achievements
                    },

                    onOpenMoreGames = {
                        android.widget.Toast.makeText(
                            context,
                            "More Games screen is not added yet.",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier.padding(innerPadding)
        ) {

            when (screen) {


                is Screen.Login -> {

                    LoginScreen(
                        onGoogleSignInClick = {

                            scope.launch {

                                val result = authManager.signIn(
                                    webClientId = webClientId,
                                    activity = activity
                                )

                                when (result) {

                                    is GoogleSignInResult.Success -> {

                                        sessionStore.save(result.profile)

                                        val isFirstTimeAccount =
                                            firestoreRepository.ensureUserDocument(
                                                uid = result.profile.id,
                                                name = result.profile.displayName,
                                                email = result.profile.email,
                                                photoUrl = result.profile.photoUrl
                                            )

                                        if (isFirstTimeAccount) {

                                            val localGuestGames =
                                                guestGameRepository.getHistory()

                                            if (localGuestGames.isNotEmpty()) {
                                                try {
                                                    firestoreRepository.migrateGuestGames(
                                                        uid = result.profile.id,
                                                        guestGames = localGuestGames
                                                    )
                                                    guestGameRepository.clearHistory()
                                                } catch (e: Exception) {
                                                    // Local data is left in place so
                                                    // nothing is lost — it'll be
                                                    // retried on the next sign-in
                                                    // attempt for this account.
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "Signed in, but couldn't sync " +
                                                                "your guest progress",
                                                        android.widget.Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            }
                                        }

                                        screen = Screen.Home
                                    }

                                    is GoogleSignInResult.Failure -> {

                                        android.widget.Toast.makeText(
                                            context,
                                            result.message,
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    }

                                    GoogleSignInResult.Cancelled -> {
                                        // User cancelled sign-in.
                                    }
                                }
                            }
                        },

                        onGuestClick = {
                            screen = Screen.Home
                        }
                    )
                }


                is Screen.Home -> {

                    HomeScreen(
                        selectedDifficulty = selectedDifficulty,

                        onDifficultySelected = {
                            selectedDifficulty = it
                        },

                        onStartGame = {
                            viewModel.startGame(selectedDifficulty)
                            screen = Screen.Game(selectedDifficulty)
                        },

                        onOpenSettings = {
                            screen = Screen.Settings
                        },

                        onOpenProfile = {
                            screen = Screen.Profile
                        },

                        username = userProfile?.displayName ?: "Guest",
                        selectedAvatar = selectedAvatar,
                        photoUrl = userProfile?.photoUrl,
                        gamesWon = userStatistics.totalScore
                    )
                }


                is Screen.Settings -> {

                    SettingsScreen(
                        soundEnabled = soundEnabled,
                        hapticsEnabled = hapticsEnabled,
                        themePreference = themePreference,
                        isSignedIn = userProfile != null,
                        userName = userProfile?.displayName,

                        onSoundToggle = { enabled ->
                            viewModel.setSoundEnabled(enabled)
                        },

                        onHapticsToggle = { enabled ->
                            viewModel.setHapticsEnabled(enabled)
                        },

                        onThemePreferenceChange = { preference ->
                            viewModel.setThemePreference(preference)
                        },

                        onFeedbackClick = {
                            screen = Screen.Feedback
                        },

                        onSignOut = {

                            scope.launch {

                                authManager.signOut()
                                sessionStore.clear()

                                screen = Screen.Login
                            }
                        },

                        onSignInClick = {
                            screen = Screen.Login
                        },

                        onDeleteAccount = {

                            scope.launch {

                                isDeletingAccount = true

                                // Captured before deletion starts, since
                                // deleting the Firebase user below clears
                                // currentUser.
                                val uid = FirebaseAuth.getInstance()
                                    .currentUser?.uid

                                if (uid == null) {
                                    isDeletingAccount = false
                                    android.widget.Toast.makeText(
                                        context,
                                        "No signed-in account to delete",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                    return@launch
                                }

                                try {
                                    // Online data first — deleting it
                                    // requires the still-valid auth
                                    // credentials that get invalidated
                                    // once the account itself is deleted.
                                    firestoreRepository.deleteAllUserData(uid)

                                    when (
                                        val result = authManager.deleteAccount(
                                            webClientId = webClientId,
                                            activity = activity
                                        )
                                    ) {
                                        is AccountDeletionResult.Success -> {

                                            // Local data — any leftover
                                            // on-device game history plus
                                            // the cached profile.
                                            guestGameRepository.clearHistory()
                                            sessionStore.clear()

                                            userStatistics = UserStatistics.EMPTY
                                            gameHistory = emptyList()

                                            android.widget.Toast.makeText(
                                                context,
                                                "Your account has been deleted",
                                                android.widget.Toast.LENGTH_LONG
                                            ).show()

                                            screen = Screen.Login
                                        }

                                        is AccountDeletionResult.Failure -> {
                                            android.widget.Toast.makeText(
                                                context,
                                                result.message,
                                                android.widget.Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }

                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(
                                        context,
                                        e.message ?: "Failed to delete account",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                } finally {
                                    isDeletingAccount = false
                                }
                            }
                        },

                        isDeletingAccount = isDeletingAccount,

                        onBack = {
                            screen = Screen.Home
                        }
                    )
                }

                /*
                 * FEEDBACK
                 */
                is Screen.Feedback -> {

                    val isSubmittingFeedback by
                    viewModel.isSubmittingFeedback.collectAsState()

                    val feedbackError by
                    viewModel.feedbackError.collectAsState()

                    val feedbackSubmitted by
                    viewModel.feedbackSubmitted.collectAsState()

                    LaunchedEffect(feedbackSubmitted) {

                        if (feedbackSubmitted) {

                            android.widget.Toast.makeText(
                                context,
                                "Thanks for the feedback!",
                                android.widget.Toast.LENGTH_LONG
                            ).show()

                            viewModel.resetFeedbackSubmitted()

                            screen = Screen.Settings
                        }
                    }

                    FeedbackScreen(
                        userName = userProfile?.displayName ?: "Guest",
                        userEmail = userProfile?.email ?: "",
                        isSubmitting = isSubmittingFeedback,
                        submitError = feedbackError,

                        onSubmit = { data ->

                            viewModel.submitFeedback(
                                data,
                                userId = userProfile?.id
                            )
                        },

                        onBack = {
                            screen = Screen.Settings
                        }
                    )
                }

                /*
                 * HOW TO PLAY
                 */
                is Screen.HowToPlay -> {

                    HowToPlayScreen(
                        onBack = {
                            screen = Screen.Home
                        }
                    )
                }

                /*
                 * ACHIEVEMENTS
                 *
                 * This is now a primary bottom-navigation destination.
                 */
                is Screen.Achievements -> {
                    AchievementScreen(
                        tracks = achievementTracks,
                        isLoading = historyLoading
                    )
                }

                /*
                 * GAME
                 */
                is Screen.Game -> {

                    val state = gameState

                    if (state != null) {

                        GameScreen(

                            difficulty = state.difficulty,

                            cells = state.cells.mapIndexed { index, cell ->

                                CellUiState(
                                    isRevealed = cell.isRevealed,
                                    isFlagged = cell.isFlagged,
                                    isMine = cell.isMine,
                                    adjacentMines = cell.adjacentMines,
                                    isDetonated =
                                        state.detonatedCellIndex == index
                                )
                            },

                            flagsPlaced = state.flagsPlaced,
                            elapsedSeconds = state.elapsedSeconds,
                            status = state.status,

                            isNewBestTime = isNewBestTime,
                            previousBestSeconds = previousBestSeconds,

                            onCellTap = { index ->
                                viewModel.revealCell(index)
                            },

                            onCellLongPress = { index ->
                                viewModel.toggleFlag(index)
                            },

                            onReset = {
                                viewModel.resetGame()
                            },

                            onBack = {
                                viewModel.goBackToHome()
                                screen = Screen.Home
                            },

                            onPause = {
                                viewModel.togglePause()
                            }
                        )

                    } else {

                        screen = Screen.Home
                    }
                }

                /*
                 * CELEBRATION
                 */
                is Screen.Celebration -> {

                    CelebrationScreen(
                        events = celebrationEvents,
                        onContinue = {
                            viewModel.consumeCelebrationEvent()
                        }
                    )
                }

                /*
                 * PROFILE / STATISTICS
                 */
                is Screen.Profile -> {

                    ProfileScreen(
                        username = userProfile?.displayName ?: "Player",
                        statistics = userStatistics,
                        isLoading = statisticsLoading || historyLoading,
                        selectedAvatar = selectedAvatar,
                        photoUrl = userProfile?.photoUrl,

                        history = gameHistory,

                        level = levelProgress.level,
                        currentXp = levelProgress.currentXp,
                        xpForNextLevel = levelProgress.xpForNextLevel,
                        keepGoingMessage = keepGoingMessage,

                        bestTimeOverall = bestTimeOverall,
                        bestTimeDifficultyLabel = bestTimeDifficultyLabel,
                        bestTimes = bestTimesByDifficultyLabel,
                        highlightedDifficultyLabel = bestTimeDifficultyLabel,

                        onAvatarSelected = { avatar ->
                            viewModel.setAvatar(avatar)
                        },

                        onOpenSettings = {
                            screen = Screen.Settings
                        },

                        onKeepGoingClick = {
                            screen = Screen.Home
                        },

                        onDifficultyClick = { label ->
                            val difficulty = Difficulty.entries.first {
                                it.displayName() == label
                            }
                            selectedDifficulty = difficulty
                            viewModel.startGame(difficulty)
                            screen = Screen.Game(difficulty)
                        },

                        onSeeAllHistory = {
                            screen = Screen.History
                        },

                        onBack = {
                            screen = Screen.Home
                        }
                    )
                }

                /*
                 * FULL GAME HISTORY
                 */
                is Screen.History -> {

                    HistoryScreen(
                        isLoading = historyLoading,
                        history = gameHistory,

                        onBack = {
                            screen = Screen.Profile
                        }
                    )
                }

                else -> {}
            }
        }
    }
}