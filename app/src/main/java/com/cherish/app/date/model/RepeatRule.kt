package com.cherish.app.date.model

/**
 * Minimal recurrence rules used by the date calculation engine.
 */
sealed interface RepeatRule {
    /** Does not repeat */
    data object None : RepeatRule

    /** Repeats every day */
    data object Daily : RepeatRule

    /** Repeats every month on the same day (or last day if month is shorter) */
    data object Monthly : RepeatRule

    /** Repeats every year (supports both Solar and Lunar calendars) */
    data object Yearly : RepeatRule

    /**
     * Custom recurrence: every [interval] of [unit]
     */
    data class Custom(
        val interval: Int,
        val unit: RepeatUnit,
    ) : RepeatRule {
        init {
            require(interval > 0) { "Interval must be greater than 0, was: $interval" }
        }
    }
}

enum class RepeatUnit {
    DAY,
    WEEK,
    MONTH,
    YEAR,
}
