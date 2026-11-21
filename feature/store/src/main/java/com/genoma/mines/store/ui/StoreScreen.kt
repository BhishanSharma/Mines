package com.genoma.mines.store.ui
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.Canvas
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.genoma.mines.core.ui.components.ScreenTopBar
import com.genoma.mines.core.theme.MinesPreviews
import com.genoma.mines.core.ui.components.ScreenSurface
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
import com.genoma.mines.store.domain.StoreItemStates
import com.genoma.mines.store.domain.StoreItemState
import com.genoma.mines.store.domain.StoreItemAction
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.PaddingValues
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
    canConvertGemToCoins: Boolean = false,
    redemptionsUsedToday: Int = 0,
    maxRedemptionsPerWindow: Int = 3,
    nextUnlockMillis: Long? = null,
    onRedeemClick: () -> Unit = {},
    onConvertGemToCoinsClick: () -> Unit = {},
    onItemClick: (StoreItem) -> Unit = {},
    onBack: () -> Unit = {},
    // Store item id of the equipped premium avatar, if any.
    equippedAvatarStoreItemId: String? = null,
    // Remote Config featured item (already validated against the catalog).
    featuredItemId: String? = null,
    // True while a purchase or conversion is in flight: buttons are disabled.
    isBusy: Boolean = false,
    onUnequip: (StoreItem) -> Unit = {},
    onResetToDefaults: () -> Unit = {}
) {
    ScreenSurface(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = StoreSpacing.screenHorizontal)
                .padding(bottom = StoreSpacing.screenBottom)
        ) {

            // Both balances up top: coins used to appear only inside the
            // redeem card, so players couldn't see them at a glance.
            ScreenTopBar(
                title = "Store",
                horizontalInset = false
            ) {
                BalancePill(
                    icon = Icons.Filled.MonetizationOn,
                    value = coinBalance,
                    contentDescription = "$coinBalance coins"
                )
                Spacer(modifier = Modifier.width(8.dp))
                BalancePill(
                    icon = Icons.Filled.Diamond,
                    value = diamondBalance,
                    contentDescription = "$diamondBalance Gems"
                )
            }

            Spacer(modifier = Modifier.height(StoreSpacing.barToContent))

            RedeemCard(
                coinBalance = coinBalance,
                diamondBalance = diamondBalance,
                canRedeem = canRedeem,
                canConvertGemToCoins = canConvertGemToCoins,
                redemptionsUsedToday = redemptionsUsedToday,
                maxRedemptionsPerWindow = maxRedemptionsPerWindow,
                nextUnlockMillis = nextUnlockMillis,
                onRedeemClick = onRedeemClick,
                onConvertGemToCoinsClick = onConvertGemToCoinsClick,
                busy = isBusy
            )

            Spacer(modifier = Modifier.height(StoreSpacing.sectionGap))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                StoreCategory.entries.forEach { category ->
                    // The featured item leads its category.
                    val categoryItems = items
                        .filter { it.category == category }
                        .sortedByDescending { it.id == featuredItemId }

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
                                val state = StoreItemStates.resolve(
                                    price = item.price,
                                    owned = ownedItemIds.contains(item.id),
                                    equipped = when (item) {
                                        is BoardThemeItem -> equippedBoardThemeId == item.id
                                        is CellSkinItem -> equippedCellSkinId == item.id
                                        is AvatarStoreItem -> equippedAvatarStoreItemId == item.id
                                    },
                                    isDefault = item.id == StoreCatalog.DEFAULT_BOARD_THEME_ID,
                                    canApplyFromStore = true,
                                    gemBalance = diamondBalance
                                )

                                StoreItemRow(
                                    item = item,
                                    state = state,
                                    featured = item.id == featuredItemId,
                                    enabled = !isBusy,
                                    onClick = { onItemClick(item) },
                                    onUnequip = { onUnequip(item) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(StoreSpacing.sectionGap))
                    }
                }

                val customised = equippedBoardThemeId != StoreCatalog.DEFAULT_BOARD_THEME_ID ||
                        equippedCellSkinId != null || equippedAvatarStoreItemId != null

                // Explicit reset (plan §1.6). Owned items stay owned.
                TextButton(
                    onClick = onResetToDefaults,
                    enabled = customised && !isBusy,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Reset to default look")
                }
                Text(
                    text = "Restores the default theme, cells and avatar. You keep everything you own.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = StoreSpacing.sectionGap)
                )
            }
        }
    }
}

@Composable
private fun BalancePill(
    icon: ImageVector,
    value: Int,
    contentDescription: String
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) { this.contentDescription = contentDescription },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$value",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RedeemCard(
    coinBalance: Int,
    diamondBalance: Int,
    canRedeem: Boolean,
    canConvertGemToCoins: Boolean,
    redemptionsUsedToday: Int,
    maxRedemptionsPerWindow: Int,
    nextUnlockMillis: Long?,
    onRedeemClick: () -> Unit,
    onConvertGemToCoinsClick: () -> Unit,
    busy: Boolean = false
) {
    val remainingConversions =
        (maxRedemptionsPerWindow - redemptionsUsedToday).coerceAtLeast(0)

    val dailyLimitReached =
        redemptionsUsedToday >= maxRedemptionsPerWindow

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
            Text(
                text = "Currency Exchange",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "500 Coins = 1 Gem",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = "1 Gem = 500 Coins",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$remainingConversions of $maxRedemptionsPerWindow conversions remaining today (IST)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )

            if (dailyLimitReached && nextUnlockMillis != null) {
                Text(
                    text = "Daily limit reached. Resets in ${formatRemaining(nextUnlockMillis)}.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$coinBalance Coins",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Convert 500 Coins into 1 Gem",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    )
                }

                Button(
                    onClick = onRedeemClick,
                    enabled = canRedeem && !busy
                ) {
                    Text("Get 1 Gem")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$diamondBalance Gems",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Convert 1 Gem into 500 Coins",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    )
                }

                Button(
                    onClick = onConvertGemToCoinsClick,
                    enabled = canConvertGemToCoins && !busy
                ) {
                    Text("Get 500 Coins")
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
    state: StoreItemState,
    featured: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onUnequip: () -> Unit
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (featured) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "FEATURED",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.tertiaryContainer)
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (state.canUnequip) {
                    TextButton(
                        onClick = onUnequip,
                        enabled = enabled,
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                    ) {
                        Text("Unequip", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            PriceBadge(
                state = state,
                price = item.price
            )
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

        // No preview art ships for these yet, so draw one rather than a
        // generic placeholder: players shouldn't buy blind.
        is CellSkinItem -> if (item.previewDrawableRes != null) {
            DrawableOrFallbackIcon(item.previewDrawableRes, Icons.Filled.GridView, modifier)
        } else {
            FrostCellsPreview(modifier = modifier)
        }

        is AvatarStoreItem -> if (item.previewDrawableRes != null) {
            DrawableOrFallbackIcon(item.previewDrawableRes, Icons.Filled.Person, modifier)
        } else {
            RoboAvatarPreview(modifier = modifier)
        }
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

/** A 2x2 sample of cells wearing the frosted skin: white glassy borders. */
@Composable
private fun FrostCellsPreview(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFDCEFF5))
    ) {
        val gap = size.width * 0.08f
        val cell = (size.width - gap * 3) / 2
        val corner = CornerRadius(cell * 0.22f)
        for (row in 0..1) {
            for (col in 0..1) {
                val topLeft = Offset(gap + col * (cell + gap), gap + row * (cell + gap))
                val revealed = row == 1 && col == 0
                drawRoundRect(
                    color = if (revealed) Color.White else Color(0xFF9BD3E3),
                    topLeft = topLeft,
                    size = Size(cell, cell),
                    cornerRadius = corner
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.9f),
                    topLeft = topLeft,
                    size = Size(cell, cell),
                    cornerRadius = corner,
                    style = Stroke(width = cell * 0.12f)
                )
            }
        }
    }
}

/** A simple robot face standing in for the Robo avatar's art. */
@Composable
private fun RoboAvatarPreview(modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
    ) {
        val w = size.width
        val h = size.height
        val headColor = Color(0xFF8A99A3)

        // Antenna.
        drawLine(headColor, Offset(w * 0.5f, h * 0.14f), Offset(w * 0.5f, h * 0.28f), w * 0.05f)
        drawCircle(Color(0xFFE5484D), radius = w * 0.06f, center = Offset(w * 0.5f, h * 0.13f))
        // Head.
        drawRoundRect(
            color = headColor,
            topLeft = Offset(w * 0.2f, h * 0.28f),
            size = Size(w * 0.6f, h * 0.5f),
            cornerRadius = CornerRadius(w * 0.12f)
        )
        // Eyes.
        drawCircle(Color.White, radius = w * 0.08f, center = Offset(w * 0.38f, h * 0.47f))
        drawCircle(Color.White, radius = w * 0.08f, center = Offset(w * 0.62f, h * 0.47f))
        drawCircle(accent, radius = w * 0.04f, center = Offset(w * 0.38f, h * 0.47f))
        drawCircle(accent, radius = w * 0.04f, center = Offset(w * 0.62f, h * 0.47f))
        // Mouth grille.
        drawRoundRect(
            color = Color(0xFF2B3134),
            topLeft = Offset(w * 0.35f, h * 0.62f),
            size = Size(w * 0.3f, h * 0.07f),
            cornerRadius = CornerRadius(w * 0.03f)
        )
    }
}

/**
 * What the row's action is, at a glance:
 * - Applied: in use now.
 * - Apply: owned, tap to use it.
 * - Buy + price: affordable, styled as a button.
 * - Price + "Need N more": not affordable yet, muted, so a failed purchase
 *   isn't a surprise after tapping.
 */
@Composable
private fun PriceBadge(
    state: StoreItemState,
    price: Int
) {
    when (state.action) {
        StoreItemAction.APPLIED -> StatusChip(
            text = "Applied",
            container = MaterialTheme.colorScheme.primaryContainer,
            content = MaterialTheme.colorScheme.onPrimaryContainer
        )

        StoreItemAction.APPLY -> StatusChip(
            text = "Apply",
            container = MaterialTheme.colorScheme.surfaceVariant,
            content = MaterialTheme.colorScheme.primary
        )

        StoreItemAction.OWNED -> StatusChip(
            text = "Owned",
            container = MaterialTheme.colorScheme.surfaceVariant,
            content = MaterialTheme.colorScheme.onSurfaceVariant
        )

        StoreItemAction.BUY -> Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Buy",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Filled.Diamond,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "$price",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        StoreItemAction.NEED_MORE_GEMS -> Column(horizontalAlignment = Alignment.End) {
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
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$price",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "Need ${state.gemsShort} more",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusChip(text: String, container: Color, content: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(container)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = content
        )
    }
}

private fun String.toComposeColor(): Color = Color(AndroidColor.parseColor(this))

@MinesPreviews
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