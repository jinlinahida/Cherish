package com.cherish.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import com.cherish.app.detail.EventDetailScreen
import com.cherish.app.detail.mapper.EventDetailMapper
import com.cherish.app.event.model.EventBackground
import com.cherish.app.event.repository.EventRepository
import com.cherish.app.home.HomeScreen
import com.cherish.app.home.HomeViewModel
import com.cherish.app.navigation.CherishRoute
import com.cherish.app.navigation.resolveBackRoute
import com.cherish.app.settings.repository.SettingsRepository
import com.cherish.app.settings.ui.SettingsScreen
import com.cherish.app.storage.EventImageStorage
import io.github.jinlinahida.shirokowear.navigation.shirokoWearPageTransition
import io.github.jinlinahida.shirokowear.ui.rememberShirokoWearHaptics
import kotlinx.coroutines.launch

/**
 * Top-level application composable with dual top-level spaces (Home & Settings)
 * hosted via [HorizontalPager], and child route navigation via [AnimatedContent].
 *
 * Space alignment:
 * - Page 0: Settings (secondary top-level space)
 * - Page 1: Home (primary top-level space, initial landing page)
 *
 * Gestures:
 * - On Home: swiping right slides into Settings.
 * - On Settings: swiping left slides back into Home; hardware Back navigates back into Home.
 * - On Home: system Back exits the application (native Wear OS behavior).
 * - Child routes (Detail, Editor, EventOrder, About) are stacked outside the pager,
 *   preventing gesture conflicts with Wear OS swipe-to-dismiss and vertical lists.
 */
@Composable
fun CherishApp(
    repository: EventRepository,
    settingsRepository: SettingsRepository,
    homeViewModel: HomeViewModel,
    modifier: Modifier = Modifier,
    imageStorage: EventImageStorage? = null,
) {
    var currentRoute by remember { mutableStateOf<CherishRoute>(CherishRoute.Home) }
    var fromRoute by remember { mutableStateOf<CherishRoute?>(null) }
    val haptics = rememberShirokoWearHaptics()
    val coroutineScope = rememberCoroutineScope()

    val homeUiState by homeViewModel.uiState.collectAsState()
    val today = homeUiState.today
    val events by repository.events.collectAsState()
    val settings by settingsRepository.settings.collectAsState()

    // Top-level dual space pager: Page 0 = Settings, Page 1 = Home (default initial page)
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 2 })

    // Sync pager drag gestures with currentRoute state
    LaunchedEffect(pagerState.currentPage) {
        if (currentRoute is CherishRoute.Home || currentRoute is CherishRoute.Settings) {
            val targetRoute = if (pagerState.currentPage == 0) CherishRoute.Settings else CherishRoute.Home
            if (currentRoute != targetRoute) {
                fromRoute = currentRoute
                currentRoute = targetRoute
            }
        }
    }

    fun navigateTo(target: CherishRoute) {
        fromRoute = currentRoute
        currentRoute = target
        if (target is CherishRoute.Home) {
            coroutineScope.launch {
                pagerState.scrollToPage(1)
            }
        } else if (target is CherishRoute.Settings) {
            coroutineScope.launch {
                pagerState.scrollToPage(0)
            }
        }
    }

    // Hardware back on Settings page animates pager back to Home
    BackHandler(enabled = currentRoute is CherishRoute.Settings) {
        haptics.back()
        coroutineScope.launch {
            pagerState.animateScrollToPage(1)
        }
    }

    // System back on child routes pops according to route hierarchy
    BackHandler(enabled = currentRoute !is CherishRoute.Home && currentRoute !is CherishRoute.Settings) {
        val target = if (currentRoute is CherishRoute.Editor && fromRoute is CherishRoute.Home) {
            CherishRoute.Home
        } else {
            resolveBackRoute(currentRoute)
        }
        if (target != null) {
            haptics.back()
            navigateTo(target)
        }
    }

    AnimatedContent(
        targetState = currentRoute,
        transitionSpec = {
            val isInitialTopLevel = initialState is CherishRoute.Home || initialState is CherishRoute.Settings
            val isTargetTopLevel = targetState is CherishRoute.Home || targetState is CherishRoute.Settings
            if (isInitialTopLevel && isTargetTopLevel) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                shirokoWearPageTransition(fromRoute, targetState)
            }
        },
        contentKey = { route ->
            if (route is CherishRoute.Home || route is CherishRoute.Settings) "top_level_pager"
            else route.routeKey
        },
        label = "cherishNavTransitions",
        modifier = modifier.fillMaxSize(),
    ) { route ->
        when (route) {
            is CherishRoute.Home,
            is CherishRoute.Settings -> {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    when (page) {
                        0 -> {
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
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(1)
                                    }
                                },
                            )
                        }

                        1 -> {
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
                            )
                        }
                    }
                }
            }

            is CherishRoute.Detail -> {
                val event = events.firstOrNull { it.id == route.eventId }
                if (event == null) {
                    LaunchedEffect(Unit) {
                        val target = resolveBackRoute(route) ?: CherishRoute.Home
                        navigateTo(target)
                    }
                } else {
                    val detailUiModel = remember(event, today) {
                        EventDetailMapper.toUiModel(event, today)
                    }
                    EventDetailScreen(
                        uiModel = detailUiModel,
                        imageStorage = imageStorage,
                        onEditClick = { id ->
                            haptics.click()
                            navigateTo(CherishRoute.Editor(id))
                        },
                        onDeleteConfirm = { id ->
                            val eventToDelete = repository.getById(id)
                            val bg = eventToDelete?.background
                            if (bg is EventBackground.Image) {
                                imageStorage?.deleteImage(bg.path)
                            }
                            repository.delete(id)
                            val target = resolveBackRoute(route) ?: CherishRoute.Home
                            navigateTo(target)
                        },
                        onBackClick = {
                            val target = resolveBackRoute(route) ?: CherishRoute.Home
                            navigateTo(target)
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
                    imageStorage = imageStorage,
                    onSave = { savedState ->
                        val result = com.cherish.app.editor.sanitizer.DatePickerSanitizer.validateAndBuildEvent(savedState, calendar)
                        if (result.isSuccess) {
                            val event = result.getOrThrow()
                            try {
                                if (savedState.isCreateMode) {
                                    repository.add(event)
                                    haptics.click()
                                    navigateTo(CherishRoute.Home)
                                } else {
                                    if (existingEvent != null) {
                                        val oldBg = existingEvent.background
                                        if (oldBg is EventBackground.Image && oldBg.path != (event.background as? EventBackground.Image)?.path) {
                                            imageStorage?.deleteImage(oldBg.path)
                                        }
                                    }
                                    repository.update(event)
                                    haptics.click()
                                    val returnTarget = if (fromRoute is CherishRoute.Home) CherishRoute.Home else CherishRoute.Detail(event.id)
                                    navigateTo(returnTarget)
                                }
                            } catch (e: Exception) {
                                // Keep on editor screen if persistent write failed
                            }
                        }
                    },
                    onCancel = {
                        val target = if (fromRoute is CherishRoute.Home) CherishRoute.Home else (resolveBackRoute(route) ?: CherishRoute.Home)
                        haptics.back()
                        navigateTo(target)
                    },
                )
            }

            is CherishRoute.EventOrder -> {
                com.cherish.app.settings.ui.EventOrderScreen(
                    events = homeUiState.items.map { it.event },
                    onReorder = { fromIndex, toIndex ->
                        repository.reorder(fromIndex, toIndex)
                    },
                    onReorderComplete = { updatedEvents ->
                        repository.reorderAll(updatedEvents)
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
