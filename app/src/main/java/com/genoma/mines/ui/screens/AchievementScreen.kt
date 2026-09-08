package com.genoma.mines.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.genoma.mines.data.AchievementCalculator
import com.genoma.mines.data.AchievementTrack
import com.genoma.mines.data.GameHistoryItem
import com.genoma.mines.game.BadgeTier
import com.genoma.mines.game.Difficulty
import com.genoma.mines.game.GameResultType
import com.genoma.mines.ui.theme.MinesTheme

@Composable
fun AchievementScreen(
    tracks: List<AchievementTrack> = emptyList(),
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
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

@Composable
private fun AchievementTrackCard(
    track: AchievementTrack
) {
    val achievedTier = track.achievedTier
    val badgeColor = achievedTier?.let { colorFor(it) }
        ?: MaterialTheme.colorScheme.surfaceVariant
    val iconTint = if (achievedTier != null) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
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
                        .clip(CircleShape)
                        .background(badgeColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconFor(track.id),
                        contentDescription = track.title,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = track.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = achievedTier?.displayName ?: "Locked",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = achievedTier?.let { colorFor(it) }
                        ?: MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                        "${track.currentValue} \u2014 Maxed out"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = next?.let { "Next: ${it.tier.displayName}" } ?: "All tiers cleared",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { track.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = achievedTier?.let { colorFor(it) }
                    ?: MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // One dot per tier, filled once reached — Bronze through Diamond.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                track.tiers.forEach { tier ->
                    val unlocked = achievedTier != null &&
                            tier.tier.ordinal <= achievedTier.ordinal

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
                                if (!unlocked) {
                                    Modifier.border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
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