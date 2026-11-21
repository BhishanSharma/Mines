package com.genoma.mines.economy

import com.genoma.mines.core.economy.GameSettlementRules
import com.genoma.mines.core.economy.PendingSettlement
import com.genoma.mines.core.economy.WalletTransaction
import com.genoma.mines.life.data.remote.FirestoreLifeRepository
import com.genoma.mines.life.domain.LifeCalculator
import com.genoma.mines.wallet.data.remote.WalletTransactionDocs
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Applies a settlement to `users/{uid}` in a single Firestore transaction.
 *
 * Coins, Gems and hearts live on the same document, so the whole result
 * (for a loss: the fee and the Life) is applied atomically. The ledger
 * document `walletTransactions/{settlement.id}` is written in the same
 * transaction and doubles as the idempotency marker.
 */
class FirestoreSettlementRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val clock: () -> Long = System::currentTimeMillis
) {

    /** Returns true when applied now, false when it had already been applied. */
    suspend fun apply(uid: String, settlement: PendingSettlement): Boolean {
        val userDoc = WalletTransactionDocs.userDoc(firestore, uid)
        val ledgerDoc = WalletTransactionDocs.transactionDoc(firestore, uid, settlement.id)
        val plan = settlement.plan

        return firestore.runTransaction { transaction ->
            if (transaction.get(ledgerDoc).exists()) return@runTransaction false

            val snapshot = transaction.get(userDoc)
            val coinsBefore = snapshot.getLong("coins") ?: 0L
            val gemsBefore = snapshot.getLong("diamonds") ?: 0L
            val coinsAfter = GameSettlementRules.applyCoins(coinsBefore, plan)
            val gemsAfter = GameSettlementRules.applyGems(gemsBefore, plan)

            val updates = mutableMapOf<String, Any>()
            if (coinsAfter != coinsBefore) updates["coins"] = coinsAfter
            if (gemsAfter != gemsBefore) updates["diamonds"] = gemsAfter

            if (plan.lifeDebit > 0 || plan.lifeCredit > 0) {
                val lifeAfter = LifeCalculator.applyChange(
                    snapshot = FirestoreLifeRepository.lifeSnapshotOf(snapshot),
                    nowMillis = clock(),
                    lifeDebit = plan.lifeDebit,
                    lifeCredit = plan.lifeCredit
                )
                updates += FirestoreLifeRepository.fieldsOf(lifeAfter)
            }

            if (updates.isNotEmpty()) {
                transaction.set(userDoc, updates, SetOptions.merge())
            }

            transaction.set(
                ledgerDoc,
                WalletTransactionDocs.toFields(
                    WalletTransaction(
                        id = settlement.id,
                        type = settlement.type,
                        coinsDelta = (coinsAfter - coinsBefore).toInt(),
                        gemsDelta = (gemsAfter - gemsBefore).toInt(),
                        livesDelta = plan.lifeCredit - plan.lifeDebit,
                        createdAtMillis = settlement.createdAtMillis
                    )
                )
            )

            true
        }.await()
    }
}
