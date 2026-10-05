package com.cherish.app.home.model

import com.cherish.app.date.model.SolarDate

/**
 * Immutable state representing the Cherish Home screen.
 *
 * @property items Ordered list of event UI models (pinned items first, original relative order preserved)
 * @property viewMode Current layout mode (LIST or GRID)
 * @property today The reference solar date used for calculations
 */
data class HomeUiState(
    val items: List<HomeEventUiModel> = emptyList(),
    val viewMode: HomeViewMode = HomeViewMode.LIST,
    val today: SolarDate,
) {
    val isEmpty: Boolean get() = items.isEmpty()
}
