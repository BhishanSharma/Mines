package com.genoma.mines.achievements.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.genoma.mines.achievements.domain.BadgeTier
import com.genoma.mines.core.theme.TierBronze
import com.genoma.mines.core.theme.TierDiamond
import com.genoma.mines.core.theme.TierGold
import com.genoma.mines.core.theme.TierPlatinum
import com.genoma.mines.core.theme.TierSilver

/*
 * The one source for how a badge looks: its tier color and its track
 * icon. Achievements, Profile and the celebration screen all use these, so
 * a badge looks the same everywhere it appears.
 */

fun BadgeTier.color(): Color = when (this) {
    BadgeTier.BRONZE -> TierBronze
    BadgeTier.SILVER -> TierSilver
    BadgeTier.GOLD -> TierGold
    BadgeTier.PLATINUM -> TierPlatinum
    BadgeTier.DIAMOND -> TierDiamond
}

fun achievementTrackIcon(trackId: String): ImageVector = when (trackId) {
    "games_played" -> Icons.Filled.SportsEsports
    "games_won" -> Icons.Filled.EmojiEvents
    "win_streak" -> Icons.Filled.Whatshot
    "total_score" -> Icons.Filled.BarChart
    "hard_wins" -> Icons.Filled.MilitaryTech
    else -> Icons.Filled.EmojiEvents
}
