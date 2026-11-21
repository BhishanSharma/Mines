package com.genoma.mines.core.remoteconfig

/**
 * Typed snapshot of remotely configurable, non-authoritative app settings.
 *
 * Every value is validated and clamped by [RemoteConfigParser]; the defaults
 * here are the safe values used when Remote Config is unavailable. See
 * docs/REMOTE_CONFIG.md for ranges, activation timing and owners.
 *
 * Remote Config controls presentation and configurable rules. It must never
 * be treated as proof that a wallet transaction or purchase is valid.
 */
data class RemoteConfigSnapshot(
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String = DEFAULT_MAINTENANCE_MESSAGE,

    val easyWinReward: Int = 50,
    val mediumWinReward: Int = 50,
    val hardWinReward: Int = 50,

    // Entry fee charged on every finished game, win or loss. The key keeps
    // its original name `loss_coin_fee` so existing console values still apply.
    val lossCoinFee: Int = 20,
    val minimumStartCoins: Int = 20,
    val conversionLimitDaily: Int = 3,

    // Normal rewarded-ad bonus offered after a game win.
    val rewardedAdBonus: Int = 50,
    val rewardedAdBonusEnabled: Boolean = false,

    val luckAdEnabled: Boolean = false,
    val luckAdCoinReward: Int = 0,
    val luckAdGemReward: Int = 0,
    val luckAdCoinProbability: Double = 0.0,
    val luckAdGemProbability: Double = 0.0,
    val luckAdNoRewardProbability: Double = 100.0,
    val luckAdPrizesJson: String = "",

    // Rewarded ad that recovers one Life from the Hearts dialog (Stage 3D).
    val lifeRecoveryAdEnabled: Boolean = false,

    // Rewarded-ad frequency caps and cooldowns (Stage 3G).
    val winBonusAdDailyCap: Int = 10,
    val lifeRecoveryAdDailyCap: Int = 3,
    val lifeRecoveryAdCooldownMinutes: Int = 30,
    val luckAdDailyCap: Int = 5,
    val luckAdCooldownMinutes: Int = 60,
    val rewardedAdsDailyCapTotal: Int = 15,

    val eventBannerEnabled: Boolean = false,
    val eventBannerText: String = "",

    val storeFeaturedItem: String = "",
    val newThemeEnabled: Boolean = false,
    val luckAdsFeatureEnabled: Boolean = false
) {
    /** Both flags must be enabled before Luck Ads can be offered. */
    val isLuckAdsAvailable: Boolean
        get() = luckAdsFeatureEnabled && luckAdEnabled

    /** The win bonus is available only when explicitly enabled and positive. */
    val isWinAdBonusAvailable: Boolean
        get() = rewardedAdBonusEnabled && rewardedAdBonus > 0

    /** The event banner shows only when enabled and it has text. */
    val isEventBannerVisible: Boolean
        get() = eventBannerEnabled && eventBannerText.isNotBlank()

    companion object {
        const val DEFAULT_MAINTENANCE_MESSAGE =
            "The game is temporarily unavailable. Please try again soon."

        val DEFAULT = RemoteConfigSnapshot()
    }
}
