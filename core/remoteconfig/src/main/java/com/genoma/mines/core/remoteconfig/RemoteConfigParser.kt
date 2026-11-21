package com.genoma.mines.core.remoteconfig

/** Raw string values by key; null means the key has no value at all. */
fun interface ConfigValueSource {
    fun rawValue(key: String): String?
}

/** One parameter that was missing, malformed or outside its allowed range. */
data class ConfigIssue(
    val key: String,
    val rawValue: String?,
    val problem: String
)

data class ParsedRemoteConfig(
    val snapshot: RemoteConfigSnapshot,
    val issues: List<ConfigIssue>
)

/**
 * Validates every Remote Config parameter (plan §0.5 rules 7 and 8).
 *
 * - A missing or malformed value falls back to its safe default.
 * - A numeric value outside its range is clamped to the nearest bound.
 * - Strings are trimmed and length-limited.
 *
 * Invalid configuration can therefore never crash the app or produce an
 * unsafe economy value. Every correction is reported in [ParsedRemoteConfig.issues].
 */
object RemoteConfigParser {

    object Keys {
        const val MAINTENANCE_MODE = "maintenance_mode"
        const val MAINTENANCE_MESSAGE = "maintenance_message"

        const val EASY_WIN_REWARD = "easy_win_reward"
        const val MEDIUM_WIN_REWARD = "medium_win_reward"
        const val HARD_WIN_REWARD = "hard_win_reward"

        const val LOSS_COIN_FEE = "loss_coin_fee"
        const val MINIMUM_START_COINS = "minimum_start_coins"
        const val CONVERSION_LIMIT_DAILY = "conversion_limit_daily"

        const val REWARDED_AD_BONUS = "rewarded_ad_bonus"
        const val REWARDED_AD_BONUS_ENABLED = "rewarded_ad_bonus_enabled"

        const val LUCK_AD_ENABLED = "luck_ad_enabled"
        const val LUCK_AD_COIN_REWARD = "luck_ad_coin_reward"
        const val LUCK_AD_GEM_REWARD = "luck_ad_gem_reward"
        const val LUCK_AD_COIN_PROBABILITY = "luck_ad_coin_probability"
        const val LUCK_AD_GEM_PROBABILITY = "luck_ad_gem_probability"
        const val LUCK_AD_NO_REWARD_PROBABILITY = "luck_ad_no_reward_probability"
        const val LUCK_AD_PRIZES = "luck_ad_prizes"

        const val LIFE_RECOVERY_AD_ENABLED = "life_recovery_ad_enabled"

        const val WIN_BONUS_AD_DAILY_CAP = "win_bonus_ad_daily_cap"
        const val LIFE_RECOVERY_AD_DAILY_CAP = "life_recovery_ad_daily_cap"
        const val LIFE_RECOVERY_AD_COOLDOWN_MINUTES = "life_recovery_ad_cooldown_minutes"
        const val LUCK_AD_DAILY_CAP = "luck_ad_daily_cap"
        const val LUCK_AD_COOLDOWN_MINUTES = "luck_ad_cooldown_minutes"
        const val REWARDED_ADS_DAILY_CAP_TOTAL = "rewarded_ads_daily_cap_total"

        const val EVENT_BANNER_ENABLED = "event_banner_enabled"
        const val EVENT_BANNER_TEXT = "event_banner_text"

        const val STORE_FEATURED_ITEM = "store_featured_item"
        const val NEW_THEME_ENABLED = "new_theme_enabled"
        const val LUCK_ADS_FEATURE_ENABLED = "feature_luck_ads_enabled"
    }

    /** Inclusive ranges for numeric parameters. */
    object Ranges {
        val WIN_REWARD = 0..1_000
        val LOSS_FEE = 0..1_000
        val MINIMUM_START_COINS = 0..10_000
        val CONVERSION_LIMIT = 0..20
        val AD_BONUS = 0..1_000
        val LUCK_COIN_REWARD = 0..100_000
        val LUCK_GEM_REWARD = 0..1_000
        val PROBABILITY = 0.0..100.0
        val DAILY_CAP = 0..100
        val COOLDOWN_MINUTES = 0..1_440
        const val MAX_MESSAGE_LENGTH = 300
        const val MAX_BANNER_LENGTH = 140
        const val MAX_ITEM_ID_LENGTH = 64
        const val MAX_PRIZES_JSON_LENGTH = 8_192
    }

    private val D = RemoteConfigSnapshot.DEFAULT

    /** In-app defaults registered with Firebase, as their string form. */
    val DEFAULTS: Map<String, Any> = mapOf(
        Keys.MAINTENANCE_MODE to D.maintenanceMode,
        Keys.MAINTENANCE_MESSAGE to D.maintenanceMessage,
        Keys.EASY_WIN_REWARD to D.easyWinReward.toLong(),
        Keys.MEDIUM_WIN_REWARD to D.mediumWinReward.toLong(),
        Keys.HARD_WIN_REWARD to D.hardWinReward.toLong(),
        Keys.LOSS_COIN_FEE to D.lossCoinFee.toLong(),
        Keys.MINIMUM_START_COINS to D.minimumStartCoins.toLong(),
        Keys.CONVERSION_LIMIT_DAILY to D.conversionLimitDaily.toLong(),
        Keys.REWARDED_AD_BONUS to D.rewardedAdBonus.toLong(),
        Keys.REWARDED_AD_BONUS_ENABLED to D.rewardedAdBonusEnabled,
        Keys.LUCK_AD_ENABLED to D.luckAdEnabled,
        Keys.LUCK_AD_COIN_REWARD to D.luckAdCoinReward.toLong(),
        Keys.LUCK_AD_GEM_REWARD to D.luckAdGemReward.toLong(),
        Keys.LUCK_AD_COIN_PROBABILITY to D.luckAdCoinProbability,
        Keys.LUCK_AD_GEM_PROBABILITY to D.luckAdGemProbability,
        Keys.LUCK_AD_NO_REWARD_PROBABILITY to D.luckAdNoRewardProbability,
        Keys.LUCK_AD_PRIZES to D.luckAdPrizesJson,
        Keys.LIFE_RECOVERY_AD_ENABLED to D.lifeRecoveryAdEnabled,
        Keys.WIN_BONUS_AD_DAILY_CAP to D.winBonusAdDailyCap.toLong(),
        Keys.LIFE_RECOVERY_AD_DAILY_CAP to D.lifeRecoveryAdDailyCap.toLong(),
        Keys.LIFE_RECOVERY_AD_COOLDOWN_MINUTES to D.lifeRecoveryAdCooldownMinutes.toLong(),
        Keys.LUCK_AD_DAILY_CAP to D.luckAdDailyCap.toLong(),
        Keys.LUCK_AD_COOLDOWN_MINUTES to D.luckAdCooldownMinutes.toLong(),
        Keys.REWARDED_ADS_DAILY_CAP_TOTAL to D.rewardedAdsDailyCapTotal.toLong(),
        Keys.EVENT_BANNER_ENABLED to D.eventBannerEnabled,
        Keys.EVENT_BANNER_TEXT to D.eventBannerText,
        Keys.STORE_FEATURED_ITEM to D.storeFeaturedItem,
        Keys.NEW_THEME_ENABLED to D.newThemeEnabled,
        Keys.LUCK_ADS_FEATURE_ENABLED to D.luckAdsFeatureEnabled
    )

    fun parse(source: ConfigValueSource): ParsedRemoteConfig {
        val issues = mutableListOf<ConfigIssue>()
        val reader = Reader(source, issues)

        val snapshot = RemoteConfigSnapshot(
            maintenanceMode = reader.bool(Keys.MAINTENANCE_MODE, D.maintenanceMode),
            maintenanceMessage = reader.string(
                Keys.MAINTENANCE_MESSAGE,
                D.maintenanceMessage,
                Ranges.MAX_MESSAGE_LENGTH
            ).ifBlank { D.maintenanceMessage },

            easyWinReward = reader.int(Keys.EASY_WIN_REWARD, D.easyWinReward, Ranges.WIN_REWARD),
            mediumWinReward = reader.int(Keys.MEDIUM_WIN_REWARD, D.mediumWinReward, Ranges.WIN_REWARD),
            hardWinReward = reader.int(Keys.HARD_WIN_REWARD, D.hardWinReward, Ranges.WIN_REWARD),

            lossCoinFee = reader.int(Keys.LOSS_COIN_FEE, D.lossCoinFee, Ranges.LOSS_FEE),
            minimumStartCoins = reader.int(
                Keys.MINIMUM_START_COINS,
                D.minimumStartCoins,
                Ranges.MINIMUM_START_COINS
            ),
            conversionLimitDaily = reader.int(
                Keys.CONVERSION_LIMIT_DAILY,
                D.conversionLimitDaily,
                Ranges.CONVERSION_LIMIT
            ),

            rewardedAdBonus = reader.int(Keys.REWARDED_AD_BONUS, D.rewardedAdBonus, Ranges.AD_BONUS),
            rewardedAdBonusEnabled = reader.bool(
                Keys.REWARDED_AD_BONUS_ENABLED,
                D.rewardedAdBonusEnabled
            ),

            luckAdEnabled = reader.bool(Keys.LUCK_AD_ENABLED, D.luckAdEnabled),
            luckAdCoinReward = reader.int(
                Keys.LUCK_AD_COIN_REWARD,
                D.luckAdCoinReward,
                Ranges.LUCK_COIN_REWARD
            ),
            luckAdGemReward = reader.int(
                Keys.LUCK_AD_GEM_REWARD,
                D.luckAdGemReward,
                Ranges.LUCK_GEM_REWARD
            ),
            luckAdCoinProbability = reader.double(
                Keys.LUCK_AD_COIN_PROBABILITY,
                D.luckAdCoinProbability,
                Ranges.PROBABILITY
            ),
            luckAdGemProbability = reader.double(
                Keys.LUCK_AD_GEM_PROBABILITY,
                D.luckAdGemProbability,
                Ranges.PROBABILITY
            ),
            luckAdNoRewardProbability = reader.double(
                Keys.LUCK_AD_NO_REWARD_PROBABILITY,
                D.luckAdNoRewardProbability,
                Ranges.PROBABILITY
            ),
            luckAdPrizesJson = reader.string(
                Keys.LUCK_AD_PRIZES,
                D.luckAdPrizesJson,
                Ranges.MAX_PRIZES_JSON_LENGTH,
                truncate = false
            ),

            lifeRecoveryAdEnabled = reader.bool(
                Keys.LIFE_RECOVERY_AD_ENABLED,
                D.lifeRecoveryAdEnabled
            ),

            winBonusAdDailyCap = reader.int(
                Keys.WIN_BONUS_AD_DAILY_CAP,
                D.winBonusAdDailyCap,
                Ranges.DAILY_CAP
            ),
            lifeRecoveryAdDailyCap = reader.int(
                Keys.LIFE_RECOVERY_AD_DAILY_CAP,
                D.lifeRecoveryAdDailyCap,
                Ranges.DAILY_CAP
            ),
            lifeRecoveryAdCooldownMinutes = reader.int(
                Keys.LIFE_RECOVERY_AD_COOLDOWN_MINUTES,
                D.lifeRecoveryAdCooldownMinutes,
                Ranges.COOLDOWN_MINUTES
            ),
            luckAdDailyCap = reader.int(Keys.LUCK_AD_DAILY_CAP, D.luckAdDailyCap, Ranges.DAILY_CAP),
            luckAdCooldownMinutes = reader.int(
                Keys.LUCK_AD_COOLDOWN_MINUTES,
                D.luckAdCooldownMinutes,
                Ranges.COOLDOWN_MINUTES
            ),
            rewardedAdsDailyCapTotal = reader.int(
                Keys.REWARDED_ADS_DAILY_CAP_TOTAL,
                D.rewardedAdsDailyCapTotal,
                Ranges.DAILY_CAP
            ),

            eventBannerEnabled = reader.bool(Keys.EVENT_BANNER_ENABLED, D.eventBannerEnabled),
            eventBannerText = reader.string(
                Keys.EVENT_BANNER_TEXT,
                D.eventBannerText,
                Ranges.MAX_BANNER_LENGTH
            ),

            storeFeaturedItem = reader.string(
                Keys.STORE_FEATURED_ITEM,
                D.storeFeaturedItem,
                Ranges.MAX_ITEM_ID_LENGTH,
                truncate = false
            ),
            newThemeEnabled = reader.bool(Keys.NEW_THEME_ENABLED, D.newThemeEnabled),
            luckAdsFeatureEnabled = reader.bool(
                Keys.LUCK_ADS_FEATURE_ENABLED,
                D.luckAdsFeatureEnabled
            )
        )

        val luckWeightTotal = snapshot.luckAdCoinProbability +
                snapshot.luckAdGemProbability +
                snapshot.luckAdNoRewardProbability
        if (snapshot.luckAdEnabled && snapshot.luckAdPrizesJson.isBlank() && luckWeightTotal <= 0.0) {
            issues += ConfigIssue(
                key = Keys.LUCK_AD_NO_REWARD_PROBABILITY,
                rawValue = null,
                problem = "All Luck Ad probabilities are zero; Luck Ads stay unavailable"
            )
        }

        return ParsedRemoteConfig(snapshot, issues)
    }

    private class Reader(
        private val source: ConfigValueSource,
        private val issues: MutableList<ConfigIssue>
    ) {
        fun bool(key: String, default: Boolean): Boolean {
            val raw = source.rawValue(key) ?: return default
            return when (raw.trim().lowercase()) {
                "true", "1", "yes", "on" -> true
                "false", "0", "no", "off", "" -> false
                else -> {
                    issues += ConfigIssue(key, raw, "not a boolean; using default $default")
                    default
                }
            }
        }

        fun int(key: String, default: Int, range: IntRange): Int {
            val raw = source.rawValue(key) ?: return default
            val parsed = raw.trim().toLongOrNull()
                ?: raw.trim().toDoubleOrNull()?.takeIf { it.isFinite() }?.toLong()

            if (parsed == null) {
                issues += ConfigIssue(key, raw, "not a number; using default $default")
                return default
            }

            val clamped = parsed.coerceIn(range.first.toLong(), range.last.toLong()).toInt()
            if (clamped.toLong() != parsed) {
                issues += ConfigIssue(key, raw, "outside $range; clamped to $clamped")
            }
            return clamped
        }

        fun double(key: String, default: Double, range: ClosedFloatingPointRange<Double>): Double {
            val raw = source.rawValue(key) ?: return default
            val parsed = raw.trim().toDoubleOrNull()?.takeIf { it.isFinite() }

            if (parsed == null) {
                issues += ConfigIssue(key, raw, "not a finite number; using default $default")
                return default
            }

            val clamped = parsed.coerceIn(range.start, range.endInclusive)
            if (clamped != parsed) {
                issues += ConfigIssue(key, raw, "outside $range; clamped to $clamped")
            }
            return clamped
        }

        fun string(key: String, default: String, maxLength: Int, truncate: Boolean = true): String {
            val raw = source.rawValue(key) ?: return default
            val trimmed = raw.trim()

            if (trimmed.length <= maxLength) return trimmed

            return if (truncate) {
                issues += ConfigIssue(key, raw.take(40), "longer than $maxLength; truncated")
                trimmed.take(maxLength)
            } else {
                issues += ConfigIssue(key, raw.take(40), "longer than $maxLength; using default")
                default
            }
        }
    }
}
