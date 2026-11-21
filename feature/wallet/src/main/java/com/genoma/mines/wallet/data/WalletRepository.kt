package com.genoma.mines.wallet.data

import com.genoma.mines.core.economy.ConversionDirection
import com.genoma.mines.core.economy.WalletTransaction
import kotlinx.coroutines.flow.Flow

interface WalletRepository {
    val coins: Flow<Int>
    val diamonds: Flow<Int>

    /** Most recent wallet changes, newest first. */
    val history: Flow<List<WalletTransaction>>

    suspend fun addCoins(amount: Int)
    suspend fun spendCoins(amount: Int): RedeemResult

    /** Conversion allowance for today, using the configured [dailyLimit]. */
    suspend fun getRedeemStatus(dailyLimit: Int): RedeemStatus

    /**
     * Converts between Coins and Gems (500:1) within the shared daily
     * allowance.
     *
     * [requestId] is an idempotency key: repeating a call with the same id
     * (a double tap, a retry after a timeout) never converts twice.
     */
    suspend fun convert(
        direction: ConversionDirection,
        dailyLimit: Int,
        requestId: String
    ): RedeemResult

    suspend fun spendDiamonds(amount: Int): RedeemResult

    /** Returns Gems to the balance, e.g. refunding a purchase that failed. */
    suspend fun addDiamonds(amount: Int)
}
