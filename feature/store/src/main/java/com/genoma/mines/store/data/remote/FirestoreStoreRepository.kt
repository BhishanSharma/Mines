package com.genoma.mines.store.data.remote
import com.genoma.mines.store.data.StoreSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private const val DEFAULT_BOARD_THEME = "board_classic_teal"

class FirestoreStoreRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun userDoc(uid: String) = firestore.collection("users").document(uid)

    /** Live owned items + equipped selections, updating in real time as they change server-side. */
    fun observeStore(uid: String): Flow<StoreSnapshot> = callbackFlow {
        val registration = userDoc(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            @Suppress("UNCHECKED_CAST")
            val ownedItemIds = (snapshot?.get("ownedStoreItemIds") as? List<String>)
                ?.toSet()
                .orEmpty()

            trySend(
                StoreSnapshot(
                    ownedItemIds = ownedItemIds,
                    equippedBoardThemeId = snapshot?.getString("equippedBoardThemeId")
                        ?: DEFAULT_BOARD_THEME,
                    equippedCellSkinId = snapshot?.getString("equippedCellSkinId")
                )
            )
        }

        awaitClose { registration.remove() }
    }

    suspend fun isOwned(uid: String, itemId: String): Boolean {
        val snapshot = userDoc(uid).get().await()

        @Suppress("UNCHECKED_CAST")
        val ownedItemIds = snapshot.get("ownedStoreItemIds") as? List<String>
        return ownedItemIds?.contains(itemId) == true
    }

    suspend fun addOwnedItem(uid: String, itemId: String) {
        userDoc(uid).set(
            mapOf("ownedStoreItemIds" to FieldValue.arrayUnion(itemId)),
            SetOptions.merge()
        ).await()
    }

    suspend fun equipBoardTheme(uid: String, itemId: String) {
        userDoc(uid).set(
            mapOf("equippedBoardThemeId" to itemId),
            SetOptions.merge()
        ).await()
    }

    suspend fun equipCellSkin(uid: String, itemId: String) {
        userDoc(uid).set(
            mapOf("equippedCellSkinId" to itemId),
            SetOptions.merge()
        ).await()
    }

    suspend fun unequipCellSkin(uid: String) {
        userDoc(uid).set(
            mapOf("equippedCellSkinId" to FieldValue.delete()),
            SetOptions.merge()
        ).await()
    }

    /**
     * Folds a guest's owned items and equipped selections into this
     * (brand new) account on first sign-in.
     *
     * Only call this right after `ensureUserDocument` reports the account
     * as brand new — merging guest store state into an account that
     * already owns items isn't handled here.
     */
    suspend fun migrateGuestStoreItems(
        uid: String,
        ownedItemIds: Set<String>,
        equippedBoardThemeId: String,
        equippedCellSkinId: String?
    ) {
        if (ownedItemIds.isEmpty()) return

        val updates = mutableMapOf<String, Any>(
            "ownedStoreItemIds" to FieldValue.arrayUnion(*ownedItemIds.toTypedArray()),
            "equippedBoardThemeId" to equippedBoardThemeId
        )

        if (equippedCellSkinId != null) {
            updates["equippedCellSkinId"] = equippedCellSkinId
        }

        userDoc(uid).set(updates, SetOptions.merge()).await()
    }
}
