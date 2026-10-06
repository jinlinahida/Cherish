package com.cherish.app.editor.sanitizer

import com.cherish.app.date.lunar.DefaultLunarCalendar
import com.cherish.app.date.lunar.LunarCalendar
import com.cherish.app.date.model.LunarDate
import com.cherish.app.date.model.SolarDate
import com.cherish.app.editor.model.EventEditorState
import com.cherish.app.event.model.CountdownEvent
import com.cherish.app.event.model.EventDate
import java.time.YearMonth
import java.util.UUID

/**
 * Validates, bounds-checks and sanitizes dates and editor submissions.
 *
 * Reuses existing Phase 2 engine ([LunarCalendar] and [YearMonth]) so that
 * all leap year and lunar intercalary month calculations remain consistent across Cherish.
 */
object DatePickerSanitizer {

    private const val MIN_YEAR = 1900
    private const val MAX_YEAR = 2100

    /**
     * Returns the maximum number of days in the specified Solar year/month.
     * Correctly evaluates leap years (e.g. 29 days in 2028-02, 28 days in 2027-02).
     */
    fun getDaysInSolarMonth(year: Int, month: Int): Int {
        val y = year.coerceIn(MIN_YEAR, MAX_YEAR)
        val m = month.coerceIn(1, 12)
        return YearMonth.of(y, m).lengthOfMonth()
    }

    /**
     * Sanitizes Solar date inputs, clamping month to 1..12 and day to the valid month length.
     */
    fun sanitizeSolar(year: Int, month: Int, day: Int): SolarDate {
        val y = year.coerceIn(MIN_YEAR, MAX_YEAR)
        val m = month.coerceIn(1, 12)
        val maxDays = getDaysInSolarMonth(y, m)
        val d = day.coerceIn(1, maxDays)
        return SolarDate(y, m, d)
    }

    /**
     * Returns the maximum number of days in the specified Lunar year/month (29 or 30).
     */
    fun getDaysInLunarMonth(
        year: Int,
        month: Int,
        isLeap: Boolean,
        calendar: LunarCalendar = DefaultLunarCalendar(),
    ): Int {
        val y = year.coerceIn(MIN_YEAR, MAX_YEAR)
        val m = month.coerceIn(1, 12)
        val actualLeap = calendar.getLeapMonth(y)
        val validLeap = isLeap && actualLeap == m
        return calendar.getDaysInMonth(y, m, validLeap)
    }

    /**
     * Sanitizes Lunar date inputs, verifying leap month validity and clamping day to valid month length.
     */
    fun sanitizeLunar(
        year: Int,
        month: Int,
        day: Int,
        isLeap: Boolean,
        calendar: LunarCalendar = DefaultLunarCalendar(),
    ): LunarDate {
        val y = year.coerceIn(MIN_YEAR, MAX_YEAR)
        val m = month.coerceIn(1, 12)
        val actualLeap = calendar.getLeapMonth(y)
        val validLeap = isLeap && actualLeap == m
        val maxDays = calendar.getDaysInMonth(y, m, validLeap)
        val d = day.coerceIn(1, maxDays)
        return LunarDate(y, m, d, isLeapMonth = validLeap)
    }

    /**
     * Validates the [state] and constructs a persistent [CountdownEvent].
     *
     * Rules:
     * - Title must not be blank after trimming.
     * - Title length must not exceed 50 characters.
     * - Dates are sanitized to legal calendar bounds.
     */
    fun validateAndBuildEvent(
        state: EventEditorState,
        calendar: LunarCalendar = DefaultLunarCalendar(),
    ): Result<CountdownEvent> {
        val trimmedTitle = state.title.trim()
        if (trimmedTitle.isEmpty()) {
            return Result.failure(IllegalArgumentException("事件名称不能为空"))
        }
        if (trimmedTitle.length > 50) {
            return Result.failure(IllegalArgumentException("事件名称不能超过 50 个字符"))
        }

        val eventDate: EventDate = if (state.isLunar) {
            val sanitized = sanitizeLunar(
                year = state.lunarDate.year,
                month = state.lunarDate.month,
                day = state.lunarDate.day,
                isLeap = state.lunarDate.isLeapMonth,
                calendar = calendar,
            )
            EventDate.Lunar(sanitized)
        } else {
            val sanitized = sanitizeSolar(
                year = state.solarDate.year,
                month = state.solarDate.month,
                day = state.solarDate.day,
            )
            EventDate.Solar(sanitized)
        }

        val event = CountdownEvent(
            id = state.eventId ?: UUID.randomUUID().toString(),
            title = trimmedTitle,
            emoji = state.emoji.trim().ifEmpty { "📅" },
            category = state.category,
            eventDate = eventDate,
            repeatRule = state.repeatRule,
            isPinned = state.isPinned,
            color = state.color,
            background = state.background,
            notes = state.notes.trim(),
        )

        return Result.success(event)
    }
}
