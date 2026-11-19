
package com.genoma.mines.core.remoteconfig

/**
 * Typed snapshot of remotely configurable, non-authoritative app settings.
 *
 * Remote Config controls presentation and configurable rules. It must never
 * be treated as proof that a wallet transaction or purchase is valid.
 */
data class RemoteConfigSnapshot(
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String =
        "The game is temporarily unavailable. Please try again soon.",

    val easyWinReward: Int = 50,
    val mediumWinReward: Int = 50,
    val hardWinReward: Int = 50,

    val lossCoinFee: Int = 20,
    val minimumStartCoins: Int = 20,
    val conversionLimitDaily: Int = 3,
    val rewardedAdBonus: Int = 50,

    val luckAdEnabled: Boolean = false,
    val luckAdCoinReward: Int = 0,
    val luckAdGemReward: Int = 0,
    val luckAdCoinProbability: Double = 0.0,
    val luckAdGemProbability: Double = 0.0,
    val luckAdNoRewardProbability: Double = 100.0,
    val luckAdPrizesJson: String = "",

    val eventBannerEnabled: Boolean = false,
    val eventBannerText: String = "",

    val storeFeaturedItem: String = "",
    val newThemeEnabled: Boolean = false,
    val luckAdsFeatureEnabled: Boolean = false
) {
    /**
     * Both flags must be enabled before Luck Ads can be offered.
     */
    val isLuckAdsAvailable: Boolean
        get() = luckAdsFeatureEnabled && luckAdEnabled
}
