package com.cherish.app.home

import com.cherish.app.date.model.SolarDate
import com.cherish.app.event.model.CountdownEvent
import com.cherish.app.event.model.EventDate
import com.cherish.app.event.repository.EventRepository
import com.cherish.app.home.model.CountdownDisplayStatus
import com.cherish.app.home.model.HomeViewMode
import com.cherish.app.settings.model.AppSettings
import com.cherish.app.settings.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HomeViewModelTest {

    private class FakeEventRepository : EventRepository {
        val list = mutableListOf<CountdownEvent>()
        private val _events = MutableStateFlow<List<CountdownEvent>>(emptyList())
        override val events: StateFlow<List<CountdownEvent>> = _events.asStateFlow()

        override fun getAll(): List<CountdownEvent> = list.toList()
        override fun getById(id: String): CountdownEvent? = list.firstOrNull { it.id == id }

        override fun add(event: CountdownEvent) {
            list.add(event)
            _events.value = list.toList()
        }

        override fun update(event: CountdownEvent) {
            val idx = list.indexOfFirst { it.id == event.id }
            if (idx != -1) {
                list[idx] = event
                _events.value = list.toList()
            }
        }

        override fun delete(id: String): Boolean {
            val removed = list.removeAll { it.id == id }
            if (removed) _events.value = list.toList()
            return removed
        }

        override fun reorder(fromIndex: Int, toIndex: Int) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            _events.value = list.toList()
        }

        override fun reorderAll(orderedEvents: List<CountdownEvent>) {
            list.clear()
            list.addAll(orderedEvents)
            _events.value = list.toList()
        }

        override fun reload() {
            _events.value = list.toList()
        }
    }

    private class FakeSettingsRepository(initial: AppSettings = AppSettings()) : SettingsRepository {
        private val _settings = MutableStateFlow(initial)
        override val settings: StateFlow<AppSettings> = _settings
        override fun getSettings(): AppSettings = _settings.value
        override fun update(transform: (AppSettings) -> AppSettings) {
            _settings.value = transform(_settings.value)
        }
        override fun reload() {}
    }

    private lateinit var repository: FakeEventRepository
    private lateinit var settingsRepository: FakeSettingsRepository
    private var simulatedToday = SolarDate(2026, 10, 5)
    private lateinit var viewModel: HomeViewModel
    private val testScope = CoroutineScope(Dispatchers.Unconfined)

    @Before
    fun setUp() {
        repository = FakeEventRepository()
        settingsRepository = FakeSettingsRepository()
        viewModel = HomeViewModel(
            repository = repository,
            settingsRepository = settingsRepository,
            todayProvider = { simulatedToday },
            coroutineScope = testScope,
        )
    }

    @Test
    fun `initial state is empty when repository has no events`() {
        val state = viewModel.uiState.value
        assertTrue(state.isEmpty)
        assertEquals(HomeViewMode.LIST, state.viewMode)
        assertEquals(simulatedToday, state.today)
    }

    @Test
    fun `adding events updates uiState reactively`() {
        val event = CountdownEvent(
            id = "ev-1",
            title = "Target Event",
            eventDate = EventDate.Solar(SolarDate(2026, 10, 15)),
        )
        repository.add(event)

        val state = viewModel.uiState.value
        assertFalse(state.isEmpty)
        assertEquals(1, state.items.size)
        assertEquals("Target Event", state.items[0].event.title)
        assertEquals(10, state.items[0].daysCount)
        assertEquals(CountdownDisplayStatus.COUNTDOWN, state.items[0].status)
    }

    @Test
    fun `pinned events appear first in presentation without altering underlying repository list order`() {
        val a = CountdownEvent(id = "A", title = "Event A", eventDate = EventDate.Solar(SolarDate(2026, 10, 10)), isPinned = false)
        val b = CountdownEvent(id = "B", title = "Event B", eventDate = EventDate.Solar(SolarDate(2026, 10, 11)), isPinned = true)
        val c = CountdownEvent(id = "C", title = "Event C", eventDate = EventDate.Solar(SolarDate(2026, 10, 12)), isPinned = false)
        val d = CountdownEvent(id = "D", title = "Event D", eventDate = EventDate.Solar(SolarDate(2026, 10, 13)), isPinned = true)

        // Storage order: [A, B, C, D]
        repository.add(a)
        repository.add(b)
        repository.add(c)
        repository.add(d)

        // Repository storage order must strictly remain [A, B, C, D]
        assertEquals(listOf("A", "B", "C", "D"), repository.getAll().map { it.id })

        // Home presentation order puts pinned first: [B, D, A, C]
        val displayedIds = viewModel.uiState.value.items.map { it.event.id }
        assertEquals(listOf("B", "D", "A", "C"), displayedIds)
    }

    @Test
    fun `toggling view mode switches between LIST and GRID`() {
        assertEquals(HomeViewMode.LIST, viewModel.uiState.value.viewMode)

        viewModel.toggleViewMode()
        assertEquals(HomeViewMode.GRID, viewModel.uiState.value.viewMode)

        viewModel.toggleViewMode()
        assertEquals(HomeViewMode.LIST, viewModel.uiState.value.viewMode)

        viewModel.setViewMode(HomeViewMode.GRID)
        assertEquals(HomeViewMode.GRID, viewModel.uiState.value.viewMode)
    }

    @Test
    fun `toggling view mode updates settingsRepository and persists`() {
        assertEquals(HomeViewMode.LIST, settingsRepository.getSettings().homeViewMode)

        viewModel.toggleViewMode()
        assertEquals(HomeViewMode.GRID, settingsRepository.getSettings().homeViewMode)
        assertEquals(HomeViewMode.GRID, viewModel.uiState.value.viewMode)

        viewModel.setViewMode(HomeViewMode.LIST)
        assertEquals(HomeViewMode.LIST, settingsRepository.getSettings().homeViewMode)
        assertEquals(HomeViewMode.LIST, viewModel.uiState.value.viewMode)
    }

    @Test
    fun `refreshToday updates calculations when date passes midnight`() {
        val event = CountdownEvent(
            id = "midnight-test",
            title = "Midnight Test",
            eventDate = EventDate.Solar(SolarDate(2026, 10, 6)),
        )
        repository.add(event)

        // On 2026-10-05, target is 1 day away
        assertEquals(1, viewModel.uiState.value.items[0].daysCount)
        assertEquals(CountdownDisplayStatus.COUNTDOWN, viewModel.uiState.value.items[0].status)

        // Midnight arrives: new day is 2026-10-06
        simulatedToday = SolarDate(2026, 10, 6)
        viewModel.refreshToday()

        // Recalculates to TODAY
        val updatedState = viewModel.uiState.value
        assertEquals(0, updatedState.items[0].daysCount)
        assertEquals(CountdownDisplayStatus.TODAY, updatedState.items[0].status)
    }

    @Test
    fun `reorder and pin coexistence preserves repository order and correctly resolves presentation order`() {
        val a = CountdownEvent(id = "A", title = "Event A", eventDate = EventDate.Solar(SolarDate(2026, 10, 10)), isPinned = false)
        val b = CountdownEvent(id = "B", title = "Event B", eventDate = EventDate.Solar(SolarDate(2026, 10, 11)), isPinned = false)
        val c = CountdownEvent(id = "C", title = "Event C", eventDate = EventDate.Solar(SolarDate(2026, 10, 12)), isPinned = false)
        val d = CountdownEvent(id = "D", title = "Event D", eventDate = EventDate.Solar(SolarDate(2026, 10, 13)), isPinned = false)

        repository.add(a)
        repository.add(b)
        repository.add(c)
        repository.add(d)

        // 1. Initial repository order: [A, B, C, D]
        assertEquals(listOf("A", "B", "C", "D"), repository.getAll().map { it.id })

        // 2. Reorder to [C, A, D, B]:
        // Move C (index 2) to 0 -> [C, A, B, D]
        repository.reorder(2, 0)
        // Move D (index 3) to 2 -> [C, A, D, B]
        repository.reorder(3, 2)
        assertEquals(listOf("C", "A", "D", "B"), repository.getAll().map { it.id })

        // 3. Pin A and B:
        repository.update(a.copy(isPinned = true))
        repository.update(b.copy(isPinned = true))
        // Repository order remains strictly [C, A, D, B]
        assertEquals(listOf("C", "A", "D", "B"), repository.getAll().map { it.id })
        // Presentation order: pinned [A, B] + unpinned [C, D] -> [A, B, C, D]
        assertEquals(listOf("A", "B", "C", "D"), viewModel.uiState.value.items.map { it.event.id })

        // 4. Unpin A:
        repository.update(a.copy(isPinned = false))
        // Repository order remains strictly [C, A, D, B]
        assertEquals(listOf("C", "A", "D", "B"), repository.getAll().map { it.id })
        // Presentation order: pinned [B] + unpinned [C, A, D] -> [B, C, A, D]
        assertEquals(listOf("B", "C", "A", "D"), viewModel.uiState.value.items.map { it.event.id })
    }
}
