package com.cherish.app.event.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Visual background configuration strategy for countdown events.
 * Pure data representations, decoupled from UI/Compose rendering.
 */
@Serializable
sealed interface EventBackground {
    @Serializable
    @SerialName("Default")
    data object Default : EventBackground

    @Serializable
    @SerialName("Color")
    data class Color(val argb: Long) : EventBackground

    @Serializable
    @SerialName("Gradient")
    data class Gradient(
        val startColor: Long,
        val endColor: Long,
        val angle: Float = 0f,
    ) : EventBackground

    @Serializable
    @SerialName("Pattern")
    data class Pattern(val patternId: String) : EventBackground
}
