package com.genoma.mines.core.economy

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class IstCalendarTest {

    private fun utc(value: String): Long =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.parse(value)!!.time

    @Test
    fun `IST day changes at 18_30 UTC`() {
        assertEquals("2026-11-20", IstCalendar.dateOf(utc("2026-11-20 18:29:59.999")))
        assertEquals("2026-11-21", IstCalendar.dateOf(utc("2026-11-20 18:30:00.000")))
    }

    @Test
    fun `next midnight is the following 18_30 UTC`() {
        val now = utc("2026-11-21 05:00:00.000")
        assertEquals(utc("2026-11-21 18:30:00.000"), IstCalendar.nextMidnightMillis(now))
    }

    @Test
    fun `exactly at midnight the next midnight is a full day away`() {
        val midnight = utc("2026-11-20 18:30:00.000")
        assertEquals(utc("2026-11-21 18:30:00.000"), IstCalendar.nextMidnightMillis(midnight))
    }

    @Test
    fun `matches the platform Asia_Kolkata calendar across years and leap days`() {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone(IstCalendar.ZONE_ID)
        }
        val start = utc("2023-12-30 00:00:00.000")
        val step = 7 * 60 * 60 * 1000L + 13 * 60 * 1000L // irregular step hits many boundaries

        var t = start
        repeat(2_000) {
            assertEquals(formatter.format(t), IstCalendar.dateOf(t))
            t += step
        }
        assertEquals("2024-02-29", IstCalendar.dateOf(utc("2024-02-29 12:00:00.000")))
    }

    @Test
    fun `epoch and pre-epoch instants`() {
        assertEquals("1970-01-01", IstCalendar.dateOf(0L))
        assertEquals("1969-12-31", IstCalendar.dateOf(utc("1969-12-31 18:29:59.999")))
    }
}
