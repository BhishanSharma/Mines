package com.genoma.mines.core.economy

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

    /** Probability of this slot within [table], 0..1. */
    fun probabilityIn(table: List<LuckPrize>): Double {
        val total = table.sumOf { it.weight }
        return if (total > 0.0) weight / total else 0.0
    }

    /** The wallet change this prize grants. */
    fun toSettlementPlan(): SettlementPlan = when (type) {
        LuckPrizeType.COINS -> SettlementPlan(coinCredit = amount)
        LuckPrizeType.GEMS -> SettlementPlan(gemCredit = amount)
        LuckPrizeType.NONE -> SettlementPlan()
    }
}

/**
 * Luck Spin prize table (Stage 3B).
 *
 * Preferred format (`luck_ad_prizes` Remote Config JSON):
 * ```
 * [
 *   {"type":"COINS","amount":10,"weight":40},
 *   {"type":"GEMS","amount":1,"weight":10},
 *   {"type":"NONE","amount":0,"weight":50}
 * ]
 * ```
 * Weights are relative, not percentages. Invalid slots are skipped. If no
 * valid slot remains, the legacy single-value parameters are used. An
 * empty table means Luck Spin is unavailable.
 */
object LuckPrizeTable {

    const val MAX_SLOTS = 12
    const val MAX_COIN_AMOUNT = 100_000
    const val MAX_GEM_AMOUNT = 1_000
    private const val MAX_WEIGHT = 1_000_000.0

    fun build(
        prizesJson: String,
        legacyCoinReward: Int,
        legacyGemReward: Int,
        legacyCoinWeight: Double,
        legacyGemWeight: Double,
        legacyNoRewardWeight: Double
    ): List<LuckPrize> {
        val configured = parse(prizesJson)
        if (configured.isNotEmpty()) return configured

        return buildList {
            val coinWeight = legacyCoinWeight.validWeight()
            val gemWeight = legacyGemWeight.validWeight()
            val noneWeight = legacyNoRewardWeight.validWeight()
            val coins = legacyCoinReward.coerceIn(0, MAX_COIN_AMOUNT)
            val gems = legacyGemReward.coerceIn(0, MAX_GEM_AMOUNT)

            if (coins > 0 && coinWeight > 0.0) add(LuckPrize(LuckPrizeType.COINS, coins, coinWeight))
            if (gems > 0 && gemWeight > 0.0) add(LuckPrize(LuckPrizeType.GEMS, gems, gemWeight))
            if (noneWeight > 0.0) add(LuckPrize(LuckPrizeType.NONE, 0, noneWeight))
        }.let { table ->
            // A table that can only ever say "no reward" is not worth offering.
            if (table.any { it.type != LuckPrizeType.NONE }) table else emptyList()
        }
    }

    fun parse(json: String): List<LuckPrize> {
        if (json.isBlank()) return emptyList()

        val table = try {
            val array = JSONArray(json)
            val result = mutableListOf<LuckPrize>()

            for (index in 0 until minOf(array.length(), MAX_SLOTS)) {
                val item = array.optJSONObject(index) ?: continue

                val type = LuckPrizeType.entries.firstOrNull {
                    it.name == item.optString("type").trim().uppercase()
                } ?: continue

                val maxAmount = if (type == LuckPrizeType.GEMS) MAX_GEM_AMOUNT else MAX_COIN_AMOUNT
                val amount = item.optLong("amount", -1L)
                    .takeIf { it in 0L..maxAmount.toLong() }
                    ?.toInt() ?: continue

                val weight = item.optDouble("weight", Double.NaN).validWeight()
                if (weight <= 0.0) continue

                val validAmount = when (type) {
                    LuckPrizeType.COINS, LuckPrizeType.GEMS -> amount > 0
                    LuckPrizeType.NONE -> amount == 0
                }
                if (!validAmount) continue

                result += LuckPrize(type, amount, weight)
            }
            result
        } catch (_: Exception) {
            emptyList()
        }

        return if (table.any { it.type != LuckPrizeType.NONE }) table else emptyList()
    }

    /**
     * Selects one slot by weight. Returns the index into [table], so the
     * wheel animation and the credited prize always refer to the same slot.
     */
    fun selectIndex(table: List<LuckPrize>, random: Random = Random.Default): Int? {
        if (table.isEmpty()) return null

        val total = table.sumOf { it.weight }
        if (!total.isFinite() || total <= 0.0) return null

        val target = random.nextDouble() * total
        var cumulative = 0.0
        table.forEachIndexed { index, prize ->
            cumulative += prize.weight
            if (target < cumulative) return index
        }
        return table.lastIndex
    }

    private fun Double.validWeight(): Double =
        if (isFinite() && this > 0.0) coerceAtMost(MAX_WEIGHT) else 0.0
}
