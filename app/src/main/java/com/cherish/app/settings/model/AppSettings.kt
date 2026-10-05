package com.cherish.app.settings.model

import com.cherish.app.home.model.HomeViewMode
import kotlinx.serialization.Serializable

/**
 * Global persistent user preferences for Cherish.
 *
 * @property homeViewMode Default display mode for the Home screen (List vs Grid).
 * @property hapticsEnabled Master switch for tactile feedback (rotary detents, button clicks, toggle clicks).
 * @property contentScale Scaling factor for font size and screen paddings.
 */
@Serializable
data class AppSettings(
    val homeViewMode: HomeViewMode = HomeViewMode.LIST,
    val hapticsEnabled: Boolean = true,
    val contentScale: AppContentScale = AppContentScale.STANDARD,
)
