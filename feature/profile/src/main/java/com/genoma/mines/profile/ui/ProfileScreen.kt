package com.genoma.mines.profile.ui
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.genoma.mines.game.data.DifficultyStatistics
import com.genoma.mines.game.data.UserStatistics
import com.genoma.mines.core.theme.MinesTheme
import com.genoma.mines.game.data.GameHistoryItem
import com.genoma.mines.achievements.domain.AchievementTrack
import com.genoma.mines.achievements.domain.BadgeTier
import com.genoma.mines.profile.domain.AvatarOption
import com.genoma.mines.game.ui.RecentHistorySection

private object ProfileSpacing {
    val screenHorizontal = 24.dp
    val screenTop = 12.dp
    val screenBottom = 24.dp
    val small = 8.dp
    val medium = 14.dp
    val large = 22.dp
}


@Composable
fun ProfileScreen(
    username: String = "Player",
    tagline: String = "Mines Explorer",
    achievementTracks: List<AchievementTrack> = emptyList(),
    statistics: UserStatistics = UserStatistics.EMPTY,
    isLoading: Boolean = false,
    selectedAvatar: AvatarOption = AvatarOption.Default,
    photoUrl: String? = null,

    history: List<GameHistoryItem> = emptyList(),

    level: Int = 8,
    currentXp: Int = 320,
    xpForNextLevel: Int = 500,
    keepGoingMessage: String = "Play more to unlock new achievements.",
    bestTimeOverall: String? = "00:42",
    bestTimeDifficultyLabel: String = "Easy",
    bestTimes: Map<String, String> = mapOf(
        "Easy" to "00:42",
        "Medium" to "01:28"
    ),
    highlightedDifficultyLabel: String = "Easy",

    onAvatarSelected: (AvatarOption) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onKeepGoingClick: () -> Unit = {},
    onDifficultyClick: (String) -> Unit = {},

    onSeeAllHistory: () -> Unit = {},

    onBack: () -> Unit = {}
) {
    var showAvatarPicker by remember {
        mutableStateOf(false)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // Faint decorative grid, built from shapes only (no image asset
            // required) — echoes the Mines tiles behind the header, like
            // in the reference design.
            ProfileHeaderDecoration(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 30.dp, y = 6.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                    return@Column
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
                        )
                ) {
                    Spacer(modifier = Modifier.height(ProfileSpacing.small))

                    // ---------- Avatar + name + level ----------
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier.size(96.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
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
                                if (photoUrl != null) {
                                    AsyncImage(
                                        model = photoUrl,
                                        contentDescription = "Profile picture",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(4.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = selectedAvatar.drawableRes),
                                        contentDescription = "Profile picture",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(4.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }

                            // Only let guests pick a stand-in avatar; Google users'
                            // photo comes from their account and isn't editable here.
                            if (photoUrl == null) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .border(
                                            width = 2.dp,
                                            color = MaterialTheme.colorScheme.background,
                                            shape = CircleShape
                                        )
                                        .clickable { showAvatarPicker = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "Change profile picture",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ){
                                Text(
                                    text = username,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                IconButton(onClick = onOpenSettings) {
                                    Icon(
                                        imageVector = Icons.Filled.Settings,
                                        contentDescription = "Settings",
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                            BadgesRow(
                                tracks = achievementTracks,
                                modifier = Modifier.padding(top = 2.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            LevelXpRow(
                                level = level,
                                currentXp = currentXp,
                                xpForNextLevel = xpForNextLevel
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(ProfileSpacing.large))

                    // ---------- Keep going banner ----------
                    KeepGoingCard(
                        message = keepGoingMessage,
                        onClick = onKeepGoingClick
                    )

                    Spacer(modifier = Modifier.height(ProfileSpacing.large))

                    // ---------- Overview ----------
                    Text(
                        text = "OVERVIEW",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(ProfileSpacing.small))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ProfileSpacing.small)
                    ) {
                        StatIconCard(
                            icon = Icons.Filled.SportsEsports,
                            iconTint = MaterialTheme.colorScheme.primary,
                            value = statistics.totalGames.toString(),
                            label = "Played",
                            modifier = Modifier.weight(1f)
                        )
                        StatIconCard(
                            icon = Icons.Filled.EmojiEvents,
                            iconTint = Color(0xFFF2A63D),
                            value = statistics.totalWins.toString(),
                            label = "Won",
                            modifier = Modifier.weight(1f)
                        )
                        StatIconCard(
                            icon = Icons.Filled.WbSunny,
                            iconTint = Color(0xFFE05353),
                            value = statistics.totalLosses.toString(),
                            label = "Lost",
                            modifier = Modifier.weight(1f)
                        )
                        StatIconCard(
                            icon = Icons.Filled.BarChart,
                            iconTint = MaterialTheme.colorScheme.primary,
                            value = "%,d".format(statistics.totalScore),
                            label = "Total Score",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(ProfileSpacing.small))

                    WinRateSummaryCard(
                        winRatio = statistics.winRatio,
                        totalWins = statistics.totalWins,
                        totalGames = statistics.totalGames,
                        bestTimeOverall = bestTimeOverall,
                        bestTimeDifficultyLabel = bestTimeDifficultyLabel
                    )

                    Spacer(modifier = Modifier.height(ProfileSpacing.large))

                    // ---------- By difficulty ----------
                    Text(
                        text = "BY DIFFICULTY",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(ProfileSpacing.small))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(ProfileSpacing.small)
                    ) {
                        DifficultyStatRow(
                            label = "Easy",
                            stats = statistics.easy,
                            accent = Color(0xFF22A06B),
                            bestTime = bestTimes["Easy"],
                            highlighted = highlightedDifficultyLabel == "Easy",
                            onClick = { onDifficultyClick("Easy") }
                        )
                        DifficultyStatRow(
                            label = "Medium",
                            stats = statistics.medium,
                            accent = Color(0xFF3B82F6),
                            bestTime = bestTimes["Medium"],
                            highlighted = highlightedDifficultyLabel == "Medium",
                            onClick = { onDifficultyClick("Medium") }
                        )
                        DifficultyStatRow(
                            label = "Hard",
                            stats = statistics.hard,
                            accent = Color(0xFFE05353),
                            bestTime = bestTimes["Hard"],
                            highlighted = highlightedDifficultyLabel == "Hard",
                            onClick = { onDifficultyClick("Hard") }
                        )
                    }

                    // ---------- History ----------

                    Spacer(
                        modifier = Modifier.height(ProfileSpacing.large)
                    )

                    RecentHistorySection(
                        history = history,
                        onSeeAllHistory = onSeeAllHistory
                    )

                    Spacer(
                        modifier = Modifier.height(ProfileSpacing.medium)
                    )
                }
            }
        }
    }

    if (showAvatarPicker) {
        AvatarPickerDialog(
            currentSelection = selectedAvatar,
            onConfirm = { picked ->
                onAvatarSelected(picked)
                showAvatarPicker = false
            },
            onDismiss = {
                showAvatarPicker = false
            }
        )
    }
}

@Composable
private fun LevelXpRow(
    level: Int,
    currentXp: Int,
    xpForNextLevel: Int
) {
    val progress = if (xpForNextLevel > 0) {
        (currentXp.toFloat() / xpForNextLevel.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Lv $level",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = progress)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = "$currentXp / $xpForNextLevel XP",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun KeepGoingCard(
    message: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.WorkspacePremium,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Keep Going!",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatIconCard(
    icon: ImageVector,
    iconTint: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
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
private fun WinRateSummaryCard(
    winRatio: Int,
    totalWins: Int,
    totalGames: Int,
    bestTimeOverall: String?,
    bestTimeDifficultyLabel: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            WinRateRing(percentage = winRatio)

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Win Rate",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "$totalWins wins out of $totalGames games",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Best Time",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = bestTimeOverall ?: "--:--",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = bestTimeDifficultyLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WinRateRing(
    percentage: Int,
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = MaterialTheme.colorScheme.primary
    val clamped = percentage.coerceIn(0, 100)

    Box(
        modifier = modifier.size(72.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx()
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
                sweepAngle = 360f * (clamped / 100f),
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Text(
            text = "$clamped%",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun DifficultyBadge(accent: Color) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(2) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(accent)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DifficultyStatRow(
    label: String,
    stats: DifficultyStatistics,
    accent: Color,
    bestTime: String?,
    highlighted: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DifficultyBadge(accent = accent)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${stats.games} played \u00B7 ${stats.winRatio}% won",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (stats.games > 0) "%,d".format(stats.score) else "\u2014",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (highlighted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onBackground
                    }
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (stats.games > 0 && bestTime != null) {
                        "Best: $bestTime"
                    } else {
                        "Not won yet"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ProfileHeaderDecoration(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(120.dp)
            .rotate(-12f)
            .alpha(0.35f),
        contentAlignment = Alignment.Center
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DecorTile()
                DecorTile(text = "1", textColor = Color(0xFF3B82F6))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DecorTile(icon = Icons.Filled.Flag, iconTint = Color(0xFFE05353))
                DecorTile(text = "2", textColor = Color(0xFF22A06B))
            }
        }
    }
}

@Composable
private fun DecorTile(
    text: String? = null,
    textColor: Color = Color.Unspecified,
    icon: ImageVector? = null,
    iconTint: Color = Color.Unspecified
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        when {
            icon != null -> Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
            text != null -> Text(
                text = text,
                color = textColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AvatarPickerDialog(
    currentSelection: AvatarOption,
    onConfirm: (AvatarOption) -> Unit,
    onDismiss: () -> Unit
) {
    // Local, unconfirmed choice — nothing is persisted (and the caller
    // isn't notified) until "Confirm" is tapped.
    var pendingSelection by remember {
        mutableStateOf(currentSelection)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Choose your avatar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                // 2 rows x 3 columns, as requested — plain Rows rather
                // than a lazy grid since the set is small and fixed.
                AvatarOption.entries
                    .chunked(3)
                    .forEach { rowOptions ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowOptions.forEach { option ->
                                AvatarGridItem(
                                    option = option,
                                    selected = option == pendingSelection,
                                    onClick = {
                                        pendingSelection = option
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )
                    }

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text("Cancel")
                    }

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

                    Button(
                        onClick = {
                            onConfirm(pendingSelection)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}

@Composable
private fun AvatarGridItem(
    option: AvatarOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .border(
                width = if (selected) 3.dp else 0.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = option.drawableRes),
            contentDescription = option.contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun BadgesRow(
    tracks: List<AchievementTrack>,
    modifier: Modifier = Modifier
) {
    val earnedTracks = tracks.filter { it.achievedTier != null }

    if (earnedTracks.isEmpty()) {
        Text(
            text = "No badges yet \u2014 play to earn your first one",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier
        )
        return
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        earnedTracks.take(5).forEach { track ->
            val tier = track.achievedTier ?: return@forEach
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(colorForBadgeTier(tier)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconForTrack(track.id),
                    contentDescription = "${track.title}: ${tier.displayName}",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

private fun iconForTrack(trackId: String): ImageVector = when (trackId) {
    "games_played" -> Icons.Filled.SportsEsports
    "games_won" -> Icons.Filled.EmojiEvents
    "win_streak" -> Icons.Filled.Whatshot
    "total_score" -> Icons.Filled.BarChart
    "hard_wins" -> Icons.Filled.MilitaryTech
    else -> Icons.Filled.EmojiEvents
}

private fun colorForBadgeTier(tier: BadgeTier): Color = when (tier) {
    BadgeTier.BRONZE -> Color(0xFFCD7F32)
    BadgeTier.SILVER -> Color(0xFFB0BEC5)
    BadgeTier.GOLD -> Color(0xFFFFC107)
    BadgeTier.PLATINUM -> Color(0xFF66C2CE)
    BadgeTier.DIAMOND -> Color(0xFF6FD8FF)
}

@Preview(
    showBackground = true
)
@Composable
private fun ProfileScreenPreview() {
    MinesTheme {
        ProfileScreen(
            username = "Alan",
            tagline = "Mines Explorer",
            statistics = UserStatistics(
                totalGames = 60,
                totalWins = 8,
                totalLosses = 52,
                totalScore = 22793,
                easy = DifficultyStatistics(58, 13, 20489),
                medium = DifficultyStatistics(2, 0, 2304),
                hard = DifficultyStatistics(0, 0, 0)
            ),
            level = 8,
            currentXp = 320,
            xpForNextLevel = 500,
            bestTimeOverall = "00:42",
            bestTimeDifficultyLabel = "Easy",
            bestTimes = mapOf("Easy" to "00:42", "Medium" to "01:28"),
            highlightedDifficultyLabel = "Easy",
            onBack = {}
        )
    }
}