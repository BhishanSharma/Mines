package com.genoma.mines

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.genoma.mines.auth.GoogleAuthManager
import com.genoma.mines.auth.GoogleSignInResult
import com.genoma.mines.auth.UserSessionStore
import com.genoma.mines.data.GameHistoryItem
import com.genoma.mines.data.UserStatistics
import com.genoma.mines.data.remote.FirestoreGameRepository
import com.genoma.mines.game.Difficulty
import com.genoma.mines.ui.screens.CellUiState
import com.genoma.mines.ui.screens.FeedbackScreen
import com.genoma.mines.ui.screens.GameScreen
import com.genoma.mines.ui.screens.GuestHistoryScreen
import com.genoma.mines.ui.screens.HomeScreen
import com.genoma.mines.ui.screens.HowToPlayScreen
import com.genoma.mines.ui.screens.LoginScreen
import com.genoma.mines.ui.screens.ProfileScreen
import com.genoma.mines.ui.screens.SettingsScreen
import com.genoma.mines.ui.theme.MinesTheme
import com.genoma.mines.viewmodel.MinesweeperViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private sealed class Screen {
    object Login : Screen()
    object Home : Screen()
    object Settings : Screen()
    object HowToPlay : Screen()
    object Profile : Screen()
    object GuestHistory : Screen()
    object Feedback : Screen()
    data class Game(val difficulty: Difficulty) : Screen()
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            val viewModel: MinesweeperViewModel = viewModel()

            // Null means "no saved preference" — fall back to the system
            // setting until the user explicitly picks one in Settings.
            val darkThemePreference by viewModel.darkTheme.collectAsState()
            val systemInDarkTheme = isSystemInDarkTheme()
            val darkTheme = darkThemePreference ?: systemInDarkTheme

            MinesTheme(darkTheme = darkTheme) {
                MinesweeperApp(
                    viewModel = viewModel,
                    darkTheme = darkTheme
                )
            }
        }
    }
}

@Composable
fun MinesweeperApp(
    viewModel: MinesweeperViewModel = viewModel(),
    darkTheme: Boolean = isSystemInDarkTheme()
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

    val userProfile by sessionStore.userProfile.collectAsState(
        initial = null
    )

    val webClientId = androidx.compose.ui.res.stringResource(
        R.string.google_web_client_id
    )

    var screen by remember {
        mutableStateOf<Screen>(Screen.Login)
    }

    var selectedDifficulty by remember {
        mutableStateOf(Difficulty.EASY)
    }

    var sessionLoaded by remember {
        mutableStateOf(false)
    }

    var guestHistory by remember { mutableStateOf<List<GameHistoryItem>>(emptyList()) }
    var guestHistoryLoading by remember { mutableStateOf(true) }

    var userStatistics by remember { mutableStateOf(UserStatistics.EMPTY) }
    var statisticsLoading by remember { mutableStateOf(true) }

    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsState()
    val gameState by viewModel.gameState.collectAsState()
    val selectedAvatar by viewModel.selectedAvatar.collectAsState()

    LaunchedEffect(Unit) {
        val savedProfile = sessionStore.userProfile.first()

        if (savedProfile != null) {
            screen = Screen.Home
        }

        sessionLoaded = true
    }

    LaunchedEffect(screen) {
        when (screen) {
            is Screen.GuestHistory -> {
                guestHistoryLoading = true
                guestHistory = viewModel.loadGameHistory()
                guestHistoryLoading = false
            }

            is Screen.Profile -> {
                statisticsLoading = true
                userStatistics = viewModel.loadStatistics()
                statisticsLoading = false
            }

            else -> Unit
        }
    }

    if (!sessionLoaded) {
        return
    }

    BackHandler(enabled = screen !is Screen.Home && screen !is Screen.Login) {
        when (screen) {
            is Screen.Game -> {
                viewModel.goBackToHome()
                screen = Screen.Home
            }

            is Screen.Settings,
            is Screen.HowToPlay,
            is Screen.Profile,
            is Screen.GuestHistory -> {
                screen = Screen.Home
            }

            is Screen.Feedback -> {
                screen = Screen.Settings
            }

            else -> {
                // Unreachable: Home/Login are excluded via `enabled` above.
            }
        }
    }

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

                                firestoreRepository.ensureUserDocument(
                                    uid = result.profile.id,
                                    name = result.profile.displayName,
                                    email = result.profile.email,
                                    photoUrl = result.profile.photoUrl
                                )

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

                onHowToPlay = {
                    screen = Screen.HowToPlay
                },

                onOpenSettings = {
                    screen = Screen.Settings
                },

                onOpenProfile = {
                    screen = Screen.Profile
                },

                username = userProfile?.displayName ?: "Guest",
                selectedAvatar = selectedAvatar,              // ← add this line
                gamesWon = userStatistics.totalScore

            )
        }

        is Screen.Settings -> {
            SettingsScreen(
                soundEnabled = soundEnabled,
                hapticsEnabled = hapticsEnabled,
                darkTheme = darkTheme,
                isSignedIn = userProfile != null,
                userName = userProfile?.displayName,

                onSoundToggle = { enabled ->
                    viewModel.setSoundEnabled(enabled)
                },

                onHapticsToggle = { enabled ->
                    viewModel.setHapticsEnabled(enabled)
                },

                onThemeToggle = { enabled ->
                    viewModel.setDarkTheme(enabled)
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

                onSignInClick = {           // ← add this block
                    screen = Screen.Login
                },

                onBack = {
                    screen = Screen.Home
                }
            )
        }

        is Screen.Feedback -> {
            FeedbackScreen(
                userName = userProfile?.displayName ?: "Guest",
                userEmail = userProfile?.email ?: "",

                onSubmit = { _ ->
                    android.widget.Toast.makeText(
                        context,
                        "Thanks for the feedback!",
                        android.widget.Toast.LENGTH_LONG
                    ).show()

                    screen = Screen.Settings
                },

                onBack = {
                    screen = Screen.Settings
                }
            )
        }

        is Screen.HowToPlay -> {
            HowToPlayScreen(
                onBack = {
                    screen = Screen.Home
                }
            )
        }

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

        is Screen.Profile -> {

            ProfileScreen(
                username = userProfile?.displayName ?: "Player",
                statistics = userStatistics,
                isLoading = statisticsLoading,
                selectedAvatar = selectedAvatar,
                onAvatarSelected = { avatar ->
                    viewModel.setAvatar(avatar)
                },
                onBack = {
                    screen = Screen.Home
                }
            )
        }

        is Screen.GuestHistory -> {
            GuestHistoryScreen(
                isLoading = guestHistoryLoading,
                history = guestHistory,
                onBack = {
                    screen = Screen.Home
                }
            )
        }
    }
}