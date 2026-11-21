package com.genoma.mines.life.domain

import com.genoma.mines.life.domain.LifeRules.MAX_HEARTS
import com.genoma.mines.life.domain.LifeRules.REGEN_INTERVAL_MILLIS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LifeCalculatorTest {

    private val hour = REGEN_INTERVAL_MILLIS
    private val t0 = 1_000_000_000L

    @Test
    fun `new player starts full with no countdown`() {
        val status = LifeCalculator.status(LifeSnapshot.FULL, t0)

        assertEquals(MAX_HEARTS, status.hearts)
        assertNull(status.nextHeartAtMillis)
        assertTrue(status.isFull)
    }

    @Test
    fun `consuming from full starts the regen clock now`() {
        val updated = LifeCalculator.consume(LifeSnapshot.FULL, t0)!!

        assertEquals(LifeSnapshot(MAX_HEARTS - 1, t0), updated)
        assertEquals(t0 + hour, LifeCalculator.status(updated, t0).nextHeartAtMillis)
    }

    @Test
    fun `consuming below full keeps the existing regen clock`() {
        val snapshot = LifeSnapshot(3, t0)

        val updated = LifeCalculator.consume(snapshot, t0 + 20 * 60 * 1000)!!

        assertEquals(LifeSnapshot(2, t0), updated)
    }

    @Test
    fun `consuming with zero hearts is refused`() {
        assertNull(LifeCalculator.consume(LifeSnapshot(0, t0), t0 + hour - 1))
    }

    @Test
    fun `a heart regenerates once a full hour has passed`() {
        val snapshot = LifeSnapshot(0, t0)

        assertEquals(0, LifeCalculator.status(snapshot, t0 + hour - 1).hearts)
        assertEquals(1, LifeCalculator.status(snapshot, t0 + hour).hearts)
        assertEquals(t0 + 2 * hour, LifeCalculator.status(snapshot, t0 + hour).nextHeartAtMillis)
    }

    @Test
    fun `partial progress toward the next heart is kept after regenerating`() {
        val normalized = LifeCalculator.normalize(LifeSnapshot(1, t0), t0 + 2 * hour + 5_000)

        assertEquals(LifeSnapshot(3, t0 + 2 * hour), normalized)
    }

    @Test
    fun `regeneration caps at max hearts`() {
        val status = LifeCalculator.status(LifeSnapshot(1, t0), t0 + 100 * hour)

        assertEquals(MAX_HEARTS, status.hearts)
        assertNull(status.nextHeartAtMillis)
    }

    @Test
    fun `a heart regenerated just in time can be consumed`() {
        val updated = LifeCalculator.consume(LifeSnapshot(0, t0), t0 + hour)

        assertEquals(LifeSnapshot(0, t0 + hour), updated)
    }

    @Test
    fun `refund adds one heart without resetting the regen clock`() {
        val updated = LifeCalculator.refund(LifeSnapshot(2, t0), t0 + 10 * 60 * 1000)

        assertEquals(LifeSnapshot(3, t0), updated)
    }

    @Test
    fun `refund never exceeds max hearts`() {
        val fromAlmostFull = LifeCalculator.refund(LifeSnapshot(MAX_HEARTS - 1, t0), t0 + 1)
        val fromFull = LifeCalculator.refund(LifeSnapshot.FULL, t0)

        assertEquals(MAX_HEARTS, fromAlmostFull.storedHearts)
        assertEquals(MAX_HEARTS, fromFull.storedHearts)
    }

    @Test
    fun `consume then refund round-trips back to full`() {
        val consumed = LifeCalculator.consume(LifeSnapshot.FULL, t0)!!
        val refunded = LifeCalculator.refund(consumed, t0 + 60_000)

        assertTrue(LifeCalculator.status(refunded, t0 + 60_000).isFull)
    }

    @Test
    fun `refill restores max hearts`() {
        assertEquals(MAX_HEARTS, LifeCalculator.status(LifeCalculator.refill(t0), t0).hearts)
    }

    @Test
    fun `clock moved backwards does not produce hearts or a far future countdown`() {
        val snapshot = LifeSnapshot(2, t0 + 10 * hour)

        val status = LifeCalculator.status(snapshot, t0)

        assertEquals(2, status.hearts)
        assertEquals(t0 + hour, status.nextHeartAtMillis)
    }

    @Test
    fun `out of range stored values are clamped`() {
        assertEquals(0, LifeCalculator.status(LifeSnapshot(-3, t0), t0).hearts)
        assertEquals(MAX_HEARTS, LifeCalculator.status(LifeSnapshot(99, t0), t0).hearts)
    }

    @Test
    fun `applyChange deducts one heart for a loss`() {
        val after = LifeCalculator.applyChange(LifeSnapshot.FULL, t0, lifeDebit = 1, lifeCredit = 0)
        assertEquals(MAX_HEARTS - 1, LifeCalculator.status(after, t0).hearts)
    }

    @Test
    fun `applyChange never goes below zero hearts`() {
        val after = LifeCalculator.applyChange(LifeSnapshot(0, t0), t0, lifeDebit = 1, lifeCredit = 0)
        assertEquals(0, LifeCalculator.status(after, t0).hearts)
    }

    @Test
    fun `applyChange recovers one heart and never exceeds the maximum`() {
        val partial = LifeCalculator.applyChange(LifeSnapshot(2, t0), t0, lifeDebit = 0, lifeCredit = 1)
        assertEquals(3, LifeCalculator.status(partial, t0).hearts)

        val full = LifeCalculator.applyChange(LifeSnapshot.FULL, t0, lifeDebit = 0, lifeCredit = 1)
        assertEquals(MAX_HEARTS, LifeCalculator.status(full, t0).hearts)
    }

    @Test
    fun `applyChange keeps natural regeneration progress`() {
        // 2 hearts, anchor 30 minutes ago: recovering a heart must not reset the regen clock.
        val snapshot = LifeSnapshot(2, t0 - 30 * 60_000L)
        val after = LifeCalculator.applyChange(snapshot, t0, lifeDebit = 0, lifeCredit = 1)
        val status = LifeCalculator.status(after, t0)

        assertEquals(3, status.hearts)
        assertEquals(t0 + 30 * 60_000L, status.nextHeartAtMillis)
    }
}
