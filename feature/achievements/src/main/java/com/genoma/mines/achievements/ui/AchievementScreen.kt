package com.genoma.mines.achievements.ui
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.genoma.mines.achievements.domain.AchievementCalculator
import com.genoma.mines.achievements.domain.AchievementTrack
import com.genoma.mines.game.data.GameHistoryItem
import com.genoma.mines.achievements.domain.BadgeTier
import com.genoma.mines.game.domain.Difficulty
import com.genoma.mines.game.domain.GameResultType
import com.genoma.mines.core.theme.MinesTheme

@Composable
fun AchievementScreen(
    modifier: Modifier = Modifier,
    tracks: List<AchievementTrack> = emptyList(),
    isLoading: Boolean = false
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {

            Text(
                text = "Achievements",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                if (tracks.isNotEmpty()) {
                    item(key = "summary") {
                        AchievementSummaryCard(tracks = tracks)
                    }
                }

                items(tracks, key = { it.id }) { track ->
                    AchievementTrackCard(track = track)
                }
            }
        }
    }
}

private fun iconFor(trackId: String): ImageVector = when (trackId) {
    "games_played" -> Icons.Filled.SportsEsports
    "games_won" -> Icons.Filled.EmojiEvents
    "win_streak" -> Icons.Filled.Whatshot
    "total_score" -> Icons.Filled.BarChart
    "hard_wins" -> Icons.Filled.MilitaryTech
    else -> Icons.Filled.EmojiEvents
}

private fun colorFor(tier: BadgeTier): Color = when (tier) {
    BadgeTier.BRONZE -> Color(0xFFCD7F32)
    BadgeTier.SILVER -> Color(0xFFB0BEC5)
    BadgeTier.GOLD -> Color(0xFFFFC107)
    BadgeTier.PLATINUM -> Color(0xFF66C2CE)
    BadgeTier.DIAMOND -> Color(0xFF6FD8FF)
}

/** Total tiers earned across every track vs. every tier that exists — a single overall score. */
@Composable
private fun AchievementSummaryCard(tracks: List<AchievementTrack>) {
    val unlockedTracks = tracks.count { it.achievedTier != null }
    val totalTiers = tracks.sumOf { it.tiers.size }
    val unlockedTiers = tracks.sumOf { track -> track.achievedTier?.let { it.ordinal + 1 } ?: 0 }
    val overallProgress = if (totalTiers > 0) unlockedTiers.toFloat() / totalTiers else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OverallProgressRing(progress = overallProgress)

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = "$unlockedTracks of ${tracks.size} unlocked",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (unlockedTracks == tracks.size) {
                        "Every track has at least one tier — nice work"
                    } else {
                        "Keep playing to unlock more tiers"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun OverallProgressRing(progress: Float) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = MaterialTheme.colorScheme.primary
    val clamped = progress.coerceIn(0f, 1f)

    Box(
        modifier = Modifier.size(56.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 6.dp.toPx()
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * clamped,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Text(
            text = "${(clamped * 100).toInt()}%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun AchievementTrackCard(
    track: AchievementTrack
) {
    val achievedTier = track.achievedTier
    val tint = achievedTier?.let { colorFor(it) } ?: MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (achievedTier != null) {
                colorFor(achievedTier).copy(alpha = 0.06f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (achievedTier != null) {
                                colorFor(achievedTier).copy(alpha = 0.18f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconFor(track.id),
                        contentDescription = track.title,
                        tint = tint,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = track.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                TierStatusChip(tier = achievedTier)
            }

            Spacer(modifier = Modifier.height(14.dp))

            val next = track.nextTier

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (next != null) {
                        "${track.currentValue} / ${next.threshold}"
                    } else {
                        "${track.currentValue} — Maxed out"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = next?.let {
                        "${(it.threshold - track.currentValue).coerceAtLeast(0)} to ${it.tier.displayName}"
                    } ?: "All tiers cleared",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { track.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = tint,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            TierLadder(track = track)
        }
    }
}

@Composable
private fun TierStatusChip(tier: BadgeTier?) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (tier != null) {
                    colorFor(tier).copy(alpha = 0.18f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = tier?.displayName ?: "Locked",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = tier?.let { colorFor(it) } ?: MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** One segment per tier — filled once reached, ringed on the tier currently being chased. */
@Composable
private fun TierLadder(track: AchievementTrack) {
    val achievedTier = track.achievedTier

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        track.tiers.forEach { tier ->
            val unlocked = achievedTier != null && tier.tier.ordinal <= achievedTier.ordinal
            val isNext = !unlocked && track.nextTier?.tier == tier.tier

            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(
                        if (unlocked) {
                            colorFor(tier.tier)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                    .then(
                        if (isNext) {
                            Modifier.border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                        } else {
                            Modifier
                        }
                    )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AchievementScreenPreview() {
    val sampleHistory = listOf(
        GameHistoryItem(
            difficulty = Difficulty.EASY,
            score = 100,
            result = GameResultType.WIN,
            durationSeconds = 45,
            createdAtMillis = 1L
        ),
        GameHistoryItem(
            difficulty = Difficulty.HARD,
            score = 350,
            result = GameResultType.WIN,
            durationSeconds = 200,
            createdAtMillis = 2L
        ),
        GameHistoryItem(
            difficulty = Difficulty.MEDIUM,
            score = -50,
            result = GameResultType.LOSS,
            durationSeconds = 30,
            createdAtMillis = 3L
        )
    )

    MinesTheme {
        AchievementScreen(
            tracks = AchievementCalculator.calculate(sampleHistory)
        )
    }
}
