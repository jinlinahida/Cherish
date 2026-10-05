package com.cherish.app.date.calc

import com.cherish.app.date.lunar.DefaultLunarCalendar
import com.cherish.app.date.lunar.LunarCalendar
import com.cherish.app.date.model.LunarDate
import com.cherish.app.date.model.RepeatRule
import com.cherish.app.date.model.RepeatUnit
import com.cherish.app.date.model.SolarDate
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth

/**
 * Pure calculation engine to resolve the next occurrence date of an event.
 */
class RecurrenceCalculator(
    private val calendar: LunarCalendar = DefaultLunarCalendar(),
) {

    /**
     * Resolves the next occurrence on or after [referenceDate].
     *
     * @param baseSolar The baseline / first solar date of the event.
     * @param baseLunar Optional original lunar date if this is a lunar-backed event.
     * @param referenceDate The reference date (typically today).
     * @param rule The recurrence rule.
     * @return The next occurrence date, or null if a non-repeating event is strictly in the past.
     */
    fun nextOccurrence(
        baseSolar: SolarDate,
        baseLunar: LunarDate? = null,
        referenceDate: SolarDate,
        rule: RepeatRule,
    ): SolarDate? {
        val baseLocal = baseSolar.toLocalDate()
        val refLocal = referenceDate.toLocalDate()

        return when (rule) {
            is RepeatRule.None -> {
                if (!baseLocal.isBefore(refLocal)) baseSolar else null
            }

            is RepeatRule.Daily -> {
                if (!baseLocal.isBefore(refLocal)) {
                    baseSolar
                } else {
                    referenceDate
                }
            }

            is RepeatRule.Monthly -> {
                resolveNextMonthly(baseSolar, refLocal)
            }

            is RepeatRule.Yearly -> {
                if (baseLunar != null) {
                    resolveNextYearlyLunar(baseSolar, baseLunar, refLocal)
                } else {
                    resolveNextYearlySolar(baseSolar, refLocal)
                }
            }

            is RepeatRule.Custom -> {
                resolveNextCustom(baseSolar, refLocal, rule.interval, rule.unit)
            }
        }
    }

    private fun resolveNextMonthly(baseSolar: SolarDate, refLocal: LocalDate): SolarDate {
        val baseLocal = baseSolar.toLocalDate()
        var candidateYearMonth = if (refLocal.isBefore(baseLocal)) {
            YearMonth.of(baseLocal.year, baseLocal.monthValue)
        } else {
            YearMonth.of(refLocal.year, refLocal.monthValue)
        }

        while (true) {
            val maxDay = candidateYearMonth.lengthOfMonth()
            val targetDay = minOf(baseSolar.day, maxDay)
            val candidate = candidateYearMonth.atDay(targetDay)

            if (!candidate.isBefore(refLocal) && !candidate.isBefore(baseLocal)) {
                return SolarDate.fromLocalDate(candidate)
            }
            candidateYearMonth = candidateYearMonth.plusMonths(1)
        }
    }

    private fun resolveNextYearlySolar(baseSolar: SolarDate, refLocal: LocalDate): SolarDate {
        val baseLocal = baseSolar.toLocalDate()
        var candidateYear = if (refLocal.isBefore(baseLocal)) baseLocal.year else refLocal.year

        while (true) {
            val targetDay = if (baseSolar.month == 2 && baseSolar.day == 29) {
                // Product rule: leap year -> Feb 29, normal year -> Feb 28
                if (Year.isLeap(candidateYear.toLong())) 29 else 28
            } else {
                baseSolar.day
            }

            val candidate = LocalDate.of(candidateYear, baseSolar.month, targetDay)
            if (!candidate.isBefore(refLocal) && !candidate.isBefore(baseLocal)) {
                return SolarDate.fromLocalDate(candidate)
            }
            candidateYear++
        }
    }

    private fun resolveNextYearlyLunar(
        baseSolar: SolarDate,
        baseLunar: LunarDate,
        refLocal: LocalDate,
    ): SolarDate {
        val baseLocal = baseSolar.toLocalDate()
        // Lunar new year can be up to ~1 month after solar new year, so scan from refLocal.year - 1
        var candidateLunarYear = if (refLocal.isBefore(baseLocal)) baseLunar.year else (refLocal.year - 1)

        while (true) {
            val leapMonth = calendar.getLeapMonth(candidateLunarYear)
            // Product rule: if leap lunar birthday, and candidate year has that leap month, use leap;
            // otherwise degrade to normal month
            val isLeap = baseLunar.isLeapMonth && (leapMonth == baseLunar.month)

            val daysInMonth = calendar.getDaysInMonth(candidateLunarYear, baseLunar.month, isLeap)
            val targetDay = minOf(baseLunar.day, daysInMonth)

            val targetLunar = LunarDate(
                year = candidateLunarYear,
                month = baseLunar.month,
                day = targetDay,
                isLeapMonth = isLeap,
            )

            val candidateSolar = calendar.lunarToSolar(targetLunar).toLocalDate()
            if (!candidateSolar.isBefore(refLocal) && !candidateSolar.isBefore(baseLocal)) {
                return SolarDate.fromLocalDate(candidateSolar)
            }
            candidateLunarYear++
        }
    }

    private fun resolveNextCustom(
        baseSolar: SolarDate,
        refLocal: LocalDate,
        interval: Int,
        unit: RepeatUnit,
    ): SolarDate {
        val baseLocal = baseSolar.toLocalDate()
        if (!baseLocal.isBefore(refLocal)) return baseSolar

        when (unit) {
            RepeatUnit.DAY -> {
                val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(baseLocal, refLocal)
                val steps = (daysBetween + interval - 1) / interval
                val result = baseLocal.plusDays(steps * interval)
                return SolarDate.fromLocalDate(result)
            }

            RepeatUnit.WEEK -> {
                val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(baseLocal, refLocal)
                val stepDays = interval * 7L
                val steps = (daysBetween + stepDays - 1) / stepDays
                val result = baseLocal.plusDays(steps * stepDays)
                return SolarDate.fromLocalDate(result)
            }

            RepeatUnit.MONTH -> {
                var candidateYearMonth = YearMonth.of(baseLocal.year, baseLocal.monthValue)
                while (true) {
                    val maxDay = candidateYearMonth.lengthOfMonth()
                    val targetDay = minOf(baseSolar.day, maxDay)
                    val candidate = candidateYearMonth.atDay(targetDay)
                    if (!candidate.isBefore(refLocal)) {
                        return SolarDate.fromLocalDate(candidate)
                    }
                    candidateYearMonth = candidateYearMonth.plusMonths(interval.toLong())
                }
            }

            RepeatUnit.YEAR -> {
                var candidateYear = baseLocal.year
                while (true) {
                    val targetDay = if (baseSolar.month == 2 && baseSolar.day == 29) {
                        if (Year.isLeap(candidateYear.toLong())) 29 else 28
                    } else {
                        baseSolar.day
                    }
                    val candidate = LocalDate.of(candidateYear, baseSolar.month, targetDay)
                    if (!candidate.isBefore(refLocal)) {
                        return SolarDate.fromLocalDate(candidate)
                    }
                    candidateYear += interval
                }
            }
        }
    }
}
