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