package com.cherish.app.event.model

import com.cherish.app.date.model.LunarDate
import com.cherish.app.date.model.RepeatRule
import com.cherish.app.date.model.RepeatUnit
import com.cherish.app.date.model.SolarDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CountdownEventTest {

    @Test
    fun `default fields initialize properly for solar event`() {
        val solarDate = SolarDate(2026, 10, 5)
        val event = CountdownEvent(
            title = "National Day",
            eventDate = EventDate.Solar(solarDate),
        )

        assertNotNull(event.id)
        assertTrue(event.id.isNotBlank())
        assertEquals("National Day", event.title)
        assertEquals("📅", event.emoji)
        assertEquals(EventCategory.GENERAL, event.category)
        assertFalse(event.isPinned)
        assertEquals(RepeatRule.None, event.repeatRule)
        assertEquals(EventBackground.Default, event.background)
        assertEquals("", event.notes)
        assertFalse(event.eventDate.isLunar)
    }

    @Test
    fun `lunar event correctly preserves lunar calendar attributes`() {
        val lunarDate = LunarDate(2026, 5, 5, isLeapMonth = false)
        val event = CountdownEvent(
            title = "Dragon Boat Festival",
            emoji = "🛶",
            category = EventCategory.HOLIDAY,
            eventDate = EventDate.Lunar(lunarDate),
            repeatRule = RepeatRule.Yearly,
            isPinned = true,
            background = EventBackground.Color(0xFF1E88E5),
            notes = "Eat zongzi",
        )

        assertTrue(event.eventDate.isLunar)
        assertEquals(lunarDate, (event.eventDate as EventDate.Lunar).date)
        assertEquals("🛶", event.emoji)
        assertEquals(EventCategory.HOLIDAY, event.category)
        assertTrue(event.isPinned)
        assertEquals(RepeatRule.Yearly, event.repeatRule)
        assertEquals(EventBackground.Color(0xFF1E88E5), event.background)
        assertEquals("Eat zongzi", event.notes)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank title throws IllegalArgumentException`() {
        CountdownEvent(
            title = "   ",
            eventDate = EventDate.Solar(SolarDate(2026, 1, 1)),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank id throws IllegalArgumentException`() {
        CountdownEvent(
            id = "",
            title = "Valid Title",
            eventDate = EventDate.Solar(SolarDate(2026, 1, 1)),
        )
    }

    @Test
    fun `copy operation allows updating pinned and background states cleanly`() {
        val original = CountdownEvent(
            id = "test-123",
            title = "Birthday",
            eventDate = EventDate.Solar(SolarDate(2026, 12, 25)),
            isPinned = false,
        )

        val updated = original.copy(
            isPinned = true,
            background = EventBackground.Gradient(0xFF000000, 0xFFFFFFFF, 45f),
        )

        assertEquals("test-123", updated.id)
        assertTrue(updated.isPinned)
        assertEquals(EventBackground.Gradient(0xFF000000, 0xFFFFFFFF, 45f), updated.background)
        assertNotEquals(original, updated)
    }

    @Test
    fun `event background variants equality`() {
        assertEquals(EventBackground.Default, EventBackground.Default)
        assertEquals(EventBackground.Color(0xAABBCCDD), EventBackground.Color(0xAABBCCDD))
        assertEquals(
            EventBackground.Gradient(0x11223344, 0x55667788, 90f),
            EventBackground.Gradient(0x11223344, 0x55667788, 90f),
        )
        assertEquals(EventBackground.Pattern("dots"), EventBackground.Pattern("dots"))
    }

    @Test
    fun `custom repeat rule can be assigned to countdown event`() {
        val event = CountdownEvent(
            title = "Water Plants",
            eventDate = EventDate.Solar(SolarDate(2026, 5, 1)),
            repeatRule = RepeatRule.Custom(interval = 3, unit = RepeatUnit.DAY),
        )

        val rule = event.repeatRule as RepeatRule.Custom
        assertEquals(3, rule.interval)
        assertEquals(RepeatUnit.DAY, rule.unit)
    }
}
