package com.cherish.app.date.calc

import com.cherish.app.date.model.SolarDate
import java.time.temporal.ChronoUnit

/**
 * Represents the relative status between reference date and target event date.
 */
sealed interface EventCountdownStatus {
    /** Target is in the future: [days] remaining */
    data class Countdown(val days: Long) : EventCountdownStatus {
        init {
            require(days > 0) { "Countdown days must be positive, was: $days" }
        }
    }

    /** Target date is exactly today */
    data object Today : EventCountdownStatus

    /** Target date is in the past: [days] have passed */
    data class CountedPast(val days: Long) : EventCountdownStatus {
        init {
            require(days > 0) { "CountedPast days must be positive, was: $days" }
        }
    }
}

/**
 * Pure date calculation engine to resolve days difference and status.
 */
object CountdownCalculator {

    /**
     * Calculates countdown status from [referenceDate] (usually today) to [targetDate].
     * Uses calendar day differences, unaffected by time of day or daylight saving time.
     */
    fun calculate(
        referenceDate: SolarDate,
        targetDate: SolarDate,
    ): EventCountdownStatus {
        val refLocal = referenceDate.toLocalDate()
        val targetLocal = targetDate.toLocalDate()
        val daysDiff = ChronoUnit.DAYS.between(refLocal, targetLocal)

        return when {
            daysDiff > 0L -> EventCountdownStatus.Countdown(daysDiff)
            daysDiff == 0L -> EventCountdownStatus.Today
            else -> EventCountdownStatus.CountedPast(-daysDiff)
        }
    }
}
