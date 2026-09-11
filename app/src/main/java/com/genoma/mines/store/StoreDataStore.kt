package com.genoma.mines.store

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.storeDataStore by preferencesDataStore(name = "store_inventory")

/** Persists owned store items and the currently equipped cosmetic selections. */
class StoreDataStore(private val context: Context) {

    companion object {
        private val OWNED_ITEMS = stringPreferencesKey("owned_item_ids")
        private val EQUIPPED_BOARD_THEME = stringPreferencesKey("equipped_board_theme")
        private val EQUIPPED_CELL_SKIN = stringPreferencesKey("equipped_cell_skin")
        private const val DEFAULT_BOARD_THEME = "board_classic_teal"
    }

    val ownedItemIds: Flow<Set<String>> = context.storeDataStore.data.map { prefs ->
        prefs[OWNED_ITEMS]
            .orEmpty()
            .split(',')
            .filter { it.isNotBlank() }
            .toSet()
    }

    val equippedBoardThemeId: Flow<String> = context.storeDataStore.data.map { prefs ->
        prefs[EQUIPPED_BOARD_THEME] ?: DEFAULT_BOARD_THEME
    }

    val equippedCellSkinId: Flow<String?> = context.storeDataStore.data.map { prefs ->
        prefs[EQUIPPED_CELL_SKIN]
    }

    suspend fun addOwnedItem(itemId: String) {
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

    suspend fun equipBoardTheme(itemId: String) {
        context.storeDataStore.edit { prefs ->
            prefs[EQUIPPED_BOARD_THEME] = itemId
        }
    }

    suspend fun equipCellSkin(itemId: String) {
        context.storeDataStore.edit { prefs ->
            prefs[EQUIPPED_CELL_SKIN] = itemId
        }
    }

    suspend fun unequipCellSkin() {
        context.storeDataStore.edit { prefs ->
            prefs.remove(EQUIPPED_CELL_SKIN)
        }
    }

    suspend fun isOwned(itemId: String): Boolean =
        ownedItemIds.first().contains(itemId)
}
