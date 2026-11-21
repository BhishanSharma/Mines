package com.genoma.mines.core.economy

sealed interface CosmeticPurchaseDecision {

    /** Already permanently owned: equipping is free, nothing is charged. */
    data object AlreadyOwned : CosmeticPurchaseDecision

    /** Charge [gemDebit] and grant permanent ownership in the same operation. */
    data class Purchase(val gemDebit: Int) : CosmeticPurchaseDecision

    /** Not owned and the player is [gemsShort] Gems short. */
    data class InsufficientGems(val gemsShort: Int) : CosmeticPurchaseDecision
}

/**
 * Cosmetic purchase rules (plan §1.6).
 *
 * Purchases are permanent. An owned item is never charged again, and
 * unequipping or re-equipping never changes ownership or the wallet.
 */
object CosmeticPurchaseRules {

    /** Idempotency key for buying [itemId]; an item can be bought at most once. */
    fun operationId(itemId: String): String = "purchase-$itemId"

    fun decide(alreadyOwned: Boolean, price: Int, gems: Long): CosmeticPurchaseDecision {
        val cost = price.coerceAtLeast(0)
        return when {
            alreadyOwned || cost == 0 -> CosmeticPurchaseDecision.AlreadyOwned
            gems >= cost -> CosmeticPurchaseDecision.Purchase(cost)
            else -> CosmeticPurchaseDecision.InsufficientGems((cost - gems.coerceAtLeast(0)).toInt())
        }
    }
}
