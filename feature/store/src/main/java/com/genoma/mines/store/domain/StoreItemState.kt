package com.genoma.mines.store.domain

/** What a Store row offers, in priority order of the rules below. */
enum class StoreItemAction {
    /** Not owned and affordable: tap buys it (and applies it). */
    BUY,

    /** Not owned and not affordable: shows how many Gems are missing. */
    NEED_MORE_GEMS,

    /** Owned and not in use: tap applies it for free. */
    APPLY,

    /** Owned but has no way to be applied from the Store. */
    OWNED,

    /** In use now. */
    APPLIED
}

data class StoreItemState(
    val action: StoreItemAction,
    /** True when an Unequip action should be offered (never for defaults). */
    val canUnequip: Boolean,
    val gemsShort: Int
)

/**
 * Store row states (plan §4 Store UI): Buy, insufficient balance, Apply,
 * Applied, Owned and Unequip. Free items count as owned. Defaults can't be
 * unequipped because "unequip" means returning to the default.
 */
object StoreItemStates {

    fun resolve(
        price: Int,
        owned: Boolean,
        equipped: Boolean,
        isDefault: Boolean,
        canApplyFromStore: Boolean,
        gemBalance: Int
    ): StoreItemState {
        val isOwned = owned || price <= 0

        return when {
            equipped -> StoreItemState(StoreItemAction.APPLIED, canUnequip = !isDefault, gemsShort = 0)
            isOwned && canApplyFromStore -> StoreItemState(StoreItemAction.APPLY, false, 0)
            isOwned -> StoreItemState(StoreItemAction.OWNED, false, 0)
            gemBalance >= price -> StoreItemState(StoreItemAction.BUY, false, 0)
            else -> StoreItemState(
                StoreItemAction.NEED_MORE_GEMS,
                canUnequip = false,
                gemsShort = price - gemBalance.coerceAtLeast(0)
            )
        }
    }
}
