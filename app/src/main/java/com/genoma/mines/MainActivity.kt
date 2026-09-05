package com.genoma.mines

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.genoma.mines.ui.screens.GameScreen
import com.genoma.mines.ui.screens.HomeScreen
import com.genoma.mines.ui.screens.HowToPlayScreen
import com.genoma.mines.ui.screens.LoginScreen
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
    data class Game(val difficulty: Difficulty) : Screen()
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            MinesTheme {
                MinesweeperApp()
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
                }
            )
        }

        is Screen.Settings -> {
            SettingsScreen(
                soundEnabled = soundEnabled,
                hapticsEnabled = hapticsEnabled,
                isSignedIn = userProfile != null,
                userName = userProfile?.displayName,

                onSoundToggle = { enabled ->
                    viewModel.setSoundEnabled(enabled)
                },

                onHapticsToggle = { enabled ->
                    viewModel.setHapticsEnabled(enabled)
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
    }
}