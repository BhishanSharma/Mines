package com.genoma.mines.core.economy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EconomyCodecTest {

    private val settlement = PendingSettlement(
        id = "game-123e4567-e89b-12d3-a456-426614174000",
        owner = "AbCdEf1234567890uidXYZ",
        type = WalletTransactionType.GAME_LOSS,
        plan = SettlementPlan(coinDebit = 20, lifeDebit = 1),
        createdAtMillis = 1_763_690_000_000L
    )

    @Test
    fun `settlements round trip`() {
        val other = settlement.copy(
            id = "winbonus-abc",
            owner = PendingSettlement.GUEST_OWNER,
            type = WalletTransactionType.WIN_AD_BONUS,
            plan = SettlementPlan(coinCredit = 50)
        )
        val decoded = EconomyCodec.decodeSettlements(EconomyCodec.encodeSettlements(listOf(settlement, other)))
        assertEquals(listOf(settlement, other), decoded)
    }

    @Test
    fun `corrupt lines are skipped`() {
        val raw = EconomyCodec.encodeSettlements(listOf(settlement)) +
                "\ngarbage\nid|owner|NOT_A_TYPE|0|0|0|0|0|0|1\nid|owner|GAME_WIN|-5|0|0|0|0|0|1"
        assertEquals(listOf(settlement), EconomyCodec.decodeSettlements(raw))
        assertTrue(EconomyCodec.decodeSettlements(null).isEmpty())
        assertTrue(EconomyCodec.decodeSettlements("").isEmpty())
    }

    @Test
    fun `transactions round trip`() {
        val items = listOf(
            WalletTransaction("t1", WalletTransactionType.GAME_WIN, 50, 0, 0, 1L),
            WalletTransaction("t2", WalletTransactionType.COINS_TO_GEMS, -500, 1, 0, 2L),
            WalletTransaction("t3", WalletTransactionType.GAME_LOSS, -20, 0, -1, 3L)
        )
        assertEquals(items, EconomyCodec.decodeTransactions(EconomyCodec.encodeTransactions(items)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `ids containing the separator are rejected`() {
        settlement.copy(id = "bad|id")
    }
}
