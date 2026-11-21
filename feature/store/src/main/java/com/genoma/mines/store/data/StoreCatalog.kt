package com.genoma.mines.store.data

import com.genoma.mines.store.domain.BoardThemeItem
import com.genoma.mines.store.domain.CellSkinItem
import com.genoma.mines.store.domain.StoreCategory
import com.genoma.mines.store.domain.StoreItem
import com.genoma.mines.store.data.catalog.classicTealTheme
import com.genoma.mines.store.data.catalog.deepOceanTheme
import com.genoma.mines.store.data.catalog.frostCellSkin
import com.genoma.mines.store.data.catalog.roboAvatar


object StoreCatalog {

    /** The free theme every player owns; "unequipping" a theme returns here. */
    const val DEFAULT_BOARD_THEME_ID = "board_classic_teal"

    val allItems: List<StoreItem> = listOf(
        classicTealTheme,
        deepOceanTheme,
        frostCellSkin,
        roboAvatar
    )

    /**
     * Items whose sale is switched on by the `new_theme_enabled` Remote
     * Config flag. Only add items that are fully implemented in this build;
     * Remote Config may hide or show them, never add new functionality.
     * Empty in this release.
     */
    val newThemeItemIds: Set<String> = emptySet()

    fun boardThemeById(id: String): BoardThemeItem? =
        allItems.filterIsInstance<BoardThemeItem>().find { it.id == id }

    fun cellSkinById(id: String?): CellSkinItem? =
        allItems.filterIsInstance<CellSkinItem>().find { it.id == id }

    fun itemById(id: String?): StoreItem? = allItems.find { it.id == id }

    fun itemsFor(category: StoreCategory): List<StoreItem> {
        return allItems.filter { it.category == category }
    }

    /**
     * Items to list in the Store. Flag-gated items are hidden while their
     * flag is off, but never from players who already own them.
     */
    fun visibleItems(newThemeEnabled: Boolean, ownedItemIds: Set<String>): List<StoreItem> =
        allItems.filter { item ->
            item.id !in newThemeItemIds || newThemeEnabled || item.id in ownedItemIds
        }

    /** The featured item id from Remote Config, if it names a listed item. */
    fun featuredItemId(configured: String, visible: List<StoreItem>): String? =
        configured.trim().takeIf { id -> id.isNotEmpty() && visible.any { it.id == id } }
}
