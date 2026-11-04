package com.genoma.mines.celebration.ui
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import com.genoma.mines.achievements.ui.achievementTrackIcon
import com.genoma.mines.achievements.ui.color
import com.genoma.mines.core.theme.TierGold
import com.genoma.mines.core.ui.components.ClassicAmbientStyle
import com.genoma.mines.core.ui.components.ScreenSurface
import com.genoma.mines.core.ui.components.ConfettiOverlay
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.genoma.mines.celebration.domain.CelebrationEvent
import com.genoma.mines.achievements.domain.BadgeTier
import com.genoma.mines.core.theme.MinesTheme
import kotlin.math.cos
import kotlin.math.sin

private object CelebrationSpacing {
    val screenHorizontal = 24.dp
    val iconToTitle = 12.dp
    val titleToSubtitle = 8.dp
    val subtitleToButton = 40.dp
}

private data class CelebrationContent(
    val icon: ImageVector,
    val iconTint: Color,
    val eyebrow: String,
    val title: String,
    val subtitle: String
)

private data class Sparkle(
    val angle: Float,
    val distance: Float,
    val size: Float,
    val alpha: Float
)

private fun colorForTier(tier: BadgeTier): Color = tier.color()

private fun contentFor(event: CelebrationEvent): CelebrationContent = when (event) {
    is CelebrationEvent.LevelUp -> CelebrationContent(
        icon = Icons.Filled.Star,
        iconTint = TierGold,
        eyebrow = "LEVEL UP",
        title = "Level ${event.newLevel}",
        subtitle = "You've leveled up! Keep playing to climb even higher."
    )

    // The badge itself (track icon in its tier color, as on Achievements
    // and Profile) rather than a stock trophy; the tier moves into the
    // eyebrow so the title is just the track name.
    is CelebrationEvent.AchievementUnlocked -> CelebrationContent(
        icon = achievementTrackIcon(event.trackId),
        iconTint = colorForTier(event.tier),
        eyebrow = "${event.tier.displayName.uppercase()} BADGE UNLOCKED",
        title = event.trackTitle,
        subtitle = "Added to your collection. Keep going for the next tier."
    )
}

/**
 * A full, dedicated screen that acknowledges the level-ups and/or
 * achievement unlocks earned by the game the player just finished.
 *
 * Each event is shown one at a time. The caller is responsible for
 * advancing the event list and navigating away after the final event.
 */
@Composable
fun CelebrationScreen(
    events: List<CelebrationEvent>,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val current = events.firstOrNull() ?: return
    val remaining = events.size

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        ScreenSurface(
            modifier = Modifier.fillMaxSize(),
            classicAmbient = ClassicAmbientStyle.CELEBRATE
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Drawn first so it sits behind the badge and text: it used to
                // be drawn last, on top, and covered the title and button.
                ConfettiOverlay(
                    visible = true,
                    modifier = Modifier.fillMaxSize()
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(WindowInsets.safeDrawing.asPaddingValues())
                        .padding(horizontal = CelebrationSpacing.screenHorizontal),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AnimatedContent(
                        targetState = current,
                        transitionSpec = {
                            (
                                    fadeIn(
                                        animationSpec = tween(220)
                                    ) +
                                            scaleIn(
                                                animationSpec = tween(220),
                                                initialScale = 0.85f
                                            )
                                    ) togetherWith (
                                    fadeOut(
                                        animationSpec = tween(150)
                                    ) +
                                            scaleOut(
                                                animationSpec = tween(150),
                                                targetScale = 0.85f
                                            )
                                    )
                        },
                        label = "celebration_content"
                    ) { event ->

                        val animatedContent = contentFor(event)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CelebrationHero(
                                icon = animatedContent.icon,
                                iconTint = animatedContent.iconTint
                            )

                            Spacer(modifier = Modifier.height(CelebrationSpacing.iconToTitle))

                            // Opaque card behind the text: confetti is drawn
                            // underneath, but transparent text still let it
                            // show through between the letters.
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Tier colors like gold are too light to
                                    // read on a white card (~1.6:1), so on
                                    // light cards pull them toward the text
                                    // color while keeping the hue.
                                    val onLightCard = MaterialTheme.colorScheme.surface.luminance() > 0.5f
                                    Text(
                                        text = animatedContent.eyebrow,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (onLightCard) {
                                            lerp(animatedContent.iconTint, MaterialTheme.colorScheme.onSurface, 0.45f)
                                        } else {
                                            animatedContent.iconTint
                                        },
                                        textAlign = TextAlign.Center
                                    )

                                    Text(
                                        text = animatedContent.title,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 6.dp)
                                    )

                                    Text(
                                        text = animatedContent.subtitle,
                                        style = MaterialTheme.typography.bodyLarge,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                top = CelebrationSpacing.titleToSubtitle
                                            )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(
                            CelebrationSpacing.subtitleToButton
                        )
                    )

                    // Only shown when more than one milestone is queued.
                    if (remaining > 1) {
                        Text(
                            text = "${remaining - 1} more to celebrate",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    Button(
                        onClick = onContinue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = if (remaining > 1) {
                                "Next"
                            } else {
                                "Continue"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Large celebration hero consisting of:
 *
 * - Soft radial glow
 * - Decorative sparkles
 * - Large central badge
 * - Very subtle breathing/pulse animation
 */
@Composable
private fun CelebrationHero(
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(
        label = "celebration_hero"
    )

    val badgeScale = infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.045f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1400,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badge_pulse"
    )

    Box(
        modifier = modifier.size(180.dp),
        contentAlignment = Alignment.Center
    ) {
        // -------------------------------------------------------------
        // Soft radial glow
        // -------------------------------------------------------------
        Box(
            modifier = Modifier
                .size(176.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            iconTint.copy(alpha = 0.22f),
                            iconTint.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // -------------------------------------------------------------
        // Small decorative sparkles
        // -------------------------------------------------------------
        CelebrationSparkles(
            tint = iconTint,
            modifier = Modifier.fillMaxSize()
        )

        // -------------------------------------------------------------
        // Main badge
        // -------------------------------------------------------------
        Box(
            modifier = Modifier
                .size(136.dp)
                .scale(badgeScale.value)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Inner colored halo.
            Box(
                modifier = Modifier
                    .size(116.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                iconTint.copy(alpha = 0.22f),
                                iconTint.copy(alpha = 0.06f)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Icon background.
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            color = iconTint.copy(alpha = 0.14f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
        }
    }
}

/**
 * Draws a handful of subtle four-point sparkles around the badge.
 *
 * The sparkles deliberately sit outside the main 136dp badge so they
 * decorate the hero without competing with the icon.
 */
@Composable
private fun CelebrationSparkles(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
    ) {
        val center = Offset(
            x = size.width / 2f,
            y = size.height / 2f
        )

        val sparkles = listOf(
            Sparkle(
                angle = -55f,
                distance = 72f,
                size = 5f,
                alpha = 0.90f
            ),
            Sparkle(
                angle = 35f,
                distance = 76f,
                size = 4f,
                alpha = 0.65f
            ),
            Sparkle(
                angle = 145f,
                distance = 75f,
                size = 5f,
                alpha = 0.75f
            ),
            Sparkle(
                angle = 210f,
                distance = 68f,
                size = 3f,
                alpha = 0.55f
            )
        )

        sparkles.forEach { sparkle ->
            val radians = Math.toRadians(
                sparkle.angle.toDouble()
            )

            val x = center.x +
                    cos(radians).toFloat() * sparkle.distance

            val y = center.y +
                    sin(radians).toFloat() * sparkle.distance

            val sparkleSize = sparkle.size.dp.toPx()

            drawLine(
                color = tint.copy(alpha = sparkle.alpha),
                start = Offset(
                    x = x - sparkleSize,
                    y = y
                ),
                end = Offset(
                    x = x + sparkleSize,
                    y = y
                ),
                strokeWidth = 2.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )

            drawLine(
                color = tint.copy(alpha = sparkle.alpha),
                start = Offset(
                    x = x,
                    y = y - sparkleSize
                ),
                end = Offset(
                    x = x,
                    y = y + sparkleSize
                ),
                strokeWidth = 2.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun CelebrationScreenPreview() {
    MinesTheme {
        CelebrationScreen(
            events = listOf(
                CelebrationEvent.LevelUp(
                    newLevel = 5
                )
            ),
            onContinue = {},
            modifier = Modifier
        )
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun CelebrationAchievementPreview() {
    MinesTheme {
        CelebrationScreen(
            events = listOf(
                CelebrationEvent.AchievementUnlocked(
                    trackId = "minesweeper_master",
                    tier = BadgeTier.GOLD,
                    trackTitle = "Minesweeper Master"
                )
            ),
            onContinue = {},
            modifier = Modifier
        )
    }
}