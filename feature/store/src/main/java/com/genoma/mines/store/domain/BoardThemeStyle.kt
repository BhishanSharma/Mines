package com.genoma.mines.store.domain
import androidx.compose.ui.graphics.Color

/**
 * Visual configuration for the Minesweeper board. Store-owned board themes
 * provide these values; the global MaterialTheme remains responsible for
 * the rest of the application UI.
 */
data class BoardThemeStyle(
    val boardColor: Color,
    val hiddenCellColor: Color,
    val revealedCellColor: Color,
    val borderColor: Color,
    val flagColor: Color,
    val mineColor: Color
)
