package com.genoma.mines.life.domain

import com.genoma.mines.life.domain.LifeRules.MAX_HEARTS
import com.genoma.mines.life.domain.LifeRules.REGEN_INTERVAL_MILLIS

/**
 * Pure heart math shared by the guest and Firestore stores, so both apply
 * regeneration identically. Every mutation first folds in the hearts earned
 * since the last write, then applies its change.
 */
object LifeCalculator {

    /** Applies regeneration up to [nowMillis] and returns the equivalent snapshot. */
    fun normalize(snapshot: LifeSnapshot, nowMillis: Long): LifeSnapshot {
        val stored = snapshot.storedHearts.coerceIn(0, MAX_HEARTS)
        if (stored >= MAX_HEARTS) return LifeSnapshot(MAX_HEARTS, nowMillis)

        // A device clock set backwards shouldn't freeze regen for the gap,
        // so a future anchor is pulled back to now.
        val anchor = minOf(snapshot.regenAnchorMillis, nowMillis)
        val gained = (nowMillis - anchor) / REGEN_INTERVAL_MILLIS
        val hearts = stored + gained

        return if (hearts >= MAX_HEARTS) {
            LifeSnapshot(MAX_HEARTS, nowMillis)
        } else {
            LifeSnapshot(hearts.toInt(), anchor + gained * REGEN_INTERVAL_MILLIS)
        }
    }

    fun status(snapshot: LifeSnapshot, nowMillis: Long): LifeStatus {
        val normalized = normalize(snapshot, nowMillis)
        val isFull = normalized.storedHearts >= MAX_HEARTS

        return LifeStatus(
            hearts = normalized.storedHearts,
            maxHearts = MAX_HEARTS,
            nextHeartAtMillis = if (isFull) null else normalized.regenAnchorMillis + REGEN_INTERVAL_MILLIS
        )
    }

    /** Spends one heart, or returns null when there are none left. */
    fun consume(snapshot: LifeSnapshot, nowMillis: Long): LifeSnapshot? {
        val normalized = normalize(snapshot, nowMillis)

        return when {
            normalized.storedHearts <= 0 -> null
            // Dropping below full is what starts the regen clock.
            normalized.storedHearts >= MAX_HEARTS -> LifeSnapshot(MAX_HEARTS - 1, nowMillis)
            else -> normalized.copy(storedHearts = normalized.storedHearts - 1)
        }
    }

    /** Gives one heart back (a won game), capped at [MAX_HEARTS]. */
    fun refund(snapshot: LifeSnapshot, nowMillis: Long): LifeSnapshot {
        val normalized = normalize(snapshot, nowMillis)
        val hearts = normalized.storedHearts + 1

        return if (hearts >= MAX_HEARTS) {
            LifeSnapshot(MAX_HEARTS, nowMillis)
        } else {
            normalized.copy(storedHearts = hearts)
        }
    }

    fun refill(nowMillis: Long): LifeSnapshot = LifeSnapshot(MAX_HEARTS, nowMillis)
}
