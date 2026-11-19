package com.genoma.mines.wallet.data.remote

import com.genoma.mines.wallet.data.CoinWalletDataStore
import com.genoma.mines.wallet.data.RedeemResult
import com.genoma.mines.wallet.data.RedeemStatus
import com.genoma.mines.wallet.data.WalletSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val IST_TIME_ZONE = "Asia/Kolkata"

class FirestoreWalletRepository(
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance()
) {

    private fun userDoc(uid: String) =
        firestore.collection("users").document(uid)

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
                            coins = snapshot
                                ?.getLong("coins")
                                ?.toInt() ?: 0,
                            diamonds = snapshot
                                ?.getLong("diamonds")
                                ?.toInt() ?: 0
                        )
                    )
                }

            awaitClose {
                registration.remove()
            }
        }

    suspend fun addCoins(uid: String, amount: Int) {
        if (amount <= 0) return

        userDoc(uid).set(
            mapOf(
                "coins" to FieldValue.increment(amount.toLong())
            ),
            SetOptions.merge()
        ).await()
    }

    suspend fun addDiamonds(uid: String, amount: Int) {
        if (amount <= 0) return

        userDoc(uid).set(
            mapOf(
                "diamonds" to FieldValue.increment(amount.toLong())
            ),
            SetOptions.merge()
        ).await()
    }

    /**
     * Atomically checks and spends Coins.
     * A failed spend never changes the wallet balance.
     */
    suspend fun spendCoins(
        uid: String,
        amount: Int
    ): RedeemResult {
        if (amount <= 0) {
            return RedeemResult(false, "Amount must be greater than zero.")
        }

        return firestore.runTransaction { transaction ->
            val document = userDoc(uid)
            val snapshot = transaction.get(document)
            val currentCoins = snapshot.getLong("coins") ?: 0L

            if (currentCoins < amount.toLong()) {
                RedeemResult(
                    false,
                    "You need $amount Coins to play."
                )
            } else {
                transaction.set(
                    document,
                    mapOf(
                        "coins" to currentCoins - amount.toLong()
                    ),
                    SetOptions.merge()
                )

                RedeemResult(
                    true,
                    "Successfully spent $amount Coins."
                )
            }
        }.await()
    }

    suspend fun getRedeemStatus(uid: String): RedeemStatus {
        val snapshot = userDoc(uid).get(Source.SERVER).await()

        val today = todayIst()
        val storedDate = snapshot.getString("conversionDateIst")

        val used = if (storedDate == today) {
            (snapshot.getLong("conversionsUsedToday") ?: 0L)
                .toInt()
                .coerceIn(
                    0,
                    CoinWalletDataStore.MAX_REDEMPTIONS_PER_WINDOW
                )
        } else {
            0
        }

        return RedeemStatus(
            coins = (snapshot.getLong("coins") ?: 0L).toInt(),
            diamonds = (snapshot.getLong("diamonds") ?: 0L).toInt(),
            redemptionsUsedToday = used,
            maxRedemptionsPerWindow =
                CoinWalletDataStore.MAX_REDEMPTIONS_PER_WINDOW,
            nextUnlockMillis = if (
                used >= CoinWalletDataStore.MAX_REDEMPTIONS_PER_WINDOW
            ) {
                nextIstMidnightMillis()
            } else {
                null
            }
        )
    }

    /**
     * Converts 500 Coins into 1 Gem.
     *
     * This is an atomic Firestore transaction, but it is still invoked
     * by the Android client. A trusted backend should replace this
     * transaction before production release.
     */
    suspend fun redeemDiamond(uid: String): RedeemResult {
        return firestore.runTransaction { transaction ->
            val document = userDoc(uid)
            val snapshot = transaction.get(document)

            val today = todayIst()
            val used = readDailyUsage(snapshot, today)
            val currentCoins = snapshot.getLong("coins") ?: 0L

            when {
                used >= CoinWalletDataStore.MAX_REDEMPTIONS_PER_WINDOW ->
                    RedeemResult(
                        false,
                        "You've used all 3 conversions for today. Try again after midnight IST."
                    )

                currentCoins < CoinWalletDataStore.COST_PER_DIAMOND ->
                    RedeemResult(
                        false,
                        "You need 500 Coins to convert into 1 Gem."
                    )

                else -> {
                    transaction.set(
                        document,
                        mapOf(
                            "coins" to currentCoins -
                                    CoinWalletDataStore.COST_PER_DIAMOND,
                            "diamonds" to FieldValue.increment(1),
                            "conversionDateIst" to today,
                            "conversionsUsedToday" to used + 1
                        ),
                        SetOptions.merge()
                    )

                    RedeemResult(
                        true,
                        "Successfully converted 500 Coins into 1 Gem."
                    )
                }
            }
        }.await()
    }

    /**
     * Converts 1 Gem into 500 Coins.
     * Shares the daily conversion count with Coins -> Gems.
     */
    suspend fun convertGemToCoins(uid: String): RedeemResult {
        return firestore.runTransaction { transaction ->
            val document = userDoc(uid)
            val snapshot = transaction.get(document)

            val today = todayIst()
            val used = readDailyUsage(snapshot, today)
            val currentDiamonds = snapshot.getLong("diamonds") ?: 0L

            when {
                used >= CoinWalletDataStore.MAX_REDEMPTIONS_PER_WINDOW ->
                    RedeemResult(
                        false,
                        "You've used all 3 conversions for today. Try again after midnight IST."
                    )

                currentDiamonds < 1L ->
                    RedeemResult(
                        false,
                        "You need 1 Gem to convert into 500 Coins."
                    )

                else -> {
                    val currentCoins =
                        snapshot.getLong("coins") ?: 0L

                    transaction.set(
                        document,
                        mapOf(
                            "diamonds" to currentDiamonds - 1L,
                            "coins" to currentCoins +
                                    CoinWalletDataStore.COINS_PER_DIAMOND,
                            "conversionDateIst" to today,
                            "conversionsUsedToday" to used + 1
                        ),
                        SetOptions.merge()
                    )

                    RedeemResult(
                        true,
                        "Successfully converted 1 Gem into 500 Coins."
                    )
                }
            }
        }.await()
    }

    suspend fun spendDiamonds(
        uid: String,
        amount: Int
    ): RedeemResult {
        if (amount <= 0) {
            return RedeemResult(true, "")
        }

        return firestore.runTransaction { transaction ->
            val document = userDoc(uid)
            val snapshot = transaction.get(document)
            val currentDiamonds =
                snapshot.getLong("diamonds") ?: 0L

            if (currentDiamonds < amount) {
                RedeemResult(
                    false,
                    "You need $amount Gems to buy this item."
                )
            } else {
                transaction.update(
                    document,
                    "diamonds",
                    currentDiamonds - amount
                )

                RedeemResult(
                    true,
                    "Purchase successful!"
                )
            }
        }.await()
    }

    suspend fun migrateGuestWallet(
        uid: String,
        coins: Int,
        diamonds: Int
    ) {
        if (coins <= 0 && diamonds <= 0) return

        userDoc(uid).set(
            mapOf(
                "coins" to FieldValue.increment(coins.toLong()),
                "diamonds" to FieldValue.increment(diamonds.toLong())
            ),
            SetOptions.merge()
        ).await()
    }

    private fun readDailyUsage(
        snapshot: com.google.firebase.firestore.DocumentSnapshot,
        today: String
    ): Int {
        if (snapshot.getString("conversionDateIst") != today) {
            return 0
        }

        return (snapshot.getLong("conversionsUsedToday") ?: 0L)
            .toInt()
            .coerceIn(
                0,
                CoinWalletDataStore.MAX_REDEMPTIONS_PER_WINDOW
            )
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