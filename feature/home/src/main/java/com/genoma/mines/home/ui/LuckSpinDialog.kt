package com.genoma.mines.home.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.genoma.mines.core.economy.LuckPrize
import com.genoma.mines.core.economy.LuckPrizeType
import com.genoma.mines.core.ui.components.rememberReduceMotion
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Result of a completed spin. [spinNonce] changes on every spin to restart the animation. */
data class LuckSpinResultUi(
    val spinNonce: Int,
    val prizes: List<LuckPrize>,
    val prizeIndex: Int,
    val message: String
)

data class LuckSpinUiState(
    val prizes: List<LuckPrize>,
    val statusText: String,
    val canSpin: Boolean,
    val isBusy: Boolean,
    val result: LuckSpinResultUi? = null
)

/**
 * Luck Spin (Stage 3F): watch a rewarded ad, then the wheel lands on the
 * one prize that was selected and credited. The wheel and the credited
 * prize always come from the same [LuckSpinResultUi.prizeIndex].
 *
 * All outcomes, including "No reward", are listed with their chances.
 * With "Remove animations" on, the wheel jumps straight to the result.
 */
@Composable
fun LuckSpinDialog(
    state: LuckSpinUiState,
    onSpin: () -> Unit,
    onDismiss: () -> Unit
) {
    val reduceMotion = rememberReduceMotion()
    val rotation = remember { Animatable(0f) }
    var revealedNonce by remember { mutableStateOf<Int?>(null) }

    val result = state.result
    val wheelPrizes = result?.prizes ?: state.prizes
    val spinning = result != null && revealedNonce != result.spinNonce

    LaunchedEffect(result?.spinNonce) {
        val outcome = result ?: return@LaunchedEffect
        val target = targetRotation(rotation.value, outcome.prizeIndex, outcome.prizes.size)

        if (reduceMotion) {
            rotation.snapTo(target)
        } else {
            rotation.animateTo(
                targetValue = target,
                animationSpec = tween(durationMillis = 3_200, easing = FastOutSlowInEasing)
            )
        }
        revealedNonce = outcome.spinNonce
    }

    AlertDialog(
        onDismissRequest = { if (!spinning && !state.isBusy) onDismiss() },
        title = { Text("Luck Spin", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(contentAlignment = Alignment.TopCenter) {
                    LuckWheel(
                        prizes = wheelPrizes,
                        rotationDegrees = rotation.value,
                        modifier = Modifier
                            .widthIn(max = 240.dp)
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .padding(top = 10.dp)
                    )
                    WheelPointer(modifier = Modifier.size(width = 22.dp, height = 18.dp))
                }

                if (result != null && revealedNonce == result.spinNonce) {
                    Text(
                        text = result.message,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                } else {
                    Text(
                        text = if (spinning) "Spinning…" else state.statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                PrizeChances(prizes = wheelPrizes)
            }
        },
        confirmButton = {
            Button(
                onClick = onSpin,
                enabled = state.canSpin && !state.isBusy && !spinning
            ) {
                if (state.isBusy && !spinning) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Watch ad & spin")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !spinning && !state.isBusy) {
                Text("Close")
            }
        }
    )
}

/** Rotation that brings the centre of [index] under the pointer after a few full turns. */
internal fun targetRotation(current: Float, index: Int, slots: Int): Float {
    if (slots <= 0) return current
    val sweep = 360f / slots
    val slotCentre = index * sweep + sweep / 2f
    val base = current - (current % 360f)
    return base + 5 * 360f + (360f - slotCentre)
}

private val CoinGold = Color(0xFFF2C94C)
private val CoinGoldDark = Color(0xFFE0B23A)
private val GemBlue = Color(0xFF56CCF2)
private val GemBlueDark = Color(0xFF3BAFD6)
private val NoneGrey = Color(0xFFB0BEC5)
private val NoneGreyDark = Color(0xFF9AA9B1)

private fun slotColor(prize: LuckPrize, index: Int): Color {
    val alt = index % 2 == 1
    return when (prize.type) {
        LuckPrizeType.COINS -> if (alt) CoinGoldDark else CoinGold
        LuckPrizeType.GEMS -> if (alt) GemBlueDark else GemBlue
        LuckPrizeType.NONE -> if (alt) NoneGreyDark else NoneGrey
    }
}

private fun LuckPrize.shortLabel(): String = when (type) {
    LuckPrizeType.COINS -> "+$amount"
    LuckPrizeType.GEMS -> "+$amount G"
    LuckPrizeType.NONE -> "—"
}

private fun LuckPrize.longLabel(): String = when (type) {
    LuckPrizeType.COINS -> "$amount Coins"
    LuckPrizeType.GEMS -> if (amount == 1) "1 Gem" else "$amount Gems"
    LuckPrizeType.NONE -> "No reward"
}

@Composable
private fun LuckWheel(
    prizes: List<LuckPrize>,
    rotationDegrees: Float,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val outline = MaterialTheme.colorScheme.surface
    val labelColor = Color(0xFF1B2A33)
    val description = "Luck Spin wheel with ${prizes.size} slots: " +
            prizes.joinToString { it.longLabel() }

    Canvas(
        modifier = modifier
            .semantics { contentDescription = description }
            .graphicsLayer { rotationZ = rotationDegrees }
    ) {
        if (prizes.isEmpty()) return@Canvas

        val sweep = 360f / prizes.size
        val diameter = size.minDimension
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)
        val centre = Offset(size.width / 2f, size.height / 2f)
        val radius = diameter / 2f

        prizes.forEachIndexed { index, prize ->
            val start = -90f + index * sweep
            drawArc(
                color = slotColor(prize, index),
                startAngle = start,
                sweepAngle = sweep,
                useCenter = true,
                topLeft = topLeft,
                size = arcSize
            )
            drawArc(
                color = outline,
                startAngle = start,
                sweepAngle = sweep,
                useCenter = true,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = 2.dp.toPx())
            )

            val mid = Math.toRadians((start + sweep / 2f).toDouble())
            val labelCentre = Offset(
                x = centre.x + (radius * 0.64f * cos(mid)).toFloat(),
                y = centre.y + (radius * 0.64f * sin(mid)).toFloat()
            )
            val layout = textMeasurer.measure(
                text = prize.shortLabel(),
                style = TextStyle(color = labelColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            )
            // Labels run outward along their slot.
            rotate(degrees = start + sweep / 2f + 90f, pivot = labelCentre) {
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(
                        labelCentre.x - layout.size.width / 2f,
                        labelCentre.y - layout.size.height / 2f
                    )
                )
            }
        }

        drawCircle(color = outline, radius = radius * 0.12f, center = centre)
    }
}

@Composable
private fun WheelPointer(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width / 2f, size.height)
            close()
        }
        drawPath(path, color)
    }
}

@Composable
private fun PrizeChances(prizes: List<LuckPrize>) {
    if (prizes.isEmpty()) return

    // Merge slots that pay the same prize, so the list shows real chances.
    val merged = prizes
        .groupBy { it.type to it.amount }
        .map { (_, slots) -> slots.first() to slots.sumOf { it.probabilityIn(prizes) } }
        .sortedByDescending { it.second }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "CHANCES",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        merged.forEach { (prize, probability) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(slotColor(prize, 0))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = prize.longLabel(),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatPercent(probability),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Rewards are added only after the ad is watched to the end.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatPercent(probability: Double): String {
    val percent = probability * 100.0
    return if (percent >= 10.0 || percent == percent.roundToInt().toDouble()) {
        "${percent.roundToInt()}%"
    } else {
        "${(percent * 10).roundToInt() / 10.0}%"
    }
}
