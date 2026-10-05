package com.cherish.app.date.calc

import com.cherish.app.date.lunar.DefaultLunarCalendar
import com.cherish.app.date.model.LunarDate
import com.cherish.app.date.model.RepeatRule
import com.cherish.app.date.model.RepeatUnit
import com.cherish.app.date.model.SolarDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecurrenceCalculatorTest {

    private val calculator = RecurrenceCalculator(DefaultLunarCalendar())

    @Test
    fun testNoneRuleReturnsDateOnlyIfNotExpired() {
        val base = SolarDate(2027, 4, 27)
        val today = SolarDate(2026, 10, 5)

        val next = calculator.nextOccurrence(base, null, today, RepeatRule.None)
        assertEquals(base, next)

        val pastToday = SolarDate(2028, 1, 1)
        val pastNext = calculator.nextOccurrence(base, null, pastToday, RepeatRule.None)
        assertNull(pastNext)
    }

    @Test
    fun testDailyRecurrence() {
        val base = SolarDate(2026, 1, 1)
        val today = SolarDate(2026, 10, 5)

        val next = calculator.nextOccurrence(base, null, today, RepeatRule.Daily)
        assertEquals(today, next)
    }

    @Test
    fun testMonthlyRecurrenceClipsAtMonthEnd() {
        // Event set on 31st
        val base = SolarDate(2026, 1, 31)

        // On Feb 1st, 2026 -> next is Feb 28th (non-leap year)
        val refFeb = SolarDate(2026, 2, 1)
        val nextFeb = calculator.nextOccurrence(base, null, refFeb, RepeatRule.Monthly)
        assertEquals(SolarDate(2026, 2, 28), nextFeb)

        // On Mar 1st, 2026 -> next is Mar 31st
        val refMar = SolarDate(2026, 3, 1)
        val nextMar = calculator.nextOccurrence(base, null, refMar, RepeatRule.Monthly)
        assertEquals(SolarDate(2026, 3, 31), nextMar)

        // On Apr 1st, 2026 -> next is Apr 30th
        val refApr = SolarDate(2026, 4, 1)
        val nextApr = calculator.nextOccurrence(base, null, refApr, RepeatRule.Monthly)
        assertEquals(SolarDate(2026, 4, 30), nextApr)
    }

    @Test
    fun testYearlySolarFeb29Strategy() {
        // Birthday on leap day 2024-02-29
        val base = SolarDate(2024, 2, 29)

        // In 2025 (normal year) -> degrades to 2025-02-28
        val ref2025 = SolarDate(2025, 1, 1)
        val next2025 = calculator.nextOccurrence(base, null, ref2025, RepeatRule.Yearly)
        assertEquals(SolarDate(2025, 2, 28), next2025)

        // In 2028 (leap year) -> resolves to 2028-02-29
        val ref2028 = SolarDate(2028, 1, 1)
        val next2028 = calculator.nextOccurrence(base, null, ref2028, RepeatRule.Yearly)
        assertEquals(SolarDate(2028, 2, 29), next2028)
    }

    @Test
    fun testYearlyLunarRegularBirthday() {
        // Lunar birthday: 5th month 5th day (端午)
        val baseLunar = LunarDate(2026, 5, 5, isLeapMonth = false)
        val calendar = DefaultLunarCalendar()
        val baseSolar = calendar.lunarToSolar(baseLunar)

        // Reference: today 2026-10-05 (which is after 2026's Dragon Boat festival on 2026-06-19)
        val today = SolarDate(2026, 10, 5)
        val nextSolar = calculator.nextOccurrence(baseSolar, baseLunar, today, RepeatRule.Yearly)

        // Next Dragon boat festival is in lunar 2027: 2027年农历五月初五 -> 2027-06-09
        val expectedSolar = calendar.lunarToSolar(LunarDate(2027, 5, 5, isLeapMonth = false))
        assertEquals(expectedSolar, nextSolar)
    }

    @Test
    fun testYearlyLunarLeapMonthBirthdayDegradesToNormalMonth() {
        // Birthday on leap 4th month 1st day (闰四月初一) in 2020 (which had leap 4th month)
        val baseLunar = LunarDate(2020, 4, 1, isLeapMonth = true)
        val calendar = DefaultLunarCalendar()
        val baseSolar = calendar.lunarToSolar(baseLunar)

        // Reference: 2026-01-01. Note: 2026 has no leap 4th month.
        // It must degrade to 2026's normal 4th month 1st day (农历四月初一)!
        val ref2026 = SolarDate(2026, 1, 1)
        val nextSolar = calculator.nextOccurrence(baseSolar, baseLunar, ref2026, RepeatRule.Yearly)

        val expectedLunar = LunarDate(2026, 4, 1, isLeapMonth = false)
        val expectedSolar = calendar.lunarToSolar(expectedLunar)
        assertEquals(expectedSolar, nextSolar)
    }

    @Test
    fun testCustomRecurrence() {
        val base = SolarDate(2026, 10, 1)
        val today = SolarDate(2026, 10, 5)

        // Every 3 days: 10-01, 10-04, 10-07 -> next is 10-07
        val ruleEvery3Days = RepeatRule.Custom(3, RepeatUnit.DAY)
        val next = calculator.nextOccurrence(base, null, today, ruleEvery3Days)
        assertEquals(SolarDate(2026, 10, 7), next)
    }

    @Test
    fun nextOccurrence_customWeekly_advancesByExact7Days() {
        val base = SolarDate(2026, 10, 1) // Thursday
        val ruleWeekly = RepeatRule.Custom(1, RepeatUnit.WEEK)

        // Reference is Monday 2026-10-05 -> next occurrence is Thursday 2026-10-08
        val next1 = calculator.nextOccurrence(base, null, SolarDate(2026, 10, 5), ruleWeekly)
        assertEquals(SolarDate(2026, 10, 8), next1)

        // Reference is on the occurrence day 2026-10-08 -> next occurrence is today (2026-10-08)
        val next2 = calculator.nextOccurrence(base, null, SolarDate(2026, 10, 8), ruleWeekly)
        assertEquals(SolarDate(2026, 10, 8), next2)

        // Reference is day after 2026-10-09 -> next occurrence is Thursday 2026-10-15
        val next3 = calculator.nextOccurrence(base, null, SolarDate(2026, 10, 9), ruleWeekly)
        assertEquals(SolarDate(2026, 10, 15), next3)
    }
}
