package com.genoma.mines.game.ui
import com.genoma.mines.core.ui.components.MineIcon
import com.genoma.mines.core.ui.components.BoardWrongFlag
import com.genoma.mines.core.ui.components.BoardExplodedMine
import com.genoma.mines.core.ui.components.BoardMine
import com.genoma.mines.core.ui.components.BoardFlag
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import com.genoma.mines.core.theme.MinesCoral
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.graphics.luminance
import com.genoma.mines.core.theme.boardNumberColor
import com.genoma.mines.core.theme.BestTimeGold
import com.genoma.mines.core.theme.MinesPreviews
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.genoma.mines.store.domain.BoardThemeStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.genoma.mines.game.domain.Difficulty
import com.genoma.mines.game.domain.GameStatus
import com.genoma.mines.core.theme.MinesTheme
import com.genoma.mines.core.theme.MinesThemeVariant
import com.genoma.mines.core.ui.components.ClassicAmbientBackdrop
import com.genoma.mines.core.ui.components.ClassicAmbientStyle
import com.genoma.mines.core.ui.components.ConfettiOverlay

data class CellUiState(
    val isRevealed: Boolean = false,
    val isFlagged: Boolean = false,
    val isMine: Boolean = false,
    val adjacentMines: Int = 0,
    val isDetonated: Boolean = false
)


/** Cell behind the exploded mine; fixed so the burst always pops. */
private val DetonatedCellRed = Color(0xFFE5484D)

private object GameSpacing {
    val screenHorizontal = 16.dp
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
    onPause: () -> Unit,
    onSettings: () -> Unit = {},
    onHowToPlay: () -> Unit = {},
    boardTheme: BoardThemeStyle? = null,
    frostCellSkinEquipped: Boolean = false,
    themeVariant: MinesThemeVariant = MinesThemeVariant.CLASSIC_TEAL,
) {
    // Local UI-only state: which action a plain tap performs. This never
    // needs to reach the ViewModel — it doesn't affect game logic, only
    // which of the two existing callbacks a tap is routed to below.
    var isFlagMode by remember { mutableStateOf(false) }

    // A heart is spent when a game starts and only refunded on a win, so
    // leaving or restarting an unfinished game forfeits it. Ask first.
    val isGameInProgress = status == GameStatus.PLAYING || status == GameStatus.PAUSED
    var pendingConfirmation by remember { mutableStateOf<GameConfirmation?>(null) }

    val requestBack = {
        if (isGameInProgress) pendingConfirmation = GameConfirmation.LEAVE else onBack()
    }
    val requestRestart = {
        if (isGameInProgress) pendingConfirmation = GameConfirmation.RESTART else onReset()
    }

    BackHandler(onBack = requestBack)

    pendingConfirmation?.let { confirmation ->
        GameConfirmationDialog(
            confirmation = confirmation,
            onConfirm = {
                pendingConfirmation = null
                when (confirmation) {
                    GameConfirmation.LEAVE -> onBack()
                    GameConfirmation.RESTART -> onReset()
                }
            },
            onDismiss = { pendingConfirmation = null }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GameSceneBackground(themeVariant = themeVariant, modifier = Modifier.fillMaxSize())

        // Classic Teal's own ambient scene, in its calm "focus" style so it
        // never competes with the board. Deep Ocean draws its scene above.
        if (themeVariant == MinesThemeVariant.CLASSIC_TEAL) {
            ClassicAmbientBackdrop(
                style = ClassicAmbientStyle.FOCUS,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Behind the board and result banner, not over them: it frames the
        // win without covering the text the player needs to read.
        ConfettiOverlay(
            visible = status == GameStatus.WON,
            modifier = Modifier.fillMaxSize()
        )

        Surface(
            modifier = Modifier.fillMaxSize(),
            // Both themes paint their own scene behind the board.
            color = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // The app Scaffold no longer pads for insets, so keep
                    // the top bar clear of the camera cutout here.
                    .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top))
                    .padding(
                        horizontal = GameSpacing.screenHorizontal
                    )
                    .padding(
                        bottom = GameSpacing.screenBottom
                    )
            ) {

                GameTopBar(
                    onBack = requestBack,
                    onSettings = onSettings
                )

                Spacer(
                    modifier = Modifier.height(GameSpacing.barToBoard)
                )

                GameStatusBar(
                    minesRemaining = difficulty.mines - flagsPlaced,
                    elapsedSeconds = elapsedSeconds,
                    isFlagMode = isFlagMode,
                    enabled = status == GameStatus.PLAYING,
                    onToggleFlagMode = { isFlagMode = !isFlagMode }
                )

                Spacer(
                    modifier = Modifier.height(GameSpacing.barToBoard)
                )

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    // The board is square: cap its width at the height
                    // available, so short phones (or the result banner
                    // below taking space) shrink it instead of clipping it.
                    val boardSide = minOf(maxWidth, maxHeight)

                    // The board stays fully visible in every state,
                    // including WON/LOST: the result banner sits below it
                    // rather than on top, so a loss shows every mine.
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
                        isPaused = status == GameStatus.PAUSED,
                        isFlagMode = isFlagMode && status == GameStatus.PLAYING,
                        showWrongFlags = status == GameStatus.LOST,
                        onResume = onPause,
                        boardTheme = boardTheme,
                        frostCellSkinEquipped = frostCellSkinEquipped,
                        modifier = Modifier.width(boardSide)
                    )
                }

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                // Once the game is over, the result banner takes the place
                // of the bottom actions (Pause/Restart are meaningless then),
                // so it never covers any of the board.
                when (status) {

                    GameStatus.WON -> {
                        ResultBanner(
                            title = if (isNewBestTime) "New best time!" else "You won!",
                            message = "Cleared in ${formatElapsedTime(elapsedSeconds)}",
                            isWin = true,
                            isNewBestTime = isNewBestTime,
                            elapsedSeconds = elapsedSeconds,
                            previousBestSeconds = previousBestSeconds,
                            onReset = onReset,
                            onHome = onBack,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    GameStatus.LOST -> {
                        // Flags left on safe cells, now drawn crossed out
                        // on the board.
                        val wrongFlags = cells.count {
                            it.isFlagged && !it.isMine && !it.isRevealed
                        }

                        ResultBanner(
                            title = "Game over",
                            message = buildString {
                                append("Hit a mine at ")
                                append(formatElapsedTime(elapsedSeconds))
                                if (wrongFlags > 0) {
                                    append(
                                        if (wrongFlags == 1) " · 1 wrong flag"
                                        else " · $wrongFlags wrong flags"
                                    )
                                }
                            },
                            isWin = false,
                            onReset = onReset,
                            onHome = onBack,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    else -> {
                        GameBottomActions(
                            status = status,
                            onPauseToggle = onPause,
                            onRestart = requestRestart,
                            onHowToPlay = onHowToPlay
                        )
                    }
                }
            }
        }
    }
}

private enum class GameConfirmation {
    LEAVE,
    RESTART
}

@Composable
private fun GameConfirmationDialog(
    confirmation: GameConfirmation,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val (title, message, confirmLabel) = when (confirmation) {
        GameConfirmation.LEAVE -> Triple(
            "Leave this game?",
            "Your progress will be lost and you won't get this game's heart back.",
            "Leave"
        )

        GameConfirmation.RESTART -> Triple(
            "Restart this game?",
            "Starting over abandons this game and costs another heart.",
            "Restart"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmLabel,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Keep playing")
            }
        }
    )
}

@Composable
private fun GameTopBar(
    onBack: () -> Unit,
    onSettings: () -> Unit
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

        IconButton(
            onClick = onSettings
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun GameStatusBar(
    minesRemaining: Int,
    elapsedSeconds: Int,
    isFlagMode: Boolean,
    enabled: Boolean,
    onToggleFlagMode: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                MaterialTheme.colorScheme.surfaceVariant
            )
            .padding(
                horizontal = 12.dp,
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

        FlagModeButton(
            isFlagMode = isFlagMode,
            enabled = enabled,
            onClick = onToggleFlagMode
        )

        StatusReadout(
            label = "TIME",
            value = formatElapsedTime(elapsedSeconds)
        )
    }
}

/**
 * Center control in the status bar: a labelled two-way Dig / Flag switch,
 * so the current tap action is always readable at a glance. The active
 * segment is filled; Flag uses the error color to match the board's
 * flag-mode outline.
 */
@Composable
private fun FlagModeButton(
    isFlagMode: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surface)
            .padding(3.dp)
            .selectableGroup()
    ) {
        TapModeSegment(
            label = "Dig",
            icon = Icons.Filled.TouchApp,
            selected = !isFlagMode,
            enabled = enabled,
            selectedContainer = MaterialTheme.colorScheme.primaryContainer,
            selectedContent = MaterialTheme.colorScheme.onPrimaryContainer,
            onClick = { if (isFlagMode) onClick() }
        )

        TapModeSegment(
            label = "Flag",
            icon = Icons.Filled.Flag,
            selected = isFlagMode,
            enabled = enabled,
            selectedContainer = MaterialTheme.colorScheme.errorContainer,
            selectedContent = MaterialTheme.colorScheme.onErrorContainer,
            onClick = { if (!isFlagMode) onClick() }
        )
    }
}

@Composable
private fun TapModeSegment(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    selectedContainer: Color,
    selectedContent: Color,
    onClick: () -> Unit
) {
    val contentColor = when {
        selected -> selectedContent
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }.copy(alpha = if (enabled) 1f else 0.4f)

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) selectedContainer else Color.Transparent)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = contentColor
        )
    }
}

private fun formatElapsedTime(totalSeconds: Int): String {
    val clamped = totalSeconds.coerceIn(0, 99 * 60 + 59)
    val minutes = clamped / 60
    val seconds = clamped % 60
    return "%02d:%02d".format(minutes, seconds)
}

@Composable
private fun ResultBanner(
    title: String,
    message: String,
    isWin: Boolean,
    onReset: () -> Unit,
    onHome: () -> Unit,
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

    // Stacked, not one row: a single row squeezed the text into ~110dp and
    // wrapped every line. Summary on top, actions full-width below.
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )

                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Only shown on a win with an actual prior best to compare
                // against (not on a loss, nor on a first win at a difficulty).
                if (isWin && previousBestSeconds != null) {
                    Spacer(modifier = Modifier.width(8.dp))

                    TimeComparisonChip(
                        isNewBestTime = isNewBestTime,
                        elapsedSeconds = elapsedSeconds,
                        previousBestSeconds = previousBestSeconds
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onHome,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Home,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Home")
                }

                // Every new game spends a heart, so the button says so.
                Button(
                    onClick = onReset,
                    modifier = Modifier
                        .weight(1.4f)
                        .semantics { contentDescription = "Play again, costs 1 heart" }
                ) {
                    Text(text = "Play again")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "−1",
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 2.dp)
                            .size(14.dp)
                    )
                }
            }
        }
    }
}

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
                // Short: the chip sits beside the title in the banner.
                if (diffSeconds > 0) "${diffSeconds}s faster" else "Tied best"
            )
        } else {
            Quadruple(
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = glowAlpha),
                MaterialTheme.colorScheme.onSecondaryContainer,
                Icons.Filled.Timer,
                "Best ${formatElapsedTime(previousBestSeconds.toInt())}"
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

/**
 * Bottom action row: play/pause, restart, and how-to-play. Replaces the
 * old status-bar pause/reset button now that the status bar's center
 * button is dedicated to the flag/dig mode toggle.
 */
@Composable
private fun GameBottomActions(
    status: GameStatus,
    onPauseToggle: () -> Unit,
    onRestart: () -> Unit,
    onHowToPlay: () -> Unit
) {
    val canPause = status == GameStatus.PLAYING || status == GameStatus.PAUSED

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GameActionButton(
            icon = if (status == GameStatus.PLAYING) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            label = if (status == GameStatus.PLAYING) "Pause" else "Play",
            onClick = onPauseToggle,
            enabled = canPause,
            modifier = Modifier.weight(1f)
        )

        GameActionButton(
            icon = Icons.Filled.RestartAlt,
            label = "Restart",
            onClick = onRestart,
            modifier = Modifier.weight(1f)
        )

        GameActionButton(
            icon = Icons.AutoMirrored.Outlined.HelpOutline,
            label = "How to Play",
            onClick = onHowToPlay,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun GameActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = contentColor
        )
    }
}

@Composable
private fun MineBoard(
    columns: Int,
    cells: List<CellUiState>,
    interactionEnabled: Boolean,
    onCellTap: (Int) -> Unit,
    onCellLongPress: (Int) -> Unit,
    isPaused: Boolean,
    isFlagMode: Boolean,
    showWrongFlags: Boolean,
    onResume: () -> Unit,
    boardTheme: BoardThemeStyle?,
    frostCellSkinEquipped: Boolean,
    modifier: Modifier = Modifier
) {
    // A thicker error-colored outline while flag mode is on, so the
    // board itself signals that taps will place flags.
    val borderColor by animateColorAsState(
        targetValue = if (isFlagMode) {
            MaterialTheme.colorScheme.error
        } else {
            boardTheme?.borderColor ?: MaterialTheme.colorScheme.outlineVariant
        },
        animationSpec = tween(durationMillis = 180),
        label = "boardBorder"
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = boardTheme?.boardColor ?: MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(
            if (isFlagMode) 2.dp else 1.dp,
            borderColor
        )
    ) {
        Box {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                userScrollEnabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp)
                    // The pause cover hides the board visually; hide it from
                    // screen readers too, or pausing still reveals it.
                    .then(if (isPaused) Modifier.clearAndSetSemantics {} else Modifier),
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
                        row = index / columns + 1,
                        column = index % columns + 1,
                        tapActionLabel = if (isFlagMode) "Toggle flag" else "Reveal",
                        enabled = interactionEnabled,
                        onTap = {
                            onCellTap(index)
                        },
                        onLongPress = {
                            onCellLongPress(index)
                        },
                        showWrongFlag = showWrongFlags,
                        boardTheme = boardTheme,
                        frostCellSkinEquipped = frostCellSkinEquipped
                    )
                }
            }

            // Hide the board while paused so pausing can't be used to study
            // it with the clock stopped.
            if (isPaused) {
                PausedBoardCover(
                    onResume = onResume,
                    modifier = Modifier.matchParentSize()
                )
            }
        }
    }
}

@Composable
private fun PausedBoardCover(
    onResume: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClickLabel = "Resume", role = Role.Button, onClick = onResume),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Paused",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Tap to resume",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MineCell(
    state: CellUiState,
    row: Int,
    column: Int,
    tapActionLabel: String,
    enabled: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    showWrongFlag: Boolean,
    boardTheme: BoardThemeStyle?,
    frostCellSkinEquipped: Boolean
) {
    // After a loss every mine is revealed, but a mine the player correctly
    // flagged keeps showing its flag (on a hidden-style tile), so they can
    // see which calls they got right.
    val showsCorrectFlag = state.isFlagged && state.isMine && state.isRevealed && !state.isDetonated
    val looksHidden = !state.isRevealed || showsCorrectFlag

    val revealedColor = boardTheme?.revealedCellColor ?: MaterialTheme.colorScheme.surface
    val targetColor = when {
        // A strong red cell under the burst, so the losing move is obvious.
        state.isDetonated -> DetonatedCellRed
        looksHidden -> boardTheme?.hiddenCellColor ?: MaterialTheme.colorScheme.primaryContainer
        // Other mines revealed on a loss get a faint danger tint.
        state.isMine -> lerp(revealedColor, MinesCoral, 0.18f)
        else -> revealedColor
    }
    val onDarkCell = targetColor.luminance() < 0.5f

    // Smoothly cross-fades when a cell flips from hidden to revealed,
    // instead of popping instantly.
    val backgroundColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 180),
        label = "cellBackground"
    )

    // Unrevealed cells get a faint highlight border to read as
    // "raised" tiles waiting to be tapped; revealed cells sit flush.
    val borderColor = if (looksHidden) {
        (boardTheme?.borderColor ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.35f)
    } else {
        Color.Transparent
    }

    BoxWithConstraints(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .border(
                width = if (frostCellSkinEquipped) 2.dp else 1.dp,
                color = if (frostCellSkinEquipped) Color.White.copy(alpha = 0.65f) else borderColor,
                shape = RoundedCornerShape(6.dp)
            )
            .combinedClickable(
                enabled = enabled,
                onClickLabel = tapActionLabel,
                onClick = onTap,
                onLongClickLabel = "Toggle flag",
                onLongClick = onLongPress
            )
            // One announcement per cell, e.g. "Row 4, column 5, 2". The
            // children's own semantics are cleared below so nothing is
            // read twice.
            .semantics {
                contentDescription = "Row $row, column $column, " +
                        cellStateDescription(state, showWrongFlag)
            },
        contentAlignment = Alignment.Center
    ) {
        // Size cell content from the cell itself rather than fixed dp, so
        // it stays proportional on small phones and tablets alike.
        val flagSize = maxWidth * 0.76f
        val mineSize = maxWidth * 0.8f
        val countFontSize = with(LocalDensity.current) { (maxWidth * 0.55f).toSp() }

        when {
            // After a loss, a flag on a safe cell is crossed out so the
            // player can see which of their guesses were wrong.
            showWrongFlag && state.isFlagged && !state.isMine && !state.isRevealed -> {
                BoardWrongFlag(
                    onDarkCell = onDarkCell,
                    crossColor = MinesCoral,
                    modifier = Modifier.size(flagSize)
                )
            }

            state.isFlagged && looksHidden -> {
                BoardFlag(
                    onDarkCell = onDarkCell,
                    modifier = Modifier.size(flagSize)
                )
            }

            state.isDetonated -> {
                BoardExplodedMine(
                    modifier = Modifier.size(maxWidth)
                )
            }

            state.isRevealed && state.isMine -> {
                BoardMine(
                    onDarkCell = onDarkCell,
                    modifier = Modifier.size(mineSize)
                )
            }

            state.isRevealed && state.adjacentMines > 0 -> {
                Text(
                    text = state.adjacentMines.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = countFontSize,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    // Pick the palette from the cell actually behind the
                    // number: store board themes keep revealed cells light
                    // even in dark mode.
                    color = colorForCount(
                        count = state.adjacentMines,
                        onLightCell = targetColor.luminance() > 0.5f
                    ),
                    // The cell's own description already includes the number.
                    modifier = Modifier.clearAndSetSemantics {}
                )
            }

            else -> {
                // Empty revealed cell or untouched cell.
            }
        }
    }
}

private fun colorForCount(
    count: Int,
    onLightCell: Boolean
): Color = boardNumberColor(count, onLightCell)

/** Spoken state of a cell, for screen readers. */
private fun cellStateDescription(state: CellUiState, showWrongFlag: Boolean): String = when {
    showWrongFlag && state.isFlagged && !state.isMine && !state.isRevealed -> "wrong flag, no mine here"
    state.isFlagged && !state.isRevealed -> "flagged"
    state.isFlagged && state.isMine && !state.isDetonated -> "flagged mine, correct"
    state.isRevealed && state.isMine -> if (state.isDetonated) "mine, exploded" else "mine"
    state.isRevealed && state.adjacentMines == 1 -> "1 adjacent mine"
    state.isRevealed && state.adjacentMines > 1 -> "${state.adjacentMines} adjacent mines"
    state.isRevealed -> "empty"
    else -> "hidden"
}

@MinesPreviews
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
            },
            onSettings = {},
            onHowToPlay = {}
        )
    }
}