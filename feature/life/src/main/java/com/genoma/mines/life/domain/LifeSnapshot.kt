package com.genoma.mines.life.domain

/**
 * Hearts exactly as persisted (DataStore for guests, Firestore for accounts).
 *
 * [storedHearts] is the count at the time of the last write, *before* any
 * time-based regeneration since then. [regenAnchorMillis] is the moment the
 * current regen hour started counting; it's meaningless while hearts are full.
 * Always read it through [LifeCalculator] rather than using it directly.
 */
data class LifeSnapshot(
    val storedHearts: Int,
    val regenAnchorMillis: Long
) {
    companion object {
        /** A player who has never lost a heart (new guest or new account). */
        val FULL = LifeSnapshot(storedHearts = LifeRules.MAX_HEARTS, regenAnchorMillis = 0L)
    }
}

/** Hearts as the player should see them right now, regen already applied. */
data class LifeStatus(
    val hearts: Int,
    val maxHearts: Int,
    /** When the next free heart lands, or null while hearts are full. */
    val nextHeartAtMillis: Long?
) {
    val isFull: Boolean
        get() = hearts >= maxHearts

    val hasHearts: Boolean
        get() = hearts > 0

    companion object {
        val FULL = LifeStatus(
            hearts = LifeRules.MAX_HEARTS,
            maxHearts = LifeRules.MAX_HEARTS,
            nextHeartAtMillis = null
        )
    }
}
