package com.genoma.mines.store.data

data class StoreSnapshot(
    val ownedItemIds: Set<String>,
    val equippedBoardThemeId: String,
    val equippedCellSkinId: String?
)
