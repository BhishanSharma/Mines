package com.genoma.mines.core.economy

enum class GameOutcome { WIN, LOSS }

/**
 * Economy values captured when a game starts.
 *
 * Remote Config can change at any moment through the real-time listener.
 * Capturing the values at the start of a game means a running game always
 * settles with the rules it started with ("next game" activation timing).
 */
data class EconomyRules(
    val easyWinReward: Int = 50,
    val mediumWinReward: Int = 50,
    val hardWinReward: Int = 50,
    /** Entry fee charged on every finished game, win or loss (Remote Config `loss_coin_fee`). */
    val entryFee: Int = 20,
    val minimumStartCoins: Int = 20,
    val winAdBonus: Int = 50,
    val winAdBonusEnabled: Boolean = false
) {
    val isWinAdBonusAvailable: Boolean
        get() = winAdBonusEnabled && winAdBonus > 0

    companion object {
        val DEFAULT = EconomyRules()
    }
}

/**
 * The balance changes a single wallet operation applies.
 *
 * Debits are clamped so a balance never goes negative; credits are applied
 * after debits.
 */
data class SettlementPlan(
    val coinCredit: Int = 0,
    val coinDebit: Int = 0,
    val gemCredit: Int = 0,
    val gemDebit: Int = 0,
    val lifeDebit: Int = 0,
    val lifeCredit: Int = 0
) {
    init {
        require(coinCredit >= 0 && coinDebit >= 0)
        require(gemCredit >= 0 && gemDebit >= 0)
        require(lifeDebit >= 0 && lifeCredit >= 0)
    }

    val isEmpty: Boolean
        get() = coinCredit == 0 && coinDebit == 0 && gemCredit == 0 &&
                gemDebit == 0 && lifeDebit == 0 && lifeCredit == 0
}

/** Game result economy. */
object GameSettlementRules {

    /**
     * The entry fee is charged on every finished game; it is settled with
     * the result rather than taken at the start.
     *
     * - Win: entry fee, then the win reward (defaults: -20 + 50 = +30 Coins).
     *   No Life is deducted.
     * - Loss: entry fee and exactly one Life (defaults: -20 Coins, -1 Life).
     */
    fun forGame(
        outcome: GameOutcome,
        winReward: Int,
        entryFee: Int
    ): SettlementPlan = when (outcome) {
        GameOutcome.WIN -> SettlementPlan(
            coinCredit = winReward.coerceAtLeast(0),
            coinDebit = entryFee.coerceAtLeast(0)
        )

        GameOutcome.LOSS -> SettlementPlan(coinDebit = entryFee.coerceAtLeast(0), lifeDebit = 1)
    }

    /** Optional post-win rewarded-ad bonus, separate from the base reward. */
    fun forWinAdBonus(amount: Int): SettlementPlan =
        SettlementPlan(coinCredit = amount.coerceAtLeast(0))

    /** Coins after [plan]; the debit is clamped at zero, then the credit is added. */
    fun applyCoins(balance: Long, plan: SettlementPlan): Long =
        (balance.coerceAtLeast(0L) - plan.coinDebit).coerceAtLeast(0L) + plan.coinCredit

    /** Gems after [plan]; the debit is clamped at zero, then the credit is added. */
    fun applyGems(balance: Long, plan: SettlementPlan): Long =
        (balance.coerceAtLeast(0L) - plan.gemDebit).coerceAtLeast(0L) + plan.gemCredit
}
