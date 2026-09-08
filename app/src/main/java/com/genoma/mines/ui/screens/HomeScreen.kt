package com.genoma.mines.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.genoma.mines.R
import com.genoma.mines.game.Difficulty
import com.genoma.mines.ui.theme.MinesTheme


private object Spacing {
    val screenHorizontal = 24.dp
    val screenTop = 20.dp
    val screenBottom = 24.dp
    val optionGap = 10.dp
    val medium = 14.dp
    val large = 22.dp
}


@Composable
fun HomeScreen(
    selectedDifficulty: Difficulty,
    onDifficultySelected: (Difficulty) -> Unit,
    onStartGame: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    username: String = "Player",
    selectedAvatar: AvatarOption = AvatarOption.Default,
    photoUrl: String? = null,
    gamesWon: Int = 0,
    onOpenPremium: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.screenHorizontal)
                .padding(
                    top = Spacing.screenTop,
                    bottom = Spacing.screenBottom
                )
        ) {

            // ---------- Top bar: avatar + name + quick actions ----------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {

                Row(verticalAlignment = Alignment.CenterVertically) {

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable(onClick = onOpenProfile),
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoUrl != null) {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = "Profile",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(3.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            AsyncImage(
                                model = selectedAvatar.drawableRes,
                                contentDescription = "Profile",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(3.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = username,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Ready to explore?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconPillButton(
                        icon = Icons.Filled.WorkspacePremium,
                        onClick = onOpenPremium,
                        filled = true
                    )
                    IconPillButton(
                        icon = Icons.Filled.Settings,
                        onClick = onOpenSettings,
                        filled = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            // ---------- Star / score pill ----------
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Score",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$gamesWon",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }


            Spacer(modifier = Modifier.height(Spacing.large))

            // ---------- Hero: copy on the left, illustration on the right ----------
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CLASSIC PUZZLE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Mines",
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Think. Uncover. Win.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                }

                Spacer(modifier = Modifier.width(16.dp))

                MinesHeroIllustration()
            }

            Spacer(modifier = Modifier.weight(0.5f))
            // ---------- Difficulty section header ----------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CHOOSE DIFFICULTY",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(Spacing.optionGap))

            // ---------- Difficulty cards ----------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Difficulty.entries.forEach { difficulty ->
                    DifficultyOption(
                        difficulty = difficulty,
                        selected = selectedDifficulty == difficulty,
                        onClick = { onDifficultySelected(difficulty) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.large))

            // ---------- Start game ----------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onStartGame),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Start Game",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.weight(0.5f))


        }
    }
}


@Composable
private fun IconPillButton(
    icon: ImageVector,
    onClick: () -> Unit,
    filled: Boolean
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (filled) {
                    Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                } else {
                    Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(12.dp)
                        )
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (filled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onBackground
            },
            modifier = Modifier.size(22.dp)
        )
    }
}


/**
 * Small illustrative widget built entirely from shapes + icons (no extra
 * drawable assets required): a soft blob backdrop, a 2x2 tile grid with a
 * flag and two number tiles, and a bomb badge with a spark. Swap this out
 * for a real illustration drawable if/when you have one.
 */
@Composable
private fun MinesHeroIllustration(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(220.dp),
        contentAlignment = Alignment.Center
    ) {

            Image(
                painter = painterResource(
                    id = R.drawable.home_banner
                ),
                contentDescription = "Mines logo",
                contentScale = ContentScale.FillBounds
            )
    }
}




@Composable
private fun MiniGridIcon(selected: Boolean) {
    val dotColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(3) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(dotColor.copy(alpha = if (selected) 0.9f else 0.35f))
                    )
                }
            }
        }
    }
}


@Composable
fun DifficultyOption(
    difficulty: Difficulty,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Card(
        modifier = modifier
            .semantics {
                contentDescription =
                    "${difficulty.displayName()} difficulty, " +
                            "${difficulty.rows} by ${difficulty.columns} grid, " +
                            "${difficulty.mines} mines"
            }
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
                interactionSource = interactionSource,
                indication = null
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (selected) 2.dp else 0.dp
        ),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                MiniGridIcon(selected = selected)
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
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = difficulty.displayName(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )

            Spacer(modifier = Modifier.height(2.dp))


            Text(
                text = "${difficulty.mines} mines",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = difficulty.tagline(),
                    style = MaterialTheme.typography.labelSmall,
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

fun Difficulty.tagline(): String {
    return when (this) {
        Difficulty.EASY -> "Good for beginners"
        Difficulty.MEDIUM -> "A balanced challenge"
        Difficulty.HARD -> "For true experts"
    }
}


@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    MinesTheme {
        HomeScreen(
            selectedDifficulty = Difficulty.EASY,
            onDifficultySelected = {},
            onStartGame = {},
            onOpenSettings = {},
            onOpenProfile = {},
            username = "Alan",
            gamesWon = 0
        )
    }
}