package com.genoma.mines

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.genoma.mines.game.Difficulty
import com.genoma.mines.game.GameStatus
import com.genoma.mines.ui.theme.MinesTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.genoma.mines.viewmodel.MinesweeperViewModel

private sealed class Screen {
    object Home : Screen()
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
    var selectedDifficulty by remember {
        mutableStateOf(Difficulty.EASY)
    }

    var screen by remember {
        mutableStateOf<Screen>(Screen.Home)
    }

    val gameState by viewModel.gameState.collectAsState()

    when (val current = screen) {

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
                    // We'll implement this later
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
                            isDetonated = state.detonatedCellIndex == index
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
                    },
                )

            } else {
                screen = Screen.Home
            }
        }
    }
}

/**
 * Screen-level spacing scale, kept in one place so it's easy to retune
 * without hunting through the composable tree for magic numbers.
 */
private object Spacing {
    val screenHorizontal = 24.dp
    val screenTop = 20.dp
    val screenBottom = 24.dp
    val titleToSubtitle = 4.dp
    val optionGap = 10.dp
    val small = 8.dp
    val medium = 14.dp
    val large = 22.dp
}

@Composable
fun HomeScreen(
    selectedDifficulty: Difficulty,
    onDifficultySelected: (Difficulty) -> Unit,
    onStartGame: () -> Unit,
    onHowToPlay: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Respect status/navigation bars since the activity draws edge-to-edge.
                .padding(WindowInsets.safeDrawing.asPaddingValues())
                .padding(horizontal = Spacing.screenHorizontal)
                .padding(top = Spacing.screenTop, bottom = Spacing.screenBottom)
        ) {

            Text(
                text = "Minesweeper",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(Spacing.titleToSubtitle))

            Text(
                text = "Clear the board without hitting a mine.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "CHOOSE DIFFICULTY",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.optionGap))

            Column(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                Difficulty.entries.forEach { difficulty ->
                    DifficultyOption(
                        difficulty = difficulty,
                        selected = selectedDifficulty == difficulty,
                        onClick = { onDifficultySelected(difficulty) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.large))

            Button(
                onClick = onStartGame,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.height(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Start game",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Timer,
                    contentDescription = null,
                    modifier = Modifier.height(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Best time: 01:42",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(Spacing.large))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onHowToPlay)
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.HelpOutline,
                    contentDescription = null,
                    modifier = Modifier.height(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "How to play",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun DifficultyOption(
    difficulty: Difficulty,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "${difficulty.displayName()} difficulty, " +
                        "${difficulty.rows} by ${difficulty.columns} grid, ${difficulty.mines} mines"
            }
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
                interactionSource = interactionSource,
                indication = null
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (selected) 2.dp else 0.dp
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (selected) {
                    Icons.Outlined.RadioButtonChecked
                } else {
                    Icons.Outlined.RadioButtonUnchecked
                },
                contentDescription = null,
                tint = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                },
                modifier = Modifier.height(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = difficulty.displayName(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${difficulty.rows} × ${difficulty.columns} · ${difficulty.mines} mines",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun Difficulty.displayName(): String {
    return when (this) {
        Difficulty.EASY -> "Easy"
        Difficulty.MEDIUM -> "Medium"
        Difficulty.HARD -> "Hard"
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    MinesTheme {
        HomeScreen(
            selectedDifficulty = Difficulty.MEDIUM,
            onDifficultySelected = {},
            onStartGame = {},
            onHowToPlay = {}
        )
    }
}