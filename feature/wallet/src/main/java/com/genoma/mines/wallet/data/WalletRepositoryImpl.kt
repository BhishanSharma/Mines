package com.genoma.mines.wallet.data

import com.genoma.mines.core.economy.ConversionDirection
import com.genoma.mines.core.economy.WalletTransaction
import com.genoma.mines.session.SessionManager
import com.genoma.mines.session.UserSession
import com.genoma.mines.wallet.data.remote.FirestoreWalletRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class WalletRepositoryImpl(
    private val sessionManager: SessionManager,
    private val guestWallet: CoinWalletDataStore,
    private val firestoreWallet: FirestoreWalletRepository
) : WalletRepository {

    private val walletSnapshot: Flow<WalletSnapshot> =
        sessionManager.sessionFlow.flatMapLatest { session ->
            when (session) {
                is UserSession.Authenticated ->
                    firestoreWallet.observeWallet(session.firebaseUid)

                UserSession.Guest ->
                    combine(guestWallet.coins, guestWallet.diamonds) { coins, diamonds ->
                        WalletSnapshot(coins = coins, diamonds = diamonds)
                    }
            }
        }

    override val coins: Flow<Int> = walletSnapshot.map { it.coins }

    override val diamonds: Flow<Int> = walletSnapshot.map { it.diamonds }

    override val history: Flow<List<WalletTransaction>> =
        sessionManager.sessionFlow.flatMapLatest { session ->
            when (session) {
                is UserSession.Authenticated -> firestoreWallet.observeHistory(session.firebaseUid)
                UserSession.Guest -> guestWallet.history
            }
        }

    override suspend fun addCoins(amount: Int) {
        when (val session = sessionManager.currentSession) {
            is UserSession.Authenticated -> firestoreWallet.addCoins(session.firebaseUid, amount)
            UserSession.Guest -> guestWallet.addCoins(amount)
        }
    }

    override suspend fun spendCoins(amount: Int): RedeemResult =
        when (val session = sessionManager.currentSession) {
            is UserSession.Authenticated -> firestoreWallet.spendCoins(session.firebaseUid, amount)
            UserSession.Guest -> guestWallet.spendCoins(amount)
        }

    override suspend fun addDiamonds(amount: Int) {
        when (val session = sessionManager.currentSession) {
            is UserSession.Authenticated -> firestoreWallet.addDiamonds(session.firebaseUid, amount)
            UserSession.Guest -> guestWallet.addDiamonds(amount)
        }
    }

    override suspend fun getRedeemStatus(dailyLimit: Int): RedeemStatus =
        when (val session = sessionManager.currentSession) {
            is UserSession.Authenticated ->
                firestoreWallet.getRedeemStatus(session.firebaseUid, dailyLimit)

            UserSession.Guest -> guestWallet.getRedeemStatus(dailyLimit)
        }

    override suspend fun spendDiamonds(amount: Int): RedeemResult =
        when (val session = sessionManager.currentSession) {
            is UserSession.Authenticated -> firestoreWallet.spendDiamonds(session.firebaseUid, amount)
            UserSession.Guest -> guestWallet.spendDiamonds(amount)
        }

    override suspend fun convert(
        direction: ConversionDirection,
        dailyLimit: Int,
        requestId: String
    ): RedeemResult =
        when (val session = sessionManager.currentSession) {
            is UserSession.Authenticated ->
                firestoreWallet.convert(session.firebaseUid, direction, dailyLimit, requestId)

            UserSession.Guest -> guestWallet.convert(direction, dailyLimit, requestId)
        }
}
