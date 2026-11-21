package com.genoma.mines.core.economy

import com.genoma.mines.core.economy.ConversionDirection.COINS_TO_GEMS
import com.genoma.mines.core.economy.ConversionDirection.GEMS_TO_COINS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversionRulesTest {

    @Test
    fun `coins to gems at 500 to 1`() {
        val decision = ConversionRules.decide(COINS_TO_GEMS, coins = 500, gems = 0, usedToday = 0, dailyLimit = 3)
        assertEquals(ConversionDecision.Approved(coinsDelta = -500, gemsDelta = 1, usedTodayAfter = 1), decision)
    }

    @Test
    fun `gems to coins at 1 to 500`() {
        val decision = ConversionRules.decide(GEMS_TO_COINS, coins = 0, gems = 1, usedToday = 2, dailyLimit = 3)
        assertEquals(ConversionDecision.Approved(coinsDelta = 500, gemsDelta = -1, usedTodayAfter = 3), decision)
    }

    @Test
    fun `exactly three successful conversions per day shared across directions`() {
        var used = 0
        var coins = 5_000L
        var gems = 5L
        val directions = listOf(COINS_TO_GEMS, GEMS_TO_COINS, COINS_TO_GEMS, GEMS_TO_COINS)
        val results = directions.map { direction ->
            val d = ConversionRules.decide(direction, coins, gems, used, dailyLimit = 3)
            if (d is ConversionDecision.Approved) {
                coins += d.coinsDelta
                gems += d.gemsDelta
                used = d.usedTodayAfter
            }
            d
        }

        assertEquals(3, results.count { it is ConversionDecision.Approved })
        assertEquals(ConversionDecision.Rejected(ConversionRejection.DAILY_LIMIT_REACHED), results.last())
        assertEquals(3, used)
    }

    @Test
    fun `rejected conversions do not consume an allowance`() {
        val poor = ConversionRules.decide(COINS_TO_GEMS, coins = 499, gems = 0, usedToday = 1, dailyLimit = 3)
        assertEquals(ConversionDecision.Rejected(ConversionRejection.INSUFFICIENT_COINS), poor)

        val noGems = ConversionRules.decide(GEMS_TO_COINS, coins = 0, gems = 0, usedToday = 1, dailyLimit = 3)
        assertEquals(ConversionDecision.Rejected(ConversionRejection.INSUFFICIENT_GEMS), noGems)
        // The caller only persists usedTodayAfter from an Approved decision.
    }

    @Test
    fun `daily limit is checked before balance`() {
        val decision = ConversionRules.decide(COINS_TO_GEMS, coins = 0, gems = 0, usedToday = 3, dailyLimit = 3)
        assertEquals(ConversionDecision.Rejected(ConversionRejection.DAILY_LIMIT_REACHED), decision)
    }

    @Test
    fun `limit of zero disables conversions and limits are clamped`() {
        assertEquals(
            ConversionDecision.Rejected(ConversionRejection.CONVERSIONS_DISABLED),
            ConversionRules.decide(COINS_TO_GEMS, coins = 10_000, gems = 0, usedToday = 0, dailyLimit = 0)
        )
        assertEquals(0, ConversionRules.sanitizeLimit(-5))
        assertEquals(ConversionRules.MAX_DAILY_LIMIT, ConversionRules.sanitizeLimit(10_000))
    }

    @Test
    fun `lowering the limit mid-day blocks further conversions`() {
        val decision = ConversionRules.decide(COINS_TO_GEMS, coins = 10_000, gems = 0, usedToday = 3, dailyLimit = 2)
        assertEquals(ConversionDecision.Rejected(ConversionRejection.DAILY_LIMIT_REACHED), decision)
    }

    @Test
    fun `counter only applies to the stored IST day`() {
        assertEquals(2, ConversionRules.usedToday("2026-11-21", 2, emptyList(), "2026-11-21"))
        assertEquals(0, ConversionRules.usedToday("2026-11-20", 3, emptyList(), "2026-11-21"))
    }

    @Test
    fun `legacy rolling-window timestamps from today are counted on upgrade`() {
        val todayIstMorning = 1_763_690_000_000L // 2025-11-21T01:53:20Z = 07:23 IST
        val yesterday = todayIstMorning - 24 * 60 * 60 * 1000L
        val today = IstCalendar.dateOf(todayIstMorning)

        val used = ConversionRules.usedToday(
            storedDate = null,
            storedCount = null,
            legacyTimestamps = listOf(yesterday, todayIstMorning, todayIstMorning + 1_000),
            todayIst = today
        )
        assertEquals(2, used)
    }

    @Test
    fun `legacy timestamps are ignored once the new counter exists`() {
        val now = 1_763_690_000_000L
        val today = IstCalendar.dateOf(now)
        assertEquals(0, ConversionRules.usedToday("2026-01-01", 3, listOf(now), today))
    }

    @Test
    fun `messages reflect the configured limit`() {
        val rejected = ConversionDecision.Rejected(ConversionRejection.DAILY_LIMIT_REACHED)
        assertTrue(ConversionRules.message(rejected, COINS_TO_GEMS, 5).contains("all 5 conversions"))
        assertTrue(ConversionRules.message(rejected, COINS_TO_GEMS, 1).contains("all 1 conversion "))
    }
}
