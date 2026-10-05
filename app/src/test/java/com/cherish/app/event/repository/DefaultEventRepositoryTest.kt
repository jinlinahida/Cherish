package com.cherish.app.event.repository

import com.cherish.app.date.model.LunarDate
import com.cherish.app.date.model.SolarDate
import com.cherish.app.event.model.CountdownEvent
import com.cherish.app.event.model.EventBackground
import com.cherish.app.event.model.EventCategory
import com.cherish.app.event.model.EventDate
import com.cherish.app.storage.EventStorage
import com.cherish.app.storage.EventStorageException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class DefaultEventRepositoryTest {

    private class InMemoryTestStorage : EventStorage {
        var eventsList = mutableListOf<CountdownEvent>()
        var failOnSave = false

        override fun load(): List<CountdownEvent> = eventsList.toList()

        override fun save(events: List<CountdownEvent>) {
            if (failOnSave) {
                throw EventStorageException.StorageWriteException("Simulated storage write error")
            }
            eventsList = events.toMutableList()
        }
    }

    private lateinit var storage: InMemoryTestStorage
    private lateinit var repository: DefaultEventRepository

    @Before
    fun setUp() {
        storage = InMemoryTestStorage()
        repository = DefaultEventRepository(storage)
    }

    @Test
    fun `initial state is empty when storage has no events`() {
        assertTrue(repository.getAll().isEmpty())
        assertTrue(repository.events.value.isEmpty())
    }

    @Test
    fun `initial state loads pre-existing events from storage`() {
        val preExisting = CountdownEvent(
            id = "pre-1",
            title = "Existing Event",
            eventDate = EventDate.Solar(SolarDate(2026, 1, 1)),
        )
        storage.eventsList.add(preExisting)

        val freshRepo = DefaultEventRepository(storage)
        assertEquals(1, freshRepo.getAll().size)
        assertEquals(preExisting, freshRepo.getAll()[0])
        assertEquals(preExisting, freshRepo.events.value[0])
    }

    @Test
    fun `add appends event, updates StateFlow, and writes to storage`() {
        val eventA = CountdownEvent(
            id = "a",
            title = "Event A",
            eventDate = EventDate.Solar(SolarDate(2026, 2, 1)),
        )
        val eventB = CountdownEvent(
            id = "b",
            title = "Event B",
            eventDate = EventDate.Solar(SolarDate(2026, 3, 1)),
        )

        repository.add(eventA)
        assertEquals(listOf(eventA), repository.getAll())
        assertEquals(listOf(eventA), repository.events.value)
        assertEquals(listOf(eventA), storage.eventsList)

        repository.add(eventB)
        assertEquals(listOf(eventA, eventB), repository.getAll())
        assertEquals(listOf(eventA, eventB), repository.events.value)
        assertEquals(listOf(eventA, eventB), storage.eventsList)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `add duplicate ID throws IllegalArgumentException`() {
        val event = CountdownEvent(
            id = "duplicate-id",
            title = "Original",
            eventDate = EventDate.Solar(SolarDate(2026, 1, 1)),
        )
        repository.add(event)
        repository.add(event.copy(title = "Duplicate"))
    }

    @Test
    fun `getById returns correct event or null`() {
        val event = CountdownEvent(
            id = "find-me",
            title = "Find Me",
            eventDate = EventDate.Solar(SolarDate(2026, 5, 1)),
        )
        repository.add(event)

        assertEquals(event, repository.getById("find-me"))
        assertNull(repository.getById("non-existent"))
    }

    @Test
    fun `update modifies existing event and persists changes`() {
        val event = CountdownEvent(
            id = "ev-update",
            title = "Old Title",
            eventDate = EventDate.Solar(SolarDate(2026, 6, 1)),
            isPinned = false,
        )
        repository.add(event)

        val updated = event.copy(title = "New Title", isPinned = true)
        repository.update(updated)

        assertEquals(updated, repository.getById("ev-update"))
        assertEquals(listOf(updated), repository.getAll())
        assertEquals(listOf(updated), repository.events.value)
        assertEquals(listOf(updated), storage.eventsList)
    }

    @Test(expected = NoSuchElementException::class)
    fun `update non-existent event throws NoSuchElementException`() {
        val ghost = CountdownEvent(
            id = "ghost",
            title = "Ghost",
            eventDate = EventDate.Solar(SolarDate(2026, 1, 1)),
        )
        repository.update(ghost)
    }

    @Test
    fun `delete removes event by ID and updates StateFlow`() {
        val eventA = CountdownEvent(id = "a", title = "A", eventDate = EventDate.Solar(SolarDate(2026, 1, 1)))
        val eventB = CountdownEvent(id = "b", title = "B", eventDate = EventDate.Solar(SolarDate(2026, 2, 1)))
        repository.add(eventA)
        repository.add(eventB)

        val deleted = repository.delete("a")
        assertTrue(deleted)
        assertEquals(listOf(eventB), repository.getAll())
        assertEquals(listOf(eventB), repository.events.value)
        assertEquals(listOf(eventB), storage.eventsList)

        // Deleting non-existent ID returns false
        val deletedAgain = repository.delete("non-existent")
        assertFalse(deletedAgain)
    }

    @Test
    fun `reorder shifts element position and updates storage`() {
        val a = CountdownEvent(id = "a", title = "A", eventDate = EventDate.Solar(SolarDate(2026, 1, 1)))
        val b = CountdownEvent(id = "b", title = "B", eventDate = EventDate.Solar(SolarDate(2026, 2, 1)))
        val c = CountdownEvent(id = "c", title = "C", eventDate = EventDate.Solar(SolarDate(2026, 3, 1)))
        repository.add(a)
        repository.add(b)
        repository.add(c)

        // Initial: [A, B, C]
        assertEquals(listOf("a", "b", "c"), repository.getAll().map { it.id })

        // Reorder A (index 0) to end (index 2) -> [B, C, A]
        repository.reorder(0, 2)
        assertEquals(listOf("b", "c", "a"), repository.getAll().map { it.id })
        assertEquals(listOf("b", "c", "a"), repository.events.value.map { it.id })
        assertEquals(listOf("b", "c", "a"), storage.eventsList.map { it.id })

        // Reorder identical indices is a no-op
        repository.reorder(1, 1)
        assertEquals(listOf("b", "c", "a"), repository.getAll().map { it.id })
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun `reorder with invalid fromIndex throws IndexOutOfBoundsException`() {
        repository.add(CountdownEvent(id = "1", title = "1", eventDate = EventDate.Solar(SolarDate(2026, 1, 1))))
        repository.reorder(-1, 0)
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun `reorder with invalid toIndex throws IndexOutOfBoundsException`() {
        repository.add(CountdownEvent(id = "1", title = "1", eventDate = EventDate.Solar(SolarDate(2026, 1, 1))))
        repository.reorder(0, 5)
    }

    @Test
    fun `failure semantics - state is rolled back and not faked if storage write fails`() {
        val eventA = CountdownEvent(id = "a", title = "A", eventDate = EventDate.Solar(SolarDate(2026, 1, 1)))
        repository.add(eventA)

        // Turn on storage failure
        storage.failOnSave = true

        val eventB = CountdownEvent(id = "b", title = "B", eventDate = EventDate.Solar(SolarDate(2026, 2, 1)))
        try {
            repository.add(eventB)
            fail("Expected storage exception")
        } catch (e: EventStorageException.StorageWriteException) {
            // Memory state must STILL be only [A], not [A, B]
            assertEquals(listOf(eventA), repository.getAll())
            assertEquals(listOf(eventA), repository.events.value)
        }

        // Test update failure
        val updatedA = eventA.copy(title = "Updated A")
        try {
            repository.update(updatedA)
            fail("Expected storage exception")
        } catch (e: EventStorageException.StorageWriteException) {
            assertEquals("A", repository.getById("a")?.title)
        }

        // Test delete failure
        try {
            repository.delete("a")
            fail("Expected storage exception")
        } catch (e: EventStorageException.StorageWriteException) {
            assertEquals(1, repository.getAll().size)
        }
    }

    @Test
    fun `full lifecycle workflow preserves state and ordering across new repository instances`() {
        val a = CountdownEvent(
            id = "a",
            title = "A",
            eventDate = EventDate.Solar(SolarDate(2026, 1, 1)),
            isPinned = true,
        )
        val b = CountdownEvent(
            id = "b",
            title = "B",
            eventDate = EventDate.Lunar(LunarDate(2026, 2, 2)),
            isPinned = false,
        )
        val c = CountdownEvent(
            id = "c",
            title = "C",
            category = EventCategory.BIRTHDAY,
            eventDate = EventDate.Solar(SolarDate(2026, 3, 3)),
            isPinned = true,
            background = EventBackground.Color(0xFF888888),
        )

        // 1. Add A, B, C -> [A, B, C]
        repository.add(a)
        repository.add(b)
        repository.add(c)
        assertEquals(listOf("a", "b", "c"), repository.getAll().map { it.id })

        // 2. Reorder A to end -> [B, C, A]
        repository.reorder(0, 2)
        assertEquals(listOf("b", "c", "a"), repository.getAll().map { it.id })

        // 3. Delete C -> [B, A]
        repository.delete("c")
        assertEquals(listOf("b", "a"), repository.getAll().map { it.id })

        // 4. Update B
        val bUpdated = b.copy(title = "B (Updated)", isPinned = true)
        repository.update(bUpdated)
        assertEquals(listOf(bUpdated, a), repository.getAll())

        // 5. Recreate repository from same storage: state must be strictly identical
        val newRepo = DefaultEventRepository(storage)
        assertEquals(listOf(bUpdated, a), newRepo.getAll())
        assertEquals(listOf(bUpdated, a), newRepo.events.value)
        assertTrue(newRepo.getById("b")!!.isPinned)
        assertTrue(newRepo.getById("a")!!.isPinned)
    }
}
