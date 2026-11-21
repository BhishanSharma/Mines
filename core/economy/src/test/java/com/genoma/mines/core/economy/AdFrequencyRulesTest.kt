package com.genoma.mines.core.economy

import com.genoma.mines.core.economy.RewardedAdPlacement.LIFE_RECOVERY
import com.genoma.mines.core.economy.RewardedAdPlacement.LUCK_SPIN
import com.genoma.mines.core.economy.RewardedAdPlacement.WIN_BONUS
import org.junit.Assert.assertEquals
import org.junit.Test

class AdFrequencyRulesTest {

    private val minute = 60_000L
    private val day = 24 * 60 * minute
    private val t0 = 1_763_690_000_000L // 07:23 IST

    private val config = AdCapConfig(
        dailyCaps = mapOf(WIN_BONUS to 10, LIFE_RECOVERY to 3, LUCK_SPIN to 2),
        cooldownMinutes = mapOf(WIN_BONUS to 0, LIFE_RECOVERY to 30, LUCK_SPIN to 60),
        totalDailyCap = 4
    )

    @Test
    fun `fresh usage is eligible`() {
        assertEquals(AdEligibility.Eligible, AdFrequencyRules.check(LUCK_SPIN, config, AdUsage(), t0))
    }

    @Test
    fun `cooldown blocks until it has elapsed`() {
        val used = AdFrequencyRules.record(LIFE_RECOVERY, AdUsage(), t0)

        assertEquals(
            AdEligibility.CoolingDown(t0 + 30 * minute),
            AdFrequencyRules.check(LIFE_RECOVERY, config, used, t0 + 10 * minute)
        )
        assertEquals(
            AdEligibility.Eligible,
            AdFrequencyRules.check(LIFE_RECOVERY, config, used, t0 + 30 * minute)
        )
    }

    @Test
    fun `per placement daily cap`() {
        var usage = AdUsage()
        repeat(2) { i -> usage = AdFrequencyRules.record(LUCK_SPIN, usage, t0 + i * 61 * minute) }

        assertEquals(
            AdEligibility.DailyCapReached,
            AdFrequencyRules.check(LUCK_SPIN, config, usage, t0 + 3 * 61 * minute)
        )
    }

    @Test
    fun `shared total cap across placements`() {
        var usage = AdUsage()
        repeat(4) { i -> usage = AdFrequencyRules.record(WIN_BONUS, usage, t0 + i * minute) }

        assertEquals(
            AdEligibility.TotalDailyCapReached,
            AdFrequencyRules.check(LIFE_RECOVERY, config, usage, t0 + 5 * minute)
        )
    }

    @Test
    fun `caps reset on the next IST day but cooldowns carry over midnight`() {
        var usage = AdUsage()
        repeat(2) { i -> usage = AdFrequencyRules.record(LUCK_SPIN, usage, t0 + i * 61 * minute) }

        val nextDay = t0 + day
        assertEquals(AdEligibility.Eligible, AdFrequencyRules.check(LUCK_SPIN, config, usage, nextDay))

        // A spin 10 minutes before midnight still cools down after midnight.
        val beforeMidnight = IstCalendar.nextMidnightMillis(t0) - 10 * minute
        val spun = AdFrequencyRules.record(LUCK_SPIN, AdUsage(), beforeMidnight)
        assertEquals(
            AdEligibility.CoolingDown(beforeMidnight + 60 * minute),
            AdFrequencyRules.check(LUCK_SPIN, config, spun, beforeMidnight + 20 * minute)
        )
    }

    @Test
    fun `clock moved backwards never extends a cooldown`() {
        val used = AdFrequencyRules.record(LIFE_RECOVERY, AdUsage(), t0 + 10 * day)
        // Device clock is now far behind the recorded time.
        val result = AdFrequencyRules.check(LIFE_RECOVERY, config, used, t0)
        assertEquals(AdEligibility.CoolingDown(t0 + 30 * minute), result)
    }

    @Test
    fun `zero cap disables a placement`() {
        val disabled = config.copy(dailyCaps = config.dailyCaps + (LUCK_SPIN to 0))
        assertEquals(AdEligibility.Disabled, AdFrequencyRules.check(LUCK_SPIN, disabled, AdUsage(), t0))
    }

    @Test
    fun `usage round trips through its codec and tolerates junk`() {
        var usage = AdFrequencyRules.record(LUCK_SPIN, AdUsage(), t0)
        usage = AdFrequencyRules.record(WIN_BONUS, usage, t0 + minute)

        assertEquals(usage, AdFrequencyRules.decode(AdFrequencyRules.encode(usage)))
        assertEquals(AdUsage(), AdFrequencyRules.decode("garbage\ncount.NOPE=3\nlast.WIN_BONUS=x"))
    }
}
