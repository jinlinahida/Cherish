package com.cherish.app.editor.model

import com.cherish.app.date.model.LunarDate
import com.cherish.app.date.model.RepeatRule
import com.cherish.app.date.model.RepeatUnit
import com.cherish.app.date.model.SolarDate
import com.cherish.app.editor.sanitizer.DatePickerSanitizer
import com.cherish.app.event.model.CountdownEvent
import com.cherish.app.event.model.EventBackground
import com.cherish.app.event.model.EventCategory
import com.cherish.app.event.model.EventColor
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
        assertEquals(EventColor.Default, defaultState.color)
        assertEquals(EventBackground.Default, defaultState.background)
        assertEquals("", defaultState.notes)
    }

    @Test
    fun `fromEvent populates all fields from existing solar event with decoupled color and background`() {
        val event = CountdownEvent(
            id = "ev-123",
            title = "结婚周年",
            emoji = "💍",
            category = EventCategory.ANNIVERSARY,
            eventDate = EventDate.Solar(SolarDate(2026, 12, 20)),
            repeatRule = RepeatRule.Yearly,
            isPinned = true,
            color = EventColor.Rose,
            background = EventBackground.Gradient(0xFF8E2DE2L, 0xFF4A00E0L),
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
        assertEquals(EventColor.Rose, state.color)
        assertEquals(EventBackground.Gradient(0xFF8E2DE2L, 0xFF4A00E0L), state.background)
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

    @Test
    fun `validateAndBuildEvent builds valid event preserving custom color and background image`() {
        val state = EventEditorState(
            eventId = "ev-photo-bg",
            title = "毕业旅行",
            emoji = "✈️",
            isLunar = false,
            solarDate = SolarDate(2027, 6, 15),
            repeatRule = RepeatRule.Custom(14, RepeatUnit.DAY),
            category = EventCategory.LIFE,
            isPinned = true,
            color = EventColor.Ocean,
            background = EventBackground.Image(path = "bg_grad_photo.jpg", dimAlpha = 0.50f),
            notes = "订机票",
        )

        val result = DatePickerSanitizer.validateAndBuildEvent(state)
        assertTrue(result.isSuccess)

        val event = result.getOrThrow()
        assertEquals("ev-photo-bg", event.id)
        assertEquals("毕业旅行", event.title)
        assertEquals("✈️", event.emoji)
        assertEquals(EventDate.Solar(SolarDate(2027, 6, 15)), event.eventDate)
        assertEquals(RepeatRule.Custom(14, RepeatUnit.DAY), event.repeatRule)
        assertEquals(EventCategory.LIFE, event.category)
        assertTrue(event.isPinned)
        assertEquals(EventColor.Ocean, event.color)
        assertEquals(EventBackground.Image("bg_grad_photo.jpg", 0.50f), event.background)
        assertEquals("订机票", event.notes)
    }

    @Test
    fun `validateAndBuildEvent fails on empty or blank title`() {
        val blankTitleState = EventEditorState.createDefault(sampleSolar, sampleLunar).copy(
            title = "   ",
        )
        val result = DatePickerSanitizer.validateAndBuildEvent(blankTitleState)
        assertTrue(result.isFailure)
        assertEquals("事件名称不能为空", result.exceptionOrNull()?.message)
    }

    @Test
    fun `validateAndBuildEvent fails on title exceeding 50 characters`() {
        val longTitle = "A".repeat(51)
        val state = EventEditorState.createDefault(sampleSolar, sampleLunar).copy(
            title = longTitle,
        )
        val result = DatePickerSanitizer.validateAndBuildEvent(state)
        assertTrue(result.isFailure)
        assertEquals("事件名称不能超过 50 个字符", result.exceptionOrNull()?.message)
    }

    @Test
    fun `validateAndBuildEvent generates new uuid in create mode`() {
        val state = EventEditorState.createDefault(sampleSolar, sampleLunar).copy(
            title = "新开户",
        )
        val result = DatePickerSanitizer.validateAndBuildEvent(state)
        assertTrue(result.isSuccess)
        val event = result.getOrThrow()
        assertTrue(event.id.isNotBlank())
    }

    @Test
    fun `custom repeat intervals preserve unit and number`() {
        val units = listOf(
            RepeatUnit.DAY,
            RepeatUnit.WEEK,
            RepeatUnit.MONTH,
            RepeatUnit.YEAR,
        )
        for ((idx, unit) in units.withIndex()) {
            val interval = (idx + 1) * 3
            val rule = RepeatRule.Custom(interval = interval, unit = unit)
            val state = EventEditorState.createDefault(sampleSolar, sampleLunar).copy(
                title = "循环测试 $idx",
                repeatRule = rule,
            )
            val builtEvent = DatePickerSanitizer.validateAndBuildEvent(state).getOrThrow()
            assertEquals(rule, builtEvent.repeatRule)
        }
    }
}
