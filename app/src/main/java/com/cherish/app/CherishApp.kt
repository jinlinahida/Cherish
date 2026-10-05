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

/**
 * Top-level application composable with ShirokoWear navigation and transitions.
 */
@Composable
fun CherishApp(
    repository: EventRepository,
    homeViewModel: HomeViewModel,
    modifier: Modifier = Modifier,
) {
    var currentRoute by remember { mutableStateOf<CherishRoute>(CherishRoute.Home) }
    var fromRoute by remember { mutableStateOf<CherishRoute?>(null) }
    val haptics = rememberShirokoWearHaptics()

    val homeUiState by homeViewModel.uiState.collectAsState()
    val today = homeUiState.today
    val events by repository.events.collectAsState()

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
                // Temporary editor placeholder until next step wires full editor screen
                ShirokoWearAmbient(spotlightKey = "cherish_editor") {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        ShirokoWearScreenTitle(text = if (route.eventId == null) "新建事件" else "编辑事件")
                        ShirokoWearCard(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = "编辑器加载中...",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        ShirokoWearCardButton(onClick = {
                            haptics.back()
                            navigateTo(CherishRoute.Home)
                        }) {
                            Text("返回")
                        }
                    }
                }
            }
        }
    }
}
