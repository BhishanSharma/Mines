package com.genoma.mines.core.remoteconfig

/**
 * A typed snapshot of remotely configurable, non-authoritative app settings.
 *
 * These values may control presentation and configurable game rules, but
 * must never be used as proof that a wallet transaction or purchase is valid.
 */
data class RemoteConfigSnapshot(
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String = "The game is temporarily unavailable. Please try again soon.",

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

    val eventBannerEnabled: Boolean = false,
    val eventBannerText: String = "",

    val storeFeaturedItem: String = "",
    val newThemeEnabled: Boolean = false,
    val luckAdsFeatureEnabled: Boolean = false
)