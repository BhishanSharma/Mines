package com.genoma.mines.core.remoteconfig

import android.util.Log
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Central access point for Firebase Remote Config.
 *
 * Responsibilities:
 * - Register safe in-app defaults.
 * - Fetch and activate remote values.
 * - Listen for real-time Remote Config updates.
 * - Expose a typed StateFlow snapshot to the rest of the app.
 *
 * This class does not perform wallet transactions, grant rewards, verify
 * purchases, or enforce backend economy rules.
 */
class RemoteConfigManager(
    private val remoteConfig: FirebaseRemoteConfig =
        FirebaseRemoteConfig.getInstance()
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _snapshot = MutableStateFlow(RemoteConfigSnapshot())
    val snapshot: StateFlow<RemoteConfigSnapshot> = _snapshot.asStateFlow()

    @Volatile
    private var initialized = false

    /**
     * Starts initialization once.
     *
     * Defaults are registered before fetching. If fetching fails, the app
     * continues using the defaults and any previously activated values.
     */
    fun initialize() {
        if (initialized) return
        initialized = true

        configureFetchSettings()
        registerRealTimeListener()

        scope.launch {
            try {
                remoteConfig.setDefaultsAsync(DEFAULTS).await()
                remoteConfig.fetchAndActivate().await()
                publishSnapshot()
            } catch (exception: Exception) {
                Log.w(TAG, "Remote Config initialization failed; using available defaults", exception)
                publishSnapshot()
            }
        }
    }

    private fun configureFetchSettings() {
        val settings = FirebaseRemoteConfigSettings.Builder()
            // Production-safe default. Real-time updates can still arrive
            // independently of this normal fetch interval.
            .setMinimumFetchIntervalInSeconds(DEFAULT_FETCH_INTERVAL_SECONDS)
            .build()

        remoteConfig.setConfigSettingsAsync(settings)
    }

    private fun registerRealTimeListener() {
        remoteConfig.addOnConfigUpdateListener(
            object : ConfigUpdateListener {

                override fun onUpdate(configUpdate: ConfigUpdate) {
                    remoteConfig.activate()
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                publishSnapshot()
                            } else {
                                Log.w(
                                    TAG,
                                    "Could not activate real-time Remote Config update",
                                    task.exception
                                )
                            }
                        }
                }

                override fun onError(error: FirebaseRemoteConfigException) {
                    Log.w(TAG, "Remote Config real-time listener error", error)
                }
            }
        )
    }

    private fun publishSnapshot() {
        _snapshot.value = RemoteConfigSnapshot(
            maintenanceMode = remoteConfig.getBoolean(KEY_MAINTENANCE_MODE),
            maintenanceMessage = remoteConfig.getString(KEY_MAINTENANCE_MESSAGE)
                .ifBlank { DEFAULT_MAINTENANCE_MESSAGE },

            easyWinReward = remoteConfig.getLong(KEY_EASY_WIN_REWARD)
                .toSafeInt(),
            mediumWinReward = remoteConfig.getLong(KEY_MEDIUM_WIN_REWARD)
                .toSafeInt(),
            hardWinReward = remoteConfig.getLong(KEY_HARD_WIN_REWARD)
                .toSafeInt(),

            lossCoinFee = remoteConfig.getLong(KEY_LOSS_COIN_FEE)
                .toSafeInt(),
            minimumStartCoins = remoteConfig.getLong(KEY_MINIMUM_START_COINS)
                .toSafeInt(),
            conversionLimitDaily = remoteConfig.getLong(KEY_CONVERSION_LIMIT_DAILY)
                .toSafeInt(),
            rewardedAdBonus = remoteConfig.getLong(KEY_REWARDED_AD_BONUS)
                .toSafeInt(),

            luckAdEnabled = remoteConfig.getBoolean(KEY_LUCK_AD_ENABLED),
            luckAdCoinReward = remoteConfig.getLong(KEY_LUCK_AD_COIN_REWARD)
                .toSafeInt(),
            luckAdGemReward = remoteConfig.getLong(KEY_LUCK_AD_GEM_REWARD)
                .toSafeInt(),
            luckAdCoinProbability = remoteConfig.getDouble(KEY_LUCK_AD_COIN_PROBABILITY)
                .coerceIn(0.0, 100.0),
            luckAdGemProbability = remoteConfig.getDouble(KEY_LUCK_AD_GEM_PROBABILITY)
                .coerceIn(0.0, 100.0),
            luckAdNoRewardProbability = remoteConfig.getDouble(KEY_LUCK_AD_NO_REWARD_PROBABILITY)
                .coerceIn(0.0, 100.0),

            eventBannerEnabled = remoteConfig.getBoolean(KEY_EVENT_BANNER_ENABLED),
            eventBannerText = remoteConfig.getString(KEY_EVENT_BANNER_TEXT),

            storeFeaturedItem = remoteConfig.getString(KEY_STORE_FEATURED_ITEM),
            newThemeEnabled = remoteConfig.getBoolean(KEY_NEW_THEME_ENABLED),
            luckAdsFeatureEnabled = remoteConfig.getBoolean(KEY_LUCK_ADS_FEATURE_ENABLED)
        )
    }

    private fun Long.toSafeInt(): Int =
        coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()

    companion object {
        private const val TAG = "RemoteConfigManager"

        private const val DEFAULT_FETCH_INTERVAL_SECONDS = 3600L

        private const val DEFAULT_MAINTENANCE_MESSAGE =
            "The game is temporarily unavailable. Please try again soon."

        private const val KEY_MAINTENANCE_MODE = "maintenance_mode"
        private const val KEY_MAINTENANCE_MESSAGE = "maintenance_message"

        private const val KEY_EASY_WIN_REWARD = "easy_win_reward"
        private const val KEY_MEDIUM_WIN_REWARD = "medium_win_reward"
        private const val KEY_HARD_WIN_REWARD = "hard_win_reward"

        private const val KEY_LOSS_COIN_FEE = "loss_coin_fee"
        private const val KEY_MINIMUM_START_COINS = "minimum_start_coins"
        private const val KEY_CONVERSION_LIMIT_DAILY = "conversion_limit_daily"
        private const val KEY_REWARDED_AD_BONUS = "rewarded_ad_bonus"

        private const val KEY_LUCK_AD_ENABLED = "luck_ad_enabled"
        private const val KEY_LUCK_AD_COIN_REWARD = "luck_ad_coin_reward"
        private const val KEY_LUCK_AD_GEM_REWARD = "luck_ad_gem_reward"
        private const val KEY_LUCK_AD_COIN_PROBABILITY = "luck_ad_coin_probability"
        private const val KEY_LUCK_AD_GEM_PROBABILITY = "luck_ad_gem_probability"
        private const val KEY_LUCK_AD_NO_REWARD_PROBABILITY =
            "luck_ad_no_reward_probability"

        private const val KEY_EVENT_BANNER_ENABLED = "event_banner_enabled"
        private const val KEY_EVENT_BANNER_TEXT = "event_banner_text"

        private const val KEY_STORE_FEATURED_ITEM = "store_featured_item"
        private const val KEY_NEW_THEME_ENABLED = "new_theme_enabled"
        private const val KEY_LUCK_ADS_FEATURE_ENABLED = "feature_luck_ads_enabled"

        private val DEFAULTS: Map<String, Any> = mapOf(
            KEY_MAINTENANCE_MODE to false,
            KEY_MAINTENANCE_MESSAGE to DEFAULT_MAINTENANCE_MESSAGE,

            KEY_EASY_WIN_REWARD to 50L,
            KEY_MEDIUM_WIN_REWARD to 50L,
            KEY_HARD_WIN_REWARD to 50L,

            KEY_LOSS_COIN_FEE to 20L,
            KEY_MINIMUM_START_COINS to 20L,
            KEY_CONVERSION_LIMIT_DAILY to 3L,
            KEY_REWARDED_AD_BONUS to 50L,

            KEY_LUCK_AD_ENABLED to false,
            KEY_LUCK_AD_COIN_REWARD to 0L,
            KEY_LUCK_AD_GEM_REWARD to 0L,
            KEY_LUCK_AD_COIN_PROBABILITY to 0.0,
            KEY_LUCK_AD_GEM_PROBABILITY to 0.0,
            KEY_LUCK_AD_NO_REWARD_PROBABILITY to 100.0,

            KEY_EVENT_BANNER_ENABLED to false,
            KEY_EVENT_BANNER_TEXT to "",

            KEY_STORE_FEATURED_ITEM to "",
            KEY_NEW_THEME_ENABLED to false,
            KEY_LUCK_ADS_FEATURE_ENABLED to false
        )
    }
}