package com.genoma.mines.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.genoma.mines.data.GameHistoryItem
import com.genoma.mines.game.Difficulty
import com.genoma.mines.game.GameResultType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


// ============================================================================
// FULL HISTORY SCREEN
// ============================================================================

@Composable
fun HistoryScreen(
    isLoading: Boolean,
    history: List<GameHistoryItem>,
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {

            // ----------------------------------------------------------------
            // Header
            // ----------------------------------------------------------------

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 8.dp,
                        bottom = 16.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Text(
                    text = "Game history",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }


            // ----------------------------------------------------------------
            // Content
            // ----------------------------------------------------------------

            when {

                // Loading
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }


                // Empty
                history.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No games played yet.\nYour results will show up here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }


                // History
                else -> {

                    val groupedHistory = groupByDay(history)

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        groupedHistory.forEach { (label, entries) ->

                            // Date heading
                            item {
                                Text(
                                    text = label.uppercase(
                                        Locale.getDefault()
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(
                                        top = 8.dp,
                                        bottom = 4.dp
                                    )
                                )
                            }

                            // Games for this date
                            items(entries) { entry ->
                                HistoryRow(
                                    entry = entry
                                )
                            }
                        }

                        // Bottom breathing room
                        item {
                            Spacer(
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}


// ============================================================================
// RECENT HISTORY SECTION
//
// Used inside ProfileScreen.
//
// Shows:
// HISTORY                                      See all
//
// Easy                         WIN          125
// Medium                       LOSS          80
// Hard                         WIN          210
//
// Only the 3 most recent games are displayed.
// ============================================================================

@Composable
fun RecentHistorySection(
    history: List<GameHistoryItem>,
    onSeeAllHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {

        // ----------------------------------------------------------------
        // Section header
        // ----------------------------------------------------------------

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "HISTORY",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "See all",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(
                    onClick = onSeeAllHistory
                )
            )
        }


        Spacer(
            modifier = Modifier.height(8.dp)
        )


        // ----------------------------------------------------------------
        // Empty state
        // ----------------------------------------------------------------

        if (history.isEmpty()) {

            HistoryEmptyPreview()

        } else {

            // ----------------------------------------------------------------
            // Three most recent games
            // ----------------------------------------------------------------

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                history
                    .sortedByDescending {
                        it.createdAtMillis
                    }
                    .take(3)
                    .forEach { entry ->

                        HistoryPreviewRow(
                            entry = entry
                        )
                    }
            }
        }
    }
}


// ============================================================================
// COMPACT HISTORY ROW
//
// Used by RecentHistorySection on Profile.
// ============================================================================

// Shown on both the Profile recent-history preview and the full History
// screen — a win's score reads with a leading "+" so it's visually obvious
// alongside a loss's "-", instead of only the loss carrying a sign.
private fun formatSignedScore(score: Int): String {
    return if (score > 0) "+$score" else score.toString()
}

@Composable
private fun HistoryPreviewRow(
    entry: GameHistoryItem
) {
    val isWin = entry.result == GameResultType.WIN

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 14.dp,
                    vertical = 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Difficulty
            Text(
                text = entry.difficulty.label(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            // Result
            Text(
                text = if (isWin) "WIN" else "LOSS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isWin) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )

            // Score
            Text(
                text = formatSignedScore(entry.score),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
        }
    }
}


// ============================================================================
// EMPTY RECENT HISTORY
// ============================================================================

@Composable
private fun HistoryEmptyPreview() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    ) {

        Text(
            text = "No games played yet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            textAlign = TextAlign.Center
        )
    }
}


// ============================================================================
// FULL HISTORY ROW
// ============================================================================

@Composable
private fun HistoryRow(
    entry: GameHistoryItem
) {
    val isWin = entry.result == GameResultType.WIN

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 14.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Difficulty
            Text(
                text = entry.difficulty.label(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            // Result
            Text(
                text = if (isWin) "WIN" else "LOSS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isWin) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )

            // Score
            Text(
                text = formatSignedScore(entry.score),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
        }
    }
}


// ============================================================================
// DIFFICULTY LABEL
// ============================================================================

private fun Difficulty.label(): String = when (this) {
    Difficulty.EASY -> "Easy"
    Difficulty.MEDIUM -> "Medium"
    Difficulty.HARD -> "Hard"
}


// ============================================================================
// GROUP HISTORY BY DAY
// ============================================================================

private fun groupByDay(
    history: List<GameHistoryItem>
): List<Pair<String, List<GameHistoryItem>>> {

    val today = startOfDay(
        System.currentTimeMillis()
    )

    val yesterday = today - DAY_MILLIS

    val formatter = SimpleDateFormat(
        "MMMM d, yyyy",
        Locale.getDefault()
    )

    return history
        .sortedByDescending {
            it.createdAtMillis
        }
        .groupBy { entry ->

            when (startOfDay(entry.createdAtMillis)) {

                today -> {
                    "Today"
                }

                yesterday -> {
                    "Yesterday"
                }

                else -> {
                    formatter.format(
                        Date(entry.createdAtMillis)
                    )
                }
            }
        }
        .toList()
}


// ============================================================================
// DATE HELPERS
// ============================================================================

private const val DAY_MILLIS =
    24L * 60 * 60 * 1000


private fun startOfDay(
    millis: Long
): Long {

    val calendar = Calendar.getInstance()

    calendar.timeInMillis = millis

    calendar.set(
        Calendar.HOUR_OF_DAY,
        0
    )

    calendar.set(
        Calendar.MINUTE,
        0
    )

    calendar.set(
        Calendar.SECOND,
        0
    )

    calendar.set(
        Calendar.MILLISECOND,
        0
    )

    return calendar.timeInMillis
}