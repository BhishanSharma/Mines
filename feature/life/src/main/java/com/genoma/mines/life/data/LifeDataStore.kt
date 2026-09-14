package com.genoma.mines.life.data
import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.genoma.mines.life.domain.LifeCalculator
import com.genoma.mines.life.domain.LifeSnapshot
import com.genoma.mines.life.domain.LifeStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.lifeDataStore by preferencesDataStore(name = "life")

/** Guest hearts, stored on-device. */
class LifeDataStore(
    private val context: Context,
    private val clock: () -> Long = System::currentTimeMillis
) : LifeRepository {

    private companion object {
        val HEARTS = intPreferencesKey("hearts")
        val REGEN_ANCHOR = longPreferencesKey("hearts_regen_anchor")
    }

    override val lifeSnapshot: Flow<LifeSnapshot> =
        context.lifeDataStore.data.map { it.toSnapshot() }

    override suspend fun getStatus(): LifeStatus =
        LifeCalculator.status(lifeSnapshot.first(), clock())

    override suspend fun consumeHeart(): Boolean {
        var consumed = false

        context.lifeDataStore.edit { prefs ->
            val updated = LifeCalculator.consume(prefs.toSnapshot(), clock()) ?: return@edit
            prefs.write(updated)
            consumed = true
        }

        return consumed
    }

    override suspend fun refundHeart() {
        context.lifeDataStore.edit { prefs ->
            prefs.write(LifeCalculator.refund(prefs.toSnapshot(), clock()))
        }
    }

    override suspend fun refillHearts() {
        context.lifeDataStore.edit { prefs ->
            prefs.write(LifeCalculator.refill(clock()))
        }
    }

    private fun Preferences.toSnapshot() = LifeSnapshot(
        storedHearts = this[HEARTS] ?: LifeSnapshot.FULL.storedHearts,
        regenAnchorMillis = this[REGEN_ANCHOR] ?: LifeSnapshot.FULL.regenAnchorMillis
    )

    private fun MutablePreferences.write(snapshot: LifeSnapshot) {
        this[HEARTS] = snapshot.storedHearts
        this[REGEN_ANCHOR] = snapshot.regenAnchorMillis
    }
}
