package com.cherish.app.date.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DateModelsTest {

    @Test
    fun testSolarDatePropertiesAndComparison() {
        val date1 = SolarDate(2026, 10, 5)
        val date2 = SolarDate(2026, 10, 6)
        val date3 = SolarDate(2027, 1, 1)

        assertEquals(2026, date1.year)
        assertEquals(10, date1.month)
        assertEquals(5, date1.day)
        assertEquals("2026-10-05", date1.toString())
        assertEquals(LocalDate.of(2026, 10, 5), date1.toLocalDate())

        assertTrue(date1 < date2)
        assertTrue(date2 < date3)
        assertEquals(date1, SolarDate.fromLocalDate(LocalDate.of(2026, 10, 5)))
    }

    @Test
    fun testLunarDatePropertiesAndComparison() {
        val normal = LunarDate(2023, 2, 1, isLeapMonth = false)
        val leap = LunarDate(2023, 2, 1, isLeapMonth = true)
        val laterDay = LunarDate(2023, 2, 2, isLeapMonth = true)

        assertEquals(2023, normal.year)
        assertEquals(2, normal.month)
        assertEquals(1, normal.day)
        assertFalse(normal.isLeapMonth)
        assertTrue(leap.isLeapMonth)

        assertEquals("农历2023年02月01日", normal.toString())
        assertEquals("农历2023年闰02月01日", leap.toString())

        assertTrue(normal < leap)
        assertTrue(leap < laterDay)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testSolarDateRejectsInvalidMonth() {
        SolarDate(2026, 13, 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testLunarDateRejectsInvalidDay() {
        LunarDate(2026, 5, 31)
    }
}
