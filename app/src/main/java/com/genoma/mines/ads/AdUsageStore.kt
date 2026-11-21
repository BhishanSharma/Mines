package com.genoma.mines.ads

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.genoma.mines.core.economy.AdFrequencyRules
import com.genoma.mines.core.economy.AdUsage
import com.genoma.mines.core.economy.RewardedAdPlacement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.adUsageDataStore by preferencesDataStore(name = "rewarded_ad_usage")

/**
 * Completed rewarded ads per IST day, for caps and cooldowns (Stage 3G).
 *
 * Kept per device, not per account, so switching accounts doesn't reset
 * the limits. Reinstalling the app or moving the clock to a new day can
 * reset them; caps are tuned through Remote Config.
 */
class AdUsageStore(private val context: Context) {

    private companion object {
        val USAGE = stringPreferencesKey("usage")
    }

    val usage: Flow<AdUsage> =
        context.adUsageDataStore.data.map { AdFrequencyRules.decode(it[USAGE]) }

    suspend fun recordRewarded(placement: RewardedAdPlacement, nowMillis: Long) {
        context.adUsageDataStore.edit { prefs ->
            val current = AdFrequencyRules.decode(prefs[USAGE])
            prefs[USAGE] = AdFrequencyRules.encode(AdFrequencyRules.record(placement, current, nowMillis))
        }
    }
}
