package com.genoma.mines.store.components

import com.genoma.mines.store.BoardThemeItem

/**
 * The board theme every player already has — included here mainly as a
 * template showing the shape a new item file should take.
 */
val classicTealTheme = BoardThemeItem(
    id = "board_classic_teal",
    name = "Classic Teal",
    description = "The default Mines board palette.",
    price = 0,
    previewColorHex = listOf("#3FA9A0", "#BEEAE4")
)