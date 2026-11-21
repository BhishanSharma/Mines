package com.genoma.mines.wallet.data.remote

import com.genoma.mines.core.economy.WalletTransaction
import com.genoma.mines.core.economy.WalletTransactionType
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Firestore layout for the wallet ledger:
 * `users/{uid}/walletTransactions/{operationId}`.
 *
 * The document id is the operation's idempotency key, so a transaction that
 * finds the document already present knows the operation was applied.
 */
object WalletTransactionDocs {

    const val USERS = "users"
    const val COLLECTION = "walletTransactions"

    fun userDoc(firestore: FirebaseFirestore, uid: String) =
        firestore.collection(USERS).document(uid)

    fun transactionDoc(firestore: FirebaseFirestore, uid: String, operationId: String) =
        userDoc(firestore, uid).collection(COLLECTION).document(operationId)

    fun toFields(transaction: WalletTransaction): Map<String, Any> = mapOf(
        "type" to transaction.type.name,
        "coinsDelta" to transaction.coinsDelta,
        "gemsDelta" to transaction.gemsDelta,
        "livesDelta" to transaction.livesDelta,
        "createdAtMillis" to transaction.createdAtMillis,
        "createdAt" to FieldValue.serverTimestamp(),
        "source" to "client"
    )

    fun fromDocument(document: DocumentSnapshot): WalletTransaction? {
        val type = document.getString("type")?.let(WalletTransactionType::fromName) ?: return null
        return WalletTransaction(
            id = document.id,
            type = type,
            coinsDelta = document.getLong("coinsDelta")?.toInt() ?: 0,
            gemsDelta = document.getLong("gemsDelta")?.toInt() ?: 0,
            livesDelta = document.getLong("livesDelta")?.toInt() ?: 0,
            createdAtMillis = document.getLong("createdAtMillis")
                ?: document.getTimestamp("createdAt")?.toDate()?.time
                ?: 0L
        )
    }
}
