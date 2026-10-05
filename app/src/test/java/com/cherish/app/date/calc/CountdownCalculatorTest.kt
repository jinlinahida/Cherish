package com.cherish.app.date.calc

import com.cherish.app.date.model.SolarDate
import org.junit.Assert.assertEquals
import org.junit.Test

class CountdownCalculatorTest {

    @Test
    fun testCountdownFutureDays() {
        val today = SolarDate(2026, 12, 30)
        val target = SolarDate(2027, 1, 2)

        val status = CountdownCalculator.calculate(today, target)
        assertEquals(EventCountdownStatus.Countdown(3), status)
    }

    @Test
    fun testTodayStatus() {
        val today = SolarDate(2026, 10, 5)
        val target = SolarDate(2026, 10, 5)

        val status = CountdownCalculator.calculate(today, target)
        assertEquals(EventCountdownStatus.Today, status)
    }

    @Test
    fun testCountedPastDays() {
        val today = SolarDate(2026, 10, 5)
        val target = SolarDate(2026, 9, 25)

        val status = CountdownCalculator.calculate(today, target)
        assertEquals(EventCountdownStatus.CountedPast(10), status)
    }
}
