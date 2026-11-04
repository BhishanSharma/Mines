package com.genoma.mines.core.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.genoma.mines.core.theme.MinesThemeVariant
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Lightweight, content-friendly ocean ambience for list/card-heavy screens
 * (Store, Settings, Profile, Wallet, Achievements, etc).
 *
 * Deliberately NOT the same treatment as GameScreen's illustrated scene —
 * no painted art, no character/wreck/floor panels, just:
 *   1. a slow vertical gradient wash
 *   2. an optional soft breathing highlight near the top (not hard "god
 *      rays" — a gentle radial glow, distinct from the game screen's look)
 *   3. a light bubble drift, fewer/slower/smaller than the game screen's
 *
 * Follows light/dark mode: the deep-water palette in dark mode, a pale
 * wash built from the light color scheme otherwise, so the scheme's
 * on-background text stays readable on top of it either way.
 *
 * Self-gates on [themeVariant] — safe to mount unconditionally, renders
 * nothing for any variant other than [MinesThemeVariant.DEEP_OCEAN].
 */
@Composable
fun AmbientOceanBackdrop(
    themeVariant: MinesThemeVariant,
    modifier: Modifier = Modifier,
    showHighlight: Boolean = true,
    bubbleCount: Int = 14
) {
    if (themeVariant != MinesThemeVariant.DEEP_OCEAN) return

    val colors = ambientOceanColors()

    val infiniteTransition = rememberInfiniteTransition(label = "ambientOcean")
    val highlightAlpha by infiniteTransition.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.24f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "highlightAlpha"
    )

    val bubbles = remember(bubbleCount) { generateAmbientBubbles(bubbleCount) }
    // With animations turned off, hold one still frame instead of looping.
    val reduceMotion = rememberReduceMotion()
    var elapsedMs by remember { mutableLongStateOf(if (reduceMotion) STILL_FRAME_MS else 0L) }
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) return@LaunchedEffect
        val startNanos = withFrameNanos { it }
        while (true) {
            val nowNanos = withFrameNanos { it }
            elapsedMs = (nowNanos - startNanos) / 1_000_000L
        }
    }

    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(colors.gradientTop, colors.gradientBottom)
            ),
            size = size
        )

        if (showHighlight) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colors.highlight.copy(alpha = highlightAlpha),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, 0f),
                    radius = size.width * 0.9f
                ),
                size = size
            )
        }

        bubbles.forEach { bubble ->
            drawAmbientBubble(bubble, elapsedMs, colors.bubble, colors.bubbleMaxAlpha)
        }
    }
}

private data class AmbientOceanColors(
    val gradientTop: Color,
    val gradientBottom: Color,
    val highlight: Color,
    val bubble: Color,
    val bubbleMaxAlpha: Float
)

@Composable
private fun ambientOceanColors(): AmbientOceanColors {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f

    return if (isDark) {
        AmbientOceanColors(
            gradientTop = Color(0xFF06222E),
            gradientBottom = Color(0xFF0B3A52),
            highlight = Color.White,
            bubble = Color(0xFFBEEAF5),
            // Capped lower than the game screen's bubbles — meant to be
            // background texture, not a focal point.
            bubbleMaxAlpha = 0.35f
        )
    } else {
        // Light mode: stay close to the scheme's background so dark text
        // keeps its contrast, with just a hint of ocean toward the bottom.
        AmbientOceanColors(
            gradientTop = scheme.background,
            gradientBottom = lerp(scheme.background, scheme.primary, 0.12f),
            highlight = Color.White,
            bubble = scheme.primary,
            bubbleMaxAlpha = 0.22f
        )
    }
}

private data class AmbientBubble(
    val xFraction: Float,
    val sizeDp: Float,
    val riseDurationMs: Long,
    val delayMs: Long,
    val driftAmplitudeDp: Float
)

private fun generateAmbientBubbles(count: Int): List<AmbientBubble> = List(count) {
    AmbientBubble(
        xFraction = Random.nextFloat(),
        sizeDp = Random.nextFloat() * 4f + 2f,
        riseDurationMs = Random.nextLong(5000L, 9000L),
        delayMs = Random.nextLong(0L, 9000L),
        driftAmplitudeDp = Random.nextFloat() * 6f + 2f
    )
}

private fun DrawScope.drawAmbientBubble(
    bubble: AmbientBubble,
    elapsedMs: Long,
    color: Color,
    maxAlpha: Float
) {
    // The delay only staggers each bubble's first rise; after that it
    // loops back-to-back, so bubbles aren't hidden for most of each cycle.
    val sinceStart = elapsedMs - bubble.delayMs
    if (sinceStart < 0L) return

    val progress = (sinceStart % bubble.riseDurationMs) / bubble.riseDurationMs.toFloat()
    val sizePx = bubble.sizeDp.dp.toPx()
    val driftPx = bubble.driftAmplitudeDp.dp.toPx()

    // Starts one bubble-size below the bottom edge and ends one above the
    // top, so it never pops in or out while visible.
    val y = size.height + sizePx - progress * (size.height + 2f * sizePx)
    val sway = sin(progress * 2f * PI.toFloat()) * driftPx
    val x = bubble.xFraction * size.width + sway

    val alpha = when {
        progress < 0.15f -> progress / 0.15f
        progress > 0.85f -> ((1f - progress) / 0.15f).coerceIn(0f, 1f)
        else -> 1f
    } * maxAlpha

    drawCircle(
        color = color.copy(alpha = alpha),
        radius = sizePx / 2f,
        center = Offset(x, y)
    )
}
