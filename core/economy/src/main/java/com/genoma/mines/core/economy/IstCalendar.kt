package com.genoma.mines.core.economy

/**
 * India Standard Time calendar arithmetic.
 *
 * IST is a fixed UTC+05:30 offset with no daylight saving, so the calendar
 * day can be computed with plain arithmetic. This keeps the daily-limit
 * boundary identical on every device and in JVM tests.
 */
object IstCalendar {

    const val ZONE_ID = "Asia/Kolkata"

    private const val OFFSET_MILLIS = (5 * 60 + 30) * 60 * 1000L
    private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

    /** IST calendar day of [epochMillis] as `yyyy-MM-dd`. */
    fun dateOf(epochMillis: Long): String {
        val epochDay = Math.floorDiv(epochMillis + OFFSET_MILLIS, DAY_MILLIS)
        return civilDate(epochDay)
    }

    /** Epoch millis of the next IST midnight strictly after [epochMillis]. */
    fun nextMidnightMillis(epochMillis: Long): Long {
        val epochDay = Math.floorDiv(epochMillis + OFFSET_MILLIS, DAY_MILLIS)
        return (epochDay + 1) * DAY_MILLIS - OFFSET_MILLIS
    }

    /**
     * Converts days since 1970-01-01 to a proleptic Gregorian date string
     * (H. Hinnant's civil_from_days algorithm).
     */
    private fun civilDate(epochDay: Long): String {
        val z = epochDay + 719_468
        val era = Math.floorDiv(z, 146_097L)
        val doe = z - era * 146_097
        val yoe = (doe - doe / 1_460 + doe / 36_524 - doe / 146_096) / 365
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val day = doy - (153 * mp + 2) / 5 + 1
        val month = if (mp < 10) mp + 3 else mp - 9
        val year = yoe + era * 400 + if (month <= 2) 1 else 0

        return buildString(10) {
            append(year.toString().padStart(4, '0'))
            append('-')
            append(month.toString().padStart(2, '0'))
            append('-')
            append(day.toString().padStart(2, '0'))
        }
    }
}
