package com.genoma.mines.economy

import com.genoma.mines.core.economy.CosmeticPurchaseDecision
import com.genoma.mines.core.economy.CosmeticPurchaseRules
import com.genoma.mines.core.economy.WalletTransaction
import com.genoma.mines.core.economy.WalletTransactionType
import com.genoma.mines.session.SessionManager
import com.genoma.mines.session.UserSession
import com.genoma.mines.store.data.StoreDataStore
import com.genoma.mines.wallet.data.ChargeOutcome
import com.genoma.mines.wallet.data.CoinWalletDataStore
import com.genoma.mines.wallet.data.remote.WalletTransactionDocs
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

sealed interface StorePurchaseResult {
    data object Purchased : StorePurchaseResult
    data object AlreadyOwned : StorePurchaseResult
    data class InsufficientGems(val gemsShort: Int) : StorePurchaseResult
}

/**
 * Buys a cosmetic so that payment and permanent ownership happen together,
 * exactly once (plan §1.6, Stage 4).
 *
 * - Signed in: one Firestore transaction on `users/{uid}` deducts the Gems,
 *   adds the item to `ownedStoreItemIds` and writes the ledger entry
 *   `walletTransactions/purchase-<itemId>`. An owned item is never charged.
 * - Guest: the wallet charge is recorded under `purchase-<itemId>`, so it
 *   can happen only once, and [reconcileGuestPurchases] restores ownership
 *   if the app stopped between the charge and the ownership write.
 */
class StorePurchaseService(
    private val sessionManager: SessionManager,
    private val guestWallet: CoinWalletDataStore,
    private val guestStore: StoreDataStore,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val clock: () -> Long = System::currentTimeMillis
) {

    suspend fun purchase(itemId: String, price: Int): StorePurchaseResult =
        when (val session = sessionManager.currentSession) {
            is UserSession.Authenticated -> purchaseForAccount(session.firebaseUid, itemId, price)

            UserSession.Guest -> purchaseForGuest(itemId, price)
        }

    /** Restores ownership for guest purchases that were charged but not recorded as owned. */
    suspend fun reconcileGuestPurchases() {
        val owned = guestStore.ownedItemIds.first()
        guestWallet.appliedOperationIds()
            .filter { it.startsWith(CoinWalletDataStore.PURCHASE_ID_PREFIX) }
            .map { it.removePrefix(CoinWalletDataStore.PURCHASE_ID_PREFIX) }
            .filter { it.isNotBlank() && it !in owned }
            .forEach { guestStore.addOwnedItem(it) }
    }

    private suspend fun purchaseForGuest(itemId: String, price: Int): StorePurchaseResult {
        if (price <= 0 || guestStore.isOwned(itemId)) return StorePurchaseResult.AlreadyOwned

        return when (
            guestWallet.chargeOnce(
                operationId = CosmeticPurchaseRules.operationId(itemId),
                gemDebit = price,
                type = WalletTransactionType.STORE_PURCHASE
            )
        ) {
            ChargeOutcome.CHARGED -> {
                guestStore.addOwnedItem(itemId)
                StorePurchaseResult.Purchased
            }

            ChargeOutcome.ALREADY_CHARGED -> {
                guestStore.addOwnedItem(itemId)
                StorePurchaseResult.AlreadyOwned
            }

            ChargeOutcome.INSUFFICIENT_FUNDS -> {
                val gems = guestWallet.diamonds.first()
                StorePurchaseResult.InsufficientGems((price - gems).coerceAtLeast(1))
            }
        }
    }

    private suspend fun purchaseForAccount(
        uid: String,
        itemId: String,
        price: Int
    ): StorePurchaseResult {
        val userDoc = WalletTransactionDocs.userDoc(firestore, uid)
        val operationId = CosmeticPurchaseRules.operationId(itemId)
        val ledgerDoc = WalletTransactionDocs.transactionDoc(firestore, uid, operationId)

        return firestore.runTransaction { transaction ->
            val alreadyCharged = transaction.get(ledgerDoc).exists()
            val snapshot = transaction.get(userDoc)
            val owned = (snapshot.get(FIELD_OWNED) as? List<*>)
                ?.filterIsInstance<String>()
                .orEmpty()
            val gems = snapshot.getLong(FIELD_GEMS) ?: 0L

            if (alreadyCharged && itemId !in owned) {
                // Charged before but ownership is missing: repair, never re-charge.
                transaction.set(userDoc, mapOf(FIELD_OWNED to owned + itemId), SetOptions.merge())
                return@runTransaction StorePurchaseResult.AlreadyOwned
            }

            when (val decision = CosmeticPurchaseRules.decide(itemId in owned, price, gems)) {
                CosmeticPurchaseDecision.AlreadyOwned -> StorePurchaseResult.AlreadyOwned

                is CosmeticPurchaseDecision.InsufficientGems ->
                    StorePurchaseResult.InsufficientGems(decision.gemsShort)

                is CosmeticPurchaseDecision.Purchase -> {
                    transaction.set(
                        userDoc,
                        mapOf(
                            FIELD_GEMS to gems - decision.gemDebit,
                            FIELD_OWNED to owned + itemId
                        ),
                        SetOptions.merge()
                    )
                    transaction.set(
                        ledgerDoc,
                        WalletTransactionDocs.toFields(
                            WalletTransaction(
                                id = operationId,
                                type = WalletTransactionType.STORE_PURCHASE,
                                coinsDelta = 0,
                                gemsDelta = -decision.gemDebit,
                                livesDelta = 0,
                                createdAtMillis = clock()
                            )
                        )
                    )
                    StorePurchaseResult.Purchased
                }
            }
        }.await()
    }

    private companion object {
        const val FIELD_GEMS = "diamonds"
        const val FIELD_OWNED = "ownedStoreItemIds"
    }
}
