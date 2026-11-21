package com.genoma.mines.core.economy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameSettlementRulesTest {

    @Test
    fun `win pays the reward minus the entry fee and costs no life`() {
        val plan = GameSettlementRules.forGame(GameOutcome.WIN, winReward = 50, entryFee = 20)
        assertEquals(SettlementPlan(coinCredit = 50, coinDebit = 20), plan)
        assertEquals(130L, GameSettlementRules.applyCoins(100, plan)) // net +30
        assertEquals(0, plan.lifeDebit)
    }

    @Test
    fun `loss charges the entry fee and exactly one life`() {
        val plan = GameSettlementRules.forGame(GameOutcome.LOSS, winReward = 50, entryFee = 20)
        assertEquals(SettlementPlan(coinDebit = 20, lifeDebit = 1), plan)
        assertEquals(80L, GameSettlementRules.applyCoins(100, plan))
    }

    @Test
    fun `entry fee never makes the balance negative`() {
        val loss = GameSettlementRules.forGame(GameOutcome.LOSS, winReward = 50, entryFee = 20)
        assertEquals(0L, GameSettlementRules.applyCoins(5, loss))
        assertEquals(0L, GameSettlementRules.applyCoins(-10, loss))

        // A win with less than the fee still ends with the full reward on top of zero.
        val win = GameSettlementRules.forGame(GameOutcome.WIN, winReward = 50, entryFee = 20)
        assertEquals(50L, GameSettlementRules.applyCoins(5, win))
    }

    @Test
    fun `negative configured values are treated as zero`() {
        assertEquals(SettlementPlan(coinDebit = 20), GameSettlementRules.forGame(GameOutcome.WIN, -1, 20))
        assertEquals(SettlementPlan(coinCredit = 50), GameSettlementRules.forGame(GameOutcome.WIN, 50, -5))
        assertEquals(SettlementPlan(lifeDebit = 1), GameSettlementRules.forGame(GameOutcome.LOSS, 50, -5))
    }

    @Test
    fun `win plus ad bonus nets 80 coins with defaults`() {
        val rules = EconomyRules.DEFAULT.copy(winAdBonusEnabled = true)
        val base = GameSettlementRules.forGame(GameOutcome.WIN, rules.easyWinReward, rules.entryFee)
        val bonus = GameSettlementRules.forWinAdBonus(rules.winAdBonus)
        val after = GameSettlementRules.applyCoins(GameSettlementRules.applyCoins(100, base), bonus)
        assertEquals(180L, after) // -20 fee + 50 reward + 50 bonus
    }

    @Test
    fun `win ad bonus availability needs both the flag and a positive amount`() {
        assertFalse(EconomyRules.DEFAULT.isWinAdBonusAvailable)
        assertTrue(EconomyRules.DEFAULT.copy(winAdBonusEnabled = true).isWinAdBonusAvailable)
        assertFalse(EconomyRules.DEFAULT.copy(winAdBonusEnabled = true, winAdBonus = 0).isWinAdBonusAvailable)
    }

    @Test
    fun `gems debit clamps then credit applies`() {
        val plan = SettlementPlan(gemDebit = 5, gemCredit = 2)
        assertEquals(2L, GameSettlementRules.applyGems(3, plan))
        assertEquals(7L, GameSettlementRules.applyGems(10, plan))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `plans reject negative components`() {
        SettlementPlan(coinCredit = -1)
    }
}
