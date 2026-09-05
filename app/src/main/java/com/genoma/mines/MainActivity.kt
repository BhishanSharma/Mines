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
import com.genoma.mines.game.Difficulty
import com.genoma.mines.ui.screens.CellUiState
import com.genoma.mines.ui.screens.FeedbackScreen
import com.genoma.mines.ui.screens.GameScreen
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

    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsState()
    val gameState by viewModel.gameState.collectAsState()

    LaunchedEffect(Unit) {
        val savedProfile = sessionStore.userProfile.first()

        if (savedProfile != null) {
            screen = Screen.Home
        }

        sessionLoaded = true
    }

    if (!sessionLoaded) {
        return
    }

    // The app manages navigation itself via `screen` rather than Navigation
    // Compose, so system/gesture back does nothing by default except close
    // the Activity. This routes it through the same "back" action each
    // screen's own back button already uses. Disabled on Home and Login
    // since those are the app's root screens — back there should behave
    // normally and exit the app.
    BackHandler(enabled = screen !is Screen.Home && screen !is Screen.Login) {
        when (screen) {
            is Screen.Game -> {
                viewModel.goBackToHome()
                screen = Screen.Home
            }

            is Screen.Settings,
            is Screen.HowToPlay,
            is Screen.Profile -> {
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
                }
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

                onBack = {
                    screen = Screen.Home
                }
            )
        }

        is Screen.Feedback -> {
            FeedbackScreen(
                userName = userProfile?.displayName ?: "Guest",
                userEmail = userProfile?.email ?: "",

                onSubmit = { feedback ->
                    // TODO: replace with a real submission path (API call,
                    // email intent, etc.) once one exists. For now this
                    // just confirms receipt and returns to Settings.
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
            val selectedAvatar by viewModel.selectedAvatar.collectAsState()

            ProfileScreen(
                username = "${userProfile?.displayName}",
                selectedAvatar = selectedAvatar,
                onAvatarSelected = { avatar ->
                    viewModel.setAvatar(avatar)
                },
                onBack = {
                    screen = Screen.Home
                }
            )
        }
    }
}