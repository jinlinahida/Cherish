package com.cherish.app.event.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Visual background configuration specifically for the Event Detail screen.
 *
 * Distinct from [EventColor] (which styles Home cards). [EventBackground] fills the Detail
 * view and supports solid colors, gradients, subtle patterns, and custom user images.
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

    /**
     * Custom image background for the Event Detail screen.
     *
     * @property path Relative file path in internal storage (e.g. "backgrounds/bg_123.jpg")
     *                 or local content URI.
     * @property dimAlpha Scrim dim overlay alpha (0f..1f) to guarantee white/accent text readability
     *                    on small Wear OS watch displays.
     */
    @Serializable
    @SerialName("Image")
    data class Image(
        val path: String,
        val dimAlpha: Float = 0.35f,
    ) : EventBackground {
        init {
            require(path.isNotBlank()) { "Image path must not be blank" }
            require(dimAlpha in 0f..1f) { "dimAlpha must be between 0.0 and 1.0 (was $dimAlpha)" }
        }
    }
}
