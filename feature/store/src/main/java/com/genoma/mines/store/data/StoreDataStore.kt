package com.genoma.mines.store.data
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.storeDataStore by preferencesDataStore(name = "store_inventory")

/** Persists owned store items and the currently equipped cosmetic selections, for guest sessions. */
class StoreDataStore(private val context: Context) : StoreRepository {

    companion object {
        private val OWNED_ITEMS = stringPreferencesKey("owned_item_ids")
        private val EQUIPPED_BOARD_THEME = stringPreferencesKey("equipped_board_theme")
        private val EQUIPPED_CELL_SKIN = stringPreferencesKey("equipped_cell_skin")
        private const val DEFAULT_BOARD_THEME = "board_classic_teal"
    }

    override val ownedItemIds: Flow<Set<String>> = context.storeDataStore.data.map { prefs ->
        prefs[OWNED_ITEMS]
            .orEmpty()
            .split(',')
            .filter { it.isNotBlank() }
            .toSet()
    }

    override val equippedBoardThemeId: Flow<String> = context.storeDataStore.data.map { prefs ->
        prefs[EQUIPPED_BOARD_THEME] ?: DEFAULT_BOARD_THEME
    }

    override val equippedCellSkinId: Flow<String?> = context.storeDataStore.data.map { prefs ->
        prefs[EQUIPPED_CELL_SKIN]
    }

    override suspend fun addOwnedItem(itemId: String) {
        context.storeDataStore.edit { prefs ->
            val owned = prefs[OWNED_ITEMS]
                .orEmpty()
                .split(',')
                .filter { it.isNotBlank() }
                .toMutableSet()
            owned.add(itemId)
            prefs[OWNED_ITEMS] = owned.joinToString(",")
        }
    }

    override suspend fun equipBoardTheme(itemId: String) {
        context.storeDataStore.edit { prefs ->
            prefs[EQUIPPED_BOARD_THEME] = itemId
        }
    }

    override suspend fun equipCellSkin(itemId: String) {
        context.storeDataStore.edit { prefs ->
            prefs[EQUIPPED_CELL_SKIN] = itemId
        }
    }

    override suspend fun unequipCellSkin() {
        context.storeDataStore.edit { prefs ->
            prefs.remove(EQUIPPED_CELL_SKIN)
        }
    }

    override suspend fun isOwned(itemId: String): Boolean =
        ownedItemIds.first().contains(itemId)

    /**
     * Clears everything stored locally. Called after a guest's owned
     * items/equipped selections have been folded into a Firestore account
     * on first sign-in, so this device's DataStore doesn't resurface stale
     * guest state if anything ever falls back to reading it.
     */
    suspend fun clear() {
        context.storeDataStore.edit { prefs ->
            prefs.remove(OWNED_ITEMS)
            prefs.remove(EQUIPPED_BOARD_THEME)
            prefs.remove(EQUIPPED_CELL_SKIN)
        }
    }
}
