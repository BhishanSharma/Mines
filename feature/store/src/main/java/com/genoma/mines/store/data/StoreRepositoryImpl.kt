package com.genoma.mines.store.data
import com.genoma.mines.store.data.remote.FirestoreStoreRepository
import com.genoma.mines.session.SessionManager
import com.genoma.mines.session.UserSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class StoreRepositoryImpl(
    private val sessionManager: SessionManager,
    private val guestStore: StoreDataStore,
    private val firestoreStore: FirestoreStoreRepository
) : StoreRepository {

    private val storeSnapshot: Flow<StoreSnapshot> =
        sessionManager.sessionFlow.flatMapLatest { session ->
            when (session) {
                is UserSession.Authenticated ->
                    firestoreStore.observeStore(session.firebaseUid)

                UserSession.Guest ->
                    combine(
                        guestStore.ownedItemIds,
                        guestStore.equippedBoardThemeId,
                        guestStore.equippedCellSkinId
                    ) { ownedItemIds, equippedBoardThemeId, equippedCellSkinId ->
                        StoreSnapshot(
                            ownedItemIds = ownedItemIds,
                            equippedBoardThemeId = equippedBoardThemeId,
                            equippedCellSkinId = equippedCellSkinId
                        )
                    }
            }
        }

    override val ownedItemIds: Flow<Set<String>> =
        storeSnapshot.map { it.ownedItemIds }

    override val equippedBoardThemeId: Flow<String> =
        storeSnapshot.map { it.equippedBoardThemeId }

    override val equippedCellSkinId: Flow<String?> =
        storeSnapshot.map { it.equippedCellSkinId }

    override suspend fun isOwned(itemId: String): Boolean {
        return when (val session = sessionManager.currentSession) {
            is UserSession.Authenticated ->
                firestoreStore.isOwned(session.firebaseUid, itemId)

            UserSession.Guest ->
                guestStore.isOwned(itemId)
        }
    }

    override suspend fun addOwnedItem(itemId: String) {
        when (val session = sessionManager.currentSession) {
            is UserSession.Authenticated ->
                firestoreStore.addOwnedItem(session.firebaseUid, itemId)

            UserSession.Guest ->
                guestStore.addOwnedItem(itemId)
        }
    }

    override suspend fun equipBoardTheme(itemId: String) {
        when (val session = sessionManager.currentSession) {
            is UserSession.Authenticated ->
                firestoreStore.equipBoardTheme(session.firebaseUid, itemId)

            UserSession.Guest ->
                guestStore.equipBoardTheme(itemId)
        }
    }

    override suspend fun equipCellSkin(itemId: String) {
        when (val session = sessionManager.currentSession) {
            is UserSession.Authenticated ->
                firestoreStore.equipCellSkin(session.firebaseUid, itemId)

            UserSession.Guest ->
                guestStore.equipCellSkin(itemId)
        }
    }

    override suspend fun unequipCellSkin() {
        when (val session = sessionManager.currentSession) {
            is UserSession.Authenticated ->
                firestoreStore.unequipCellSkin(session.firebaseUid)

            UserSession.Guest ->
                guestStore.unequipCellSkin()
        }
    }
}
