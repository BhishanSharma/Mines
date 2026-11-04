package com.genoma.mines.core.ui.components

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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.genoma.mines.core.theme.MinesThemeVariant
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Ocean ambience for the Deep Ocean theme outside the game board, which
 * has its own illustrated scene.
 *
 * Calm (default) — for list/card-heavy screens (Store, Settings, Profile…):
 *   1. a slow vertical gradient wash
 *   2. a soft breathing highlight near the top
 *   3. a light bubble drift
 *
 * [lively] — for showcase screens (Home, Celebration), adding:
 *   4. slow-swaying light rays from the surface
 *   5. a small school of silhouette fish at different depths
 *   6. more, larger bubbles drawn as rings with a glint
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
    lively: Boolean = false
) {
    if (themeVariant != MinesThemeVariant.DEEP_OCEAN) return

    val colors = ambientOceanColors()

    val bubbles = remember(lively) { generateAmbientBubbles(if (lively) 26 else 14, lively) }
    val fish = remember(lively) { if (lively) generateFish() else emptyList() }

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
        val seconds = elapsedMs / 1000f

        // 1. Water.
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(colors.gradientTop, colors.gradientBottom)
            ),
            size = size
        )

        // 2. Breathing surface glow (~4s each way).
        val highlightAlpha = 0.17f + 0.07f * sin(seconds * PI.toFloat() / 4.2f)
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

        if (lively) {
            // 4. Light rays, behind the fish.
            drawLightRays(seconds, colors.ray)

            // 5. Fish.
            fish.forEach { drawFish(it, elapsedMs, colors.fish) }
        }

        // 3 / 6. Bubbles in front.
        bubbles.forEach { bubble ->
            drawAmbientBubble(bubble, elapsedMs, colors.bubble, colors.bubbleMaxAlpha, ringed = lively)
        }
    }
}

private data class AmbientOceanColors(
    val gradientTop: Color,
    val gradientBottom: Color,
    val highlight: Color,
    val bubble: Color,
    val bubbleMaxAlpha: Float,
    val ray: Color,
    val fish: Color
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
            bubbleMaxAlpha = 0.45f,
            ray = Color(0xFFBEEAF5).copy(alpha = 0.10f),
            // Darker than the water, like silhouettes seen from below.
            fish = Color(0xFF021219).copy(alpha = 0.55f)
        )
    } else {
        // Light mode: stay close to the scheme's background so dark text
        // keeps its contrast, with a hint of ocean toward the bottom.
        AmbientOceanColors(
            gradientTop = scheme.background,
            gradientBottom = lerp(scheme.background, scheme.primary, 0.16f),
            highlight = Color.White,
            bubble = scheme.primary,
            bubbleMaxAlpha = 0.30f,
            ray = Color.White.copy(alpha = 0.45f),
            fish = scheme.primary.copy(alpha = 0.20f)
        )
    }
}

// ---------- Light rays ----------

private fun DrawScope.drawLightRays(seconds: Float, color: Color) {
    // Three tapered beams from just above the top edge, each swaying on
    // its own slow cycle and slanting slightly, like light through waves.
    val rays = listOf(
        Triple(0.20f, 0.10f, 0f),
        Triple(0.52f, 0.14f, 1.9f),
        Triple(0.82f, 0.09f, 3.7f)
    )
    rays.forEach { (xFraction, widthFraction, phase) ->
        val sway = sin(seconds * 0.25f + phase) * size.width * 0.04f
        val pulse = 0.7f + 0.3f * sin(seconds * 0.4f + phase * 1.3f)
        val topX = size.width * xFraction + sway
        val topHalf = size.width * widthFraction * 0.35f
        val bottomX = topX + size.width * 0.18f
        val bottomHalf = size.width * widthFraction
        val bottomY = size.height * 0.75f

        val beam = Path().apply {
            moveTo(topX - topHalf, 0f)
            lineTo(topX + topHalf, 0f)
            lineTo(bottomX + bottomHalf, bottomY)
            lineTo(bottomX - bottomHalf, bottomY)
            close()
        }
        drawPath(
            path = beam,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = color.alpha * pulse), Color.Transparent),
                startY = 0f,
                endY = bottomY
            )
        )
    }
}

// ---------- Fish ----------

private data class Fish(
    val yFraction: Float,
    val lengthDp: Float,
    val crossMs: Long,
    val phaseMs: Long,
    val leftToRight: Boolean,
    val bobDp: Float
)

private fun generateFish(): List<Fish> = listOf(
    // Spread across depths; two pairs swimming opposite ways.
    Fish(yFraction = 0.30f, lengthDp = 42f, crossMs = 26_000L, phaseMs = 4_000L, leftToRight = true, bobDp = 6f),
    Fish(yFraction = 0.36f, lengthDp = 30f, crossMs = 30_000L, phaseMs = 9_000L, leftToRight = true, bobDp = 5f),
    Fish(yFraction = 0.58f, lengthDp = 54f, crossMs = 34_000L, phaseMs = 20_000L, leftToRight = false, bobDp = 8f),
    Fish(yFraction = 0.78f, lengthDp = 36f, crossMs = 28_000L, phaseMs = 2_000L, leftToRight = false, bobDp = 5f),
    Fish(yFraction = 0.84f, lengthDp = 26f, crossMs = 24_000L, phaseMs = 14_000L, leftToRight = true, bobDp = 4f)
)

// The same shape as feature:game's fish_silhouette_small vector (170x100
// viewport, facing right), so Home and the game board share one fish.
private val FishPath: Path = Path().apply {
    // Forked tail.
    moveTo(0f, 50f); lineTo(38f, 28f); lineTo(30f, 50f); lineTo(38f, 72f); close()
    // Body.
    moveTo(35f, 50f)
    cubicTo(35f, 32f, 60f, 20f, 95f, 20f)
    cubicTo(130f, 20f, 158f, 34f, 167f, 50f)
    cubicTo(158f, 66f, 130f, 80f, 95f, 80f)
    cubicTo(60f, 80f, 35f, 68f, 35f, 50f)
    close()
    // Dorsal, ventral and pectoral fins.
    moveTo(85f, 20f); lineTo(100f, 2f); lineTo(112f, 22f); close()
    moveTo(90f, 80f); lineTo(100f, 98f); lineTo(110f, 80f); close()
    moveTo(110f, 55f); lineTo(130f, 68f); lineTo(112f, 72f); close()
}
private const val FISH_VIEW_W = 170f
private const val FISH_VIEW_H = 100f

private fun DrawScope.drawFish(fish: Fish, elapsedMs: Long, color: Color) {
    val lengthPx = fish.lengthDp.dp.toPx()
    val heightPx = lengthPx * FISH_VIEW_H / FISH_VIEW_W
    val progress = ((elapsedMs + fish.phaseMs) % fish.crossMs) / fish.crossMs.toFloat()

    // Left edge of the fish, from fully off one side to fully off the other.
    val travel = size.width + lengthPx
    val x = if (fish.leftToRight) -lengthPx + travel * progress else size.width - travel * progress
    val y = size.height * fish.yFraction +
            sin(progress * 6f * PI.toFloat()) * fish.bobDp.dp.toPx() - heightPx / 2f

    val factor = lengthPx / FISH_VIEW_W
    // Scale the 170x100 viewport from the origin. A right-to-left fish is
    // mirrored (negative x scale spans -length..0), so shift it right by
    // one length to land in the same box.
    translate(left = if (fish.leftToRight) x else x + lengthPx, top = y) {
        scale(
            scaleX = if (fish.leftToRight) factor else -factor,
            scaleY = factor,
            pivot = Offset.Zero
        ) {
            drawPath(FishPath, color)
        }
    }
}

// ---------- Bubbles ----------

private data class AmbientBubble(
    val xFraction: Float,
    val sizeDp: Float,
    val riseDurationMs: Long,
    val delayMs: Long,
    val driftAmplitudeDp: Float
)

private fun generateAmbientBubbles(count: Int, lively: Boolean): List<AmbientBubble> = List(count) {
    AmbientBubble(
        xFraction = Random.nextFloat(),
        sizeDp = if (lively) Random.nextFloat() * 9f + 4f else Random.nextFloat() * 4f + 2f,
        riseDurationMs = Random.nextLong(5000L, 9000L),
        delayMs = Random.nextLong(0L, 9000L),
        driftAmplitudeDp = Random.nextFloat() * 6f + 2f
    )
}

private fun DrawScope.drawAmbientBubble(
    bubble: AmbientBubble,
    elapsedMs: Long,
    color: Color,
    maxAlpha: Float,
    ringed: Boolean
) {
    // The offset starts each bubble partway through its rise, so the water
    // is already full of bubbles on the first frame instead of filling in
    // over the first few seconds.
    val progress = ((elapsedMs + bubble.delayMs) % bubble.riseDurationMs) / bubble.riseDurationMs.toFloat()
    val sizePx = bubble.sizeDp.dp.toPx()
    val driftPx = bubble.driftAmplitudeDp.dp.toPx()

    // Starts one bubble-size below the bottom edge and ends one above the
    // top, so it never pops in or out while visible.
    val y = size.height + sizePx - progress * (size.height + 2f * sizePx)
    val sway = sin(progress * 2f * PI.toFloat()) * driftPx
    val x = bubble.xFraction * size.width + sway
    val center = Offset(x, y)
    val radius = sizePx / 2f

    val alpha = when {
        progress < 0.15f -> progress / 0.15f
        progress > 0.85f -> ((1f - progress) / 0.15f).coerceIn(0f, 1f)
        else -> 1f
    } * maxAlpha

    if (ringed) {
        // A real-looking bubble: faint fill, defined rim, small glint.
        drawCircle(color = color.copy(alpha = alpha * 0.25f), radius = radius, center = center)
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = radius,
            center = center,
            style = Stroke(width = (radius * 0.18f).coerceAtLeast(1f))
        )
        drawCircle(
            color = Color.White.copy(alpha = alpha * 1.4f),
            radius = radius * 0.22f,
            center = Offset(center.x - radius * 0.35f, center.y - radius * 0.35f)
        )
    } else {
        drawCircle(color = color.copy(alpha = alpha), radius = radius, center = center)
    }
}
