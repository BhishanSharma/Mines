package com.genoma.mines.wallet.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val Context.walletDataStore by preferencesDataStore(name = "wallet")

private const val IST_TIME_ZONE = "Asia/Kolkata"

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
    val canRedeem: Boolean
        get() = redemptionsUsedToday < maxRedemptionsPerWindow &&
                coins >= CoinWalletDataStore.COST_PER_DIAMOND

    val canConvertCoinsToGem: Boolean
        get() = canRedeem

    val canConvertGemToCoins: Boolean
        get() = redemptionsUsedToday < maxRedemptionsPerWindow &&
                diamonds >= 1
}

class CoinWalletDataStore(
    private val context: Context
) : WalletRepository {

    companion object {
        const val COST_PER_DIAMOND = 500
        const val COINS_PER_DIAMOND = 500
        const val MAX_REDEMPTIONS_PER_WINDOW = 3

        private val COINS = intPreferencesKey("coins")
        private val DIAMONDS = intPreferencesKey("diamonds")

        private val CONVERSION_DATE =
            stringPreferencesKey("conversion_date_ist")

        private val CONVERSIONS_USED =
            intPreferencesKey("conversions_used_today")

        // Retained for migration from the previous rolling-window implementation.
        private val LEGACY_REDEEM_TIMESTAMPS =
            stringPreferencesKey("redeem_timestamps")
    }

    override val coins: Flow<Int> =
        context.walletDataStore.data.map { it[COINS] ?: 0 }

    override val diamonds: Flow<Int> =
        context.walletDataStore.data.map { it[DIAMONDS] ?: 0 }

    override suspend fun addCoins(amount: Int) {
        if (amount <= 0) return

        context.walletDataStore.edit { prefs ->
            prefs[COINS] = (prefs[COINS] ?: 0) + amount
        }
    }

    /**
     * Atomically checks and spends Coins.
     * A failed spend never changes the wallet balance.
     */
    override suspend fun spendCoins(amount: Int): RedeemResult {
        if (amount <= 0) {
            return RedeemResult(false, "Amount must be greater than zero.")
        }

        var result = RedeemResult(false, "")

        context.walletDataStore.edit { prefs ->
            val currentCoins = prefs[COINS] ?: 0

            result = if (currentCoins < amount) {
                RedeemResult(
                    false,
                    "You need $amount Coins to play."
                )
            } else {
                prefs[COINS] = currentCoins - amount

                RedeemResult(
                    true,
                    "Successfully spent $amount Coins."
                )
            }
        }

        return result
    }

    override suspend fun addDiamonds(amount: Int) {
        if (amount <= 0) return

        context.walletDataStore.edit { prefs ->
            prefs[DIAMONDS] = (prefs[DIAMONDS] ?: 0) + amount
        }
    }

    override suspend fun getRedeemStatus(): RedeemStatus {
        var status: RedeemStatus? = null

        context.walletDataStore.edit { prefs ->
            val today = todayIst()
            val used = readAndRefreshDailyUsage(prefs, today)

            val coins = prefs[COINS] ?: 0
            val diamonds = prefs[DIAMONDS] ?: 0

            status = RedeemStatus(
                coins = coins,
                diamonds = diamonds,
                redemptionsUsedToday = used,
                maxRedemptionsPerWindow = MAX_REDEMPTIONS_PER_WINDOW,
                nextUnlockMillis = if (
                    used >= MAX_REDEMPTIONS_PER_WINDOW
                ) {
                    nextIstMidnightMillis()
                } else {
                    null
                }
            )
        }

        return checkNotNull(status)
    }

    /**
     * Converts 500 Coins into 1 Gem.
     * A successful conversion consumes one of the shared daily slots.
     */
    override suspend fun redeemDiamond(): RedeemResult {
        var result = RedeemResult(false, "")

        context.walletDataStore.edit { prefs ->
            val today = todayIst()
            val used = readAndRefreshDailyUsage(prefs, today)
            val currentCoins = prefs[COINS] ?: 0

            result = when {
                used >= MAX_REDEMPTIONS_PER_WINDOW -> RedeemResult(
                    false,
                    "You've used all 3 conversions for today. Try again after midnight IST."
                )

                currentCoins < COST_PER_DIAMOND -> RedeemResult(
                    false,
                    "You need $COST_PER_DIAMOND Coins to convert into 1 Gem."
                )

                else -> {
                    prefs[COINS] = currentCoins - COST_PER_DIAMOND
                    prefs[DIAMONDS] = (prefs[DIAMONDS] ?: 0) + 1
                    prefs[CONVERSION_DATE] = today
                    prefs[CONVERSIONS_USED] = used + 1

                    RedeemResult(
                        true,
                        "Successfully converted 500 Coins into 1 Gem."
                    )
                }
            }
        }

        return result
    }

    /**
     * Converts 1 Gem into 500 Coins.
     * Uses the same daily conversion limit as Coins -> Gems.
     */
    override suspend fun convertGemToCoins(): RedeemResult {
        var result = RedeemResult(false, "")

        context.walletDataStore.edit { prefs ->
            val today = todayIst()
            val used = readAndRefreshDailyUsage(prefs, today)
            val currentDiamonds = prefs[DIAMONDS] ?: 0

            result = when {
                used >= MAX_REDEMPTIONS_PER_WINDOW -> RedeemResult(
                    false,
                    "You've used all 3 conversions for today. Try again after midnight IST."
                )

                currentDiamonds < 1 -> RedeemResult(
                    false,
                    "You need 1 Gem to convert into 500 Coins."
                )

                else -> {
                    prefs[DIAMONDS] = currentDiamonds - 1
                    prefs[COINS] =
                        (prefs[COINS] ?: 0) + COINS_PER_DIAMOND

                    prefs[CONVERSION_DATE] = today
                    prefs[CONVERSIONS_USED] = used + 1

                    RedeemResult(
                        true,
                        "Successfully converted 1 Gem into 500 Coins."
                    )
                }
            }
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
                RedeemResult(
                    false,
                    "You need $amount Gems to buy this item."
                )
            } else {
                prefs[DIAMONDS] = current - amount

                RedeemResult(
                    true,
                    "Purchase successful!"
                )
            }
        }

        return result
    }

    /**
     * Clears the local balance after guest wallet migration.
     */
    suspend fun clear() {
        context.walletDataStore.edit { prefs ->
            prefs.remove(COINS)
            prefs.remove(DIAMONDS)
            prefs.remove(CONVERSION_DATE)
            prefs.remove(CONVERSIONS_USED)
            prefs.remove(LEGACY_REDEEM_TIMESTAMPS)
        }
    }

    /**
     * Reads the current IST-day counter.
     *
     * If this is an existing installation without the new counter,
     * migrate today's legacy timestamps into the new daily counter.
     */
    private fun readAndRefreshDailyUsage(
        prefs: androidx.datastore.preferences.core.MutablePreferences,
        today: String
    ): Int {
        val storedDate = prefs[CONVERSION_DATE]

        if (storedDate == today) {
            return (prefs[CONVERSIONS_USED] ?: 0)
                .coerceIn(0, MAX_REDEMPTIONS_PER_WINDOW)
        }

        val migratedCount = if (storedDate == null) {
            parseTimestamps(prefs[LEGACY_REDEEM_TIMESTAMPS])
                .count { formatIstDate(it) == today }
                .coerceIn(0, MAX_REDEMPTIONS_PER_WINDOW)
        } else {
            0
        }

        prefs[CONVERSION_DATE] = today
        prefs[CONVERSIONS_USED] = migratedCount
        prefs.remove(LEGACY_REDEEM_TIMESTAMPS)

        return migratedCount
    }

    private fun parseTimestamps(raw: String?): List<Long> {
        if (raw.isNullOrBlank()) return emptyList()

        return raw.split(",").mapNotNull { it.toLongOrNull() }
    }

    private fun todayIst(): String =
        formatIstDate(System.currentTimeMillis())

    private fun formatIstDate(timestamp: Long): String {
        val formatter = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).apply {
            timeZone = TimeZone.getTimeZone(IST_TIME_ZONE)
        }

        return formatter.format(Date(timestamp))
    }

    private fun nextIstMidnightMillis(): Long {
        return Calendar.getInstance(
            TimeZone.getTimeZone(IST_TIME_ZONE),
            Locale.US
        ).apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}