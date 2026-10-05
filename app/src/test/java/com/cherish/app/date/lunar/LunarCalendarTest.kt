package com.cherish.app.date.lunar

import com.cherish.app.date.model.LunarDate
import com.cherish.app.date.model.SolarDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
    fun testLeapMonthQuery() {
        // 2023 has leap month 2 (闰二月)
        assertEquals(2, calendar.getLeapMonth(2023))
        // 2026 has no leap month (0)
        assertEquals(0, calendar.getLeapMonth(2026))
    }
}
