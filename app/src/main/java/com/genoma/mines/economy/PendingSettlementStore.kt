package com.genoma.mines.economy

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.genoma.mines.core.economy.EconomyCodec
import com.genoma.mines.core.economy.PendingSettlement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.pendingSettlementDataStore by preferencesDataStore(name = "pending_settlements")

/**
 * Durable queue of wallet/Life changes that have been earned but not yet
 * confirmed as applied.
 *
 * A settlement is written here before it is applied and removed only after
 * the target store confirms it. If the app is killed, the device goes
 * offline or a Firestore transaction fails, the settlement survives and is
 * retried. Each settlement carries an idempotency key, so retrying never
 * applies it twice.
 */
class PendingSettlementStore(private val context: Context) {

    private companion object {
        val PENDING = stringPreferencesKey("pending")
        const val MAX_PENDING = 200
    }

    val pending: Flow<List<PendingSettlement>> =
        context.pendingSettlementDataStore.data.map { EconomyCodec.decodeSettlements(it[PENDING]) }

    suspend fun all(): List<PendingSettlement> = pending.first()

    suspend fun add(settlement: PendingSettlement) {
        context.pendingSettlementDataStore.edit { prefs ->
            val current = EconomyCodec.decodeSettlements(prefs[PENDING])
            if (current.any { it.id == settlement.id }) return@edit
            prefs[PENDING] = EconomyCodec.encodeSettlements((current + settlement).takeLast(MAX_PENDING))
        }
    }

    suspend fun remove(id: String) {
        context.pendingSettlementDataStore.edit { prefs ->
            val current = EconomyCodec.decodeSettlements(prefs[PENDING])
            prefs[PENDING] = EconomyCodec.encodeSettlements(current.filterNot { it.id == id })
        }
    }
}
