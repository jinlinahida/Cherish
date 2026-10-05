package com.cherish.app.date.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Minimal recurrence rules used by the date calculation engine.
 */
@Serializable
sealed interface RepeatRule {
    /** Does not repeat */
    @Serializable
    @SerialName("None")
    data object None : RepeatRule

    /** Repeats every day */
    @Serializable
    @SerialName("Daily")
    data object Daily : RepeatRule

    /** Repeats every month on the same day (or last day if month is shorter) */
    @Serializable
    @SerialName("Monthly")
    data object Monthly : RepeatRule

    /** Repeats every year (supports both Solar and Lunar calendars) */
    @Serializable
    @SerialName("Yearly")
    data object Yearly : RepeatRule

    /**
     * Custom recurrence: every [interval] of [unit]
     */
    @Serializable
    @SerialName("Custom")
    data class Custom(
        val interval: Int,
        val unit: RepeatUnit,
    ) : RepeatRule {
        init {
            require(interval > 0) { "Interval must be greater than 0, was: $interval" }
        }
    }
}

@Serializable
enum class RepeatUnit {
    DAY,
    WEEK,
    MONTH,
    YEAR,
}
