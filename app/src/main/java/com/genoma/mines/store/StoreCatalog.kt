package com.genoma.mines.store

import com.genoma.mines.store.components.classicTealTheme
import com.genoma.mines.store.components.deepOceanTheme
import com.genoma.mines.store.components.frostCellSkin
import com.genoma.mines.store.components.roboAvatar

/**
 * The full store catalog. This file intentionally holds no item data of
 * its own — every item is defined in its own file under store/components/
 * (see StoreItem.kt for the types, and any file in components/ for the
 * pattern), and this just assembles references to them.
 *
 * To add a new item:
 * 1. Create a new file in store/components/, e.g. GoldCellSkin.kt,
 *    defining one top-level `val` of the right StoreItem subtype.
 * 2. Import it here and add it to allItems below.
 *
 * That import-and-list-it step is deliberate rather than a limitation to
 * work around: Kotlin has no safe way to auto-discover plain top-level
 * declarations across files at compile time (unlike, say, an annotation
 * processor-based registry), so this keeps the catalog's contents
 * explicit, typo-proof, and easy to see in one place — nothing added
 * under components/ affects the app until it's listed here.
 */
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