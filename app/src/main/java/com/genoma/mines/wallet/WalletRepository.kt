package com.genoma.mines.wallet

import kotlinx.coroutines.flow.Flow
interface WalletRepository {
    val coins: Flow<Int>
    val diamonds: Flow<Int>

    suspend fun addCoins(amount: Int)
    suspend fun getRedeemStatus(): RedeemStatus
    suspend fun redeemDiamond(): RedeemResult

    suspend fun spendDiamonds(amount: Int): RedeemResult
}