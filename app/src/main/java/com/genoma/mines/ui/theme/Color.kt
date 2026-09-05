package com.genoma.mines.ui.theme

import androidx.compose.ui.graphics.Color

// ---- Brand ----
// Teal/blue reads as "calm, safe" — fitting for a puzzle game where
// the goal is to avoid danger, not create it.
val MinesTealLight = Color(0xFF3FA9A0)
val MinesTealDark = Color(0xFF4FE0CE)

// Warm coral used sparingly for danger states (mines, "game over").
val MinesCoral = Color(0xFFE8604C)
val MinesCoralContainerLight = Color(0xFFFFDAD3)
val MinesCoralContainerDark = Color(0xFF5C271D)

// ---- Light theme surfaces ----
val LightBackground = Color(0xFFF6F8F8)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE7EEED)
val LightOnBackground = Color(0xFF1A1C1C)
val LightOnSurfaceVariant = Color(0xFF5A6A68)
val LightOutline = Color(0xFFB9C5C3)
val LightPrimaryContainer = Color(0xFFBEEAE4)
val LightOnPrimaryContainer = Color(0xFF00201D)

// ---- Dark theme surfaces ----
// Background pushed darker (near-black) so the board and cells read as
// clearly lighter layers on top of it, instead of blending together.
val DarkBackground = Color(0xFF0A0F0E)
val DarkSurface = Color(0xFF17211F)
val DarkSurfaceVariant = Color(0xFF1C2826)
val DarkOnBackground = Color(0xFFF2F5F4)
val DarkOnSurfaceVariant = Color(0xFFAFC2BE)
val DarkOutline = Color(0xFF3E5450)
// Unrevealed cells: brightened well above the board/background so they
// visibly pop as "tappable tiles" instead of nearly matching the board.
val DarkPrimaryContainer = Color(0xFF2C6259)
val DarkOnPrimaryContainer = Color(0xFFEAFFFA)

// ---- Cell number colors (classic Minesweeper convention, tuned for
// contrast on both light and dark cell backgrounds) ----
val CountOne = Color(0xFF3B82F6)
val CountTwo = Color(0xFF22A55D)
val CountThree = Color(0xFFE8604C)
val CountFour = Color(0xFF8B5CF6)
val CountFive = Color(0xFFC2410C)
val CountSix = Color(0xFF0D9488)
val CountSeven = Color(0xFF1F2937)
val CountEight = Color(0xFF6B7280)