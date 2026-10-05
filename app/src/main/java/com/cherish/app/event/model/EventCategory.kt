package com.cherish.app.event.model

import kotlinx.serialization.Serializable

/**
 * Categorization for countdown events.
 * Pure Kotlin enum independent of UI and platform frameworks.
 */
@Serializable
enum class EventCategory {
    GENERAL,
    BIRTHDAY,
    ANNIVERSARY,
    HOLIDAY,
    WORK,
    LIFE,
    OTHER,
}
