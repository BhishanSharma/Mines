package com.genoma.mines.store.domain
import androidx.compose.ui.graphics.Color
import com.genoma.mines.core.theme.MinesThemeVariant
data class BoardThemeStyle(
    val boardColor: Color,
    val hiddenCellColor: Color,
    val revealedCellColor: Color,
    val borderColor: Color,
    val flagColor: Color,
    val mineColor: Color,
    val themeVariant: MinesThemeVariant = MinesThemeVariant.CLASSIC_TEAL
)