package com.cherish.app.event.model

import com.cherish.app.date.model.LunarDate
import com.cherish.app.date.model.SolarDate

/**
 * Domain representation of an event date, distinguishing between Gregorian (Solar)
 * and traditional Chinese Lunar dates.
 */
sealed interface EventDate {
    val isLunar: Boolean

    data class Solar(val date: SolarDate) : EventDate {
        override val isLunar: Boolean get() = false
    }

    data class Lunar(val date: LunarDate) : EventDate {
        override val isLunar: Boolean get() = true
    }
}
