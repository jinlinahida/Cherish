package com.cherish.app.editor.model

import com.cherish.app.date.model.LunarDate
import com.cherish.app.date.model.RepeatRule
import com.cherish.app.date.model.SolarDate
import com.cherish.app.event.model.CountdownEvent
import com.cherish.app.event.model.EventBackground
import com.cherish.app.event.model.EventCategory
import com.cherish.app.event.model.EventColor
import com.cherish.app.event.model.EventDate

/**
 * Mutable/reactive state for the Event Editor screen.
 *
 * Supports both Create mode (when [eventId] is null) and Edit mode (when [eventId] is non-null).
 */
data class EventEditorState(
    val eventId: String? = null,
    val title: String = "",
    val emoji: String = "📅",
    val isLunar: Boolean = false,
    val solarDate: SolarDate = SolarDate(2026, 10, 5),
    val lunarDate: LunarDate = LunarDate(2026, 8, 25),
    val repeatRule: RepeatRule = RepeatRule.None,
    val category: EventCategory = EventCategory.GENERAL,
    val isPinned: Boolean = false,
    val color: EventColor = EventColor.Default,
    val background: EventBackground = EventBackground.Default,
    val notes: String = "",
    val titleError: String? = null,
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
) {
    val isCreateMode: Boolean get() = eventId == null

    companion object {
        /**
         * Creates an initial [EventEditorState] for creating a new event, defaulting to [today].
         */
        fun createDefault(today: SolarDate, initialLunar: LunarDate): EventEditorState =
            EventEditorState(
                eventId = null,
                title = "",
                emoji = "📅",
                isLunar = false,
                solarDate = today,
                lunarDate = initialLunar,
                repeatRule = RepeatRule.None,
                category = EventCategory.GENERAL,
                isPinned = false,
                color = EventColor.Default,
                background = EventBackground.Default,
                notes = "",
            )

        /**
         * Initializes [EventEditorState] populated from an existing domain [event].
         */
        fun fromEvent(
            event: CountdownEvent,
            fallbackSolar: SolarDate,
            fallbackLunar: LunarDate,
        ): EventEditorState {
            val (isLunar, solar, lunar) = when (val date = event.eventDate) {
                is EventDate.Solar -> Triple(false, date.date, fallbackLunar)
                is EventDate.Lunar -> Triple(true, fallbackSolar, date.date)
            }
            return EventEditorState(
                eventId = event.id,
                title = event.title,
                emoji = event.emoji,
                isLunar = isLunar,
                solarDate = solar,
                lunarDate = lunar,
                repeatRule = event.repeatRule,
                category = event.category,
                isPinned = event.isPinned,
                color = event.resolvedColor(),
                background = event.background,
                notes = event.notes,
            )
        }
    }
}
