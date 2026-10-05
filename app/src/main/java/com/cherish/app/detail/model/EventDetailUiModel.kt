package com.cherish.app.detail.model

import com.cherish.app.date.model.SolarDate
import com.cherish.app.event.model.CountdownEvent
import com.cherish.app.event.model.EventBackground
import com.cherish.app.home.model.CountdownDisplayStatus

/**
 * UI presentation model for the Event Detail screen.
 *
 * All dates, countdown states, and localization strings are precomputed
 * by [com.cherish.app.detail.mapper.EventDetailMapper] so the UI remains
 * purely declarative with zero business logic.
 */
data class EventDetailUiModel(
    val event: CountdownEvent,
    val targetSolarDate: SolarDate,
    val daysCount: Int,
    val status: CountdownDisplayStatus,
    val heroNumberText: String,
    val heroUnitText: String,
    val targetDateDescription: String,
    val calendarTypeDescription: String,
    val recurrenceDescription: String,
    val categoryDescription: String,
    val isPinned: Boolean,
    val background: EventBackground,
    val notes: String,
)
