package com.cherish.app.home.model

import com.cherish.app.date.model.SolarDate
import com.cherish.app.event.model.CountdownEvent
import com.cherish.app.event.model.EventBackground

/**
 * Display status indicating whether an event is in the future, today, or in the past.
 */
enum class CountdownDisplayStatus {
    COUNTDOWN,
    TODAY,
    PAST,
}

/**
 * UI presentation model for an event card on the Home screen.
 *
 * All date calculations (recurrence resolution, countdown calculation)
 * are computed beforehand into this model so Composable cards remain purely declarative.
 *
 * @property event Original domain entity
 * @property targetSolarDate Resolved target solar date for this occurrence
 * @property daysCount Days count (remaining or passed)
 * @property status Display status (COUNTDOWN, TODAY, PAST)
 * @property unitLabel Unit text ("DAYS", "TODAY", "DAYS AGO")
 * @property targetDateFormatted Formatted target date string (e.g. "2026.10.17")
 * @property isPinned Whether the event is pinned
 * @property background Custom background configuration
 */
data class HomeEventUiModel(
    val event: CountdownEvent,
    val targetSolarDate: SolarDate,
    val daysCount: Int,
    val status: CountdownDisplayStatus,
    val unitLabel: String,
    val targetDateFormatted: String,
    val isPinned: Boolean,
    val background: EventBackground,
)
