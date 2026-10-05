package com.cherish.app.editor.model

import com.cherish.app.date.model.LunarDate
import com.cherish.app.date.model.RepeatRule
import com.cherish.app.date.model.SolarDate
import com.cherish.app.event.model.CountdownEvent
import com.cherish.app.event.model.EventBackground
import com.cherish.app.event.model.EventCategory
import com.cherish.app.event.model.EventDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventEditorValidationTest {

    private val sampleSolar = SolarDate(2026, 10, 5)
    private val sampleLunar = LunarDate(2026, 8, 25)

    @Test
    fun `createDefault initializes with today and empty title`() {
        val defaultState = EventEditorState.createDefault(sampleSolar, sampleLunar)

        assertTrue(defaultState.isCreateMode)
        assertEquals("", defaultState.title)
        assertEquals("📅", defaultState.emoji)
        assertFalse(defaultState.isLunar)
        assertEquals(sampleSolar, defaultState.solarDate)
        assertEquals(sampleLunar, defaultState.lunarDate)
        assertEquals(RepeatRule.None, defaultState.repeatRule)
        assertEquals(EventCategory.GENERAL, defaultState.category)
        assertFalse(defaultState.isPinned)
        assertEquals(EventBackground.Default, defaultState.background)
    }

    @Test
    fun `fromEvent populates all fields from existing solar event`() {
        val event = CountdownEvent(
            id = "ev-123",
            title = "结婚周年",
            emoji = "💍",
            category = EventCategory.ANNIVERSARY,
            eventDate = EventDate.Solar(SolarDate(2026, 12, 20)),
            repeatRule = RepeatRule.Yearly,
            isPinned = true,
            background = EventBackground.Color(0xFFE91E63),
            notes = "订花和餐厅",
        )

        val state = EventEditorState.fromEvent(event, sampleSolar, sampleLunar)

        assertFalse(state.isCreateMode)
        assertEquals("ev-123", state.eventId)
        assertEquals("结婚周年", state.title)
        assertEquals("💍", state.emoji)
        assertFalse(state.isLunar)
        assertEquals(SolarDate(2026, 12, 20), state.solarDate)
        assertEquals(RepeatRule.Yearly, state.repeatRule)
        assertEquals(EventCategory.ANNIVERSARY, state.category)
        assertTrue(state.isPinned)
        assertEquals(EventBackground.Color(0xFFE91E63), state.background)
        assertEquals("订花和餐厅", state.notes)
    }

    @Test
    fun `fromEvent populates lunar date when event date is lunar`() {
        val lunar = LunarDate(2026, 1, 1, isLeapMonth = false)
        val event = CountdownEvent(
            id = "ev-spring",
            title = "春节",
            emoji = "🧨",
            category = EventCategory.HOLIDAY,
            eventDate = EventDate.Lunar(lunar),
            repeatRule = RepeatRule.Yearly,
        )

        val state = EventEditorState.fromEvent(event, sampleSolar, sampleLunar)

        assertTrue(state.isLunar)
        assertEquals(lunar, state.lunarDate)
    }
}
