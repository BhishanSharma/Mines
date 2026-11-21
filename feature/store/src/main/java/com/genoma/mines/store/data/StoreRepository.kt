package com.genoma.mines.store.data
import kotlinx.coroutines.flow.Flow

/** Owned store items and equipped cosmetic selections, for whichever session is active. */
interface StoreRepository {
    val ownedItemIds: Flow<Set<String>>
    val equippedBoardThemeId: Flow<String>
    val equippedCellSkinId: Flow<String?>

    /** Equipped avatar id (see AvatarOption), or null when never chosen on this account. */
    val equippedAvatarId: Flow<String?>

    suspend fun isOwned(itemId: String): Boolean
    suspend fun addOwnedItem(itemId: String)
    suspend fun equipBoardTheme(itemId: String)
    suspend fun equipCellSkin(itemId: String)
    suspend fun unequipCellSkin()
    suspend fun equipAvatar(avatarId: String)
}
