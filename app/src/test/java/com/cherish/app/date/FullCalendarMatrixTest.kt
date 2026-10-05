package com.cherish.app.date

import com.cherish.app.date.calc.CountdownCalculator
import com.cherish.app.date.calc.EventCountdownStatus
import com.cherish.app.date.calc.RecurrenceCalculator
import com.cherish.app.date.lunar.DefaultLunarCalendar
import com.cherish.app.date.lunar.LunarCalendar
import com.cherish.app.date.model.LunarDate
import com.cherish.app.date.model.RepeatRule
import com.cherish.app.date.model.SolarDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FullCalendarMatrixTest {

    private val calendar: LunarCalendar = DefaultLunarCalendar()
    private val recurrenceCalculator = RecurrenceCalculator(calendar)

    /** Matrix Case 1: Basic Solar to Lunar */
    @Test
    fun test01_BasicSolarToLunar() {
        val solar = SolarDate(2027, 4, 27)
        val lunar = calendar.solarToLunar(solar)
        assertEquals(2027, lunar.year)
        assertEquals(3, lunar.month)
        assertEquals(21, lunar.day)
        assertFalse(lunar.isLeapMonth)
    }

    /** Matrix Case 2: Basic Lunar to Solar */
    @Test
    fun test02_BasicLunarToSolar() {
        val lunar = LunarDate(2026, 3, 10, isLeapMonth = false)
        val solar = calendar.lunarToSolar(lunar)
        assertEquals(2026, solar.year)
        assertEquals(4, solar.month)
        assertEquals(26, solar.day)
    }

    /** Matrix Case 3: Big and Small Lunar months (30 vs 29 days) */
    @Test
    fun test03_BigAndSmallLunarMonths() {
        // Query multiple months to verify small (29) vs big (30) days
        val month1Days = calendar.getDaysInMonth(2026, 1, false)
        val month2Days = calendar.getDaysInMonth(2026, 2, false)
        assertTrue(month1Days in 29..30)
        assertTrue(month2Days in 29..30)
    }

    /** Matrix Case 4: Leap Month Identification */
    @Test
    fun test04_LeapMonthIdentification() {
        assertEquals(2, calendar.getLeapMonth(2023)) // 2023 has leap month 2
        assertEquals(0, calendar.getLeapMonth(2026)) // 2026 has no leap month
    }

    /** Matrix Case 5: Leap Month to Solar */
    @Test
    fun test05_LeapMonthToSolar() {
        val leapFeb1 = LunarDate(2023, 2, 1, isLeapMonth = true)
        val solar = calendar.lunarToSolar(leapFeb1)
        assertEquals(SolarDate(2023, 3, 22), solar)

        val reverse = calendar.solarToLunar(solar)
        assertEquals(leapFeb1, reverse)
        assertTrue(reverse.isLeapMonth)
    }

    /** Matrix Case 6: Lunar Birthday Yearly Recurrence */
    @Test
    fun test06_LunarBirthdayYearlyRecurrence() {
        val baseLunar = LunarDate(2026, 5, 5, isLeapMonth = false)
        val baseSolar = calendar.lunarToSolar(baseLunar) // 2026-06-19
        val today = SolarDate(2026, 10, 5)

        val nextSolar = recurrenceCalculator.nextOccurrence(baseSolar, baseLunar, today, RepeatRule.Yearly)
        val expectedSolar = calendar.lunarToSolar(LunarDate(2027, 5, 5, isLeapMonth = false))
        assertEquals(expectedSolar, nextSolar)
    }

    /** Matrix Case 7: Leap Month Birthday Fallback in Normal Year */
    @Test
    fun test07_LeapMonthBirthdayFallbackInNormalYear() {
        // Born on leap 4th month 1st day in 2020
        val baseLunar = LunarDate(2020, 4, 1, isLeapMonth = true)
        val baseSolar = calendar.lunarToSolar(baseLunar)
        val ref2026 = SolarDate(2026, 1, 1) // 2026 has no leap 4th month

        val nextSolar = recurrenceCalculator.nextOccurrence(baseSolar, baseLunar, ref2026, RepeatRule.Yearly)
        // Must fallback to 2026 normal 4th month 1st day
        val expectedSolar = calendar.lunarToSolar(LunarDate(2026, 4, 1, isLeapMonth = false))
        assertEquals(expectedSolar, nextSolar)
    }

    /** Matrix Case 8: Leap Day Feb 29 Strategy in Normal Year */
    @Test
    fun test08_LeapDayFeb29InNormalYear() {
        val leapDay = SolarDate(2024, 2, 29)

        // In 2025 (normal year): degrades to Feb 28
        val next2025 = recurrenceCalculator.nextOccurrence(leapDay, null, SolarDate(2025, 1, 1), RepeatRule.Yearly)
        assertEquals(SolarDate(2025, 2, 28), next2025)

        // In 2028 (leap year): remains Feb 29
        val next2028 = recurrenceCalculator.nextOccurrence(leapDay, null, SolarDate(2028, 1, 1), RepeatRule.Yearly)
        assertEquals(SolarDate(2028, 2, 29), next2028)
    }

    /** Matrix Case 9: Month-end Day 31 Clipping */
    @Test
    fun test09_MonthEndDay31Clipping() {
        val base31st = SolarDate(2026, 1, 31)

        // Feb 2026 (28 days)
        val nextFeb = recurrenceCalculator.nextOccurrence(base31st, null, SolarDate(2026, 2, 1), RepeatRule.Monthly)
        assertEquals(SolarDate(2026, 2, 28), nextFeb)

        // Apr 2026 (30 days)
        val nextApr = recurrenceCalculator.nextOccurrence(base31st, null, SolarDate(2026, 4, 1), RepeatRule.Monthly)
        assertEquals(SolarDate(2026, 4, 30), nextApr)
    }

    /** Matrix Case 10: Cross-year Countdown */
    @Test
    fun test10_CrossYearCountdown() {
        val today = SolarDate(2026, 12, 30)
        val target = SolarDate(2027, 1, 2)
        val status = CountdownCalculator.calculate(today, target)
        assertEquals(EventCountdownStatus.Countdown(3), status)
    }

    /** Matrix Case 11: Today Status */
    @Test
    fun test11_TodayStatus() {
        val today = SolarDate(2026, 10, 5)
        val target = SolarDate(2026, 10, 5)
        val status = CountdownCalculator.calculate(today, target)
        assertEquals(EventCountdownStatus.Today, status)
    }

    /** Matrix Case 12: CountedPast Status */
    @Test
    fun test12_CountedPastStatus() {
        val today = SolarDate(2026, 10, 5)
        val pastDate = SolarDate(2026, 9, 25)
        val status = CountdownCalculator.calculate(today, pastDate)
        assertEquals(EventCountdownStatus.CountedPast(10), status)
    }

    /** Property Test: 1000+ consecutive days round-trip identity */
    @Test
    fun test13_RoundTripPropertyStyleSolarLunar() {
        var date = LocalDate.of(2024, 1, 1)
        val end = LocalDate.of(2027, 1, 1) // ~1096 days

        while (!date.isAfter(end)) {
            val solar = SolarDate.fromLocalDate(date)
            val lunar = calendar.solarToLunar(solar)
            val roundTrip = calendar.lunarToSolar(lunar)
            assertEquals("Mismatch for date $solar", solar, roundTrip)
            date = date.plusDays(1)
        }
    }
}
