package com.genoma.mines.wallet.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.genoma.mines.core.economy.ConversionDecision
import com.genoma.mines.core.economy.ConversionDirection
import com.genoma.mines.core.economy.ConversionRules
import com.genoma.mines.core.economy.EconomyCodec
import com.genoma.mines.core.economy.GameSettlementRules
import com.genoma.mines.core.economy.IstCalendar
import com.genoma.mines.core.economy.PendingSettlement
import com.genoma.mines.core.economy.WalletTransaction
import com.genoma.mines.core.economy.WalletTransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.walletDataStore by preferencesDataStore(name = "wallet")

data class RedeemResult(
    val success: Boolean,
    val message: String
)

data class WalletSnapshot(
    val coins: Int,
    val diamonds: Int
)

data class RedeemStatus(
    val coins: Int,
    val diamonds: Int,
    val redemptionsUsedToday: Int,
    val maxRedemptionsPerWindow: Int,
    val nextUnlockMillis: Long?
) {
    private val hasAllowance: Boolean
        get() = redemptionsUsedToday < maxRedemptionsPerWindow

    val canRedeem: Boolean
        get() = hasAllowance && coins >= CoinWalletDataStore.COST_PER_DIAMOND

    val canConvertCoinsToGem: Boolean
        get() = canRedeem

    val canConvertGemToCoins: Boolean
        get() = hasAllowance && diamonds >= 1

    companion object {
        fun of(
            coins: Long,
            diamonds: Long,
            usedToday: Int,
            dailyLimit: Int,
            nowMillis: Long
        ): RedeemStatus {
            val limit = ConversionRules.sanitizeLimit(dailyLimit)
            return RedeemStatus(
                coins = coins.toSafeInt(),
                diamonds = diamonds.toSafeInt(),
                redemptionsUsedToday = usedToday,
                maxRedemptionsPerWindow = limit,
                nextUnlockMillis = if (limit > 0 && usedToday >= limit) {
                    IstCalendar.nextMidnightMillis(nowMillis)
                } else {
                    null
                }
            )
        }
    }
}

internal fun Long.toSafeInt(): Int = coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()

enum class ChargeOutcome {
    /** Charged now. */
    CHARGED,

    /** Charged earlier for the same operation id; nothing changed now. */
    ALREADY_CHARGED,

    /** Balance too low; nothing changed. */
    INSUFFICIENT_FUNDS
}

/**
 * Guest wallet stored on the device.
 *
 * Every balance change happens inside a single DataStore `edit`, so it is
 * atomic. Settlements and conversions record their idempotency key in the
 * same edit, so a retried or duplicated request is applied at most once.
 */
class CoinWalletDataStore(
    private val context: Context,
    private val clock: () -> Long = System::currentTimeMillis
) : WalletRepository {

    companion object {
        const val COST_PER_DIAMOND = ConversionRules.COINS_PER_GEM
        const val COINS_PER_DIAMOND = ConversionRules.COINS_PER_GEM
        const val MAX_REDEMPTIONS_PER_WINDOW = ConversionRules.DEFAULT_DAILY_LIMIT

        private const val MAX_HISTORY = 100
        private const val MAX_APPLIED_IDS = 300

        private val COINS = intPreferencesKey("coins")
        private val DIAMONDS = intPreferencesKey("diamonds")

        private val CONVERSION_DATE = stringPreferencesKey("conversion_date_ist")
        private val CONVERSIONS_USED = intPreferencesKey("conversions_used_today")

        // Written by the previous rolling-window implementation; read once for migration.
        private val LEGACY_REDEEM_TIMESTAMPS = stringPreferencesKey("redeem_timestamps")

        private val APPLIED_IDS = stringPreferencesKey("applied_operation_ids")
        private val MIGRATION_ID = stringPreferencesKey("guest_migration_id")

        /** Prefix of cosmetic purchase operation ids (see CosmeticPurchaseRules). */
        const val PURCHASE_ID_PREFIX = "purchase-"
        private val HISTORY = stringPreferencesKey("wallet_history")
    }

    override val coins: Flow<Int> =
        context.walletDataStore.data.map { it[COINS] ?: 0 }

    override val diamonds: Flow<Int> =
        context.walletDataStore.data.map { it[DIAMONDS] ?: 0 }

    override val history: Flow<List<WalletTransaction>> =
        context.walletDataStore.data.map { prefs ->
            EconomyCodec.decodeTransactions(prefs[HISTORY])
                .sortedByDescending { it.createdAtMillis }
        }

    override suspend fun addCoins(amount: Int) {
        if (amount <= 0) return

        context.walletDataStore.edit { prefs ->
            prefs[COINS] = ((prefs[COINS] ?: 0).toLong() + amount).toSafeInt()
        }
    }

    /** Atomically checks and spends Coins. A failed spend never changes the balance. */
    override suspend fun spendCoins(amount: Int): RedeemResult {
        if (amount <= 0) {
            return RedeemResult(false, "Amount must be greater than zero.")
        }

        var result = RedeemResult(false, "")

        context.walletDataStore.edit { prefs ->
            val current = prefs[COINS] ?: 0

            result = if (current < amount) {
                RedeemResult(false, "You need $amount Coins.")
            } else {
                prefs[COINS] = current - amount
                RedeemResult(true, "Spent $amount Coins.")
            }
        }

        return result
    }

    override suspend fun addDiamonds(amount: Int) {
        if (amount <= 0) return

        context.walletDataStore.edit { prefs ->
            prefs[DIAMONDS] = ((prefs[DIAMONDS] ?: 0).toLong() + amount).toSafeInt()
        }
    }

    override suspend fun getRedeemStatus(dailyLimit: Int): RedeemStatus {
        val now = clock()
        val prefs = context.walletDataStore.data.first()

        return RedeemStatus.of(
            coins = (prefs[COINS] ?: 0).toLong(),
            diamonds = (prefs[DIAMONDS] ?: 0).toLong(),
            usedToday = prefs.usedToday(IstCalendar.dateOf(now)),
            dailyLimit = dailyLimit,
            nowMillis = now
        )
    }

    override suspend fun convert(
        direction: ConversionDirection,
        dailyLimit: Int,
        requestId: String
    ): RedeemResult {
        val now = clock()
        val today = IstCalendar.dateOf(now)
        var result = RedeemResult(false, "")

        context.walletDataStore.edit { prefs ->
            val applied = prefs.appliedIds()

            if (requestId in applied) {
                result = RedeemResult(true, "That conversion was already completed.")
                return@edit
            }

            val coins = (prefs[COINS] ?: 0).toLong()
            val gems = (prefs[DIAMONDS] ?: 0).toLong()
            val used = prefs.usedToday(today)

            val decision = ConversionRules.decide(direction, coins, gems, used, dailyLimit)
            result = RedeemResult(
                success = decision is ConversionDecision.Approved,
                message = ConversionRules.message(decision, direction, dailyLimit)
            )

            if (decision !is ConversionDecision.Approved) return@edit

            prefs[COINS] = (coins + decision.coinsDelta).toSafeInt()
            prefs[DIAMONDS] = (gems + decision.gemsDelta).toSafeInt()
            prefs[CONVERSION_DATE] = today
            prefs[CONVERSIONS_USED] = decision.usedTodayAfter
            prefs.remove(LEGACY_REDEEM_TIMESTAMPS)

            prefs.recordApplied(applied, requestId)
            prefs.appendHistory(
                WalletTransaction(
                    id = requestId,
                    type = if (direction == ConversionDirection.COINS_TO_GEMS) {
                        WalletTransactionType.COINS_TO_GEMS
                    } else {
                        WalletTransactionType.GEMS_TO_COINS
                    },
                    coinsDelta = decision.coinsDelta,
                    gemsDelta = decision.gemsDelta,
                    livesDelta = 0,
                    createdAtMillis = now
                )
            )
        }

        return result
    }

    override suspend fun spendDiamonds(amount: Int): RedeemResult {
        if (amount <= 0) {
            return RedeemResult(true, "")
        }

        var result = RedeemResult(false, "")

        context.walletDataStore.edit { prefs ->
            val current = prefs[DIAMONDS] ?: 0

            result = if (current < amount) {
                RedeemResult(false, "You need $amount Gems.")
            } else {
                prefs[DIAMONDS] = current - amount
                RedeemResult(true, "Purchase successful!")
            }
        }

        return result
    }

    /**
     * Applies the Coins/Gems part of [settlement] exactly once.
     *
     * Returns true when it was applied now, false when it had already been
     * applied earlier (a retry). Life changes are applied by the Life store.
     */
    suspend fun applySettlement(settlement: PendingSettlement): Boolean {
        var appliedNow = false

        context.walletDataStore.edit { prefs ->
            val applied = prefs.appliedIds()
            if (settlement.id in applied) return@edit

            val coinsBefore = (prefs[COINS] ?: 0).toLong()
            val gemsBefore = (prefs[DIAMONDS] ?: 0).toLong()
            val coinsAfter = GameSettlementRules.applyCoins(coinsBefore, settlement.plan)
            val gemsAfter = GameSettlementRules.applyGems(gemsBefore, settlement.plan)

            prefs[COINS] = coinsAfter.toSafeInt()
            prefs[DIAMONDS] = gemsAfter.toSafeInt()
            prefs.recordApplied(applied, settlement.id)
            prefs.appendHistory(
                WalletTransaction(
                    id = settlement.id,
                    type = settlement.type,
                    coinsDelta = (coinsAfter - coinsBefore).toInt(),
                    gemsDelta = (gemsAfter - gemsBefore).toInt(),
                    livesDelta = settlement.plan.lifeCredit - settlement.plan.lifeDebit,
                    createdAtMillis = settlement.createdAtMillis
                )
            )
            appliedNow = true
        }

        return appliedNow
    }

    /** Records a wallet change made outside a settlement (purchases, refills). */
    suspend fun recordHistory(transaction: WalletTransaction) {
        context.walletDataStore.edit { prefs -> prefs.appendHistory(transaction) }
    }

    /**
     * Charges [gemDebit] Gems for the operation [operationId] at most once.
     *
     * Unlike a settlement, a charge never clamps: it fails without changing
     * anything when the balance is too low. Purchases use a deterministic id
     * (for example `purchase-<itemId>`), so retrying never charges twice.
     */
    suspend fun chargeOnce(
        operationId: String,
        gemDebit: Int,
        type: WalletTransactionType
    ): ChargeOutcome {
        var outcome = ChargeOutcome.INSUFFICIENT_FUNDS

        context.walletDataStore.edit { prefs ->
            val applied = prefs.appliedIds()
            if (operationId in applied) {
                outcome = ChargeOutcome.ALREADY_CHARGED
                return@edit
            }

            val gems = (prefs[DIAMONDS] ?: 0).toLong()
            if (gems < gemDebit) {
                outcome = ChargeOutcome.INSUFFICIENT_FUNDS
                return@edit
            }

            prefs[DIAMONDS] = (gems - gemDebit.coerceAtLeast(0)).toSafeInt()
            prefs.recordApplied(applied, operationId)
            prefs.appendHistory(
                WalletTransaction(
                    id = operationId,
                    type = type,
                    coinsDelta = 0,
                    gemsDelta = -gemDebit.coerceAtLeast(0),
                    livesDelta = 0,
                    createdAtMillis = clock()
                )
            )
            outcome = ChargeOutcome.CHARGED
        }

        return outcome
    }

    /** Operation ids already applied, for reconciling interrupted purchases. */
    suspend fun appliedOperationIds(): List<String> =
        context.walletDataStore.data.first().appliedIds()

    /** Conversions used today, for carrying the allowance over at sign-in. */
    suspend fun conversionsUsedToday(): Int =
        context.walletDataStore.data.first().usedToday(IstCalendar.dateOf(clock()))

    /**
     * Stable id for the guest data currently on this device, used as the
     * idempotency key when it is migrated into an account. A new id is
     * created after [clear], when the guest starts collecting fresh data.
     */
    suspend fun migrationId(): String {
        var id = ""
        context.walletDataStore.edit { prefs ->
            id = prefs[MIGRATION_ID] ?: "guestmigration-${java.util.UUID.randomUUID()}".also {
                prefs[MIGRATION_ID] = it
            }
        }
        return id
    }

    /** Clears the local wallet after it has been migrated to an account. */
    suspend fun clear() {
        context.walletDataStore.edit { prefs ->
            prefs.remove(COINS)
            prefs.remove(DIAMONDS)
            prefs.remove(CONVERSION_DATE)
            prefs.remove(CONVERSIONS_USED)
            prefs.remove(LEGACY_REDEEM_TIMESTAMPS)
            prefs.remove(HISTORY)
            prefs.remove(MIGRATION_ID)
            // Applied settlement ids are kept so a late retry can never
            // re-credit a settlement that was already migrated. Purchase ids
            // are dropped: the items moved to the account with the guest
            // store, so this device's guest starts with nothing owned.
            prefs[APPLIED_IDS] = prefs.appliedIds()
                .filterNot { it.startsWith(PURCHASE_ID_PREFIX) }
                .joinToString("\n")
        }
    }

    private fun Preferences.usedToday(today: String): Int =
        ConversionRules.usedToday(
            storedDate = this[CONVERSION_DATE],
            storedCount = this[CONVERSIONS_USED]?.toLong(),
            legacyTimestamps = this[LEGACY_REDEEM_TIMESTAMPS]
                ?.split(",")
                ?.mapNotNull { it.trim().toLongOrNull() }
                .orEmpty(),
            todayIst = today
        )

    private fun Preferences.appliedIds(): List<String> =
        this[APPLIED_IDS]?.split("\n")?.filter { it.isNotBlank() }.orEmpty()

    private fun MutablePreferences.recordApplied(current: List<String>, id: String) {
        this[APPLIED_IDS] = (current + id).takeLast(MAX_APPLIED_IDS).joinToString("\n")
    }

    private fun MutablePreferences.appendHistory(transaction: WalletTransaction) {
        val existing = EconomyCodec.decodeTransactions(this[HISTORY])
        this[HISTORY] = EconomyCodec.encodeTransactions(
            (existing + transaction).takeLast(MAX_HISTORY)
        )
    }
}
