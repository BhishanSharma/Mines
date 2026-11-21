package com.genoma.mines.core.economy

enum class ConversionDirection {
    /** 500 Coins -> 1 Gem. */
    COINS_TO_GEMS,

    /** 1 Gem -> 500 Coins. */
    GEMS_TO_COINS
}

enum class ConversionRejection {
    CONVERSIONS_DISABLED,
    DAILY_LIMIT_REACHED,
    INSUFFICIENT_COINS,
    INSUFFICIENT_GEMS
}

sealed interface ConversionDecision {

    data class Approved(
        val coinsDelta: Int,
        val gemsDelta: Int,
        val usedTodayAfter: Int
    ) : ConversionDecision

    data class Rejected(val reason: ConversionRejection) : ConversionDecision
}

/**
 * Coins <-> Gems conversion rules (plan §1.2).
 *
 * - 500 Coins = 1 Gem in both directions.
 * - The daily allowance is shared across both directions.
 * - Only successful conversions consume an allowance slot.
 * - The allowance resets at midnight IST.
 */
object ConversionRules {

    const val COINS_PER_GEM = 500
    const val DEFAULT_DAILY_LIMIT = 3
    const val MAX_DAILY_LIMIT = 20

    /** Remote Config may lower or raise the limit within safe bounds; 0 disables conversions. */
    fun sanitizeLimit(limit: Int): Int = limit.coerceIn(0, MAX_DAILY_LIMIT)

    /**
     * Conversions already used on [todayIst].
     *
     * [storedDate]/[storedCount] are the current counter fields. When the
     * counter has never been written ([storedDate] == null), today's entries
     * from the previous rolling-window implementation ([legacyTimestamps])
     * are counted so an upgrade never hands out a fresh allowance.
     */
    fun usedToday(
        storedDate: String?,
        storedCount: Long?,
        legacyTimestamps: List<Long>,
        todayIst: String
    ): Int = when {
        storedDate == todayIst ->
            (storedCount ?: 0L).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()

        storedDate == null ->
            legacyTimestamps.count { IstCalendar.dateOf(it) == todayIst }

        else -> 0
    }

    fun decide(
        direction: ConversionDirection,
        coins: Long,
        gems: Long,
        usedToday: Int,
        dailyLimit: Int
    ): ConversionDecision {
        val limit = sanitizeLimit(dailyLimit)

        if (limit == 0) {
            return ConversionDecision.Rejected(ConversionRejection.CONVERSIONS_DISABLED)
        }

        if (usedToday >= limit) {
            return ConversionDecision.Rejected(ConversionRejection.DAILY_LIMIT_REACHED)
        }

        return when (direction) {
            ConversionDirection.COINS_TO_GEMS ->
                if (coins < COINS_PER_GEM) {
                    ConversionDecision.Rejected(ConversionRejection.INSUFFICIENT_COINS)
                } else {
                    ConversionDecision.Approved(
                        coinsDelta = -COINS_PER_GEM,
                        gemsDelta = 1,
                        usedTodayAfter = usedToday + 1
                    )
                }

            ConversionDirection.GEMS_TO_COINS ->
                if (gems < 1) {
                    ConversionDecision.Rejected(ConversionRejection.INSUFFICIENT_GEMS)
                } else {
                    ConversionDecision.Approved(
                        coinsDelta = COINS_PER_GEM,
                        gemsDelta = -1,
                        usedTodayAfter = usedToday + 1
                    )
                }
        }
    }

    fun message(
        decision: ConversionDecision,
        direction: ConversionDirection,
        dailyLimit: Int
    ): String = when (decision) {
        is ConversionDecision.Approved -> when (direction) {
            ConversionDirection.COINS_TO_GEMS ->
                "Converted $COINS_PER_GEM Coins into 1 Gem."

            ConversionDirection.GEMS_TO_COINS ->
                "Converted 1 Gem into $COINS_PER_GEM Coins."
        }

        is ConversionDecision.Rejected -> when (decision.reason) {
            ConversionRejection.CONVERSIONS_DISABLED ->
                "Conversions are unavailable right now."

            ConversionRejection.DAILY_LIMIT_REACHED -> {
                val limit = sanitizeLimit(dailyLimit)
                val noun = if (limit == 1) "conversion" else "conversions"
                "You've used all $limit $noun for today. Try again after midnight IST."
            }

            ConversionRejection.INSUFFICIENT_COINS ->
                "You need $COINS_PER_GEM Coins to convert into 1 Gem."

            ConversionRejection.INSUFFICIENT_GEMS ->
                "You need 1 Gem to convert into $COINS_PER_GEM Coins."
        }
    }
}
