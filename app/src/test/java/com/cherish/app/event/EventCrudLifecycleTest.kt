package com.cherish.app.event

import com.cherish.app.date.calc.CountdownCalculator
import com.cherish.app.date.calc.EventCountdownStatus
import com.cherish.app.date.calc.RecurrenceCalculator
import com.cherish.app.date.lunar.DefaultLunarCalendar
import com.cherish.app.date.model.LunarDate
import com.cherish.app.date.model.RepeatRule
import com.cherish.app.date.model.SolarDate
import com.cherish.app.editor.model.EventEditorState
import com.cherish.app.editor.sanitizer.DatePickerSanitizer
import com.cherish.app.event.model.CountdownEvent
import com.cherish.app.event.model.EventBackground
import com.cherish.app.event.model.EventCategory
import com.cherish.app.event.model.EventDate
import com.cherish.app.event.repository.DefaultEventRepository
import com.cherish.app.storage.AtomicFileEventStorage
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * End-to-end lifecycle and persistence tests for Stage 5.
 *
 * Verifies:
 * - Full CRUD cycle: add -> get -> update -> get -> delete -> get == null
 * - Atomic persistence verification: disk reload preserves created and updated events
 * - Date calculation engine accuracy on created/updated events (future, today, past, leap years, lunar rollover)
 * - Repository ordering stability across edits
 */
class EventCrudLifecycleTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val calendar = DefaultLunarCalendar()
    private val recurrenceCalculator = RecurrenceCalculator(calendar)
    private val referenceToday = SolarDate(2026, 10, 5)

    @Test
    fun `full repository crud cycle functions atomically`() {
        val storageFile = File(tempFolder.root, "test_events.json")
        val storage = AtomicFileEventStorage(com.cherish.app.storage.JvmAtomicFileWriter(storageFile))
        val repository = DefaultEventRepository(storage)

        // 1. Create & Add
        val event = CountdownEvent(
            id = "test-1",
            title = "妈妈生日",
            emoji = "🎂",
            category = EventCategory.BIRTHDAY,
            eventDate = EventDate.Solar(SolarDate(2026, 10, 15)),
            repeatRule = RepeatRule.Yearly,
            isPinned = false,
        )
        repository.add(event)

        // 2. Read
        val loaded = repository.getById("test-1")
        assertNotNull(loaded)
        assertEquals("妈妈生日", loaded?.title)
        assertFalse(loaded?.isPinned ?: true)

        // 3. Update (edit title and pin it)
        val updated = loaded!!.copy(
            title = "亲爱的妈妈生日",
            isPinned = true,
            background = EventBackground.Color(0xFFE91E63),
        )
        repository.update(updated)

        val updatedLoaded = repository.getById("test-1")
        assertNotNull(updatedLoaded)
        assertEquals("亲爱的妈妈生日", updatedLoaded?.title)
        assertTrue(updatedLoaded?.isPinned ?: false)
        assertEquals(EventBackground.Color(0xFFE91E63), updatedLoaded?.background)

        // 4. Delete
        val deleteSuccess = repository.delete("test-1")
        assertTrue(deleteSuccess)
        assertNull(repository.getById("test-1"))
        assertTrue(repository.getAll().isEmpty())
    }

    @Test
    fun `atomic persistence survives repository reload across create and update`() {
        val storageFile = File(tempFolder.root, "persist_events.json")
        val storage1 = AtomicFileEventStorage(com.cherish.app.storage.JvmAtomicFileWriter(storageFile))
        val repo1 = DefaultEventRepository(storage1)

        val event1 = CountdownEvent(
            id = "ev-1",
            title = "Event One",
            eventDate = EventDate.Solar(SolarDate(2026, 11, 1)),
        )
        val event2 = CountdownEvent(
            id = "ev-2",
            title = "Event Two",
            eventDate = EventDate.Solar(SolarDate(2026, 12, 1)),
        )
        repo1.add(event1)
        repo1.add(event2)

        // Simulate app kill & restart by instantiating repo2 on same file
        val repo2 = DefaultEventRepository(AtomicFileEventStorage(com.cherish.app.storage.JvmAtomicFileWriter(storageFile)))
        assertEquals(2, repo2.getAll().size)
        assertEquals("Event One", repo2.getById("ev-1")?.title)
        assertEquals("Event Two", repo2.getById("ev-2")?.title)

        // Update event1 in repo2
        val modifiedEvent1 = repo2.getById("ev-1")!!.copy(title = "Event One Modified")
        repo2.update(modifiedEvent1)

        // Simulate second reload
        val repo3 = DefaultEventRepository(AtomicFileEventStorage(com.cherish.app.storage.JvmAtomicFileWriter(storageFile)))
        assertEquals("Event One Modified", repo3.getById("ev-1")?.title)
        assertEquals(listOf("ev-1", "ev-2"), repo3.getAll().map { it.id })
    }

    @Test
    fun `created and edited events correctly evaluate through countdown calculation engine`() {
        // Future event (10 days away)
        val futureEvent = CountdownEvent(
            title = "Future",
            eventDate = EventDate.Solar(SolarDate(2026, 10, 15)),
        )
        val statusFuture = CountdownCalculator.calculate(referenceToday, (futureEvent.eventDate as EventDate.Solar).date)
        assertTrue(statusFuture is EventCountdownStatus.Countdown)
        assertEquals(10L, (statusFuture as EventCountdownStatus.Countdown).days)

        // Today event
        val todayEvent = CountdownEvent(
            title = "Today",
            eventDate = EventDate.Solar(referenceToday),
        )
        val statusToday = CountdownCalculator.calculate(referenceToday, (todayEvent.eventDate as EventDate.Solar).date)
        assertTrue(statusToday is EventCountdownStatus.Today)

        // Past event (4 days ago)
        val pastEvent = CountdownEvent(
            title = "Past",
            eventDate = EventDate.Solar(SolarDate(2026, 10, 1)),
        )
        val statusPast = CountdownCalculator.calculate(referenceToday, (pastEvent.eventDate as EventDate.Solar).date)
        assertTrue(statusPast is EventCountdownStatus.CountedPast)
        assertEquals(4L, (statusPast as EventCountdownStatus.CountedPast).days)

        // Yearly recurrence rollover across past date
        val nextOccurrence = recurrenceCalculator.nextOccurrence(
            baseSolar = SolarDate(2026, 5, 1),
            baseLunar = null,
            referenceDate = referenceToday,
            rule = RepeatRule.Yearly,
        )
        assertEquals(SolarDate(2027, 5, 1), nextOccurrence)

        // Monthly 31st recurrence in month with 30 days (clamped to 30)
        val nextOccurrenceMonthly = recurrenceCalculator.nextOccurrence(
            baseSolar = SolarDate(2026, 1, 31),
            baseLunar = null,
            referenceDate = SolarDate(2026, 4, 1),
            rule = RepeatRule.Monthly,
        )
        assertEquals(SolarDate(2026, 4, 30), nextOccurrenceMonthly)

        // Feb 29 leap year rollover into non-leap year (clamped to Feb 28)
        val nextOccurrenceFeb29 = recurrenceCalculator.nextOccurrence(
            baseSolar = SolarDate(2024, 2, 29),
            baseLunar = null,
            referenceDate = SolarDate(2025, 1, 1),
            rule = RepeatRule.Yearly,
        )
        assertEquals(SolarDate(2025, 2, 28), nextOccurrenceFeb29)
    }

    @Test
    fun `editor state creation and build validation passes full cycle`() {
        val editorState = EventEditorState.createDefault(
            today = referenceToday,
            initialLunar = LunarDate(2026, 8, 25),
        ).copy(
            title = "公司年会",
            emoji = "🎉",
            category = EventCategory.WORK,
            solarDate = SolarDate(2026, 12, 31),
            isPinned = true,
        )

        val buildResult = DatePickerSanitizer.validateAndBuildEvent(editorState, calendar)
        assertTrue(buildResult.isSuccess)
        val event = buildResult.getOrThrow()

        assertEquals("公司年会", event.title)
        assertEquals("🎉", event.emoji)
        assertEquals(EventCategory.WORK, event.category)
        assertTrue(event.isPinned)
        assertEquals(SolarDate(2026, 12, 31), (event.eventDate as EventDate.Solar).date)
    }
}
