
package com.genoma.mines.ads

import com.genoma.mines.core.remoteconfig.RemoteConfigSnapshot
import org.json.JSONArray
import kotlin.random.Random

enum class LuckPrizeType {
    COINS,
    GEMS,
    NONE
}

data class LuckPrize(
    val type: LuckPrizeType,
    val amount: Int,
    val weight: Double
) {
    init {
        require(weight > 0.0 && weight.isFinite())
        require(amount >= 0)
        require(type == LuckPrizeType.NONE || amount > 0)
        require(type != LuckPrizeType.NONE || amount == 0)
    }
}

/**
 * Builds the configured prize slots and selects a weighted result.
 *
 * JSON format:
 * [
 *   {"type":"COINS","amount":10,"weight":40},
 *   {"type":"GEMS","amount":1,"weight":10},
 *   {"type":"NONE","amount":0,"weight":50}
 * ]
 *
 * Weights are relative values, not percentages.
 */
object LuckAdsPrizeSelector {

    private const val MAX_SLOTS = 30
    private const val MAX_AMOUNT = 1_000_000
    private const val MAX_WEIGHT = 1_000_000.0

    /**
     * Returns configured slots. If the JSON is blank or invalid, builds
     * a compatible list from the existing legacy Remote Config values.
     */
    fun getPrizes(config: RemoteConfigSnapshot): List<LuckPrize> {
        if (!config.isLuckAdsAvailable) return emptyList()

        val configured = parseConfiguredPrizes(config.luckAdPrizesJson)
        if (configured.isNotEmpty()) return configured

        return buildLegacyPrizes(config)
    }

    /**
     * Selects one prize using its relative weight.
     *
     * Call this only after the rewarded-ad SDK confirms the reward callback.
     * This client-side selection is for prototype integration only; production
     * prize selection and crediting must be performed by a trusted backend.
     */
    fun selectPrize(
        prizes: List<LuckPrize>,
        random: Random = Random.Default
    ): LuckPrize? {
        if (prizes.isEmpty()) return null

        val totalWeight = prizes.sumOf { it.weight }
        if (!totalWeight.isFinite() || totalWeight <= 0.0) return null

        val selectedWeight = random.nextDouble() * totalWeight
        var cumulativeWeight = 0.0

        for (prize in prizes) {
            cumulativeWeight += prize.weight
            if (selectedWeight < cumulativeWeight) {
                return prize
            }
        }

        return prizes.last()
    }

    private fun parseConfiguredPrizes(json: String): List<LuckPrize> {
        if (json.isBlank()) return emptyList()

        return try {
            val array = JSONArray(json)
            val result = mutableListOf<LuckPrize>()

            for (index in 0 until minOf(array.length(), MAX_SLOTS)) {
                val item = array.optJSONObject(index) ?: continue

                val type = runCatching {
                    LuckPrizeType.valueOf(
                        item.optString("type").trim().uppercase()
                    )
                }.getOrNull() ?: continue

                val amount = item.optLong("amount", -1L)
                    .takeIf { it in 0L..MAX_AMOUNT.toLong() }
                    ?.toInt() ?: continue

                val weight = item.optDouble("weight", Double.NaN)
                    .takeIf {
                        it.isFinite() && it > 0.0 && it <= MAX_WEIGHT
                    } ?: continue

                val validAmount = when (type) {
                    LuckPrizeType.COINS,
                    LuckPrizeType.GEMS -> amount > 0

                    LuckPrizeType.NONE -> amount == 0
                }

                if (!validAmount) continue

                result += LuckPrize(
                    type = type,
                    amount = amount,
                    weight = weight
                )
            }

            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun buildLegacyPrizes(
        config: RemoteConfigSnapshot
    ): List<LuckPrize> {
        val result = mutableListOf<LuckPrize>()

        val coinAmount = config.luckAdCoinReward.coerceIn(0, MAX_AMOUNT)
        val gemAmount = config.luckAdGemReward.coerceIn(0, MAX_AMOUNT)

        val coinWeight = config.luckAdCoinProbability
            .takeIf { it.isFinite() }
            ?.coerceIn(0.0, 100.0) ?: 0.0

        val gemWeight = config.luckAdGemProbability
            .takeIf { it.isFinite() }
            ?.coerceIn(0.0, 100.0) ?: 0.0

        val noRewardWeight = config.luckAdNoRewardProbability
            .takeIf { it.isFinite() }
            ?.coerceIn(0.0, 100.0) ?: 0.0

        if (coinAmount > 0 && coinWeight > 0.0) {
            result += LuckPrize(
                type = LuckPrizeType.COINS,
                amount = coinAmount,
                weight = coinWeight
            )
        }

        if (gemAmount > 0 && gemWeight > 0.0) {
            result += LuckPrize(
                type = LuckPrizeType.GEMS,
                amount = gemAmount,
                weight = gemWeight
            )
        }

        if (noRewardWeight > 0.0) {
            result += LuckPrize(
                type = LuckPrizeType.NONE,
                amount = 0,
                weight = noRewardWeight
            )
        }

        return result
    }
}
