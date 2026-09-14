package com.genoma.mines.life.ui
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.genoma.mines.core.theme.MinesTheme
import com.genoma.mines.life.domain.LifeCalculator
import com.genoma.mines.life.domain.LifeSnapshot
import com.genoma.mines.life.domain.LifeStatus
import kotlinx.coroutines.delay

internal val HeartRed = Color(0xFFE53950)

/**
 * Live hearts for [snapshot], re-evaluated every second so a regenerated
 * heart and the countdown to the next one appear without any new data.
 */
@Composable
fun rememberLifeStatus(snapshot: LifeSnapshot): LifeStatus {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1_000)
        }
    }

    return LifeCalculator.status(snapshot, nowMillis)
}

/** "mm:ss" (or "h:mm:ss") until [targetMillis]; never negative. */
fun formatCountdown(targetMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val totalSeconds = ((targetMillis - nowMillis).coerceAtLeast(0) + 999) / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

@Composable
fun HeartRow(
    hearts: Int,
    maxHearts: Int,
    modifier: Modifier = Modifier,
    heartSize: Dp = 16.dp
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(maxHearts) { index ->
            val filled = index < hearts

            Icon(
                imageVector = if (filled) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = null,
                tint = if (filled) HeartRed else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(heartSize)
            )
        }
    }
}

/** Compact hearts pill for the top of the home screen. */
@Composable
fun HeartsIndicator(
    status: LifeStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nextHeartAt = status.nextHeartAtMillis

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = buildString {
                    append("${status.hearts} of ${status.maxHearts} hearts")
                    if (nextHeartAt != null) append(", next heart in ${formatCountdown(nextHeartAt)}")
                }
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeartRow(hearts = status.hearts, maxHearts = status.maxHearts)

        if (nextHeartAt != null) {
            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = formatCountdown(nextHeartAt),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HeartsIndicatorPreview() {
    MinesTheme {
        HeartsIndicator(
            status = LifeStatus(
                hearts = 3,
                maxHearts = 5,
                nextHeartAtMillis = System.currentTimeMillis() + 42 * 60 * 1000
            ),
            onClick = {}
        )
    }
}
