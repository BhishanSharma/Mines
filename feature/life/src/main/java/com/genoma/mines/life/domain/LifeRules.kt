package com.genoma.mines.life.domain

/** Tunable numbers for the hearts (lives) system, kept in one place. */
object LifeRules {
    const val MAX_HEARTS = 5

    /** Gems (diamonds) charged to refill hearts back to [MAX_HEARTS]. */
    const val REFILL_COST_DIAMONDS = 25

    /** One heart regenerates for free after this long, up to [MAX_HEARTS]. */
    const val REGEN_INTERVAL_MILLIS = 60L * 60 * 1000
}
