package com.cherish.app.event.model

/**
 * Visual background configuration strategy for countdown events.
 * Pure data representations, decoupled from UI/Compose rendering.
 */
sealed interface EventBackground {
    data object Default : EventBackground

    data class Color(val argb: Long) : EventBackground

    data class Gradient(
        val startColor: Long,
        val endColor: Long,
        val angle: Float = 0f,
    ) : EventBackground

    data class Pattern(val patternId: String) : EventBackground
}
