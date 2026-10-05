package com.cherish.app.home.model

import kotlinx.serialization.Serializable

/**
 * Display modes for the Cherish home screen:
 * - [LIST]: Single-column full-width event cards with complete detail.
 * - [GRID]: Two-column compact event cards optimized for rapid visual scanning.
 */
@Serializable
enum class HomeViewMode {
    LIST,
    GRID,
}
