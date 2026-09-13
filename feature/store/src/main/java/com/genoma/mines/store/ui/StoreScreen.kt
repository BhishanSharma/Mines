package com.genoma.mines.store.ui
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.genoma.mines.store.domain.AvatarStoreItem
import com.genoma.mines.store.domain.BoardThemeItem
import com.genoma.mines.store.domain.CellSkinItem
import com.genoma.mines.store.data.StoreCatalog
import com.genoma.mines.store.domain.StoreCategory
import com.genoma.mines.store.domain.StoreItem
import com.genoma.mines.core.theme.MinesTheme

private object StoreSpacing {
    val screenHorizontal = 16.dp
    val screenTop = 16.dp
    val screenBottom = 16.dp
    val barToContent = 20.dp
    val sectionGap = 20.dp
    val rowGap = 10.dp
}

@Composable
fun StoreScreen(
    items: List<StoreItem> = StoreCatalog.allItems,
    ownedItemIds: Set<String> = emptySet(),
    equippedBoardThemeId: String = "board_classic_teal",
    equippedCellSkinId: String? = null,
    coinBalance: Int = 0,
    diamondBalance: Int = 0,
    canRedeem: Boolean = false,
    redemptionsUsedToday: Int = 0,
    maxRedemptionsPerWindow: Int = 2,
    nextUnlockMillis: Long? = null,
    onRedeemClick: () -> Unit = {},
    onItemClick: (StoreItem) -> Unit = {},
    onBack: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = StoreSpacing.screenHorizontal)
                .padding(bottom = StoreSpacing.screenBottom)
        ) {

            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Store",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Diamond,
                        contentDescription = "Diamond balance",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$diamondBalance",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(StoreSpacing.barToContent))

            RedeemCard(
                coinBalance = coinBalance,
                canRedeem = canRedeem,
                redemptionsUsedToday = redemptionsUsedToday,
                maxRedemptionsPerWindow = maxRedemptionsPerWindow,
                nextUnlockMillis = nextUnlockMillis,
                onRedeemClick = onRedeemClick
            )

            Spacer(modifier = Modifier.height(StoreSpacing.sectionGap))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                StoreCategory.entries.forEach { category ->
                    val categoryItems = items.filter { it.category == category }

                    if (categoryItems.isNotEmpty()) {
                        Text(
                            text = category.displayName.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(StoreSpacing.rowGap))

                        Column(verticalArrangement = Arrangement.spacedBy(StoreSpacing.rowGap)) {
                            categoryItems.forEach { item ->
                                StoreItemRow(
                                    item = item,
                                    owned = item.price == 0 || ownedItemIds.contains(item.id),
                                    equipped = when (item) {
                                        is BoardThemeItem -> equippedBoardThemeId == item.id
                                        is CellSkinItem -> equippedCellSkinId == item.id
                                        is AvatarStoreItem -> false
                                    },
                                    onClick = { onItemClick(item) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(StoreSpacing.sectionGap))
                    }
                }
            }
        }
    }
}

@Composable
private fun RedeemCard(
    coinBalance: Int,
    canRedeem: Boolean,
    redemptionsUsedToday: Int,
    maxRedemptionsPerWindow: Int,
    nextUnlockMillis: Long?,
    onRedeemClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.MonetizationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Redeem coins for diamonds",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "500 coins = 1 diamond \u00B7 up to $maxRedemptionsPerWindow times a day",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$coinBalance coins",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = when {
                            canRedeem ->
                                "${maxRedemptionsPerWindow - redemptionsUsedToday} redemptions left today"
                            nextUnlockMillis != null ->
                                "Locked \u2014 more in ${formatRemaining(nextUnlockMillis)}"
                            else -> "Not enough coins yet"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    )
                }

                Button(
                    onClick = onRedeemClick,
                    enabled = canRedeem,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Redeem")
                }
            }
        }
    }
}

private fun formatRemaining(unlockAtMillis: Long): String {
    val remainingMs = (unlockAtMillis - System.currentTimeMillis()).coerceAtLeast(0)
    val hours = remainingMs / (60 * 60 * 1000)
    val minutes = (remainingMs / (60 * 1000)) % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

@Composable
private fun StoreItemRow(
    item: StoreItem,
    owned: Boolean,
    equipped: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StoreItemPreview(item = item, modifier = Modifier.size(48.dp))

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            PriceBadge(owned = owned, equipped = equipped, price = item.price)
        }
    }
}

@Composable
private fun StoreItemPreview(item: StoreItem, modifier: Modifier = Modifier) {
    when (item) {
        is BoardThemeItem -> {
            Row(modifier = modifier.clip(RoundedCornerShape(10.dp))) {
                item.previewColorHex.forEach { hex ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(hex.toComposeColor())
                    )
                }
            }
        }

        is CellSkinItem -> DrawableOrFallbackIcon(
            drawableRes = item.previewDrawableRes,
            fallbackIcon = Icons.Filled.GridView,
            modifier = modifier
        )

        is AvatarStoreItem -> DrawableOrFallbackIcon(
            drawableRes = item.previewDrawableRes,
            fallbackIcon = Icons.Filled.Person,
            modifier = modifier
        )
    }
}

@Composable
private fun DrawableOrFallbackIcon(
    drawableRes: Int?,
    fallbackIcon: ImageVector,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (drawableRes != null) {
            Image(
                painter = painterResource(id = drawableRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().padding(4.dp),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = fallbackIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun PriceBadge(owned: Boolean, equipped: Boolean, price: Int) {
    if (equipped) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Applied",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    } else if (owned) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Owned",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Diamond,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$price",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun String.toComposeColor(): Color = Color(AndroidColor.parseColor(this))

@Preview(showBackground = true)
@Composable
private fun StoreScreenPreview() {
    MinesTheme {
        StoreScreen(
            ownedItemIds = setOf("board_classic_teal"),
            coinBalance = 850,
            diamondBalance = 3,
            canRedeem = true,
            redemptionsUsedToday = 0
        )
    }
}