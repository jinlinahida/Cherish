package com.cherish.app.event.repository

import com.cherish.app.date.model.LunarDate
import com.cherish.app.date.model.RepeatRule
import com.cherish.app.date.model.SolarDate
import com.cherish.app.event.model.CountdownEvent
import com.cherish.app.event.model.EventBackground
import com.cherish.app.event.model.EventCategory
import com.cherish.app.event.model.EventDate
import com.cherish.app.storage.AtomicFileEventStorage
import com.cherish.app.storage.JvmAtomicFileWriter
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EventPersistenceOrderingTest {

    private lateinit var tempDir: File
    private lateinit var storageFile: File
    private lateinit var storage: AtomicFileEventStorage
    private lateinit var repository: DefaultEventRepository

    @Before
    fun setUp() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "cherish_ordering_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        storageFile = File(tempDir, "events.json")
        storage = AtomicFileEventStorage(JvmAtomicFileWriter(storageFile))
        repository = DefaultEventRepository(storage)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `natural list order is strictly maintained and persisted on reorder`() {
        val a = CountdownEvent(id = "A", title = "Event A", eventDate = EventDate.Solar(SolarDate(2026, 1, 1)))
        val b = CountdownEvent(id = "B", title = "Event B", eventDate = EventDate.Solar(SolarDate(2026, 2, 1)))
        val c = CountdownEvent(id = "C", title = "Event C", eventDate = EventDate.Solar(SolarDate(2026, 3, 1)))

        repository.add(a)
        repository.add(b)
        repository.add(c)

        // Initial: [A, B, C]
        assertEquals(listOf("A", "B", "C"), repository.getAll().map { it.id })

        // 1. Move A (index 0) to end (index 2) -> [B, C, A]
        repository.reorder(0, 2)
        assertEquals(listOf("B", "C", "A"), repository.getAll().map { it.id })

        // Reload storage freshly from disk
        val reloaded1 = AtomicFileEventStorage(JvmAtomicFileWriter(storageFile)).load()
        assertEquals(listOf("B", "C", "A"), reloaded1.map { it.id })

        // 2. Move A (index 2) to first (index 0) -> [A, B, C]
        repository.reorder(2, 0)
        assertEquals(listOf("A", "B", "C"), repository.getAll().map { it.id })
        val reloaded2 = AtomicFileEventStorage(JvmAtomicFileWriter(storageFile)).load()
        assertEquals(listOf("A", "B", "C"), reloaded2.map { it.id })

        // 3. Move C (index 2) to middle (index 1) -> [A, C, B]
        repository.reorder(2, 1)
        assertEquals(listOf("A", "C", "B"), repository.getAll().map { it.id })
        val reloaded3 = AtomicFileEventStorage(JvmAtomicFileWriter(storageFile)).load()
        assertEquals(listOf("A", "C", "B"), reloaded3.map { it.id })
    }

    @Test
    fun `pinned status does not trigger automatic sorting in storage or repository`() {
        val pinnedA = CountdownEvent(
            id = "pin-A",
            title = "Pinned A",
            eventDate = EventDate.Solar(SolarDate(2026, 1, 1)),
            isPinned = true,
        )
        val normalB = CountdownEvent(
            id = "norm-B",
            title = "Normal B",
            eventDate = EventDate.Solar(SolarDate(2026, 2, 1)),
            isPinned = false,
        )
        val pinnedC = CountdownEvent(
            id = "pin-C",
            title = "Pinned C",
            eventDate = EventDate.Solar(SolarDate(2026, 3, 1)),
            isPinned = true,
        )
        val normalD = CountdownEvent(
            id = "norm-D",
            title = "Normal D",
            eventDate = EventDate.Solar(SolarDate(2026, 4, 1)),
            isPinned = false,
        )

        repository.add(pinnedA)
        repository.add(normalB)
        repository.add(pinnedC)
        repository.add(normalD)

        // Memory order must match insertion order exactly
        val expectedOrder = listOf("pin-A", "norm-B", "pin-C", "norm-D")
        assertEquals(expectedOrder, repository.getAll().map { it.id })

        // Disk order must also match insertion order exactly without auto-grouping by isPinned
        val diskStorage = AtomicFileEventStorage(JvmAtomicFileWriter(storageFile))
        val diskEvents = diskStorage.load()
        assertEquals(expectedOrder, diskEvents.map { it.id })

        // Assert isPinned flags are preserved intact
        assertTrue(diskEvents[0].isPinned)
        assertFalse(diskEvents[1].isPinned)
        assertTrue(diskEvents[2].isPinned)
        assertFalse(diskEvents[3].isPinned)

        // Reorder normalD to the very top: [norm-D, pin-A, norm-B, pin-C]
        repository.reorder(3, 0)
        val reorderedExpected = listOf("norm-D", "pin-A", "norm-B", "pin-C")
        assertEquals(reorderedExpected, repository.getAll().map { it.id })
        assertEquals(reorderedExpected, diskStorage.load().map { it.id })
    }

    @Test
    fun `specification lifecycle workflow - add, reorder, delete, update across process restarts`() {
        val a = CountdownEvent(
            id = "A",
            title = "A",
            eventDate = EventDate.Solar(SolarDate(2026, 1, 1)),
        )
        val b = CountdownEvent(
            id = "B",
            title = "B",
            eventDate = EventDate.Lunar(LunarDate(2026, 1, 1)),
        )
        val c = CountdownEvent(
            id = "C",
            title = "C",
            eventDate = EventDate.Solar(SolarDate(2026, 3, 1)),
        )

        // add A -> add B -> add C -> [A, B, C]
        repository.add(a)
        repository.add(b)
        repository.add(c)
        assertEquals(listOf("A", "B", "C"), repository.getAll().map { it.id })

        // reorder A -> end -> [B, C, A]
        repository.reorder(0, 2)
        assertEquals(listOf("B", "C", "A"), repository.getAll().map { it.id })

        // delete C -> [B, A]
        val deleted = repository.delete("C")
        assertTrue(deleted)
        assertEquals(listOf("B", "A"), repository.getAll().map { it.id })

        // update B -> [B(updated), A]
        val bUpdated = b.copy(
            title = "B(updated)",
            category = EventCategory.BIRTHDAY,
            repeatRule = RepeatRule.Yearly,
            isPinned = true,
            background = EventBackground.Color(0xFF00FF00),
        )
        repository.update(bUpdated)
        assertEquals(listOf(bUpdated, a), repository.getAll())

        // Recreate repository from the same on-disk file
        val recreatedStorage = AtomicFileEventStorage(JvmAtomicFileWriter(storageFile))
        val recreatedRepo = DefaultEventRepository(recreatedStorage)

        val persistedList = recreatedRepo.getAll()
        assertEquals(2, persistedList.size)
        assertEquals("B", persistedList[0].id)
        assertEquals("B(updated)", persistedList[0].title)
        assertEquals(EventCategory.BIRTHDAY, persistedList[0].category)
        assertTrue(persistedList[0].isPinned)
        assertEquals(EventBackground.Color(0xFF00FF00), persistedList[0].background)

        assertEquals("A", persistedList[1].id)
        assertEquals("A", persistedList[1].title)
        assertFalse(persistedList[1].isPinned)
    }

    @Test
    fun `StateFlow emits consistent snapshots through all mutations`() = runBlocking {
        assertEquals(emptyList<CountdownEvent>(), repository.events.first())

        val event = CountdownEvent(
            id = "flow-1",
            title = "Flow Event",
            eventDate = EventDate.Solar(SolarDate(2026, 5, 5)),
        )
        repository.add(event)
        assertEquals(listOf(event), repository.events.first())

        val updated = event.copy(title = "Flow Event Updated")
        repository.update(updated)
        assertEquals(listOf(updated), repository.events.first())

        repository.delete("flow-1")
        assertEquals(emptyList<CountdownEvent>(), repository.events.first())
    }
}
