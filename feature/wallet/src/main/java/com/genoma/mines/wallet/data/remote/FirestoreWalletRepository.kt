package com.genoma.mines.wallet.data.remote

import com.genoma.mines.core.economy.ConversionDecision
import com.genoma.mines.core.economy.ConversionDirection
import com.genoma.mines.core.economy.ConversionRules
import com.genoma.mines.core.economy.IstCalendar
import com.genoma.mines.core.economy.WalletTransaction
import com.genoma.mines.core.economy.WalletTransactionType
import com.genoma.mines.wallet.data.RedeemResult
import com.genoma.mines.wallet.data.RedeemStatus
import com.genoma.mines.wallet.data.WalletSnapshot
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Wallet for signed-in players, on `users/{uid}`.
 *
 * Conversions run in Firestore transactions and write a ledger document in
 * `walletTransactions/{requestId}` in the same transaction, which makes them
 * atomic and idempotent.
 */
class FirestoreWalletRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val clock: () -> Long = System::currentTimeMillis
) {

    private companion object {
        const val FIELD_COINS = "coins"
        const val FIELD_DIAMONDS = "diamonds"
        const val FIELD_CONVERSION_DATE = "conversionDateIst"
        const val FIELD_CONVERSIONS_USED = "conversionsUsedToday"

        // Written by the previous rolling-window implementation.
        const val FIELD_LEGACY_REDEEM_TIMESTAMPS = "redeemTimestamps"

        const val HISTORY_LIMIT = 50L
    }

    private fun userDoc(uid: String) = WalletTransactionDocs.userDoc(firestore, uid)

    /** Live Coins and Gems balance. */
    fun observeWallet(uid: String): Flow<WalletSnapshot> =
        callbackFlow {
            val registration =
                userDoc(uid).addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    trySend(
                        WalletSnapshot(
                            coins = snapshot?.getLong(FIELD_COINS)?.toInt() ?: 0,
                            diamonds = snapshot?.getLong(FIELD_DIAMONDS)?.toInt() ?: 0
                        )
                    )
                }

            awaitClose { registration.remove() }
        }

    /** Most recent ledger entries, newest first. */
    fun observeHistory(uid: String): Flow<List<WalletTransaction>> =
        callbackFlow {
            val registration = userDoc(uid)
                .collection(WalletTransactionDocs.COLLECTION)
                .orderBy("createdAtMillis", Query.Direction.DESCENDING)
                .limit(HISTORY_LIMIT)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        // History is informational; never break the wallet over it.
                        trySend(emptyList())
                        return@addSnapshotListener
                    }

                    trySend(
                        snapshot?.documents
                            ?.mapNotNull(WalletTransactionDocs::fromDocument)
                            .orEmpty()
                    )
                }

            awaitClose { registration.remove() }
        }

    suspend fun addCoins(uid: String, amount: Int) {
        if (amount <= 0) return

        userDoc(uid).set(
            mapOf(FIELD_COINS to FieldValue.increment(amount.toLong())),
            SetOptions.merge()
        ).await()
    }

    suspend fun addDiamonds(uid: String, amount: Int) {
        if (amount <= 0) return

        userDoc(uid).set(
            mapOf(FIELD_DIAMONDS to FieldValue.increment(amount.toLong())),
            SetOptions.merge()
        ).await()
    }

    /** Atomically checks and spends Coins. A failed spend never changes the balance. */
    suspend fun spendCoins(uid: String, amount: Int): RedeemResult {
        if (amount <= 0) {
            return RedeemResult(false, "Amount must be greater than zero.")
        }

        return firestore.runTransaction { transaction ->
            val document = userDoc(uid)
            val currentCoins = transaction.get(document).getLong(FIELD_COINS) ?: 0L

            if (currentCoins < amount.toLong()) {
                RedeemResult(false, "You need $amount Coins.")
            } else {
                transaction.set(
                    document,
                    mapOf(FIELD_COINS to currentCoins - amount.toLong()),
                    SetOptions.merge()
                )
                RedeemResult(true, "Spent $amount Coins.")
            }
        }.await()
    }

    /**
     * Conversion allowance for today. Uses the default source, so it falls
     * back to the local cache when offline instead of throwing.
     */
    suspend fun getRedeemStatus(uid: String, dailyLimit: Int): RedeemStatus {
        val snapshot = userDoc(uid).get().await()
        val now = clock()

        return RedeemStatus.of(
            coins = snapshot.getLong(FIELD_COINS) ?: 0L,
            diamonds = snapshot.getLong(FIELD_DIAMONDS) ?: 0L,
            usedToday = snapshot.usedToday(IstCalendar.dateOf(now)),
            dailyLimit = dailyLimit,
            nowMillis = now
        )
    }

    /**
     * Converts Coins <-> Gems in one transaction that also writes the ledger
     * entry `walletTransactions/{requestId}`. If that entry already exists
     * the request was applied before and nothing changes.
     */
    suspend fun convert(
        uid: String,
        direction: ConversionDirection,
        dailyLimit: Int,
        requestId: String
    ): RedeemResult {
        val now = clock()
        val today = IstCalendar.dateOf(now)
        val document = userDoc(uid)
        val ledgerDoc = WalletTransactionDocs.transactionDoc(firestore, uid, requestId)

        return firestore.runTransaction { transaction ->
            if (transaction.get(ledgerDoc).exists()) {
                return@runTransaction RedeemResult(true, "That conversion was already completed.")
            }

            val snapshot = transaction.get(document)
            val coins = snapshot.getLong(FIELD_COINS) ?: 0L
            val gems = snapshot.getLong(FIELD_DIAMONDS) ?: 0L
            val used = snapshot.usedToday(today)

            val decision = ConversionRules.decide(direction, coins, gems, used, dailyLimit)
            val message = ConversionRules.message(decision, direction, dailyLimit)

            if (decision !is ConversionDecision.Approved) {
                return@runTransaction RedeemResult(false, message)
            }

            transaction.set(
                document,
                mapOf(
                    FIELD_COINS to coins + decision.coinsDelta,
                    FIELD_DIAMONDS to gems + decision.gemsDelta,
                    FIELD_CONVERSION_DATE to today,
                    FIELD_CONVERSIONS_USED to decision.usedTodayAfter,
                    FIELD_LEGACY_REDEEM_TIMESTAMPS to FieldValue.delete()
                ),
                SetOptions.merge()
            )
            transaction.set(
                ledgerDoc,
                WalletTransactionDocs.toFields(
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
            )

            RedeemResult(true, message)
        }.await()
    }

    suspend fun spendDiamonds(uid: String, amount: Int): RedeemResult {
        if (amount <= 0) {
            return RedeemResult(true, "")
        }

        return firestore.runTransaction { transaction ->
            val document = userDoc(uid)
            val currentDiamonds = transaction.get(document).getLong(FIELD_DIAMONDS) ?: 0L

            if (currentDiamonds < amount) {
                RedeemResult(false, "You need $amount Gems.")
            } else {
                transaction.set(
                    document,
                    mapOf(FIELD_DIAMONDS to currentDiamonds - amount),
                    SetOptions.merge()
                )
                RedeemResult(true, "Purchase successful!")
            }
        }.await()
    }

    /**
     * Folds a guest wallet into the account exactly once.
     *
     * [migrationId] identifies this device's guest data. The ledger entry
     * written in the same transaction stops a retried sign-in (for example
     * after the local clear failed) from crediting the same Coins twice.
     * The guest's conversions used today carry over, so signing in never
     * grants a fresh allowance.
     */
    suspend fun migrateGuestWallet(
        uid: String,
        migrationId: String,
        coins: Int,
        diamonds: Int,
        conversionsUsedToday: Int
    ) {
        if (coins <= 0 && diamonds <= 0 && conversionsUsedToday <= 0) return

        val now = clock()
        val today = IstCalendar.dateOf(now)
        val document = userDoc(uid)
        val ledgerDoc = WalletTransactionDocs.transactionDoc(firestore, uid, migrationId)

        firestore.runTransaction { transaction ->
            if (transaction.get(ledgerDoc).exists()) return@runTransaction

            val snapshot = transaction.get(document)
            val usedOnAccount = snapshot.usedToday(today)

            transaction.set(
                document,
                mapOf(
                    FIELD_COINS to (snapshot.getLong(FIELD_COINS) ?: 0L) + coins.coerceAtLeast(0),
                    FIELD_DIAMONDS to (snapshot.getLong(FIELD_DIAMONDS) ?: 0L) + diamonds.coerceAtLeast(0),
                    FIELD_CONVERSION_DATE to today,
                    FIELD_CONVERSIONS_USED to usedOnAccount + conversionsUsedToday.coerceAtLeast(0)
                ),
                SetOptions.merge()
            )
            transaction.set(
                ledgerDoc,
                WalletTransactionDocs.toFields(
                    WalletTransaction(
                        id = migrationId,
                        type = WalletTransactionType.GUEST_MIGRATION,
                        coinsDelta = coins.coerceAtLeast(0),
                        gemsDelta = diamonds.coerceAtLeast(0),
                        livesDelta = 0,
                        createdAtMillis = now
                    )
                )
            )
        }.await()
    }

    private fun DocumentSnapshot.usedToday(today: String): Int =
        ConversionRules.usedToday(
            storedDate = getString(FIELD_CONVERSION_DATE),
            storedCount = getLong(FIELD_CONVERSIONS_USED),
            legacyTimestamps = (get(FIELD_LEGACY_REDEEM_TIMESTAMPS) as? List<*>)
                ?.mapNotNull { (it as? Number)?.toLong() }
                .orEmpty(),
            todayIst = today
        )
}
