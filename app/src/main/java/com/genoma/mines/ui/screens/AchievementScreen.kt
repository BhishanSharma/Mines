package com.genoma.mines.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

data class GameAchievement(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val tint: Color,
    val unlocked: Boolean
)

@Composable
fun AchievementScreen(
    modifier: Modifier = Modifier
) {
    val achievements = listOf(
        GameAchievement(
            title = "First Flag",
            description = "Place your first flag on a mine.",
            icon = Icons.Default.Flag,
            tint = Color.Red,
            unlocked = true
        ),
        GameAchievement(
            title = "First Victory",
            description = "Win your first game.",
            icon = Icons.Default.MilitaryTech,
            tint = Color(0xFFFF9800),
            unlocked = false
        ),
        GameAchievement(
            title = "5 Wins",
            description = "Win 5 games.",
            icon = Icons.Default.MilitaryTech,
            tint = Color(0xFFFF9800),
            unlocked = false
        ),
        GameAchievement(
            title = "10 Wins",
            description = "Win 10 games.",
            icon = Icons.Default.MilitaryTech,
            tint = Color(0xFFFF9800),
            unlocked = false
        ),
        GameAchievement(
            title = "Win Streak",
            description = "Win 3 games in a row.",
            icon = Icons.Default.Whatshot,
            tint = Color(0xFFFF9800),
            unlocked = false
        ),
        GameAchievement(
            title = "Speedster",
            description = "Complete a game in under one minute.",
            icon = Icons.Default.Bolt,
            tint = Color(0xFF2196F3),
            unlocked = false
        ),
        GameAchievement(
            title = "Quick Thinker",
            description = "Complete 5 games in under two minutes.",
            icon = Icons.Default.Bolt,
            tint = Color(0xFF2196F3),
            unlocked = false
        ),
        GameAchievement(
            title = "Perfect",
            description = "Win a game without making a mistake.",
            icon = Icons.Default.Star,
            tint = Color(0xFF4CAF50),
            unlocked = false
        ),
        GameAchievement(
            title = "Mine Expert",
            description = "Win a Hard difficulty game.",
            icon = Icons.Default.Star,
            tint = Color(0xFF4CAF50),
            unlocked = false
        )
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {

            // Screen title
            Text(
                text = "Achievements",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(
                    top = 16.dp,
                    bottom = 16.dp
                )
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(achievements) { achievement ->
                    AchievementCard(
                        achievement = achievement
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementCard(
    achievement: GameAchievement
) {
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .padding(end = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = achievement.icon,
                    contentDescription = achievement.title,
                    tint = if (achievement.unlocked) {
                        achievement.tint
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier
                        .padding(4.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = achievement.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.padding(start = 8.dp))

            Text(
                text = if (achievement.unlocked) {
                    "UNLOCKED"
                } else {
                    "LOCKED"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (achievement.unlocked) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Preview(
    showBackground = true
)
@Composable
fun AchievementScreenPreview() {
    AchievementScreen()

}