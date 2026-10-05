package com.cherish.app.event.model

import com.cherish.app.date.model.RepeatRule
import java.util.UUID
import kotlinx.serialization.Serializable

/**
 * Domain entity representing a countdown / count-up event.
 *
 * @property id Unique identifier for the event
 * @property title Event title/name
 * @property emoji Emoji icon representing the event
 * @property category Event category classification
 * @property eventDate Target date (Solar or Lunar)
 * @property repeatRule Recurrence rule (None, Daily, Monthly, Yearly, Custom)
 * @property isPinned Whether the event is pinned by the user
 * @property background Custom background style configuration
 * @property notes Optional notes or remarks
 */
@Serializable
data class CountdownEvent(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val emoji: String = "📅",
    val category: EventCategory = EventCategory.GENERAL,
    val eventDate: EventDate,
    val repeatRule: RepeatRule = RepeatRule.None,
    val isPinned: Boolean = false,
    val background: EventBackground = EventBackground.Default,
    val notes: String = "",
) {
    init {
        require(id.isNotBlank()) { "Event id must not be blank" }
        require(title.isNotBlank()) { "Event title must not be blank" }
    }
}
