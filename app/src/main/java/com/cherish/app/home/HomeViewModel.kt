package com.cherish.app.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cherish.app.date.model.SolarDate
import com.cherish.app.event.repository.EventRepository
import com.cherish.app.home.mapper.HomeEventMapper
import com.cherish.app.home.model.HomeUiState
import com.cherish.app.home.model.HomeViewMode
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * State holder for the Cherish Home screen.
 *
 * Responsibilities:
 * 1. Observes [EventRepository.events] reactively.
 * 2. Emits pinned events first, while strictly preserving user-defined order within pinned and unpinned groups.
 *    (Repository's underlying list order is NEVER mutated for presentation).
 * 3. Manages [HomeViewMode] (List vs Grid).
 * 4. Manages reference today date and allows onResume / midnight refreshes.
 */
class HomeViewModel(
    private val repository: EventRepository,
    private val todayProvider: () -> SolarDate = { SolarDate.fromLocalDate(LocalDate.now()) },
    private val mapper: HomeEventMapper = HomeEventMapper(),
    coroutineScope: CoroutineScope? = null,
) : ViewModel() {

    private val scope: CoroutineScope = coroutineScope ?: viewModelScope

    private val _viewMode = MutableStateFlow(HomeViewMode.LIST)
    val viewMode: StateFlow<HomeViewMode> = _viewMode

    private val _currentToday = MutableStateFlow(todayProvider())
    val currentToday: StateFlow<SolarDate> = _currentToday

    fun toggleViewMode() {
        _viewMode.value = if (_viewMode.value == HomeViewMode.LIST) {
            HomeViewMode.GRID
        } else {
            HomeViewMode.LIST
        }
    }

    fun setViewMode(mode: HomeViewMode) {
        _viewMode.value = mode
    }

    fun refreshToday() {
        val newToday = todayProvider()
        if (_currentToday.value != newToday) {
            _currentToday.value = newToday
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        repository.events,
        _viewMode,
        _currentToday,
    ) { events, mode, today ->
        val pinned = events.filter { it.isPinned }
        val unpinned = events.filter { !it.isPinned }
        val ordered = pinned + unpinned
        val uiModels = ordered.map { mapper.toUiModel(it, today) }

        HomeUiState(
            items = uiModels,
            viewMode = mode,
            today = today,
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = run {
            val initialToday = todayProvider()
            val allEvents = repository.getAll()
            val pinned = allEvents.filter { it.isPinned }
            val unpinned = allEvents.filter { !it.isPinned }
            val ordered = pinned + unpinned
            HomeUiState(
                items = ordered.map { mapper.toUiModel(it, initialToday) },
                viewMode = HomeViewMode.LIST,
                today = initialToday,
            )
        },
    )
}
