package com.genoma.mines.home.ui
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.IntrinsicSize
import com.genoma.mines.core.theme.MinesPreviews
import com.genoma.mines.core.ui.components.ClassicAmbientStyle
import com.genoma.mines.core.ui.components.ScreenSurface
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.genoma.mines.home.R
import com.genoma.mines.game.domain.Difficulty
import com.genoma.mines.core.theme.MinesTheme
import com.genoma.mines.core.theme.LocalMinesThemeVariant
import com.genoma.mines.core.theme.MinesThemeVariant
import com.genoma.mines.life.domain.LifeStatus
import com.genoma.mines.life.ui.HeartsIndicator
import com.genoma.mines.profile.domain.AvatarOption


private object Spacing {
    val screenTop = 16.dp
    val screenHorizontal = 24.dp
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
    coins: Int = 0,
    gems: Int = 0,
    lifeStatus: LifeStatus = LifeStatus.FULL,
    onHeartsClick: () -> Unit = {},

    // XP / level information
    hasPlayedGame: Boolean = false,
    level: Int = 1,
    currentXp: Int = 0,
    xpForNextLevel: Int = 100,

    // Formatted fastest win per difficulty; missing = no win yet.
    bestTimes: Map<Difficulty, String> = emptyMap(),

    onOpenTournament: () -> Unit,
    themeVariant: MinesThemeVariant = MinesThemeVariant.CLASSIC_TEAL
) {
    ScreenSurface(
        modifier = Modifier.fillMaxSize(),
        classicAmbient = ClassicAmbientStyle.SHOWCASE
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Status bars are hidden app-wide, so keep the header clear
                // of a camera cutout and off the very top edge.
                .windowInsetsPadding(
                    WindowInsets.displayCutout.only(WindowInsetsSides.Top)
                )
                .padding(horizontal = Spacing.screenHorizontal)
                .padding(
                    top = Spacing.screenTop,
                    bottom = Spacing.screenBottom
                )
        ) {

            // ---------- Top bar: avatar + name + quick actions ----------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {


                // Avatar + name together form one profile entry point, so
                // the whole block is a single, larger tap target. Takes all
                // remaining width so the XP row below has a real bound.
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(
                            onClickLabel = "Open profile",
                            role = Role.Button,
                            onClick = onOpenProfile
                        )
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                            .background(
                                MaterialTheme.colorScheme.primaryContainer
                            ),
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

                    Column(modifier = Modifier.weight(1f)) {

                        // Chevron hints that the block opens the profile.
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = username,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // -------------------------------------------------
                        // First-time user:
                        // Show "Ready to explore?"
                        //
                        // After the first game:
                        // Show level + XP progress bar.
                        // -------------------------------------------------
                        if (!hasPlayedGame) {

                            Text(
                                text = "Ready to explore?",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                        } else {

                            val xpProgress = if (xpForNextLevel > 0) {
                                (
                                        currentXp.toFloat() /
                                                xpForNextLevel.toFloat()
                                        ).coerceIn(0f, 1f)
                            } else {
                                0f
                            }

                            // Text on its own line, bar full-width below it:
                            // sharing one row, large fonts on narrow phones
                            // squeezed the bar to nothing and clipped the XP.
                            Row(
                                modifier = Modifier.padding(top = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {

                                Text(
                                    text = "Lv $level",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    softWrap = false
                                )

                                Text(
                                    text = "$currentXp/$xpForNextLevel XP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            LinearProgressIndicator(
                                progress = { xpProgress },
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(
                                        RoundedCornerShape(50)
                                    ),
                                trackColor = MaterialTheme
                                    .colorScheme
                                    .surfaceVariant,
                                // M3 draws a stop dot at the track end;
                                // it reads as a stray glyph at this size.
                                drawStopIndicator = {}
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    // Medal, not trophy: the trophy icon is Achievements
                    // in the bottom nav.
                    IconPillButton(
                        icon = Icons.Filled.MilitaryTech,
                        contentDescription = "Tournament",
                        onClick = onOpenTournament,
                        filled = true
                    )

                    IconPillButton(
                        icon = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        onClick = onOpenSettings,
                        filled = false
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(Spacing.medium)
            )

            // ---------- Hearts, diamond & coin pills ----------
            // FlowRow so the pills wrap instead of clipping on narrow phones.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                HeartsIndicator(
                    status = lifeStatus,
                    onClick = onHeartsClick
                )

                CurrencyPill(
                    icon = Icons.Filled.Diamond,
                    contentDescription = "Diamonds",
                    value = gems
                )

                CurrencyPill(
                    icon = Icons.Filled.MonetizationOn,
                    contentDescription = "Coins",
                    value = coins
                )
            }

            // ---------- Hero: copy on the left, illustration on the right ----------
            // The hero owns all the flexible space and is centered in it, so
            // the difficulty picker and Start sit at the bottom (thumb
            // reach) instead of floating mid-screen between two empty gaps.
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = Spacing.large),
                contentAlignment = Alignment.Center
            ) {
                // Small phones with large fonts leave little height here:
                // cap the illustration to what's available and drop the
                // eyebrow label so the title and tagline never get clipped.
                val roomy = maxHeight >= 150.dp
                val heroMaxHeight = maxHeight

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        if (roomy) {
                            Text(
                                text = "CLASSIC PUZZLE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(
                                modifier = Modifier.height(4.dp)
                            )
                        }

                        Text(
                            text = "Mines",
                            style = MaterialTheme.typography
                                .headlineLarge
                                .copy(fontSize = 36.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(
                            modifier = Modifier.height(2.dp)
                        )

                        Text(
                            text = "Think. Uncover. Win.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(
                        modifier = Modifier.width(16.dp)
                    )

                    // Shares the row with the title instead of taking a fixed
                    // 220dp, so narrow phones don't squeeze the copy.
                    MinesHeroIllustration(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .widthIn(max = 220.dp)
                            .heightIn(max = heroMaxHeight)
                    )
                }
            }

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

            Spacer(
                modifier = Modifier.height(Spacing.optionGap)
            )

            // ---------- Difficulty cards ----------
            // IntrinsicSize.Min + fillMaxHeight keeps all three cards the
            // same height even when one card's text wraps and another's
            // doesn't.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Difficulty.entries.forEach { difficulty ->

                    DifficultyOption(
                        difficulty = difficulty,
                        bestTime = bestTimes[difficulty],
                        selected = selectedDifficulty == difficulty,
                        onClick = {
                            onDifficultySelected(difficulty)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(Spacing.large)
            )

            // ---------- Start game ----------
            StartGameButton(
                hasHearts = lifeStatus.hasHearts,
                onStartGame = onStartGame,
                onRefillHearts = onHeartsClick
            )
        }
    }
}


/**
 * Primary call to action. Shows the heart a game costs, and with no hearts
 * left turns into a refill button instead of a Start that can't start.
 */
@Composable
private fun StartGameButton(
    hasHearts: Boolean,
    onStartGame: () -> Unit,
    onRefillHearts: () -> Unit
) {
    val containerColor = if (hasHearts) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = if (hasHearts) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .clickable(
                role = Role.Button,
                onClick = if (hasHearts) onStartGame else onRefillHearts
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            imageVector = if (hasHearts) Icons.Filled.PlayArrow else Icons.Filled.Favorite,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )

        Spacer(
            modifier = Modifier.width(8.dp)
        )

        Text(
            text = if (hasHearts) "Start Game" else "Out of hearts · Refill",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )

        if (hasHearts) {
            Spacer(
                modifier = Modifier.width(10.dp)
            )

            // Cost chip: every game spends one heart (refunded on a win).
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(contentColor.copy(alpha = 0.18f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "−1",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )

                Spacer(
                    modifier = Modifier.width(3.dp)
                )

                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "heart",
                    tint = contentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun IconPillButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    filled: Boolean
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (filled) {
                    Modifier.background(
                        MaterialTheme.colorScheme.primaryContainer
                    )
                } else {
                    Modifier
                        .background(
                            MaterialTheme.colorScheme.surface
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(12.dp)
                        )
                }
            )
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {

        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (filled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onBackground
            },
            modifier = Modifier.size(22.dp)
        )
    }
}


@Composable
private fun MinesHeroIllustration(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {

        // Deep Ocean gets a blue-recolored copy of the same art (only the
        // mint blob and tiles shift; flag, bomb and numbers are unchanged).
        val banner = if (LocalMinesThemeVariant.current == MinesThemeVariant.DEEP_OCEAN) {
            R.drawable.home_banner_ocean
        } else {
            R.drawable.home_banner
        }

        Image(
            painter = painterResource(
                id = banner
            ),
            contentDescription = "Mines logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}


@Composable
private fun MiniGridIcon(
    selected: Boolean
) {
    val dotColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {

        repeat(3) {

            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {

                repeat(3) {

                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                dotColor.copy(
                                    alpha = if (selected) {
                                        0.9f
                                    } else {
                                        0.35f
                                    }
                                )
                            )
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
    modifier: Modifier = Modifier,
    bestTime: String? = null
) {
    val interactionSource =
        remember { MutableInteractionSource() }

    Card(
        modifier = modifier
            .semantics {
                contentDescription =
                    "${difficulty.displayName()} difficulty, " +
                            "${difficulty.rows} by ${difficulty.columns} grid, " +
                            "${difficulty.mines} mines" +
                            (bestTime?.let { ", best time $it" } ?: "")
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
                .fillMaxSize()
                .padding(12.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {

                MiniGridIcon(
                    selected = selected
                )

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

            Spacer(
                modifier = Modifier.height(10.dp)
            )

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

            Spacer(
                modifier = Modifier.height(2.dp)
            )

            Text(
                text = "${difficulty.mines} mines",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Pushes the chip to the card's bottom edge, so chips line up
            // across cards of equal height.
            Spacer(
                modifier = Modifier
                    .height(8.dp)
                    .weight(1f)
            )

            // Personal best once there is one (a reason to replay this
            // difficulty); the tagline until then. Full width, and the best
            // time always uses a fixed "BEST / mm:ss" two-line layout so it
            // never wraps differently from card to card.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                            .copy(alpha = 0.08f)
                    )
                    .padding(
                        horizontal = 8.dp,
                        vertical = 6.dp
                    )
            ) {

                if (bestTime != null) {
                    Text(
                        text = "BEST",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )

                        Spacer(
                            modifier = Modifier.width(4.dp)
                        )

                        // Shrinks slightly instead of clipping when a large
                        // font scale meets a narrow card.
                        BasicText(
                            text = bestTime,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = 0.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            autoSize = TextAutoSize.StepBased(
                                minFontSize = 9.sp,
                                maxFontSize = MaterialTheme.typography.labelMedium.fontSize
                            )
                        )
                    }
                } else {
                    BasicText(
                        text = difficulty.tagline(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            // The default label tracking is too wide for a
                            // ~60dp chip at large font scales.
                            letterSpacing = 0.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        autoSize = TextAutoSize.StepBased(
                            minFontSize = 9.sp,
                            maxFontSize = MaterialTheme.typography.labelSmall.fontSize
                        )
                    )
                }
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


// One word each: the cards are narrow, and longer phrases broke mid-word
// at large font sizes.
fun Difficulty.tagline(): String {
    return when (this) {
        Difficulty.EASY -> "Beginner"
        Difficulty.MEDIUM -> "Balanced"
        Difficulty.HARD -> "Expert"
    }
}


@Composable
private fun CurrencyPill(
    icon: ImageVector,
    contentDescription: String,
    value: Int
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                MaterialTheme.colorScheme.primaryContainer
            )
            .padding(
                horizontal = 14.dp,
                vertical = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )

        Spacer(
            modifier = Modifier.width(6.dp)
        )

        Text(
            text = "$value",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}


@MinesPreviews
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
            onOpenTournament = {},
            coins = 0,
            hasPlayedGame = true,
            level = 3,
            currentXp = 40,
            xpForNextLevel = 150,
            bestTimes = mapOf(Difficulty.EASY to "01:12")
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun HomeScreenOutOfHeartsPreview() {
    MinesTheme {
        HomeScreen(
            selectedDifficulty = Difficulty.MEDIUM,
            onDifficultySelected = {},
            onStartGame = {},
            onOpenSettings = {},
            onOpenProfile = {},
            username = "A very long player name",
            onOpenTournament = {},
            lifeStatus = LifeStatus(
                hearts = 0,
                maxHearts = 5,
                nextHeartAtMillis = System.currentTimeMillis() + 30 * 60 * 1000
            )
        )
    }
}