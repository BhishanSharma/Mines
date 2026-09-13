package com.genoma.mines.store.data.catalog
import androidx.compose.ui.graphics.Color
import com.genoma.mines.store.domain.BoardThemeItem
import com.genoma.mines.store.domain.BoardThemeStyle

/**
 * The board theme every player already has — included here mainly as a
 * template showing the shape a new item file should take.
 */
val classicTealTheme = BoardThemeItem(
    id = "board_classic_teal",
    name = "Classic Teal",
    description = "The default Mines board palette.",
    price = 0,
    previewColorHex = listOf(
        "#3FA9A0",
        "#BEEAE4"
    ),
    style = BoardThemeStyle(
        boardColor = Color(0xFFE7EEED),
        hiddenCellColor = Color(0xFFBEEAE4),
        revealedCellColor = Color.White,
        borderColor = Color(0xFF3FA9A0),
        flagColor = Color(0xFFE8604C),
        mineColor = Color(0xFF5A6A68)
    )
)