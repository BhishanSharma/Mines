package com.genoma.mines.economy

import com.genoma.mines.core.economy.AdCapConfig
import com.genoma.mines.core.economy.EconomyRules
import com.genoma.mines.core.economy.RewardedAdPlacement
import com.genoma.mines.core.remoteconfig.RemoteConfigSnapshot
import com.genoma.mines.game.domain.Difficulty

/** Economy values to capture at the start of a game. */
fun RemoteConfigSnapshot.toEconomyRules(): EconomyRules = EconomyRules(
    easyWinReward = easyWinReward,
    mediumWinReward = mediumWinReward,
    hardWinReward = hardWinReward,
    entryFee = lossCoinFee,
    minimumStartCoins = minimumStartCoins,
    winAdBonus = rewardedAdBonus,
    winAdBonusEnabled = rewardedAdBonusEnabled
)

fun EconomyRules.winRewardFor(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> easyWinReward
    Difficulty.MEDIUM -> mediumWinReward
    Difficulty.HARD -> hardWinReward
}

/** Identifies the won game a rewarded ad was shown for. */
data class WinBonusTicket(
    val gameId: String,
    val amount: Int
)

/** Rewarded-ad caps and cooldowns from Remote Config (Stage 3G). */
fun RemoteConfigSnapshot.toAdCapConfig(): AdCapConfig = AdCapConfig(
    dailyCaps = mapOf(
        RewardedAdPlacement.WIN_BONUS to winBonusAdDailyCap,
        RewardedAdPlacement.LIFE_RECOVERY to lifeRecoveryAdDailyCap,
        RewardedAdPlacement.LUCK_SPIN to luckAdDailyCap
    ),
    cooldownMinutes = mapOf(
        RewardedAdPlacement.WIN_BONUS to 0,
        RewardedAdPlacement.LIFE_RECOVERY to lifeRecoveryAdCooldownMinutes,
        RewardedAdPlacement.LUCK_SPIN to luckAdCooldownMinutes
    ),
    totalDailyCap = rewardedAdsDailyCapTotal
)
