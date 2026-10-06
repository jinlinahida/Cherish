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
 * @property color Visual color scheme specifically for Home event cards (subtle gradient)
 * @property background Visual background configuration specifically for Event Detail screen
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
    val color: EventColor = EventColor.Default,
    val background: EventBackground = EventBackground.Default,
    val notes: String = "",
) {
    init {
        require(id.isNotBlank()) { "Event id must not be blank" }
        require(title.isNotBlank()) { "Event title must not be blank" }
    }

    /**
     * Resolves the effective [EventColor] for this event.
     *
     * Provides automatic backward-compatibility for legacy data where card colors
     * were stored under [background] as [EventBackground.Color] or [EventBackground.Gradient].
     */
    fun resolvedColor(): EventColor {
        if (color != EventColor.Default) {
            return color
        }
        return when (val bg = background) {
            is EventBackground.Color -> EventColor.Single(bg.argb)
            is EventBackground.Gradient -> EventColor.Gradient(bg.startColor, bg.endColor, bg.angle)
            else -> EventColor.Default
        }
    }
}
