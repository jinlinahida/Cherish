package com.cherish.app.home.mapper

import com.cherish.app.date.model.LunarDate
import com.cherish.app.date.model.RepeatRule
import com.cherish.app.date.model.RepeatUnit
import com.cherish.app.date.model.SolarDate
import com.cherish.app.event.model.CountdownEvent
import com.cherish.app.event.model.EventBackground
import com.cherish.app.event.model.EventCategory
import com.cherish.app.event.model.EventDate
import com.cherish.app.home.model.CountdownDisplayStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeEventMapperTest {

    private val referenceToday = SolarDate(2026, 10, 5)

    @Test
    fun `future solar non-repeating event maps to COUNTDOWN`() {
        val event = CountdownEvent(
            id = "test-1",
            title = "New Year 2027",
            emoji = "🎆",
            eventDate = EventDate.Solar(SolarDate(2027, 1, 1)),
            repeatRule = RepeatRule.None,
            isPinned = false,
        )

        val uiModel = HomeEventMapper.toUiModel(event, referenceToday)

        assertEquals(CountdownDisplayStatus.COUNTDOWN, uiModel.status)
        assertEquals(88, uiModel.daysCount)
        assertEquals("DAYS", uiModel.unitLabel)
        assertEquals("2027.01.01", uiModel.targetDateFormatted)
        assertFalse(uiModel.isPinned)
    }

    @Test
    fun `event occurring today maps to TODAY status`() {
        val event = CountdownEvent(
            id = "test-today",
            title = "Current Event",
            eventDate = EventDate.Solar(referenceToday),
            repeatRule = RepeatRule.None,
            isPinned = true,
        )

        val uiModel = HomeEventMapper.toUiModel(event, referenceToday)

        assertEquals(CountdownDisplayStatus.TODAY, uiModel.status)
        assertEquals(0, uiModel.daysCount)
        assertEquals("TODAY", uiModel.unitLabel)
        assertTrue(uiModel.isPinned)
    }

    @Test
    fun `past solar non-repeating event maps to PAST with DAYS AGO`() {
        val event = CountdownEvent(
            id = "test-past",
            title = "National Day",
            eventDate = EventDate.Solar(SolarDate(2026, 10, 1)),
            repeatRule = RepeatRule.None,
        )

        val uiModel = HomeEventMapper.toUiModel(event, referenceToday)

        assertEquals(CountdownDisplayStatus.PAST, uiModel.status)
        assertEquals(4, uiModel.daysCount)
        assertEquals("DAYS AGO", uiModel.unitLabel)
        assertEquals("2026.10.01", uiModel.targetDateFormatted)
    }

    @Test
    fun `single day differences format singular DAY and DAY AGO`() {
        val tomorrowEvent = CountdownEvent(
            id = "tomorrow",
            title = "Tomorrow",
            eventDate = EventDate.Solar(SolarDate(2026, 10, 6)),
        )
        val tomorrowUi = HomeEventMapper.toUiModel(tomorrowEvent, referenceToday)
        assertEquals(1, tomorrowUi.daysCount)
        assertEquals("DAY", tomorrowUi.unitLabel)

        val yesterdayEvent = CountdownEvent(
            id = "yesterday",
            title = "Yesterday",
            eventDate = EventDate.Solar(SolarDate(2026, 10, 4)),
        )
        val yesterdayUi = HomeEventMapper.toUiModel(yesterdayEvent, referenceToday)
        assertEquals(1, yesterdayUi.daysCount)
        assertEquals("DAY AGO", yesterdayUi.unitLabel)
    }

    @Test
    fun `recurring yearly event whose original date passed advances to next occurrence`() {
        // Birthday on May 1st was in 2026-05-01 (past relative to 2026-10-05)
        // With Yearly repeat, next occurrence is 2027-05-01
        val event = CountdownEvent(
            id = "bday",
            title = "My Birthday",
            category = EventCategory.BIRTHDAY,
            eventDate = EventDate.Solar(SolarDate(2026, 5, 1)),
            repeatRule = RepeatRule.Yearly,
        )

        val uiModel = HomeEventMapper.toUiModel(event, referenceToday)

        assertEquals(CountdownDisplayStatus.COUNTDOWN, uiModel.status)
        assertEquals(SolarDate(2027, 5, 1), uiModel.targetSolarDate)
        assertEquals("2027.05.01", uiModel.targetDateFormatted)
        assertTrue(uiModel.daysCount > 0)
    }

    @Test
    fun `lunar recurring event accurately maps to next lunar occurrence`() {
        // Lunar Spring Festival: Month 1, Day 1
        // Today is 2026-10-05. Lunar 2026 1-1 passed. Next is Lunar 2027-01-01 (Solar 2027-02-06)
        val event = CountdownEvent(
            id = "lunar-spring",
            title = "Spring Festival",
            emoji = "🧨",
            eventDate = EventDate.Lunar(LunarDate(2026, 1, 1)),
            repeatRule = RepeatRule.Yearly,
            isPinned = true,
            background = EventBackground.Color(0xFFE53935),
        )

        val uiModel = HomeEventMapper.toUiModel(event, referenceToday)

        assertEquals(CountdownDisplayStatus.COUNTDOWN, uiModel.status)
        assertEquals(SolarDate(2027, 2, 6), uiModel.targetSolarDate)
        assertEquals("2027.02.06 农历", uiModel.targetDateFormatted)
        assertTrue(uiModel.isPinned)
        assertEquals(EventBackground.Color(0xFFE53935), uiModel.background)
    }

    @Test
    fun `custom repeat rule advances accurately`() {
        // Repeats every 2 weeks from 2026-10-01
        // 2026-10-01 + 14 days = 2026-10-15
        val event = CountdownEvent(
            id = "biweekly",
            title = "Sprint Review",
            eventDate = EventDate.Solar(SolarDate(2026, 10, 1)),
            repeatRule = RepeatRule.Custom(interval = 2, unit = RepeatUnit.WEEK),
        )

        val uiModel = HomeEventMapper.toUiModel(event, referenceToday)

        assertEquals(CountdownDisplayStatus.COUNTDOWN, uiModel.status)
        assertEquals(SolarDate(2026, 10, 15), uiModel.targetSolarDate)
        assertEquals(10, uiModel.daysCount)
    }
}
