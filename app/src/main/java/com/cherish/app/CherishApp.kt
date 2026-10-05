package com.cherish.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.cherish.app.detail.EventDetailScreen
import com.cherish.app.detail.mapper.EventDetailMapper
import com.cherish.app.event.repository.EventRepository
import com.cherish.app.home.HomeScreen
import com.cherish.app.home.HomeViewModel
import com.cherish.app.navigation.CherishRoute
import io.github.jinlinahida.shirokowear.navigation.shirokoWearPageTransition
import io.github.jinlinahida.shirokowear.ui.ShirokoWearAmbient
import io.github.jinlinahida.shirokowear.ui.ShirokoWearCard
import io.github.jinlinahida.shirokowear.ui.ShirokoWearCardButton
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScreenTitle
import io.github.jinlinahida.shirokowear.ui.rememberShirokoWearHaptics

import com.cherish.app.settings.repository.SettingsRepository
import com.cherish.app.settings.ui.SettingsScreen

/**
 * Top-level application composable with ShirokoWear navigation and transitions.
 */
@Composable
fun CherishApp(
    repository: EventRepository,
    settingsRepository: SettingsRepository,
    homeViewModel: HomeViewModel,
    modifier: Modifier = Modifier,
) {
    var currentRoute by remember { mutableStateOf<CherishRoute>(CherishRoute.Home) }
    var fromRoute by remember { mutableStateOf<CherishRoute?>(null) }
    val haptics = rememberShirokoWearHaptics()

    val homeUiState by homeViewModel.uiState.collectAsState()
    val today = homeUiState.today
    val events by repository.events.collectAsState()
    val settings by settingsRepository.settings.collectAsState()

    fun navigateTo(target: CherishRoute) {
        fromRoute = currentRoute
        currentRoute = target
    }

    AnimatedContent(
        targetState = currentRoute,
        transitionSpec = {
            shirokoWearPageTransition(fromRoute, currentRoute)
        },
        label = "cherishNavTransitions",
        modifier = modifier.fillMaxSize(),
    ) { route ->
        when (route) {
            is CherishRoute.Home -> {
                HomeScreen(
                    viewModel = homeViewModel,
                    onEventClick = { eventId ->
                        haptics.click()
                        navigateTo(CherishRoute.Detail(eventId))
                    },
                    onEventLongClick = { eventId ->
                        haptics.click()
                        navigateTo(CherishRoute.Editor(eventId))
                    },
                    onAddEventClick = {
                        haptics.click()
                        navigateTo(CherishRoute.Editor(null))
                    },
                    onSettingsClick = {
                        haptics.click()
                        navigateTo(CherishRoute.Settings)
                    },
                )
            }

            is CherishRoute.Detail -> {
                val event = events.firstOrNull { it.id == route.eventId }
                if (event == null) {
                    LaunchedEffect(Unit) {
                        navigateTo(CherishRoute.Home)
                    }
                } else {
                    val detailUiModel = remember(event, today) {
                        EventDetailMapper.toUiModel(event, today)
                    }
                    EventDetailScreen(
                        uiModel = detailUiModel,
                        onEditClick = { id ->
                            haptics.click()
                            navigateTo(CherishRoute.Editor(id))
                        },
                        onDeleteConfirm = { id ->
                            repository.delete(id)
                            navigateTo(CherishRoute.Home)
                        },
                        onBackClick = {
                            navigateTo(CherishRoute.Home)
                        },
                    )
                }
            }

            is CherishRoute.Editor -> {
                val existingEvent = route.eventId?.let { id -> events.firstOrNull { it.id == id } }
                val calendar = remember { com.cherish.app.date.lunar.DefaultLunarCalendar() }
                val initialLunar = remember(today) { calendar.solarToLunar(today) }

                val editorState = remember(route.eventId, existingEvent) {
                    if (existingEvent != null) {
                        com.cherish.app.editor.model.EventEditorState.fromEvent(existingEvent, today, initialLunar)
                    } else {
                        com.cherish.app.editor.model.EventEditorState.createDefault(today, initialLunar)
                    }
                }

                com.cherish.app.editor.EventEditorScreen(
                    initialState = editorState,
                    onSave = { savedState ->
                        val result = com.cherish.app.editor.sanitizer.DatePickerSanitizer.validateAndBuildEvent(savedState, calendar)
                        if (result.isSuccess) {
                            val event = result.getOrThrow()
                            try {
                                if (savedState.isCreateMode) {
                                    repository.add(event)
                                } else {
                                    repository.update(event)
                                }
                                haptics.click()
                                navigateTo(CherishRoute.Home)
                            } catch (e: Exception) {
                                // Keep on editor screen if persistent write failed
                            }
                        }
                    },
                    onCancel = {
                        haptics.back()
                        navigateTo(CherishRoute.Home)
                    },
                )
            }

            is CherishRoute.Settings -> {
                SettingsScreen(
                    settings = settings,
                    eventCount = events.size,
                    onUpdateSettings = { newSettings ->
                        settingsRepository.update { newSettings }
                    },
                    onNavigateToEventOrder = {
                        haptics.click()
                        navigateTo(CherishRoute.EventOrder)
                    },
                    onNavigateToAbout = {
                        haptics.click()
                        navigateTo(CherishRoute.About)
                    },
                    onBack = {
                        navigateTo(CherishRoute.Home)
                    },
                )
            }

            is CherishRoute.EventOrder -> {
                com.cherish.app.settings.ui.EventOrderScreen(
                    events = events,
                    onMoveUp = { index ->
                        if (index > 0) {
                            repository.reorder(index, index - 1)
                        }
                    },
                    onMoveDown = { index ->
                        if (index < events.size - 1) {
                            repository.reorder(index, index + 1)
                        }
                    },
                    onBack = {
                        navigateTo(CherishRoute.Settings)
                    },
                )
            }

            is CherishRoute.About -> {
                com.cherish.app.settings.ui.AboutScreen(
                    onBack = {
                        navigateTo(CherishRoute.Settings)
                    },
                )
            }
        }
    }
}
