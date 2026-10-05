package com.cherish.app.date.lunar

import com.cherish.app.date.model.LunarDate
import com.cherish.app.date.model.SolarDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LunarCalendarTest {

    private val calendar: LunarCalendar = DefaultLunarCalendar()

    @Test
    fun testSolarToLunarConversion() {
        val solar = SolarDate(2027, 4, 27)
        val lunar = calendar.solarToLunar(solar)

        assertEquals(2027, lunar.year)
        assertEquals(3, lunar.month)
        assertEquals(21, lunar.day)
        assertFalse(lunar.isLeapMonth)
    }

    @Test
    fun testLunarToSolarConversion() {
        val lunar = LunarDate(2026, 3, 10, isLeapMonth = false)
        val solar = calendar.lunarToSolar(lunar)

        assertEquals(2026, solar.year)
        assertEquals(4, solar.month)
        assertEquals(26, solar.day)
    }

    @Test
    fun testLeapMonthIdentificationAndConversion() {
        // 2023 has leap month 2 (闰二月)
        assertEquals(2, calendar.getLeapMonth(2023))
        assertEquals(0, calendar.getLeapMonth(2026))

        // Normal Feb 1st vs Leap Feb 1st in 2023
        val normalFeb1 = LunarDate(2023, 2, 1, isLeapMonth = false)
        val leapFeb1 = LunarDate(2023, 2, 1, isLeapMonth = true)

        val normalSolar = calendar.lunarToSolar(normalFeb1)
        val leapSolar = calendar.lunarToSolar(leapFeb1)

        assertEquals(SolarDate(2023, 2, 20), normalSolar)
        assertEquals(SolarDate(2023, 3, 22), leapSolar)

        // Reverse check: 2023-03-22 must map to leap Feb 1st
        val reverseLeap = calendar.solarToLunar(SolarDate(2023, 3, 22))
        assertEquals(leapFeb1, reverseLeap)
        assertTrue(reverseLeap.isLeapMonth)
    }

    @Test
    fun testDaysInMonthBigAndSmall() {
        // Query lunar month length: must be either 29 (small) or 30 (big)
        val days2023M1 = calendar.getDaysInMonth(2023, 1, false)
        assertTrue(days2023M1 in 29..30)

        val leapDays = calendar.getDaysInMonth(2023, 2, true)
        assertTrue(leapDays in 29..30)
    }

    @Test
    fun testSolarToLunarToSolarRoundTripAcrossFullYear() {
        // Round-trip check across all 365 days of 2026
        var current = LocalDate.of(2026, 1, 1)
        val end = LocalDate.of(2026, 12, 31)

        while (!current.isAfter(end)) {
            val solar = SolarDate.fromLocalDate(current)
            val lunar = calendar.solarToLunar(solar)
            val roundTripSolar = calendar.lunarToSolar(lunar)

            assertEquals(
                "Round trip failed for date $solar",
                solar,
                roundTripSolar,
            )
            current = current.plusDays(1)
        }
    }

    @Test
    fun testLunarToSolarToLunarRoundTrip() {
        // Round trip test for leap year lunar dates in 2023
        for (month in 1..12) {
            val normalDate = LunarDate(2023, month, 15, isLeapMonth = false)
            val solar = calendar.lunarToSolar(normalDate)
            val roundTripLunar = calendar.solarToLunar(solar)
            assertEquals(normalDate, roundTripLunar)
        }

        // Test the leap month in 2023 (leap 2nd month)
        val leapDate = LunarDate(2023, 2, 15, isLeapMonth = true)
        val leapSolar = calendar.lunarToSolar(leapDate)
        val roundTripLeap = calendar.solarToLunar(leapSolar)
        assertEquals(leapDate, roundTripLeap)
    }
}
