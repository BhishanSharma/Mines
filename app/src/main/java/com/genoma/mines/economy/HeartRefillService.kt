package com.genoma.mines.economy

import com.genoma.mines.core.economy.WalletTransaction
import com.genoma.mines.core.economy.WalletTransactionType
import com.genoma.mines.life.data.LifeDataStore
import com.genoma.mines.life.data.remote.FirestoreLifeRepository
import com.genoma.mines.life.domain.LifeCalculator
import com.genoma.mines.life.domain.LifeRules
import com.genoma.mines.session.SessionManager
import com.genoma.mines.session.UserSession
import com.genoma.mines.wallet.data.ChargeOutcome
import com.genoma.mines.wallet.data.CoinWalletDataStore
import com.genoma.mines.wallet.data.remote.WalletTransactionDocs
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.UUID

sealed interface HeartRefillResult {
    data class Refilled(val heartsAdded: Int, val cost: Int) : HeartRefillResult
    data object AlreadyFull : HeartRefillResult
    data class InsufficientGems(val cost: Int, val missingHearts: Int) : HeartRefillResult
}

/**
 * Refills hearts with Gems so the charge and the refill happen together.
 *
 * Signed in, both live on `users/{uid}`, so a single Firestore transaction
 * checks the hearts, charges the Gems, refills and writes the ledger entry.
 * Guest stores are separate files, so a failed refill refunds the charge.
 */
class HeartRefillService(
    private val sessionManager: SessionManager,
    private val guestWallet: CoinWalletDataStore,
    private val guestLife: LifeDataStore,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val clock: () -> Long = System::currentTimeMillis
) {

    suspend fun refill(): HeartRefillResult =
        when (val session = sessionManager.currentSession) {
            is UserSession.Authenticated -> refillForAccount(session.firebaseUid)

            UserSession.Guest -> refillForGuest()
        }

    private suspend fun refillForGuest(): HeartRefillResult {
        val status = guestLife.getStatus()
        if (status.isFull) return HeartRefillResult.AlreadyFull

        val missing = status.maxHearts - status.hearts
        val cost = LifeRules.refillCost(status.hearts)

        return when (
            guestWallet.chargeOnce(
                operationId = "refill-${UUID.randomUUID()}",
                gemDebit = cost,
                type = WalletTransactionType.HEART_REFILL
            )
        ) {
            ChargeOutcome.INSUFFICIENT_FUNDS -> HeartRefillResult.InsufficientGems(cost, missing)

            else -> {
                try {
                    guestLife.refillHearts()
                } catch (exception: Exception) {
                    guestWallet.addDiamonds(cost)
                    throw exception
                }
                HeartRefillResult.Refilled(missing, cost)
            }
        }
    }

    private suspend fun refillForAccount(uid: String): HeartRefillResult {
        val userDoc = WalletTransactionDocs.userDoc(firestore, uid)
        val operationId = "refill-${UUID.randomUUID()}"
        val ledgerDoc = WalletTransactionDocs.transactionDoc(firestore, uid, operationId)

        return firestore.runTransaction { transaction ->
            val snapshot = transaction.get(userDoc)
            val now = clock()
            val status = LifeCalculator.status(FirestoreLifeRepository.lifeSnapshotOf(snapshot), now)

            if (status.isFull) return@runTransaction HeartRefillResult.AlreadyFull

            val missing = status.maxHearts - status.hearts
            val cost = LifeRules.refillCost(status.hearts)
            val gems = snapshot.getLong("diamonds") ?: 0L

            if (gems < cost) return@runTransaction HeartRefillResult.InsufficientGems(cost, missing)

            transaction.set(
                userDoc,
                mapOf("diamonds" to gems - cost) +
                        FirestoreLifeRepository.fieldsOf(LifeCalculator.refill(now)),
                SetOptions.merge()
            )
            transaction.set(
                ledgerDoc,
                WalletTransactionDocs.toFields(
                    WalletTransaction(
                        id = operationId,
                        type = WalletTransactionType.HEART_REFILL,
                        coinsDelta = 0,
                        gemsDelta = -cost,
                        livesDelta = missing,
                        createdAtMillis = now
                    )
                )
            )
            HeartRefillResult.Refilled(missing, cost)
        }.await()
    }
}
