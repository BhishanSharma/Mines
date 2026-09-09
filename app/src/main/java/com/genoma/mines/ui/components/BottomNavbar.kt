package com.genoma.mines.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector


@Composable
fun BottomNavbar(
    selectedItem: BottomNavItem,
    onHome: () -> Unit,
    onOpenSTORE: () -> Unit,
    onOpenAchievements: () -> Unit,
    onOpenMoreGames: () -> Unit,
    onHowToPlay: () -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        BottomNavItem.entries.forEach { item ->
            NavigationBarItem(
                selected = selectedItem == item,
                onClick = {
                    when (item) {
                        BottomNavItem.HOME -> onHome()
                        BottomNavItem.STORE -> onOpenSTORE()
                        BottomNavItem.ACHIEVEMENTS -> onOpenAchievements()
                        BottomNavItem.MORE_GAMES -> onOpenMoreGames()
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

/**
 * Destinations available from the app-wide bottom navigation.
 */
enum class BottomNavItem(
    val label: String,
    val icon: ImageVector
) {
    HOME(
        label = "Home",
        icon = Icons.Filled.Home
    ),
    STORE(
        label = "Store",
        icon = Icons.Filled.ShoppingCart
    ),
    ACHIEVEMENTS(
        label = "Achievements",
        icon = Icons.Filled.EmojiEvents
    ),
    MORE_GAMES(
        label = "More Games",
        icon = Icons.Filled.SportsEsports
    )
}