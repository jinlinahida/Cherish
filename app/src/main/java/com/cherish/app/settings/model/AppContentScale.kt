package com.cherish.app.settings.model

import kotlinx.serialization.Serializable

/**
 * Text and UI scaling preferences for Cherish on Wear OS.
 *
 * Kept pure in the domain model to avoid direct coupling to UI design systems.
 */
@Serializable
enum class AppContentScale {
    SMALL,
    STANDARD,
    LARGE,
}
