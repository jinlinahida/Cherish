package com.cherish.app.home.mapper

import com.cherish.app.date.calc.CountdownCalculator
import com.cherish.app.date.calc.EventCountdownStatus
import com.cherish.app.date.calc.RecurrenceCalculator
import com.cherish.app.date.lunar.DefaultLunarCalendar
import com.cherish.app.date.lunar.LunarCalendar
import com.cherish.app.date.model.RepeatRule
import com.cherish.app.date.model.SolarDate
import com.cherish.app.event.model.CountdownEvent
import com.cherish.app.event.model.EventDate
import com.cherish.app.home.model.CountdownDisplayStatus
import com.cherish.app.home.model.HomeEventUiModel

/**
 * Pure Kotlin mapper connecting [CountdownEvent] and the Phase 2 date engine
 * ([RecurrenceCalculator], [CountdownCalculator], [LunarCalendar]) into [HomeEventUiModel].
 *
 * Guarantees:
 * - Recurrence rules are respected: past occurrences for recurring events automatically calculate next occurrence.
 * - Non-recurring past events remain in PAST status.
 * - Lunar dates are accurately converted to Gregorian target dates.
 * - Zero temporal logic inside Composable functions.
 */
class HomeEventMapper(
    private val calendar: LunarCalendar = DefaultLunarCalendar(),
    private val recurrenceCalculator: RecurrenceCalculator = RecurrenceCalculator(calendar),
) {

    fun toUiModel(
        event: CountdownEvent,
        today: SolarDate,
    ): HomeEventUiModel {
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

        val (daysCount, displayStatus, unitLabel) = when (countdownStatus) {
            is EventCountdownStatus.Countdown -> {
                val label = if (countdownStatus.days == 1L) "DAY" else "DAYS"
                Triple(countdownStatus.days.toInt(), CountdownDisplayStatus.COUNTDOWN, label)
            }
            is EventCountdownStatus.Today -> {
                Triple(0, CountdownDisplayStatus.TODAY, "TODAY")
            }
            is EventCountdownStatus.CountedPast -> {
                val label = if (countdownStatus.days == 1L) "DAY AGO" else "DAYS AGO"
                Triple(countdownStatus.days.toInt(), CountdownDisplayStatus.PAST, label)
            }
        }

        val targetFormatted = "%04d.%02d.%02d".format(
            targetSolarDate.year,
            targetSolarDate.month,
            targetSolarDate.day,
        )

        val dateLabel = if (event.eventDate.isLunar) {
            "$targetFormatted 农历"
        } else {
            targetFormatted
        }

        return HomeEventUiModel(
            event = event,
            targetSolarDate = targetSolarDate,
            daysCount = daysCount,
            status = displayStatus,
            unitLabel = unitLabel,
            targetDateFormatted = dateLabel,
            isPinned = event.isPinned,
            color = event.resolvedColor(),
            background = event.background,
        )
    }

    companion object {
        private val defaultInstance = HomeEventMapper()

        fun toUiModel(
            event: CountdownEvent,
            today: SolarDate,
        ): HomeEventUiModel = defaultInstance.toUiModel(event, today)
    }
}
