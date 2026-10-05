package com.cherish.app.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.items
import com.cherish.app.date.model.SolarDate
import com.cherish.app.home.components.CountdownEventCard
import com.cherish.app.home.components.HomeEmptyState
import com.cherish.app.home.components.HomeHeader
import com.cherish.app.home.mapper.HomeEventMapper
import com.cherish.app.home.model.HomeUiState
import com.cherish.app.home.model.HomeViewMode
import com.cherish.app.home.preview.DemoEvents
import io.github.jinlinahida.shirokowear.ui.ShirokoWearAmbient
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScalingRotaryColumn
import io.github.jinlinahida.shirokowear.ui.ShirokoWearTheme

/**
 * Main Home screen for Cherish.
 *
 * Integrates:
 * - ShirokoWearAmbient: subtle background spotlight that shines through frosted cards.
 * - ShirokoWearScalingRotaryColumn: rotary bezel/crown scrolling + fish-eye scaling for circular bezels.
 * - Seamless switching between List (full-width) and Grid (2-column compact) modes.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
    onEventClick: ((String) -> Unit)? = null,
    onEventLongClick: ((String) -> Unit)? = null,
    onAddEventClick: (() -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsState()

    HomeScreenContent(
        uiState = uiState,
        onToggleViewMode = { viewModel.toggleViewMode() },
        onEventClick = onEventClick,
        onEventLongClick = onEventLongClick,
        onAddEventClick = onAddEventClick,
        modifier = modifier,
    )
}

@Composable
fun HomeScreenContent(
    uiState: HomeUiState,
    onToggleViewMode: () -> Unit,
    modifier: Modifier = Modifier,
    onEventClick: ((String) -> Unit)? = null,
    onEventLongClick: ((String) -> Unit)? = null,
    onAddEventClick: (() -> Unit)? = null,
) {
    ShirokoWearAmbient(spotlightKey = "cherish_home") {
        ShirokoWearScalingRotaryColumn(
            modifier = modifier.fillMaxSize(),
            itemSpacing = 6.dp,
            contentPadding = ShirokoWearTheme.dimens.screenPadding,
        ) {
            // Header
            item(key = "home_header") {
                HomeHeader(
                    viewMode = uiState.viewMode,
                    hasEvents = !uiState.isEmpty,
                    onToggleViewMode = onToggleViewMode,
                    onAddEventClick = onAddEventClick,
                )
            }

            // Empty State
            if (uiState.isEmpty) {
                item(key = "home_empty") {
                    HomeEmptyState()
                }
            } else if (uiState.viewMode == HomeViewMode.LIST) {
                // List Mode: single column full-width cards
                items(
                    items = uiState.items,
                    key = { it.event.id },
                ) { item ->
                    CountdownEventCard(
                        uiModel = item,
                        isCompact = false,
                        onClick = onEventClick?.let { onClick -> { onClick(item.event.id) } },
                        onLongClick = onEventLongClick?.let { onLongClick -> { onLongClick(item.event.id) } },
                    )
                }
            } else {
                // Grid Mode: 2-column compact cards
                val rows = uiState.items.chunked(2)
                items(
                    items = rows,
                    key = { row -> "grid_row_${row.first().event.id}" },
                ) { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        for (item in rowItems) {
                            CountdownEventCard(
                                uiModel = item,
                                isCompact = true,
                                modifier = Modifier.weight(1f),
                                onClick = onEventClick?.let { onClick -> { onClick(item.event.id) } },
                                onLongClick = onEventLongClick?.let { onLongClick -> { onLongClick(item.event.id) } },
                            )
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
private fun HomeScreenListPreview() {
    val sampleDate = SolarDate(2026, 10, 5)
    val demoEvents = DemoEvents.samples(sampleDate)
    val uiModels = demoEvents.map { HomeEventMapper.toUiModel(it, sampleDate) }

    ShirokoWearTheme {
        HomeScreenContent(
            uiState = HomeUiState(
                items = uiModels,
                viewMode = HomeViewMode.LIST,
                today = sampleDate,
            ),
            onToggleViewMode = {},
        )
    }
}

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
private fun HomeScreenGridPreview() {
    val sampleDate = SolarDate(2026, 10, 5)
    val demoEvents = DemoEvents.samples(sampleDate)
    val uiModels = demoEvents.map { HomeEventMapper.toUiModel(it, sampleDate) }

    ShirokoWearTheme {
        HomeScreenContent(
            uiState = HomeUiState(
                items = uiModels,
                viewMode = HomeViewMode.GRID,
                today = sampleDate,
            ),
            onToggleViewMode = {},
        )
    }
}
