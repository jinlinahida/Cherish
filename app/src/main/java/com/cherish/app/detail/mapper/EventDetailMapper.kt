package com.cherish.app.detail.mapper

import com.cherish.app.date.calc.CountdownCalculator
import com.cherish.app.date.calc.EventCountdownStatus
import com.cherish.app.date.calc.RecurrenceCalculator
import com.cherish.app.date.lunar.DefaultLunarCalendar
import com.cherish.app.date.lunar.LunarCalendar
import com.cherish.app.date.model.RepeatRule
import com.cherish.app.date.model.RepeatUnit
import com.cherish.app.date.model.SolarDate
import com.cherish.app.detail.model.EventDetailUiModel
import com.cherish.app.event.model.CountdownEvent
import com.cherish.app.event.model.EventCategory
import com.cherish.app.event.model.EventDate
import com.cherish.app.home.model.CountdownDisplayStatus

/**
 * Pure Kotlin mapper that transforms a [CountdownEvent] into an [EventDetailUiModel].
 *
 * Uses the Phase 2 date engine ([CountdownCalculator], [RecurrenceCalculator], [LunarCalendar])
 * to compute current target dates and countdown units for presentation.
 */
class EventDetailMapper(
    private val calendar: LunarCalendar = DefaultLunarCalendar(),
    private val recurrenceCalculator: RecurrenceCalculator = RecurrenceCalculator(calendar),
) {

    fun toUiModel(
        event: CountdownEvent,
        today: SolarDate,
    ): EventDetailUiModel {
        val (baseSolar, baseLunar) = when (val eventDate = event.eventDate) {
            is EventDate.Solar -> Pair(eventDate.date, null)
            is EventDate.Lunar -> Pair(calendar.lunarToSolar(eventDate.date), eventDate.date)
        }

        val targetSolarDate: SolarDate = if (event.repeatRule == RepeatRule.None) {
            baseSolar
        } else {
            recurrenceCalculator.nextOccurrence(
                baseSolar = baseSolar,
                baseLunar = baseLunar,
                referenceDate = today,
                rule = event.repeatRule,
            ) ?: baseSolar
        }

        val countdownStatus = CountdownCalculator.calculate(
            referenceDate = today,
            targetDate = targetSolarDate,
        )

        val (daysCount, displayStatus, heroNumber, heroUnit) = when (countdownStatus) {
            is EventCountdownStatus.Countdown -> {
                val unit = if (countdownStatus.days == 1L) "天" else "天"
                Tuple4(countdownStatus.days.toInt(), CountdownDisplayStatus.COUNTDOWN, "${countdownStatus.days}", unit)
            }
            is EventCountdownStatus.Today -> {
                Tuple4(0, CountdownDisplayStatus.TODAY, "今天", "TODAY")
            }
            is EventCountdownStatus.CountedPast -> {
                Tuple4(countdownStatus.days.toInt(), CountdownDisplayStatus.PAST, "${countdownStatus.days}", "天前")
            }
        }

        val targetFormatted = "%04d年%d月%d日".format(
            targetSolarDate.year,
            targetSolarDate.month,
            targetSolarDate.day,
        )

        val calendarDesc = when (val eventDate = event.eventDate) {
            is EventDate.Solar -> "公历 %04d年%d月%d日".format(
                eventDate.date.year,
                eventDate.date.month,
                eventDate.date.day,
            )
            is EventDate.Lunar -> {
                val leapPrefix = if (eventDate.date.isLeapMonth) "闰" else ""
                "农历 %04d年%s%d月%d日".format(
                    eventDate.date.year,
                    leapPrefix,
                    eventDate.date.month,
                    eventDate.date.day,
                )
            }
        }

        val recurrenceDesc = formatRepeatRule(event.repeatRule)
        val categoryDesc = formatCategory(event.category)

        return EventDetailUiModel(
            event = event,
            targetSolarDate = targetSolarDate,
            daysCount = daysCount,
            status = displayStatus,
            heroNumberText = heroNumber,
            heroUnitText = heroUnit,
            targetDateDescription = targetFormatted,
            calendarTypeDescription = calendarDesc,
            recurrenceDescription = recurrenceDesc,
            categoryDescription = categoryDesc,
            isPinned = event.isPinned,
            background = event.background,
            notes = event.notes,
        )
    }

    private fun formatRepeatRule(rule: RepeatRule): String = when (rule) {
        RepeatRule.None -> "不重复"
        RepeatRule.Daily -> "每天重复"
        RepeatRule.Monthly -> "每月重复"
        RepeatRule.Yearly -> "每年重复"
        is RepeatRule.Custom -> when (rule.unit) {
            RepeatUnit.DAY -> "每 ${rule.interval} 天"
            RepeatUnit.WEEK -> "每 ${rule.interval} 周"
            RepeatUnit.MONTH -> "每 ${rule.interval} 个月"
            RepeatUnit.YEAR -> "每 ${rule.interval} 年"
        }
    }

    private fun formatCategory(category: EventCategory): String = when (category) {
        EventCategory.GENERAL -> "通用"
        EventCategory.BIRTHDAY -> "生日"
        EventCategory.ANNIVERSARY -> "纪念日"
        EventCategory.HOLIDAY -> "节日"
        EventCategory.WORK -> "工作"
        EventCategory.LIFE -> "生活"
        EventCategory.OTHER -> "其他"
    }

    private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

    companion object {
        private val defaultInstance = EventDetailMapper()

        fun toUiModel(
            event: CountdownEvent,
            today: SolarDate,
        ): EventDetailUiModel = defaultInstance.toUiModel(event, today)
    }
}
