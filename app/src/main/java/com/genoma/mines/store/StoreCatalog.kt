package com.genoma.mines.store

import com.genoma.mines.store.components.classicTealTheme

import com.genoma.mines.store.components.deepOceanTheme
import com.genoma.mines.store.components.frostCellSkin
import com.genoma.mines.store.components.roboAvatar


object StoreCatalog {

    val allItems: List<StoreItem> = listOf(
        classicTealTheme,
        deepOceanTheme,
        frostCellSkin,
        roboAvatar
    )

    fun boardThemeById(id: String): BoardThemeItem? =
        allItems.filterIsInstance<BoardThemeItem>().find { it.id == id }

    fun cellSkinById(id: String?): CellSkinItem? =
        allItems.filterIsInstance<CellSkinItem>().find { it.id == id }

    fun itemsFor(category: StoreCategory): List<StoreItem> {
        return allItems.filter { it.category == category }
    }
}