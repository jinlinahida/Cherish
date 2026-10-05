package com.cherish.app.event.model

import com.cherish.app.date.model.LunarDate
import com.cherish.app.date.model.SolarDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Domain representation of an event date, distinguishing between Gregorian (Solar)
 * and traditional Chinese Lunar dates.
 */
@Serializable
sealed interface EventDate {
    val isLunar: Boolean

    @Serializable
    @SerialName("Solar")
    data class Solar(val date: SolarDate) : EventDate {
        override val isLunar: Boolean get() = false
    }

    @Serializable
    @SerialName("Lunar")
    data class Lunar(val date: LunarDate) : EventDate {
        override val isLunar: Boolean get() = true
    }
}
