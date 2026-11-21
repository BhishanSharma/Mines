package com.genoma.mines.core.economy

/** Where a rewarded ad is offered. Persisted by name; never rename entries. */
enum class RewardedAdPlacement {
    WIN_BONUS,
    LIFE_RECOVERY,
    LUCK_SPIN
}

/**
 * Frequency limits (Stage 3G), configured through Remote Config.
 *
 * Each placement has its own daily cap and cooldown, and all placements
 * share [totalDailyCap]. A cap of 0 disables the placement.
 */
data class AdCapConfig(
    val dailyCaps: Map<RewardedAdPlacement, Int>,
    val cooldownMinutes: Map<RewardedAdPlacement, Int>,
    val totalDailyCap: Int
)

/** Rewarded ads completed on one IST day. */
data class AdUsage(
    val dateIst: String = "",
    val counts: Map<RewardedAdPlacement, Int> = emptyMap(),
    val lastRewardedAtMillis: Map<RewardedAdPlacement, Long> = emptyMap()
)

sealed interface AdEligibility {
    data object Eligible : AdEligibility
    data object Disabled : AdEligibility
    data object DailyCapReached : AdEligibility
    data object TotalDailyCapReached : AdEligibility
    data class CoolingDown(val availableAtMillis: Long) : AdEligibility
}

/**
 * Pure cap/cooldown rules. Days follow IST like the conversion allowance.
 *
 * Clock safety: a cooldown never blocks longer than its configured length,
 * even if the device clock moved backwards after the last ad. Counts are
 * only kept for the current IST day, so moving the clock forward to a new
 * day resets them.
 */
object AdFrequencyRules {

    fun check(
        placement: RewardedAdPlacement,
        config: AdCapConfig,
        usage: AdUsage,
        nowMillis: Long
    ): AdEligibility {
        val cap = config.dailyCaps[placement] ?: 0
        if (cap <= 0 || config.totalDailyCap <= 0) return AdEligibility.Disabled

        val today = forToday(usage, nowMillis)
        if ((today.counts[placement] ?: 0) >= cap) return AdEligibility.DailyCapReached
        if (today.counts.values.sum() >= config.totalDailyCap) return AdEligibility.TotalDailyCapReached

        val cooldownMillis = (config.cooldownMinutes[placement] ?: 0).coerceAtLeast(0) * 60_000L
        val last = usage.lastRewardedAtMillis[placement]
        if (cooldownMillis > 0 && last != null) {
            // A last time "in the future" (clock moved back) is capped at now.
            val availableAt = minOf(last, nowMillis) + cooldownMillis
            if (nowMillis < availableAt) return AdEligibility.CoolingDown(availableAt)
        }

        return AdEligibility.Eligible
    }

    /** Usage after a rewarded ad for [placement] was completed at [nowMillis]. */
    fun record(placement: RewardedAdPlacement, usage: AdUsage, nowMillis: Long): AdUsage {
        val today = forToday(usage, nowMillis)
        return today.copy(
            counts = today.counts + (placement to (today.counts[placement] ?: 0) + 1),
            lastRewardedAtMillis = usage.lastRewardedAtMillis + (placement to nowMillis)
        )
    }

    /** Drops counts from earlier days; cooldown timestamps are kept across midnight. */
    private fun forToday(usage: AdUsage, nowMillis: Long): AdUsage {
        val today = IstCalendar.dateOf(nowMillis)
        return if (usage.dateIst == today) usage else usage.copy(dateIst = today, counts = emptyMap())
    }

    fun encode(usage: AdUsage): String = buildList {
        add("date=${usage.dateIst}")
        usage.counts.forEach { (placement, count) -> add("count.${placement.name}=$count") }
        usage.lastRewardedAtMillis.forEach { (placement, at) -> add("last.${placement.name}=$at") }
    }.joinToString("\n")

    fun decode(raw: String?): AdUsage {
        if (raw.isNullOrBlank()) return AdUsage()

        var date = ""
        val counts = mutableMapOf<RewardedAdPlacement, Int>()
        val last = mutableMapOf<RewardedAdPlacement, Long>()

        raw.lineSequence().forEach { line ->
            val key = line.substringBefore('=', missingDelimiterValue = "")
            val value = line.substringAfter('=', missingDelimiterValue = "")
            when {
                key == "date" -> date = value
                key.startsWith("count.") -> {
                    val placement = placementOf(key.removePrefix("count."))
                    val count = value.toIntOrNull()
                    if (placement != null && count != null && count >= 0) counts[placement] = count
                }

                key.startsWith("last.") -> {
                    val placement = placementOf(key.removePrefix("last."))
                    val at = value.toLongOrNull()
                    if (placement != null && at != null) last[placement] = at
                }
            }
        }

        return AdUsage(date, counts, last)
    }

    private fun placementOf(name: String): RewardedAdPlacement? =
        RewardedAdPlacement.entries.firstOrNull { it.name == name }
}
