package com.genoma.mines.life.ui
import com.genoma.mines.core.theme.HeartRed
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.genoma.mines.core.theme.MinesTheme
import com.genoma.mines.life.domain.LifeRules
import com.genoma.mines.life.domain.LifeStatus

/**
 * Shows hearts, the regen countdown, and the diamond refill offer. Opened by
 * tapping the home-screen hearts, or automatically when a game can't start.
 */
@Composable
fun HeartsDialog(
    status: LifeStatus,
    diamondBalance: Int,
    isRefilling: Boolean,
    onRefill: () -> Unit,
    onDismiss: () -> Unit,
    // Where to send a player who can't afford a refill (the Store, which
    // has the coin-to-diamond redeem). Null hides the option.
    onGetDiamonds: (() -> Unit)? = null,
    refillCost: Int = LifeRules.refillCost(status.hearts)
) {
    val missingHearts = (status.maxHearts - status.hearts).coerceAtLeast(0)
    val canAfford = diamondBalance >= refillCost
    val nextHeartAt = status.nextHeartAtMillis

    AlertDialog(
        onDismissRequest = { if (!isRefilling) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = HeartRed
            )
        },
        title = {
            Text(
                text = when {
                    !status.hasHearts -> "Out of hearts"
                    status.isFull -> "Hearts full"
                    else -> "Hearts"
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HeartRow(
                    hearts = status.hearts,
                    maxHearts = status.maxHearts,
                    heartSize = 28.dp
                )

                Text(
                    text = if (nextHeartAt == null) {
                        "You're ready to play. Losing or quitting a game costs 1 heart."
                    } else {
                        "Next free heart in ${formatCountdown(nextHeartAt)}.\n" +
                                "Losing or quitting a game costs 1 heart."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )

                if (!status.isFull) {
                    Text(
                        // "Diamonds" everywhere: this used to say "gems".
                        text = "You have $diamondBalance diamonds. " +
                            "${LifeRules.DIAMONDS_PER_HEART} diamonds per heart.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (canAfford) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
        },
        confirmButton = {
            if (!status.isFull && !canAfford && onGetDiamonds != null) {
                // Can't afford it: offer the way to get diamonds instead of a
                // dead, disabled refill button.
                Button(onClick = onGetDiamonds) {
                    Icon(
                        imageVector = Icons.Filled.Diamond,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Get diamonds")
                }
            } else if (!status.isFull) {
                Button(
                    onClick = onRefill,
                    enabled = canAfford && !isRefilling,
                    colors = ButtonDefaults.buttonColors()
                ) {
                    if (isRefilling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Diamond,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (canAfford) {
                                if (missingHearts == 1) "Refill 1 heart for $refillCost"
                                else "Refill $missingHearts hearts for $refillCost"
                            } else {
                                "Need $refillCost diamonds"
                            }
                        )
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isRefilling) {
                Text(if (status.isFull) "OK" else "Close")
            }
        }
    )
}

@Preview
@Composable
private fun HeartsDialogPreview() {
    MinesTheme {
        HeartsDialog(
            status = LifeStatus(
                hearts = 0,
                maxHearts = 5,
                nextHeartAtMillis = System.currentTimeMillis() + 15 * 60 * 1000
            ),
            diamondBalance = 40,
            isRefilling = false,
            onRefill = {},
            onDismiss = {}
        )
    }
}
