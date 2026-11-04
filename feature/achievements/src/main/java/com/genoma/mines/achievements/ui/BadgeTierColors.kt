package com.genoma.mines.achievements.ui

import androidx.compose.ui.graphics.Color
import com.genoma.mines.achievements.domain.BadgeTier
import com.genoma.mines.core.theme.TierBronze
import com.genoma.mines.core.theme.TierDiamond
import com.genoma.mines.core.theme.TierGold
import com.genoma.mines.core.theme.TierPlatinum
import com.genoma.mines.core.theme.TierSilver

/**
 * The one mapping from tier to color. Achievements, Profile and the
 * celebration overlay all use this, so a badge looks the same everywhere.
 */
fun BadgeTier.color(): Color = when (this) {
    BadgeTier.BRONZE -> TierBronze
    BadgeTier.SILVER -> TierSilver
    BadgeTier.GOLD -> TierGold
    BadgeTier.PLATINUM -> TierPlatinum
    BadgeTier.DIAMOND -> TierDiamond
}
