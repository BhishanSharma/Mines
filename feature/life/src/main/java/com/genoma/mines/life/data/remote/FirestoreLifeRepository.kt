package com.genoma.mines.life.data.remote
import com.genoma.mines.life.domain.LifeCalculator
import com.genoma.mines.life.domain.LifeSnapshot
import com.genoma.mines.life.domain.LifeStatus
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Account hearts, kept on the same `users/{uid}` document as the wallet.
 * A document without these fields (existing or brand-new account) reads as full.
 */
class FirestoreLifeRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val clock: () -> Long = System::currentTimeMillis
) {

    private companion object {
        const val FIELD_HEARTS = "hearts"
        const val FIELD_REGEN_ANCHOR = "heartsRegenAnchor"
    }

    private fun userDoc(uid: String) = firestore.collection("users").document(uid)

    fun observeLife(uid: String): Flow<LifeSnapshot> = callbackFlow {
        val registration = userDoc(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            trySend(snapshot.toLifeSnapshot())
        }

        awaitClose { registration.remove() }
    }

    suspend fun getStatus(uid: String): LifeStatus {
        val snapshot = userDoc(uid).get(Source.SERVER).await()
        return LifeCalculator.status(snapshot.toLifeSnapshot(), clock())
    }

    suspend fun consumeHeart(uid: String): Boolean {
        return firestore.runTransaction { transaction ->
            val current = transaction.get(userDoc(uid)).toLifeSnapshot()
            val updated = LifeCalculator.consume(current, clock())

            if (updated == null) {
                false
            } else {
                transaction.set(userDoc(uid), updated.toFields(), SetOptions.merge())
                true
            }
        }.await()
    }

    suspend fun refundHeart(uid: String) {
        firestore.runTransaction { transaction ->
            val current = transaction.get(userDoc(uid)).toLifeSnapshot()
            transaction.set(
                userDoc(uid),
                LifeCalculator.refund(current, clock()).toFields(),
                SetOptions.merge()
            )
        }.await()
    }

    suspend fun refillHearts(uid: String) {
        userDoc(uid).set(
            LifeCalculator.refill(clock()).toFields(),
            SetOptions.merge()
        ).await()
    }

    private fun DocumentSnapshot?.toLifeSnapshot() = LifeSnapshot(
        storedHearts = this?.getLong(FIELD_HEARTS)?.toInt() ?: LifeSnapshot.FULL.storedHearts,
        regenAnchorMillis = this?.getLong(FIELD_REGEN_ANCHOR) ?: LifeSnapshot.FULL.regenAnchorMillis
    )

    private fun LifeSnapshot.toFields() = mapOf(
        FIELD_HEARTS to storedHearts,
        FIELD_REGEN_ANCHOR to regenAnchorMillis
    )
}
