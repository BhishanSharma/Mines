package com.genoma.mines.feedback

import com.genoma.mines.game.BadgeTier

/**
 * A one-off milestone reached by the game that was just completed — a
 * level-up, a newly unlocked achievement tier, or both at once.
 *
 * Detected in [com.genoma.mines.viewmodel.MinesweeperViewModel] by diffing
 * progress before/after the just-finished game is saved, queued, and then
 * drained by the UI one celebration at a time. This is what lets a level-up
 * or badge unlock be acknowledged the moment it happens instead of only
 * showing up silently the next time the player opens Profile or
 * Achievements.
 */
sealed class CelebrationEvent {

    /** The player's total XP crossed into a new level. */
    data class LevelUp(
        val newLevel: Int
    ) : CelebrationEvent()

    /** An achievement track reached a new (higher) [BadgeTier]. */
    data class AchievementUnlocked(
        val trackId: String,
        val trackTitle: String,
        val tier: BadgeTier
    ) : CelebrationEvent()
}