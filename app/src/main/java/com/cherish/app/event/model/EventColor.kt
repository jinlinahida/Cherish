package com.cherish.app.event.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Visual color scheme specifically for Home event cards.
 *
 * Implements subtle gradient rendering on Home cards without tinting the entire screen.
 * Pure data representations, decoupled from UI/Compose rendering.
 */
@Serializable
sealed interface EventColor {

    @Serializable
    @SerialName("Default")
    data object Default : EventColor

    @Serializable
    @SerialName("Single")
    data class Single(val argb: Long) : EventColor

    @Serializable
    @SerialName("Gradient")
    data class Gradient(
        val startColor: Long,
        val endColor: Long,
        val angle: Float = 45f,
    ) : EventColor

    companion object {
        val Rose = Single(0xFFE91E63L)
        val Amber = Single(0xFFFFA000L)
        val Emerald = Single(0xFF009688L)
        val Ocean = Single(0xFF1E88E5L)
        val Violet = Single(0xFF9C27B0L)
        val Coral = Single(0xFFFF5722L)

        val Sunset = Gradient(0xFFFFA000L, 0xFFE91E63L)
        val Aurora = Gradient(0xFF009688L, 0xFF1E88E5L)
        val Lavender = Gradient(0xFF1E88E5L, 0xFF9C27B0L)

        val presets: List<EventColor> = listOf(
            Default,
            Rose,
            Amber,
            Emerald,
            Ocean,
            Violet,
            Coral,
            Sunset,
            Aurora,
            Lavender,
        )
    }
}
