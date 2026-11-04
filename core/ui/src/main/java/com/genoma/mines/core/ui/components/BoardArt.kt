package com.genoma.mines.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * Hand-drawn board art for flags and mines, matching the Home hero
 * illustration (red pennant on a dark pole, dark round bomb). Drawn on a
 * Canvas so it scales cleanly with the cell instead of using flat stock
 * icons. All sizes are fractions of the canvas, so any square works.
 */

// Fixed art colors: these are illustrations, so they intentionally don't
// follow the Material color scheme (a red flag stays red in every theme).
private val FlagRed = Color(0xFFE5484D)
private val FlagRedShade = Color(0xFFB4232A)
private val FlagRedHighlight = Color(0xFFFF8A8E)
private val PoleDark = Color(0xFF2B3134)
private val PoleLight = Color(0xFFE3E9E8)
private val MineBody = Color(0xFF262B2F)
private val MineBodyLight = Color(0xFF4A535A)
private val MineRim = Color(0x59FFFFFF)
// Spikes on dark cells: dark spikes vanish there, so they go steel-gray.
private val MineSpikeOnDark = Color(0xFF9AA6AD)
private val BurstOuter = Color(0xFFFF9F43)
private val BurstInner = Color(0xFFFFE08A)

/**
 * A planted flag: pole on a small base, with a waving red pennant.
 * [onDarkCell] lightens the pole and base so they stay visible on dark
 * cells.
 */
@Composable
fun BoardFlag(
    modifier: Modifier = Modifier,
    onDarkCell: Boolean = false,
    alpha: Float = 1f
) {
    Canvas(modifier = modifier) {
        drawFlag(onDarkCell, alpha)
    }
}

private fun DrawScope.drawFlag(onDarkCell: Boolean, alpha: Float) {
    val w = size.width
    val h = size.height
    val poleColor = (if (onDarkCell) PoleLight else PoleDark).copy(alpha = alpha)

    val poleX = w * 0.34f
    val poleTop = h * 0.10f
    val poleBottom = h * 0.80f
    val poleWidth = w * 0.09f

    // Base: a short rounded slab the pole stands on.
    drawRoundRect(
        color = poleColor,
        topLeft = Offset(w * 0.18f, poleBottom - h * 0.02f),
        size = Size(w * 0.46f, h * 0.10f),
        cornerRadius = CornerRadius(h * 0.05f)
    )

    // Pole.
    drawLine(
        color = poleColor,
        start = Offset(poleX, poleTop),
        end = Offset(poleX, poleBottom),
        strokeWidth = poleWidth,
        cap = StrokeCap.Round
    )

    // Pennant: a triangle from the pole whose edges bow slightly, so it
    // reads as cloth in the wind rather than a flat wedge.
    val attachTop = Offset(poleX + poleWidth * 0.3f, poleTop + h * 0.02f)
    val attachBottom = Offset(poleX + poleWidth * 0.3f, poleTop + h * 0.46f)
    val tip = Offset(w * 0.90f, poleTop + h * 0.24f)

    val pennant = Path().apply {
        moveTo(attachTop.x, attachTop.y)
        quadraticTo(w * 0.62f, poleTop - h * 0.02f, tip.x, tip.y)
        quadraticTo(w * 0.60f, poleTop + h * 0.40f, attachBottom.x, attachBottom.y)
        close()
    }
    drawPath(
        path = pennant,
        brush = Brush.verticalGradient(
            colors = listOf(FlagRed.copy(alpha = alpha), FlagRedShade.copy(alpha = alpha)),
            startY = attachTop.y,
            endY = attachBottom.y
        )
    )

    // A thin highlight along the top edge for a bit of depth.
    val highlight = Path().apply {
        moveTo(attachTop.x, attachTop.y + h * 0.02f)
        quadraticTo(w * 0.60f, poleTop + h * 0.02f, w * 0.78f, poleTop + h * 0.17f)
    }
    drawPath(
        path = highlight,
        color = FlagRedHighlight.copy(alpha = 0.7f * alpha),
        style = Stroke(width = w * 0.035f, cap = StrokeCap.Round)
    )
}

/**
 * A classic sea mine: dark round body, eight spikes, a glossy highlight.
 * A faint light rim keeps it readable on dark cells.
 */
@Composable
fun BoardMine(
    modifier: Modifier = Modifier,
    onDarkCell: Boolean = false
) {
    Canvas(modifier = modifier) {
        drawMine(
            body = MineBody,
            bodyLight = MineBodyLight,
            rim = MineRim,
            gloss = true,
            spikeColor = if (onDarkCell) MineSpikeOnDark else MineBody
        )
    }
}

/**
 * The exploded mine: an orange-to-yellow starburst behind the mine, so
 * the losing cell stands out at a glance.
 */
@Composable
fun BoardExplodedMine(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawBurst()
        drawMine(
            body = MineBody,
            bodyLight = MineBodyLight,
            rim = MineRim,
            gloss = true,
            scale = 0.82f
        )
    }
}

private fun DrawScope.drawBurst() {
    val center = Offset(size.width / 2f, size.height / 2f)
    val outer = size.minDimension * 0.50f
    val inner = size.minDimension * 0.30f
    val points = 10

    val burst = Path().apply {
        for (i in 0 until points * 2) {
            val radius = if (i % 2 == 0) outer else inner
            // Offset half a step so the star doesn't line up with the spikes.
            val angle = (i * PI / points + PI / (points * 2)).toFloat()
            val point = Offset(center.x + radius * cos(angle), center.y + radius * sin(angle))
            if (i == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
        }
        close()
    }
    drawPath(
        path = burst,
        brush = Brush.radialGradient(
            colors = listOf(BurstInner, BurstOuter),
            center = center,
            radius = outer
        )
    )
}

private fun DrawScope.drawMine(
    body: Color,
    bodyLight: Color,
    rim: Color?,
    gloss: Boolean,
    scale: Float = 1f,
    spikeColor: Color = body
) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = size.minDimension * 0.27f * scale
    val spikeLength = size.minDimension * 0.17f * scale
    val spikeWidth = size.minDimension * 0.09f * scale

    // Eight spikes: the four axis-aligned ones a touch longer, like the
    // classic mine glyph.
    for (i in 0 until 8) {
        val angle = (i * PI / 4).toFloat()
        val length = if (i % 2 == 0) spikeLength else spikeLength * 0.75f
        val start = Offset(center.x + radius * 0.6f * cos(angle), center.y + radius * 0.6f * sin(angle))
        val end = Offset(
            center.x + (radius + length) * cos(angle),
            center.y + (radius + length) * sin(angle)
        )
        drawLine(color = spikeColor, start = start, end = end, strokeWidth = spikeWidth, cap = StrokeCap.Round)
    }

    // Body with a soft top-left light.
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(bodyLight, body),
            center = Offset(center.x - radius * 0.35f, center.y - radius * 0.35f),
            radius = radius * 1.4f
        ),
        radius = radius,
        center = center
    )

    if (rim != null) {
        drawCircle(
            color = rim,
            radius = radius,
            center = center,
            style = Stroke(width = size.minDimension * 0.025f)
        )
    }

    if (gloss) {
        drawCircle(
            color = Color.White.copy(alpha = 0.85f),
            radius = radius * 0.24f,
            center = Offset(center.x - radius * 0.38f, center.y - radius * 0.38f)
        )
    }
}

/**
 * Single-color mine silhouette (same shape as [BoardMine]) for tinted
 * contexts such as the result banner badge.
 */
@Composable
fun MineIcon(
    modifier: Modifier = Modifier,
    color: Color
) {
    Canvas(modifier = modifier) {
        drawMine(body = color, bodyLight = color, rim = null, gloss = false)
    }
}

/**
 * A wrong flag after a loss: the flag faded back, with a bold X over it.
 * The X has a light outline so it reads on any cell color.
 */
@Composable
fun BoardWrongFlag(
    modifier: Modifier = Modifier,
    onDarkCell: Boolean = false,
    crossColor: Color
) {
    Canvas(modifier = modifier) {
        drawFlag(onDarkCell, alpha = 0.45f)

        val inset = size.minDimension * 0.14f
        val outlineWidth = size.minDimension * 0.17f
        val crossWidth = size.minDimension * 0.10f
        val a1 = Offset(inset, inset)
        val a2 = Offset(size.width - inset, size.height - inset)
        val b1 = Offset(size.width - inset, inset)
        val b2 = Offset(inset, size.height - inset)
        val outline = if (onDarkCell) Color.Black.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.9f)

        drawLine(outline, a1, a2, outlineWidth, StrokeCap.Round)
        drawLine(outline, b1, b2, outlineWidth, StrokeCap.Round)
        drawLine(crossColor, a1, a2, crossWidth, StrokeCap.Round)
        drawLine(crossColor, b1, b2, crossWidth, StrokeCap.Round)
    }
}
