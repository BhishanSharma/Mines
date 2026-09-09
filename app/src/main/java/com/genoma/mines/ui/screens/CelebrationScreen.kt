package com.genoma.mines.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.genoma.mines.feedback.CelebrationEvent
import com.genoma.mines.game.BadgeTier

private object CelebrationSpacing {
    val screenHorizontal = 24.dp
    val iconSize = 108.dp
    val iconToTitle = 28.dp
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

private fun colorForTier(tier: BadgeTier): Color = when (tier) {
    BadgeTier.BRONZE -> Color(0xFFCD7F32)
    BadgeTier.SILVER -> Color(0xFFA8A9AD)
    BadgeTier.GOLD -> Color(0xFFD4A017)
    BadgeTier.PLATINUM -> Color(0xFF7DA0C4)
    BadgeTier.DIAMOND -> Color(0xFF63D2E8)
}

private fun contentFor(event: CelebrationEvent): CelebrationContent = when (event) {
    is CelebrationEvent.LevelUp -> CelebrationContent(
        icon = Icons.Filled.Star,
        iconTint = Color(0xFFFFC107),
        eyebrow = "LEVEL UP",
        title = "Level ${event.newLevel}",
        subtitle = "You've leveled up! Keep playing to climb even higher."
    )

    is CelebrationEvent.AchievementUnlocked -> CelebrationContent(
        icon = Icons.Filled.EmojiEvents,
        iconTint = colorForTier(event.tier),
        eyebrow = "ACHIEVEMENT UNLOCKED",
        title = "${event.tier.displayName} \u2014 ${event.trackTitle}",
        subtitle = "A new badge has been added to your collection."
    )
}

/**
 * A full, dedicated screen that acknowledges the level-ups and/or
 * achievement unlocks earned by the game the player just finished.
 *
 * Rather than an overlay squeezed on top of the game board, this takes over
 * the whole screen — confetti, a big badge, and a clear headline — so a
 * milestone gets the same weight as any other main destination in the app
 * instead of competing with the board for attention. [events] is shown one
 * at a time; [onContinue] advances to the next one, and once the last is
 * dismissed the caller is expected to navigate away (typically back Home).
 */
@Composable
fun CelebrationScreen(
    events: List<CelebrationEvent>,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val current = events.firstOrNull() ?: return
    val remaining = events.size

    Box(modifier = modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(WindowInsets.safeDrawing.asPaddingValues())
                    .padding(horizontal = CelebrationSpacing.screenHorizontal),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Keyed on the event itself so back-to-back milestones (a
                // level-up immediately followed by an achievement, say)
                // each get a fresh pop-in rather than silently swapping text
                // underneath a single static icon.
                AnimatedContent(
                    targetState = current,
                    transitionSpec = {
                        (fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.85f)) togetherWith
                                (fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.85f))
                    },
                    label = "celebration_content"
                ) { event ->
                    val animatedContent = contentFor(event)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(CelebrationSpacing.iconSize)
                                .background(
                                    animatedContent.iconTint.copy(alpha = 0.15f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = animatedContent.icon,
                                contentDescription = null,
                                tint = animatedContent.iconTint,
                                modifier = Modifier.size(56.dp)
                            )
                        }

                        Text(
                            text = animatedContent.eyebrow,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = animatedContent.iconTint,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = CelebrationSpacing.iconToTitle)
                        )

                        Text(
                            text = animatedContent.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground,
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
                                .padding(top = CelebrationSpacing.titleToSubtitle)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(CelebrationSpacing.subtitleToButton))

                // Only shown when more than one milestone is queued from
                // the same game, so the player knows another is coming
                // right after this one.
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
                        text = if (remaining > 1) "Next" else "Continue",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        ConfettiOverlay(
            visible = true,
            modifier = Modifier.fillMaxSize()
        )
    }
}