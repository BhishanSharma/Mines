package com.genoma.mines.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
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
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem

data class CellUiState(
    val isRevealed: Boolean = false,
    val isFlagged: Boolean = false,
    val isMine: Boolean = false,
    val adjacentMines: Int = 0,
    val isDetonated: Boolean = false
)

/** Accent used to make a new-best-time win banner stand out from a normal win. */
private val BestTimeGold = Color(0xFFD4A017)

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
    isNewBestTime: Boolean = false,
    previousBestSeconds: Long? = null,
    onCellTap: (index: Int) -> Unit,
    onCellLongPress: (index: Int) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
    onPause: () -> Unit
) {
    // Local UI-only state: which action a plain tap performs. This never
    // needs to reach the ViewModel — it doesn't affect game logic, only
    // which of the two existing callbacks a tap is routed to below.
    var isFlagMode by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize()
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
                    onBack = onBack,
                    onNewGame = onReset,
                    onQuit = onBack
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
                    modifier = Modifier.height(12.dp)
                )

                FlagModeRow(
                    isFlagMode = isFlagMode,
                    enabled = status == GameStatus.PLAYING,
                    onToggle = { isFlagMode = it }
                )

                Spacer(
                    modifier = Modifier.height(GameSpacing.barToBoard - 12.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    // The board stays visible in every non-READY state,
                    // including WON/LOST, so a loss reveals where every mine
                    // was instead of hiding the board behind the result card.
                    MineBoard(
                        columns = difficulty.columns,
                        cells = cells,
                        interactionEnabled = status == GameStatus.PLAYING,
                        // In flag mode, a plain tap flags instead of
                        // revealing. Long-press always flags regardless
                        // of mode, so it keeps working as a shortcut.
                        onCellTap = { index ->
                            if (isFlagMode) {
                                onCellLongPress(index)
                            } else {
                                onCellTap(index)
                            }
                        },
                        onCellLongPress = onCellLongPress,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Win/loss is shown as a slim banner pinned to the bottom
                    // of the board rather than a big centered card, so the
                    // whole grid — every revealed mine included — stays
                    // visible instead of being blacked out underneath it.
                    when (status) {

                        GameStatus.WON -> {
                            ResultBanner(
                                title = if (isNewBestTime) {
                                    "New best time!"
                                } else {
                                    "You won!"
                                },
                                message = if (isNewBestTime) {
                                    "Great job! Your fastest clear yet at this difficulty."
                                } else {
                                    "Great job! You cleared the board."
                                },
                                isWin = true,
                                isNewBestTime = isNewBestTime,
                                elapsedSeconds = elapsedSeconds,
                                previousBestSeconds = previousBestSeconds,
                                onReset = onReset,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                            )
                        }

                        GameStatus.LOST -> {
                            ResultBanner(
                                title = "Game over",
                                message = "You hit a mine.",
                                isWin = false,
                                onReset = onReset,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                            )
                        }

                        else -> {
                            // PLAYING/PAUSED: board only, no overlay.
                        }
                    }
                }
            }
        }

        ConfettiOverlay(
            visible = status == GameStatus.WON,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun FlagModeRow(
    isFlagMode: Boolean,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Flag,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = "Flag mode",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = isFlagMode,
            onCheckedChange = onToggle,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun GameTopBar(
    onBack: () -> Unit,
    onNewGame: () -> Unit,
    onQuit: () -> Unit
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
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )

        Box {
            var menuExpanded by remember { mutableStateOf(false) }

            IconButton(
                onClick = { menuExpanded = true }
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(20.dp)
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("New game") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.RestartAlt,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onNewGame()
                    }
                )

                DropdownMenuItem(
                    text = { Text("Quit") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onQuit()
                    }
                )
            }
        }
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
            value = formatElapsedTime(elapsedSeconds)
        )
    }
}

/**
 * Formats a running game timer as MM:SS. The 99:59 ceiling is just a
 * display safety net — in practice the game auto-quits at 30:00 (see
 * MinesweeperViewModel's MAX_GAME_DURATION_SECONDS), so this never
 * actually gets exercised past 30:00.
 */
private fun formatElapsedTime(totalSeconds: Int): String {
    val clamped = totalSeconds.coerceIn(0, 99 * 60 + 59)
    val minutes = clamped / 60
    val seconds = clamped % 60
    return "%02d:%02d".format(minutes, seconds)
}

/**
 * Compact win/loss banner pinned to the bottom of the board. Deliberately
 * slim (icon + two lines of text + a button, in a single row) so it only
 * covers a strip at the bottom of the grid instead of the board's center —
 * the point of showing it over the board at all is so a loss reveals where
 * every mine was, which a big centered card would otherwise hide.
 */
@Composable
private fun ResultBanner(
    title: String,
    message: String,
    isWin: Boolean,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    isNewBestTime: Boolean = false,
    elapsedSeconds: Int = 0,
    previousBestSeconds: Long? = null
) {
    // A new best time gets a gold badge instead of the usual win color, so
    // it visually stands out from an ordinary clear at a glance.
    val badgeColor = when {
        isNewBestTime -> BestTimeGold
        isWin -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.errorContainer
    }
    val badgeIconColor = when {
        isNewBestTime -> Color.White
        isWin -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onErrorContainer
    }

    Card(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(badgeColor),
                contentAlignment = Alignment.Center
            ) {
                if (isWin) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        tint = badgeIconColor,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    MineIcon(
                        color = badgeIconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Only shown on a win where there's an actual prior best to
                // compare against — omitted for a loss, and omitted for a
                // player's very first win at a difficulty (nothing to
                // compare yet).
                if (isWin && previousBestSeconds != null) {
                    Spacer(modifier = Modifier.height(6.dp))

                    TimeComparisonChip(
                        isNewBestTime = isNewBestTime,
                        elapsedSeconds = elapsedSeconds,
                        previousBestSeconds = previousBestSeconds
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        MaterialTheme.colorScheme.primaryContainer
                    )
                    .clickable(onClick = onReset)
                    .padding(
                        horizontal = 16.dp,
                        vertical = 10.dp
                    )
            ) {
                Text(
                    text = "Play again",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * A small pill comparing this win's time against the player's best at this
 * difficulty. It pops in with a bouncy scale + fade entrance so it draws
 * the eye right after the banner appears, then settles into a slow,
 * continuous pulse — a stronger gold glow for a new record, a gentler one
 * otherwise — so it keeps reading as "live" feedback rather than static
 * text.
 */
@Composable
private fun TimeComparisonChip(
    isNewBestTime: Boolean,
    elapsedSeconds: Int,
    previousBestSeconds: Long,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(220)) + scaleIn(
            initialScale = 0.6f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ),
        modifier = modifier
    ) {
        val infiniteTransition = rememberInfiniteTransition(
            label = "timeComparisonPulse"
        )

        // A gentle breathing glow behind the chip — noticeably stronger
        // for a new record so it feels like a celebration, subtler for a
        // near-miss so it informs without competing with the banner.
        val glowAlpha by infiniteTransition.animateFloat(
            initialValue = if (isNewBestTime) 0.55f else 0.85f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = if (isNewBestTime) 650 else 1100,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowAlpha"
        )

        val diffSeconds = kotlin.math.abs(elapsedSeconds - previousBestSeconds.toInt())

        val (chipBackground, chipContentColor, chipIcon, chipLabel) = if (isNewBestTime) {
            Quadruple(
                BestTimeGold.copy(alpha = glowAlpha),
                Color.White,
                Icons.Filled.EmojiEvents,
                if (diffSeconds > 0) {
                    "${diffSeconds}s faster than your best"
                } else {
                    "Matched your best time"
                }
            )
        } else {
            Quadruple(
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = glowAlpha),
                MaterialTheme.colorScheme.onSecondaryContainer,
                Icons.Filled.Timer,
                "+${diffSeconds}s off your best (${formatElapsedTime(previousBestSeconds.toInt())})"
            )
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(chipBackground)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = chipIcon,
                contentDescription = null,
                tint = chipContentColor,
                modifier = Modifier.size(13.dp)
            )

            Spacer(modifier = Modifier.width(5.dp))

            Text(
                text = chipLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = chipContentColor
            )
        }
    }
}

/** Small local helper — `TimeComparisonChip` is the only place that needs a 4-tuple. */
private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

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
            when (index) {
                4 ->
                    CellUiState(
                        isRevealed = true,
                        adjacentMines = 2
                    )

                5 ->
                    CellUiState(
                        isRevealed = true,
                        adjacentMines = 0
                    )

                6 ->
                    CellUiState(
                        isFlagged = true
                    )

                10 ->
                    CellUiState(
                        isRevealed = true,
                        isMine = true,
                        isDetonated = true
                    )

                else -> CellUiState()
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