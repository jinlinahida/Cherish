package com.cherish.app.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cherish.app.date.model.SolarDate
import com.cherish.app.event.repository.EventRepository
import com.cherish.app.home.mapper.HomeEventMapper
import com.cherish.app.home.model.HomeUiState
import com.cherish.app.home.model.HomeViewMode
import com.cherish.app.settings.repository.SettingsRepository
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * State holder for the Cherish Home screen.
 *
 * Responsibilities:
 * 1. Observes [EventRepository.events] reactively.
 * 2. Emits pinned events first, while strictly preserving user-defined order within pinned and unpinned groups.
 *    (Repository's underlying list order is NEVER mutated for presentation).
 * 3. Observes and persists [HomeViewMode] via [SettingsRepository].
 * 4. Manages reference today date and allows onResume / midnight refreshes.
 */
class HomeViewModel(
    private val repository: EventRepository,
    private val settingsRepository: SettingsRepository,
    private val todayProvider: () -> SolarDate = { SolarDate.fromLocalDate(LocalDate.now()) },
    private val mapper: HomeEventMapper = HomeEventMapper(),
    coroutineScope: CoroutineScope? = null,
) : ViewModel() {

    private val scope: CoroutineScope = coroutineScope ?: viewModelScope

    val viewMode: StateFlow<HomeViewMode> = settingsRepository.settings
        .map { it.homeViewMode }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = settingsRepository.getSettings().homeViewMode,
        )

    private val _currentToday = MutableStateFlow(todayProvider())
    val currentToday: StateFlow<SolarDate> = _currentToday

    fun toggleViewMode() {
        val current = settingsRepository.getSettings().homeViewMode
        val next = if (current == HomeViewMode.LIST) HomeViewMode.GRID else HomeViewMode.LIST
        settingsRepository.update { it.copy(homeViewMode = next) }
    }

    fun setViewMode(mode: HomeViewMode) {
        settingsRepository.update { it.copy(homeViewMode = mode) }
    }

    fun refreshToday() {
        val newToday = todayProvider()
        if (_currentToday.value != newToday) {
            _currentToday.value = newToday
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        repository.events,
        settingsRepository.settings,
        _currentToday,
    ) { events, settings, today ->
        val pinned = events.filter { it.isPinned }
        val unpinned = events.filter { !it.isPinned }
        val ordered = pinned + unpinned
        val uiModels = ordered.map { mapper.toUiModel(it, today) }

        HomeUiState(
            items = uiModels,
            viewMode = settings.homeViewMode,
            today = today,
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = run {
            val initialToday = todayProvider()
            val initialSettings = settingsRepository.getSettings()
            val allEvents = repository.getAll()
            val pinned = allEvents.filter { it.isPinned }
            val unpinned = allEvents.filter { !it.isPinned }
            val ordered = pinned + unpinned
            HomeUiState(
                items = ordered.map { mapper.toUiModel(it, initialToday) },
                viewMode = initialSettings.homeViewMode,
                today = initialToday,
            )
        },
    )
}
