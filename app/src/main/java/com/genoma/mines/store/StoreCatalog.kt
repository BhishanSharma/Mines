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

    fun itemsFor(category: StoreCategory): List<StoreItem> {
        return allItems.filter { it.category == category }
    }
}