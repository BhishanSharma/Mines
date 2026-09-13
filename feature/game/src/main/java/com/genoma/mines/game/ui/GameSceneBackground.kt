package com.genoma.mines.game.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.genoma.mines.core.theme.MinesThemeVariant
import com.genoma.mines.game.R
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun GameSceneBackground(
    themeVariant: MinesThemeVariant,
    modifier: Modifier = Modifier
) {
    if (themeVariant != MinesThemeVariant.DEEP_OCEAN) return

    BoxWithConstraints(modifier = modifier) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        // 1. Water gradient — sits behind everything else as a fallback
        // base color so panel edges never reveal a hard seam.
        WaterGradient(modifier = Modifier.matchParentSize())

        // 2. Light rays — slow shimmer, never fully off.
        val infiniteTransition = rememberInfiniteTransition(label = "raysShimmer")
        val raysAlpha by infiniteTransition.animateFloat(
            initialValue = 0.55f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(3200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "raysAlpha"
        )
        Image(
            painter = painterResource(id = R.drawable.bg_ocean_rays),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = raysAlpha,
            modifier = Modifier.matchParentSize()
        )

        // 3. Shipwreck silhouette — static, sits on the rocks in the
        // upper-right of the composition (matches the source art).
        Image(
            painter = painterResource(id = R.drawable.bg_ocean_wreck),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )

        // 4. Sea floor — coral/seaweed/treasure chest band, static.
        Image(
            painter = painterResource(id = R.drawable.bg_ocean_floor),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )

        // 5. Small silhouette fish school, drifting left-to-right and
        // looping, at three different heights/speeds so they don't
        // move in lockstep.
        DriftingSilhouetteFish(
            screenWidth = screenWidth,
            y = screenHeight * 0.28f,
            durationMs = 9000,
            fishSize = 30.dp
        )
        DriftingSilhouetteFish(
            screenWidth = screenWidth,
            y = screenHeight * 0.33f,
            durationMs = 12500,
            fishSize = 22.dp,
            startDelayFractionOfDuration = 0.4f
        )
        DriftingSilhouetteFish(
            screenWidth = screenWidth,
            y = screenHeight * 0.45f,
            durationMs = 15000,
            fishSize = 18.dp,
            startDelayFractionOfDuration = 0.7f
        )

        // 6. Diver — gentle vertical bob, positioned upper-left over the
        // rocks like the reference mockup.
        val diverBob by infiniteTransition.animateFloat(
            initialValue = -5f,
            targetValue = 5f,
            animationSpec = infiniteRepeatable(
                animation = tween(2200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "diverBob"
        )
        Image(
            painter = painterResource(id = R.drawable.char_diver),
            contentDescription = null,
            modifier = Modifier
                .size(width = 150.dp, height = 100.dp)
                .offset(
                    x = screenWidth * 0.06f,
                    y = screenHeight * 0.33f + diverBob.dp
                )
        )

        // 7. Foreground fish — idle wander near the sea floor band.
        IdleWanderingFish(
            drawableRes = R.drawable.fish_clownfish,
            anchorX = screenWidth * 0.14f,
            anchorY = screenHeight * 0.86f,
            fishSize = 56.dp,
            wanderPeriodMs = 4200
        )
        IdleWanderingFish(
            drawableRes = R.drawable.fish_yellowtang,
            anchorX = screenWidth * 0.68f,
            anchorY = screenHeight * 0.9f,
            fishSize = 46.dp,
            wanderPeriodMs = 5100,
            phaseOffset = 0.5f
        )

        // 8. Bubbles — continuous rising stream, drawn last so they read
        // as being closest to the "camera".
        BubbleField(modifier = Modifier.matchParentSize())
    }
}

@Composable
private fun WaterGradient(modifier: Modifier = Modifier) {
    val topColor = Color(0xFF04141C)
    val bottomColor = Color(0xFF0D3049)
    Canvas(modifier = modifier) {
        drawRect(
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(topColor, bottomColor)
            ),
            size = size
        )
    }
}

/**
 * One instance of the small flat fish silhouette, drifting left-to-right
 * across [screenWidth] and looping back off the left edge once it clears
 * the right side.
 */
@Composable
private fun DriftingSilhouetteFish(
    screenWidth: Dp,
    y: Dp,
    durationMs: Int,
    fishSize: Dp,
    startDelayFractionOfDuration: Float = 0f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fishDrift")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = androidx.compose.animation.core.StartOffset(
                (durationMs * startDelayFractionOfDuration).toInt()
            )
        ),
        label = "fishProgress"
    )

    // Travels from just off-screen left to just off-screen right.
    val travel = screenWidth + fishSize * 2
    val x = travel * progress - fishSize

    Image(
        painter = painterResource(id = R.drawable.fish_silhouette_small),
        contentDescription = null,
        modifier = Modifier
            .size(fishSize)
            .offset(x = x, y = y)
    )
}

/**
 * A small foreground fish that idles near [anchorX]/[anchorY] with a gentle
 * looping figure-eight-ish wander instead of a straight path, so it reads
 * as "swimming in place" rather than commuting across the screen.
 */
@Composable
private fun IdleWanderingFish(
    drawableRes: Int,
    anchorX: Dp,
    anchorY: Dp,
    fishSize: Dp,
    wanderPeriodMs: Int,
    phaseOffset: Float = 0f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fishWander")
    val t by infiniteTransition.animateFloat(
        initialValue = phaseOffset,
        targetValue = phaseOffset + 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(wanderPeriodMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wanderT"
    )

    val angle = t * 2f * PI.toFloat()
    val dx = (sin(angle) * 14).dp
    val dy = (sin(angle * 2f) * 6).dp

    Image(
        painter = painterResource(id = drawableRes),
        contentDescription = null,
        modifier = Modifier
            .size(fishSize)
            .offset(x = anchorX + dx, y = anchorY + dy)
    )
}

private data class BubbleParticle(
    val xFraction: Float,
    val sizeDp: Float,
    val riseDurationMs: Long,
    val delayMs: Long,
    val driftAmplitudeDp: Float,
    val driftFrequency: Float
)

private fun generateBubbles(count: Int): List<BubbleParticle> = List(count) {
    BubbleParticle(
        xFraction = Random.nextFloat(),
        sizeDp = Random.nextFloat() * 6f + 3f,
        riseDurationMs = Random.nextLong(3200L, 6000L),
        delayMs = Random.nextLong(0L, 6000L),
        driftAmplitudeDp = Random.nextFloat() * 10f + 4f,
        driftFrequency = Random.nextFloat() * 1.2f + 0.6f
    )
}

private fun DrawScope.drawBubble(bubble: BubbleParticle, elapsedMs: Long) {
    // Looping: once a bubble's cycle (delay + rise) finishes, wrap it back
    // to the start instead of stopping, so the stream runs indefinitely.
    val cycleMs = bubble.delayMs + bubble.riseDurationMs
    val localElapsed = elapsedMs % cycleMs
    if (localElapsed < bubble.delayMs) return

    val progress = (localElapsed - bubble.delayMs) / bubble.riseDurationMs.toFloat()
    val bubbleSizePx = bubble.sizeDp.dp.toPx()
    val driftPx = bubble.driftAmplitudeDp.dp.toPx()

    val y = size.height + 20f - progress * (size.height + 60f)
    val sway = sin(progress * bubble.driftFrequency * 2f * PI.toFloat()) * driftPx
    val x = bubble.xFraction * size.width + sway

    val alpha = when {
        progress < 0.1f -> progress / 0.1f
        progress > 0.85f -> ((1f - progress) / 0.15f).coerceIn(0f, 1f)
        else -> 1f
    } * 0.5f

    drawCircle(
        color = Color(0xFFBEEAF5).copy(alpha = alpha),
        radius = bubbleSizePx / 2f,
        center = Offset(x, y)
    )
}

@Composable
private fun BubbleField(modifier: Modifier = Modifier, bubbleCount: Int = 26) {
    val bubbles = remember { generateBubbles(bubbleCount) }
    var elapsedMs by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        val startNanos = withFrameNanos { it }
        while (true) {
            val nowNanos = withFrameNanos { it }
            elapsedMs = (nowNanos - startNanos) / 1_000_000L
        }
    }

    Canvas(modifier = modifier) {
        bubbles.forEach { bubble ->
            drawBubble(bubble, elapsedMs)
        }
    }
}