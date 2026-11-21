package com.genoma.mines.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.genoma.mines.core.economy.WalletTransaction
import com.genoma.mines.core.theme.MinesPreviews
import com.genoma.mines.core.theme.MinesTheme
import com.genoma.mines.core.ui.components.ScreenSurface
import com.genoma.mines.core.ui.components.ScreenTopBar
import com.genoma.mines.core.economy.WalletTransactionType
import java.text.DateFormat
import java.util.Date

/**
 * Wallet transaction history (UX carry-over): recent Coins, Gems and Lives
 * changes for the current session, newest first. Signed-in players see the
 * Firestore ledger; guests see the on-device history.
 */
@Composable
fun WalletHistoryScreen(
    transactions: List<WalletTransaction>,
    onBack: () -> Unit
) {
    ScreenSurface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            ScreenTopBar(title = "Wallet history", onBack = onBack, horizontalInset = false)

            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No wallet activity yet. Win games, convert Coins or buy items to see them here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    items(transactions, key = { it.id }) { transaction ->
                        WalletHistoryRow(transaction)
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletHistoryRow(transaction: WalletTransaction) {
    val changes = listOfNotNull(
        delta(transaction.coinsDelta, "Coin", "Coins"),
        delta(transaction.gemsDelta, "Gem", "Gems"),
        delta(transaction.livesDelta, "Life", "Lives")
    ).ifEmpty { listOf("No change") }
    val time = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        .format(Date(transaction.createdAtMillis))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = "${transaction.type.label}, ${changes.joinToString()}, $time"
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.type.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = time,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                changes.forEach { change ->
                    Text(
                        text = change,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (change.startsWith("-")) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }
            }
        }
    }
}

private fun delta(value: Int, singular: String, plural: String): String? {
    if (value == 0) return null
    val sign = if (value > 0) "+" else "-"
    val magnitude = kotlin.math.abs(value)
    return "$sign$magnitude ${if (magnitude == 1) singular else plural}"
}

@MinesPreviews
@Composable
private fun WalletHistoryScreenPreview() {
    MinesTheme {
        WalletHistoryScreen(
            transactions = listOf(
                WalletTransaction("a", WalletTransactionType.GAME_WIN, 50, 0, 0, 1_763_690_000_000),
                WalletTransaction("b", WalletTransactionType.GAME_LOSS, -20, 0, -1, 1_763_680_000_000),
                WalletTransaction("c", WalletTransactionType.COINS_TO_GEMS, -500, 1, 0, 1_763_670_000_000)
            ),
            onBack = {}
        )
    }
}
