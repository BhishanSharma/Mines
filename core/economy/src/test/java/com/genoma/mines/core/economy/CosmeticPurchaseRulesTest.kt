package com.genoma.mines.core.economy

import org.junit.Assert.assertEquals
import org.junit.Test

class CosmeticPurchaseRulesTest {

    @Test
    fun `affordable unowned item is charged once`() {
        assertEquals(CosmeticPurchaseDecision.Purchase(20), CosmeticPurchaseRules.decide(false, 20, 25))
        assertEquals(CosmeticPurchaseDecision.Purchase(20), CosmeticPurchaseRules.decide(false, 20, 20))
    }

    @Test
    fun `owned items are never charged again`() {
        assertEquals(CosmeticPurchaseDecision.AlreadyOwned, CosmeticPurchaseRules.decide(true, 20, 0))
        assertEquals(CosmeticPurchaseDecision.AlreadyOwned, CosmeticPurchaseRules.decide(true, 20, 999))
    }

    @Test
    fun `free items count as owned`() {
        assertEquals(CosmeticPurchaseDecision.AlreadyOwned, CosmeticPurchaseRules.decide(false, 0, 0))
    }

    @Test
    fun `insufficient gems reports the shortfall and charges nothing`() {
        assertEquals(CosmeticPurchaseDecision.InsufficientGems(5), CosmeticPurchaseRules.decide(false, 20, 15))
        assertEquals(CosmeticPurchaseDecision.InsufficientGems(20), CosmeticPurchaseRules.decide(false, 20, -3))
    }

    @Test
    fun `purchase operation id is stable per item`() {
        assertEquals("purchase-cellskin_frost", CosmeticPurchaseRules.operationId("cellskin_frost"))
    }
}
