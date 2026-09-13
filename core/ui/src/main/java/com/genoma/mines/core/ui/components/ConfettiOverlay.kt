package com.genoma.mines.core.ui.components
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** How long a single confetti piece takes to fall the length of the screen. */
private const val MIN_FALL_MS = 1600L
private const val MAX_FALL_MS = 2800L

/** Total window the overlay stays composed for — last piece's fall + delay. */
private const val CONFETTI_DURATION_MS = MAX_FALL_MS + 500L

private val CONFETTI_COLORS = listOf(
    Color(0xFFEF476F),
    Color(0xFFFFD166),
    Color(0xFF06D6A0),
    Color(0xFF118AB2),
    Color(0xFF8338EC),
    Color(0xFFFF9F1C)
)

private data class ConfettiPiece(
    val startXFraction: Float,
    val color: Color,
    val sizeDp: Float,
    val fallDurationMs: Long,
    val delayMs: Long,
    val swayAmplitudeDp: Float,
    val swayFrequency: Float,
    val rotationDegPerMs: Float,
    val isCircle: Boolean
)

private fun generateConfettiPieces(count: Int): List<ConfettiPiece> {
    return List(count) {
        ConfettiPiece(
            startXFraction = Random.nextFloat(),
            color = CONFETTI_COLORS[Random.nextInt(CONFETTI_COLORS.size)],
            sizeDp = Random.nextFloat() * 5f + 5f,
            fallDurationMs = Random.nextLong(MIN_FALL_MS, MAX_FALL_MS),
            delayMs = Random.nextLong(0, 450),
            swayAmplitudeDp = Random.nextFloat() * 22f + 8f,
            swayFrequency = Random.nextFloat() * 1.4f + 0.7f,
            rotationDegPerMs = (Random.nextFloat() * 0.5f + 0.15f) *
                    if (Random.nextBoolean()) 1f else -1f,
            isCircle = Random.nextBoolean()
        )
    }
}

private fun DrawScope.drawConfettiPiece(piece: ConfettiPiece, elapsedMs: Long) {
    val localElapsed = elapsedMs - piece.delayMs
    if (localElapsed < 0) return

    val progress = localElapsed / piece.fallDurationMs.toFloat()
    if (progress > 1f) return

    val pieceSizePx = piece.sizeDp.dp.toPx()
    val swayPx = piece.swayAmplitudeDp.dp.toPx()

    val y = -40f + progress * (size.height + 80f)
    val sway = sin(progress * piece.swayFrequency * 2f * PI.toFloat()) * swayPx
    val x = piece.startXFraction * size.width + sway
    val rotationDeg = localElapsed * piece.rotationDegPerMs

    // Fade out over the final 15% of the fall instead of popping off-screen.
    val alpha = if (progress > 0.85f) {
        ((1f - progress) / 0.15f).coerceIn(0f, 1f)
    } else {
        1f
    }

    rotate(degrees = rotationDeg, pivot = Offset(x, y)) {
        if (piece.isCircle) {
            drawCircle(
                color = piece.color.copy(alpha = alpha),
                radius = pieceSizePx / 2f,
                center = Offset(x, y)
            )
        } else {
            drawRect(
                color = piece.color.copy(alpha = alpha),
                topLeft = Offset(x - pieceSizePx / 2f, y - pieceSizePx / 4f),
                size = Size(pieceSizePx, pieceSizePx / 2f)
            )
        }
    }
}

/**
 * A one-shot confetti burst drawn as a full-screen overlay. Stateless by
 * design: it composes nothing and holds no state while [visible] is false,
 * so each time [visible] flips to true (i.e. each new win) it regenerates
 * a fresh burst and replays from the start.
 *
 * Doesn't intercept touches — it's a plain [Canvas] with no pointer input
 * handling, so taps still reach whatever is underneath it.
 */
@Composable
fun ConfettiOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
    pieceCount: Int = 90
) {
    if (!visible) return

    val pieces = remember(visible) { generateConfettiPieces(pieceCount) }
    var elapsedMs by remember(visible) { mutableStateOf(0L) }

    LaunchedEffect(visible) {
        val startNanos = withFrameNanos { it }
        while (true) {
            val nowNanos = withFrameNanos { it }
            elapsedMs = (nowNanos - startNanos) / 1_000_000L
            if (elapsedMs > CONFETTI_DURATION_MS) break
        }
    }

    Canvas(modifier = modifier) {
        pieces.forEach { piece ->
            drawConfettiPiece(piece, elapsedMs)
        }
    }
}