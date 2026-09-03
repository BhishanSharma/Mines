package com.genoma.mines

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.genoma.mines.game.Difficulty
import com.genoma.mines.game.GameStatus
import com.genoma.mines.ui.theme.CountEight
import com.genoma.mines.ui.theme.CountFive
import com.genoma.mines.ui.theme.CountFour
import com.genoma.mines.ui.theme.CountOne
import com.genoma.mines.ui.theme.CountSeven
import com.genoma.mines.ui.theme.CountSix
import com.genoma.mines.ui.theme.CountThree
import com.genoma.mines.ui.theme.CountTwo
import com.genoma.mines.ui.theme.MinesTheme

data class CellUiState(
    val isRevealed: Boolean = false,
    val isFlagged: Boolean = false,
    val isMine: Boolean = false,
    val adjacentMines: Int = 0,
    val isDetonated: Boolean = false
)

private object GameSpacing {
    val screenHorizontal = 16.dp
    val screenTop = 16.dp
    val screenBottom = 16.dp
    val barToBoard = 20.dp
    val cellGap = 3.dp
}

@Composable
fun GameScreen(
    difficulty: Difficulty,
    cells: List<CellUiState>,
    flagsPlaced: Int,
    elapsedSeconds: Int,
    status: GameStatus,
    onCellTap: (index: Int) -> Unit,
    onCellLongPress: (index: Int) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
    onPause: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.safeDrawing.asPaddingValues())
                .padding(
                    horizontal = GameSpacing.screenHorizontal
                )
                .padding(
                    top = GameSpacing.screenTop,
                    bottom = GameSpacing.screenBottom
                )
        ) {

            GameTopBar(
                onBack = onBack
            )

            Spacer(
                modifier = Modifier.height(GameSpacing.barToBoard)
            )

            GameStatusBar(
                minesRemaining = difficulty.mines - flagsPlaced,
                elapsedSeconds = elapsedSeconds,
                status = status,
                onReset = onReset,
                onPause = onPause
            )

            Spacer(
                modifier = Modifier.height(GameSpacing.barToBoard)
            )

            when (status) {

                GameStatus.WON -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        ResultMessage(
                            title = "You won!",
                            message = "Great job! You cleared the board.",
                            isWin = true,
                            onReset = onReset
                        )
                    }
                }

                GameStatus.LOST -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        ResultMessage(
                            title = "Game over",
                            message = "You hit a mine.",
                            isWin = false,
                            onReset = onReset
                        )
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        MineBoard(
                            columns = difficulty.columns,
                            cells = cells,
                            interactionEnabled = status == GameStatus.PLAYING,
                            onCellTap = onCellTap,
                            onCellLongPress = onCellLongPress,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GameTopBar(
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to home",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(20.dp)
            )
        }

        Text(
            text = "Mines",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun GameStatusBar(
    minesRemaining: Int,
    elapsedSeconds: Int,
    status: GameStatus,
    onReset: () -> Unit,
    onPause: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                MaterialTheme.colorScheme.surfaceVariant
            )
            .padding(
                horizontal = 18.dp,
                vertical = 12.dp
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        StatusReadout(
            label = "MINES",
            value = minesRemaining
                .coerceAtLeast(0)
                .toString()
                .padStart(3, '0')
        )

        ResetButton(
            status = status,
            onClick = {
                when (status) {
                    GameStatus.PLAYING,
                    GameStatus.PAUSED -> onPause()

                    else -> onReset()
                }
            }
        )

        StatusReadout(
            label = "TIME",
            value = elapsedSeconds
                .coerceIn(0, 999)
                .toString()
                .padStart(3, '0')
        )
    }
}

@Composable
private fun ResultMessage(
    title: String,
    message: String,
    isWin: Boolean,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val badgeColor = if (isWin) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer
        }
        val badgeIconColor = if (isWin) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onErrorContainer
        }

        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(badgeColor),
            contentAlignment = Alignment.Center
        ) {
            if (isWin) {
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = badgeIconColor,
                    modifier = Modifier.size(30.dp)
                )
            } else {
                MineIcon(
                    color = badgeIconColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    MaterialTheme.colorScheme.primaryContainer
                )
                .clickable(onClick = onReset)
                .padding(
                    horizontal = 24.dp,
                    vertical = 12.dp
                )
        ) {
            Text(
                text = "Play again",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun StatusReadout(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(
                    MaterialTheme.colorScheme.surface
                )
                .padding(
                    horizontal = 10.dp,
                    vertical = 4.dp
                )
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(
            modifier = Modifier.height(2.dp)
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ResetButton(
    status: GameStatus,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(
                MaterialTheme.colorScheme.primaryContainer
            )
            .clickable(
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        val iconColor = MaterialTheme.colorScheme.onPrimaryContainer

        when (status) {
            GameStatus.READY -> Icon(
                imageVector = Icons.Filled.SentimentSatisfied,
                contentDescription = "Start game",
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )

            GameStatus.PLAYING -> Icon(
                imageVector = Icons.Filled.Pause,
                contentDescription = "Pause game",
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )

            GameStatus.PAUSED -> Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Resume game",
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )

            GameStatus.WON -> Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = "You won, tap to play again",
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )

            GameStatus.LOST -> Icon(
                imageVector = Icons.Filled.SentimentVeryDissatisfied,
                contentDescription = "Game over, tap to play again",
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun MineBoard(
    columns: Int,
    cells: List<CellUiState>,
    interactionEnabled: Boolean,
    onCellTap: (Int) -> Unit,
    onCellLongPress: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            userScrollEnabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(
                GameSpacing.cellGap
            ),
            verticalArrangement = Arrangement.spacedBy(
                GameSpacing.cellGap
            )
        ) {
            items(cells.size) { index ->
                MineCell(
                    state = cells[index],
                    enabled = interactionEnabled,
                    onTap = {
                        onCellTap(index)
                    },
                    onLongPress = {
                        onCellLongPress(index)
                    }
                )
            }
        }
    }
}

@Composable
private fun MineCell(
    state: CellUiState,
    enabled: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val targetColor = when {
        state.isDetonated ->
            MaterialTheme.colorScheme.errorContainer

        state.isRevealed ->
            MaterialTheme.colorScheme.surface

        else ->
            MaterialTheme.colorScheme.primaryContainer
    }

    // Smoothly cross-fades when a cell flips from hidden to revealed,
    // instead of popping instantly.
    val backgroundColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 180),
        label = "cellBackground"
    )

    // Unrevealed cells get a faint highlight border to read as
    // "raised" tiles waiting to be tapped; revealed cells sit flush.
    val borderColor = if (!state.isRevealed) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
    } else {
        Color.Transparent
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(6.dp)
            )
            .combinedClickable(
                enabled = enabled,
                onClick = onTap,
                onLongClick = onLongPress
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            state.isFlagged && !state.isRevealed -> {
                Icon(
                    imageVector = Icons.Filled.Flag,
                    contentDescription = "Flagged",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }

            state.isRevealed && state.isMine -> {
                MineIcon(
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(16.dp)
                )
            }

            state.isRevealed && state.adjacentMines > 0 -> {
                Text(
                    text = state.adjacentMines.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = colorForCount(
                        state.adjacentMines
                    )
                )
            }

            else -> {
                // Empty revealed cell or untouched cell.
            }
        }
    }
}

/**
 * A minimal vector-drawn bomb: a round body with a short fuse.
 * Avoids relying on emoji (which render inconsistently across devices)
 * or a mismatched stock icon, since Material Icons has no literal bomb.
 */
@Composable
fun MineIcon(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Canvas(modifier = modifier) {
        val bodyRadius = size.minDimension * 0.36f
        val bodyCenter = Offset(
            x = size.width / 2f,
            y = size.height / 2f + size.height * 0.08f
        )

        // Body
        drawCircle(
            color = color,
            radius = bodyRadius,
            center = bodyCenter
        )

        // Fuse, angled up and to the right
        val fuseStart = Offset(
            x = bodyCenter.x + bodyRadius * 0.55f,
            y = bodyCenter.y - bodyRadius * 0.75f
        )
        val fuseEnd = Offset(
            x = fuseStart.x + size.width * 0.16f,
            y = fuseStart.y - size.height * 0.22f
        )
        drawLine(
            color = color,
            start = fuseStart,
            end = fuseEnd,
            strokeWidth = size.minDimension * 0.09f,
            cap = StrokeCap.Round
        )

        // Spark at the fuse tip
        drawCircle(
            color = color,
            radius = size.minDimension * 0.07f,
            center = fuseEnd
        )

        // Small highlight to give the body some dimension
        drawCircle(
            color = Color.White.copy(alpha = 0.35f),
            radius = bodyRadius * 0.28f,
            center = Offset(
                x = bodyCenter.x - bodyRadius * 0.35f,
                y = bodyCenter.y - bodyRadius * 0.35f
            )
        )
    }
}

@Composable
private fun colorForCount(
    count: Int
): Color {
    return when (count) {
        1 -> CountOne
        2 -> CountTwo
        3 -> CountThree
        4 -> CountFour
        5 -> CountFive
        6 -> CountSix
        7 -> CountSeven
        else -> CountEight
    }
}

@Preview(showBackground = true)
@Composable
private fun GameScreenPreview() {
    val difficulty = Difficulty.EASY

    val sampleCells = remember {
        List(
            difficulty.rows * difficulty.columns
        ) { index ->
            when {
                index == 4 ->
                    CellUiState(
                        isRevealed = true,
                        adjacentMines = 2
                    )

                index == 5 ->
                    CellUiState(
                        isRevealed = true,
                        adjacentMines = 0
                    )

                index == 6 ->
                    CellUiState(
                        isFlagged = true
                    )

                index == 10 ->
                    CellUiState(
                        isRevealed = true,
                        isMine = true,
                        isDetonated = true
                    )

                else ->
                    CellUiState()
            }
        }
    }

    var status by remember {
        mutableStateOf(GameStatus.PLAYING)
    }

    MinesTheme {
        GameScreen(
            difficulty = difficulty,
            cells = sampleCells,
            flagsPlaced = 1,
            elapsedSeconds = 42,
            status = status,
            onCellTap = {},
            onCellLongPress = {},
            onReset = {
                status = GameStatus.PLAYING
            },
            onBack = {},
            onPause = {
                status =
                    if (status == GameStatus.PLAYING) {
                        GameStatus.PAUSED
                    } else {
                        GameStatus.PLAYING
                    }
            }
        )
    }
}