package com.cherish.app.editor.sanitizer

import com.cherish.app.date.lunar.DefaultLunarCalendar
import com.cherish.app.date.model.LunarDate
import com.cherish.app.date.model.SolarDate
import com.cherish.app.editor.model.EventEditorState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DatePickerSanitizerTest {

    private val calendar = DefaultLunarCalendar()

    @Test
    fun `solar leap year february 29 is preserved as valid`() {
        val sanitized = DatePickerSanitizer.sanitizeSolar(2028, 2, 29)
        assertEquals(2028, sanitized.year)
        assertEquals(2, sanitized.month)
        assertEquals(29, sanitized.day)
    }

    @Test
    fun `solar non-leap year february 29 is clamped to february 28`() {
        val sanitized = DatePickerSanitizer.sanitizeSolar(2027, 2, 29)
        assertEquals(2027, sanitized.year)
        assertEquals(2, sanitized.month)
        assertEquals(28, sanitized.day)
    }

    @Test
    fun `solar 30-day month day 31 is clamped to day 30`() {
        // April has 30 days
        val april = DatePickerSanitizer.sanitizeSolar(2026, 4, 31)
        assertEquals(30, april.day)

        // September has 30 days
        val sept = DatePickerSanitizer.sanitizeSolar(2026, 9, 31)
        assertEquals(30, sept.day)

        // October has 31 days (preserved)
        val oct = DatePickerSanitizer.sanitizeSolar(2026, 10, 31)
        assertEquals(31, oct.day)
    }

    @Test
    fun `lunar date clamps day to valid month length`() {
        // Test clamping day to valid month length (29 or 30 days in lunar calendar)
        val sanitized = DatePickerSanitizer.sanitizeLunar(2026, 8, 31, isLeap = false, calendar = calendar)
        assertTrue(sanitized.day in 29..30)
    }

    @Test
    fun `lunar leap month flag is reset to false if year has no such leap month`() {
        // In 2026, there is no leap month 1
        val actualLeap = calendar.getLeapMonth(2026)
        val sanitized = DatePickerSanitizer.sanitizeLunar(2026, 1, 15, isLeap = true, calendar = calendar)
        if (actualLeap != 1) {
            assertFalse(sanitized.isLeapMonth)
        }
    }

    @Test
    fun `validateAndBuildEvent fails on empty or whitespace title`() {
        val stateEmpty = EventEditorState.createDefault(
            today = SolarDate(2026, 10, 5),
            initialLunar = LunarDate(2026, 8, 25),
        ).copy(title = "   ")

        val result = DatePickerSanitizer.validateAndBuildEvent(stateEmpty, calendar)
        assertTrue(result.isFailure)
        assertEquals("事件名称不能为空", result.exceptionOrNull()?.message)
    }

    @Test
    fun `validateAndBuildEvent fails on title longer than 50 chars`() {
        val longTitle = "A".repeat(51)
        val stateLong = EventEditorState.createDefault(
            today = SolarDate(2026, 10, 5),
            initialLunar = LunarDate(2026, 8, 25),
        ).copy(title = longTitle)

        val result = DatePickerSanitizer.validateAndBuildEvent(stateLong, calendar)
        assertTrue(result.isFailure)
        assertEquals("事件名称不能超过 50 个字符", result.exceptionOrNull()?.message)
    }

    @Test
    fun `validateAndBuildEvent succeeds and trims title properly`() {
        val state = EventEditorState.createDefault(
            today = SolarDate(2026, 10, 5),
            initialLunar = LunarDate(2026, 8, 25),
        ).copy(
            title = "  妈妈生日  ",
            emoji = " 🎂 ",
        )

        val result = DatePickerSanitizer.validateAndBuildEvent(state, calendar)
        assertTrue(result.isSuccess)
        val event = result.getOrThrow()
        assertEquals("妈妈生日", event.title)
        assertEquals("🎂", event.emoji)
    }
}
