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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.genoma.mines.core.theme.CountOne
import com.genoma.mines.core.theme.CountThree
import com.genoma.mines.core.theme.CountTwo
import com.genoma.mines.core.theme.TierGold
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * How lively the Classic Teal backdrop is. Each screen in the play flow
 * gets the same visual language (teal wash, light blobs, board grid,
 * floating tiles) tuned to its job, so the theme feels consistent without
 * every screen looking identical.
 */
enum class ClassicAmbientStyle {
    /** Home: full showcase — grid behind the hero, numbered tiles. */
    SHOWCASE,

    /**
     * Game: calm. Grid only around the edges (clear behind the board),
     * few small blank tiles — no numbers, so nothing can be mistaken for
     * a real board number.
     */
    FOCUS,

    /** Celebration: lively — faster, more numbered tiles plus gold sparkles. */
    CELEBRATE
}

private data class StyleSpec(
    val tileCount: Int,
    val tileSizeDp: ClosedFloatingPointRange<Float>,
    val riseMs: LongRange,
    val numberChance: Float,
    val tileAlphaScale: Float,
    val gridCenterY: Float,
    val gridAlphaScale: Float,
    // Grid visible near the edges instead of the center.
    val gridAtEdges: Boolean,
    val blobSpeedScale: Float,
    val sparkleCount: Int
)

private fun ClassicAmbientStyle.spec(): StyleSpec = when (this) {
    ClassicAmbientStyle.SHOWCASE -> StyleSpec(
        tileCount = 18,
        tileSizeDp = 22f..44f,
        riseMs = 16_000L..26_000L,
        numberChance = 0.45f,
        tileAlphaScale = 1f,
        gridCenterY = 0.42f,
        gridAlphaScale = 1f,
        gridAtEdges = false,
        blobSpeedScale = 1f,
        sparkleCount = 0
    )

    ClassicAmbientStyle.FOCUS -> StyleSpec(
        tileCount = 6,
        tileSizeDp = 14f..24f,
        riseMs = 26_000L..38_000L,
        numberChance = 0f,
        tileAlphaScale = 0.7f,
        gridCenterY = 0.5f,
        gridAlphaScale = 0.4f,
        gridAtEdges = true,
        blobSpeedScale = 0.6f,
        sparkleCount = 0
    )

    ClassicAmbientStyle.CELEBRATE -> StyleSpec(
        tileCount = 12,
        tileSizeDp = 20f..36f,
        riseMs = 9_000L..15_000L,
        numberChance = 0.4f,
        tileAlphaScale = 0.65f,
        gridCenterY = 0.38f,
        gridAlphaScale = 0.8f,
        gridAtEdges = false,
        blobSpeedScale = 1.6f,
        sparkleCount = 16
    )
}

/**
 * Ambient background for the free Classic Teal theme: a soft teal wash,
 * slow-drifting light blobs, a faint board grid, and Minesweeper tiles
 * floating upward while gently turning; [style] tunes it per screen.
 *
 * Intentionally distinct from the paid Deep Ocean backdrop (gradient water
 * + bubbles), so the paid theme still reads as an upgrade. Everything is
 * kept faint: it fills empty space without competing with content.
 *
 * Colors come from the current [MaterialTheme], so it works in light and
 * dark mode.
 */
@Composable
fun ClassicAmbientBackdrop(
    modifier: Modifier = Modifier,
    style: ClassicAmbientStyle = ClassicAmbientStyle.SHOWCASE
) {
    val spec = remember(style) { style.spec() }
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f

    // Light mode needs noticeably stronger alphas than dark to read at all
    // against a near-white background.
    val washBottom = lerp(scheme.background, scheme.primaryContainer, if (isDark) 0.25f else 0.55f)
    val blobColor = scheme.primary.copy(alpha = if (isDark) 0.16f else 0.14f)
    val gridColor = scheme.primary.copy(alpha = (if (isDark) 0.07f else 0.08f) * spec.gridAlphaScale)
    val tileFill = scheme.primary.copy(alpha = (if (isDark) 0.10f else 0.12f) * spec.tileAlphaScale)
    val tileEdge = scheme.primary.copy(alpha = (if (isDark) 0.22f else 0.28f) * spec.tileAlphaScale)
    val numberAlpha = (if (isDark) 0.40f else 0.50f) * spec.tileAlphaScale
    // Gold reads on dark; on light it needs to be deeper and stronger.
    val sparkleColor = if (isDark) TierGold else lerp(TierGold, scheme.primary, 0.25f)

    val textMeasurer = rememberTextMeasurer()
    val tiles = remember(spec) { generateFloatingTiles(spec) }
    val sparkles = remember(spec) { generateSparkles(spec.sparkleCount) }

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
        val blobSeconds = seconds * spec.blobSpeedScale

        // 1. Wash: theme background fading into a hint of teal at the bottom.
        drawRect(
            brush = Brush.verticalGradient(listOf(scheme.background, washBottom)),
            size = size
        )

        // 2. Light blobs drifting on slow, out-of-phase loops.
        drawBlob(blobColor, blobSeconds, anchorX = 0.15f, anchorY = 0.18f, radiusFraction = 0.75f, speed = 0.05f, phase = 0f)
        drawBlob(blobColor, blobSeconds, anchorX = 0.90f, anchorY = 0.50f, radiusFraction = 0.65f, speed = 0.04f, phase = 2.1f)
        drawBlob(blobColor, blobSeconds, anchorX = 0.30f, anchorY = 0.92f, radiusFraction = 0.70f, speed = 0.035f, phase = 4.2f)

        // 3. A faint board grid, drifting diagonally.
        drawBoardGrid(gridColor, seconds, spec.gridCenterY, spec.gridAtEdges)

        // 4. Floating tiles.
        tiles.forEach { tile ->
            drawFloatingTile(tile, elapsedMs, tileFill, tileEdge, numberAlpha, textMeasurer)
        }

        // 5. Twinkling sparkles (celebration only).
        sparkles.forEach { sparkle ->
            drawSparkle(sparkle, seconds, sparkleColor)
        }
    }
}

private fun DrawScope.drawBlob(
    color: Color,
    seconds: Float,
    anchorX: Float,
    anchorY: Float,
    radiusFraction: Float,
    speed: Float,
    phase: Float
) {
    val angle = seconds * speed * 2f * PI.toFloat() + phase
    val center = Offset(
        x = size.width * (anchorX + 0.08f * sin(angle)),
        y = size.height * (anchorY + 0.05f * cos(angle * 0.8f))
    )
    val radius = size.width * radiusFraction

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color, Color.Transparent),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}

private fun DrawScope.drawBoardGrid(
    color: Color,
    seconds: Float,
    centerY: Float,
    atEdges: Boolean
) {
    val cellPx = 44.dp.toPx()
    val gapPx = 6.dp.toPx()
    val pitch = cellPx + gapPx
    val corner = CornerRadius(cellPx * 0.2f)

    // Slow diagonal pan (~one cell every 12s), wrapped to one pitch so the
    // pattern loops seamlessly.
    val shift = (seconds * pitch / 12f) % pitch

    val center = Offset(size.width * 0.5f, size.height * centerY)
    val fadeRadius = size.width * 0.85f

    var y = -pitch + shift
    while (y < size.height) {
        var x = -pitch + shift
        while (x < size.width) {
            val cellCenter = Offset(x + cellPx / 2f, y + cellPx / 2f)
            val distance = (cellCenter - center).getDistance() / fadeRadius

            // Centered: strongest at the focal point, gone by the edges.
            // At edges: clear around the center (the game board), rising
            // toward the screen edges.
            val falloff = if (atEdges) {
                ((distance - 0.6f) / 0.4f).coerceIn(0f, 1f)
            } else {
                (1f - distance).coerceIn(0f, 1f)
            }

            if (falloff > 0f) {
                drawRoundRect(
                    color = color.copy(alpha = color.alpha * falloff * falloff),
                    topLeft = Offset(x, y),
                    size = Size(cellPx, cellPx),
                    cornerRadius = corner
                )
            }
            x += pitch
        }
        y += pitch
    }
}

private data class FloatingTile(
    val xFraction: Float,
    val sizeDp: Float,
    val riseDurationMs: Long,
    // Starting point in the rise cycle, so the screen is already populated
    // on the first frame instead of tiles trickling in from the bottom.
    val phaseMs: Long,
    val baseRotation: Float,
    val spinDegrees: Float,
    val swayDp: Float,
    // 0 = blank tile, 1..3 = shows that number.
    val number: Int
)

private fun generateFloatingTiles(spec: StyleSpec): List<FloatingTile> = List(spec.tileCount) { index ->
    val riseDurationMs = Random.nextLong(spec.riseMs.first, spec.riseMs.last)
    val sizeRange = spec.tileSizeDp
    FloatingTile(
        // Spread evenly across the width with jitter, so tiles don't clump.
        xFraction = ((index + Random.nextFloat()) / spec.tileCount).coerceIn(0.03f, 0.97f),
        sizeDp = sizeRange.start + Random.nextFloat() * (sizeRange.endInclusive - sizeRange.start),
        riseDurationMs = riseDurationMs,
        phaseMs = Random.nextLong(0L, riseDurationMs),
        baseRotation = Random.nextFloat() * 40f - 20f,
        spinDegrees = Random.nextFloat() * 50f - 25f,
        swayDp = Random.nextFloat() * 10f + 4f,
        number = if (Random.nextFloat() < spec.numberChance) Random.nextInt(1, 4) else 0
    )
}

private fun DrawScope.drawFloatingTile(
    tile: FloatingTile,
    elapsedMs: Long,
    fill: Color,
    edge: Color,
    numberAlpha: Float,
    textMeasurer: TextMeasurer
) {
    val progress = ((elapsedMs + tile.phaseMs) % tile.riseDurationMs) / tile.riseDurationMs.toFloat()
    val sizePx = tile.sizeDp.dp.toPx()

    // Starts fully below the bottom edge and ends fully above the top.
    val y = size.height + sizePx - progress * (size.height + 2f * sizePx)
    val x = tile.xFraction * size.width + sin(progress * 2f * PI.toFloat()) * tile.swayDp.dp.toPx()

    val fade = when {
        progress < 0.12f -> progress / 0.12f
        progress > 0.88f -> (1f - progress) / 0.12f
        else -> 1f
    }.coerceIn(0f, 1f)

    val topLeft = Offset(x - sizePx / 2f, y - sizePx / 2f)
    val corner = CornerRadius(sizePx * 0.22f)

    rotate(degrees = tile.baseRotation + tile.spinDegrees * progress, pivot = Offset(x, y)) {
        drawRoundRect(
            color = fill.copy(alpha = fill.alpha * fade),
            topLeft = topLeft,
            size = Size(sizePx, sizePx),
            cornerRadius = corner
        )
        drawRoundRect(
            color = edge.copy(alpha = edge.alpha * fade),
            topLeft = topLeft,
            size = Size(sizePx, sizePx),
            cornerRadius = corner,
            style = Stroke(width = 1.dp.toPx())
        )

        if (tile.number > 0) {
            val numberColor = when (tile.number) {
                1 -> CountOne
                2 -> CountTwo
                else -> CountThree
            }
            val layout = textMeasurer.measure(
                text = tile.number.toString(),
                style = TextStyle(
                    color = numberColor.copy(alpha = numberAlpha * fade),
                    // From px, not sp, so the number stays inside its tile
                    // regardless of the user's font scale.
                    fontSize = (sizePx * 0.55f).toSp(),
                    fontWeight = FontWeight.Bold
                )
            )
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    x - layout.size.width / 2f,
                    y - layout.size.height / 2f
                )
            )
        }
    }
}

private data class Sparkle(
    val xFraction: Float,
    val yFraction: Float,
    val sizeDp: Float,
    val periodSeconds: Float,
    val phase: Float
)

private fun generateSparkles(count: Int): List<Sparkle> = List(count) {
    Sparkle(
        xFraction = Random.nextFloat(),
        // Kept to the upper two-thirds, around the celebration hero.
        yFraction = Random.nextFloat() * 0.7f,
        sizeDp = Random.nextFloat() * 8f + 6f,
        periodSeconds = Random.nextFloat() * 1.6f + 1.4f,
        phase = Random.nextFloat() * 2f * PI.toFloat()
    )
}

/** A four-point twinkle that grows and fades on its own rhythm. */
private fun DrawScope.drawSparkle(sparkle: Sparkle, seconds: Float, color: Color) {
    val wave = sin(seconds / sparkle.periodSeconds * 2f * PI.toFloat() + sparkle.phase)
    // Visible for the upper half of the wave only, so sparkles blink in
    // and out rather than all glowing at once.
    val intensity = wave.coerceAtLeast(0f)
    if (intensity <= 0.01f) return

    val center = Offset(sparkle.xFraction * size.width, sparkle.yFraction * size.height)
    val arm = sparkle.sizeDp.dp.toPx() * (0.5f + 0.5f * intensity)
    val stroke = 2.dp.toPx()
    val sparkleColor = color.copy(alpha = 0.75f * intensity)

    drawLine(sparkleColor, Offset(center.x - arm, center.y), Offset(center.x + arm, center.y), stroke, StrokeCap.Round)
    drawLine(sparkleColor, Offset(center.x, center.y - arm), Offset(center.x, center.y + arm), stroke, StrokeCap.Round)
    drawCircle(sparkleColor, radius = stroke * 1.2f, center = center)
}
