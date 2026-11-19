package com.genoma.mines.wallet.data

import kotlinx.coroutines.flow.Flow

interface WalletRepository {
    val coins: Flow<Int>
    val diamonds: Flow<Int>

    suspend fun addCoins(amount: Int)
    suspend fun getRedeemStatus(): RedeemStatus

    /** Converts 500 Coins into 1 Gem. */
    suspend fun redeemDiamond(): RedeemResult

    /** Converts 1 Gem into 500 Coins. */
    suspend fun convertGemToCoins(): RedeemResult

    suspend fun spendDiamonds(amount: Int): RedeemResult

    /** Returns Gems to the balance, e.g. refunding a purchase that failed. */
    suspend fun addDiamonds(amount: Int)
}