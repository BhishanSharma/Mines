package com.genoma.mines.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.genoma.mines.ui.theme.MinesTheme

private object ProfileSpacing {
    val screenHorizontal = 24.dp
    val screenTop = 12.dp
    val screenBottom = 24.dp
    val small = 8.dp
    val medium = 14.dp
    val large = 22.dp
}

private data class ProfileStats(
    val gamesPlayed: Int,
    val gamesWon: Int,
    val gamesLost: Int,
    val currentStreak: Int,
    val bestStreak: Int,
    val recentGames: List<Boolean>
) {
    val winRate: Int
        get() = if (gamesPlayed == 0) 0 else (gamesWon * 100) / gamesPlayed
}

@Composable
fun ProfileScreen(
    username: String = "Player",
    onBack: () -> Unit = {}
) {
    val stats = ProfileStats(
        gamesPlayed = 42,
        gamesWon = 29,
        gamesLost = 13,
        currentStreak = 4,
        bestStreak = 8,
        recentGames = listOf(
            true,
            true,
            false,
            true,
            false,
            true,
            true
        )
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.safeDrawing.asPaddingValues())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = ProfileSpacing.screenHorizontal,
                        vertical = ProfileSpacing.screenTop
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ProfileSpacing.screenHorizontal)
                    .padding(
                        bottom = WindowInsets.navigationBars
                            .asPaddingValues()
                            .calculateBottomPadding() + ProfileSpacing.screenBottom
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(
                    modifier = Modifier.height(ProfileSpacing.medium)
                )

                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        )
                        .border(
                            width = 3.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Profile picture",
                        modifier = Modifier.size(58.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(
                    modifier = Modifier.height(ProfileSpacing.medium)
                )

                Text(
                    text = username,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(
                    modifier = Modifier.height(ProfileSpacing.small)
                )

                Text(
                    text = "Mines player",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(
                    modifier = Modifier.height(ProfileSpacing.large)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ProfileSpacing.small)
                ) {
                    ProfileStatCard(
                        value = stats.gamesPlayed.toString(),
                        label = "Played",
                        modifier = Modifier.weight(1f)
                    )

                    ProfileStatCard(
                        value = stats.gamesWon.toString(),
                        label = "Won",
                        modifier = Modifier.weight(1f)
                    )

                    ProfileStatCard(
                        value = stats.gamesLost.toString(),
                        label = "Lost",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(
                    modifier = Modifier.height(ProfileSpacing.small)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ProfileSpacing.small)
                ) {
                    ProfileStatCard(
                        value = "${stats.winRate}%",
                        label = "Win rate",
                        modifier = Modifier.weight(1f)
                    )

                    ProfileStatCard(
                        value = stats.currentStreak.toString(),
                        label = "Streak",
                        modifier = Modifier.weight(1f)
                    )

                    ProfileStatCard(
                        value = stats.bestStreak.toString(),
                        label = "Best streak",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(
                    modifier = Modifier.height(ProfileSpacing.large)
                )

                Text(
                    text = "Recent performance",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(
                    modifier = Modifier.height(ProfileSpacing.medium)
                )

                PerformanceCard(
                    results = stats.recentGames
                )

                Spacer(
                    modifier = Modifier.height(ProfileSpacing.large)
                )

                StreakCard(
                    currentStreak = stats.currentStreak,
                    bestStreak = stats.bestStreak
                )

                Spacer(
                    modifier = Modifier.height(ProfileSpacing.large)
                )
            }
        }
    }
}

@Composable
private fun ProfileStatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 14.dp,
                    horizontal = 8.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(
                modifier = Modifier.height(3.dp)
            )

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PerformanceCard(
    results: List<Boolean>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Last ${results.size} games",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(
                        modifier = Modifier.height(2.dp)
                    )

                    Text(
                        text = "Win / loss history",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "${results.count { it }} wins",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            PerformanceGraph(
                results = results,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                GraphLegend(
                    color = MaterialTheme.colorScheme.primary,
                    label = "Won"
                )

                Spacer(
                    modifier = Modifier.width(20.dp)
                )

                GraphLegend(
                    color = MaterialTheme.colorScheme.error,
                    label = "Lost"
                )
            }
        }
    }
}

@Composable
private fun PerformanceGraph(
    results: List<Boolean>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val surfaceColor = MaterialTheme.colorScheme.surface
    val outlineColor = MaterialTheme.colorScheme.outline
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceVariantColor)
            .padding(16.dp)
    ) {
        if (results.isEmpty()) return@Canvas

        val centerY = size.height / 2f
        val topY = size.height * 0.22f
        val bottomY = size.height * 0.78f

        drawLine(
            color = outlineColor,
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = 1.dp.toPx()
        )

        val points = results.mapIndexed { index, won ->
            val x = if (results.size == 1) {
                size.width / 2f
            } else {
                index * size.width / (results.size - 1)
            }

            val y = if (won) topY else bottomY

            Offset(x, y)
        }

        if (points.size > 1) {
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)

                for (index in 1 until points.size) {
                    lineTo(
                        points[index].x,
                        points[index].y
                    )
                }
            }

            drawPath(
                path = path,
                color = primaryColor,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        points.forEachIndexed { index, point ->
            val color = if (results[index]) {
                primaryColor
            } else {
                errorColor
            }

            drawCircle(
                color = color,
                radius = 7.dp.toPx(),
                center = point
            )

            drawCircle(
                color = surfaceColor,
                radius = 3.dp.toPx(),
                center = point
            )
        }
    }
}

@Composable
private fun GraphLegend(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = color,
                    shape = CircleShape
                )
        )

        Spacer(
            modifier = Modifier.width(6.dp)
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StreakCard(
    currentStreak: Int,
    bestStreak: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentStreak.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(
                modifier = Modifier.width(14.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Current win streak",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(
                    modifier = Modifier.height(2.dp)
                )

                Text(
                    text = "Best streak: $bestStreak games",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun ProfileScreenPreview() {
    MinesTheme {
        ProfileScreen(
            username = "Alex",
            onBack = {}
        )
    }
}