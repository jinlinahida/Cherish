package com.cherish.app.settings.model

import com.cherish.app.date.model.SolarDate
import com.cherish.app.event.model.CountdownEvent
import com.cherish.app.event.model.EventDate
import com.cherish.app.event.repository.DefaultEventRepository
import com.cherish.app.storage.AtomicFileEventStorage
import com.cherish.app.storage.JvmAtomicFileWriter
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EventReorderHelperTest {

    private lateinit var tempDir: File
    private lateinit var storageFile: File
    private lateinit var repository: DefaultEventRepository

    @Before
    fun setUp() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "cherish_reorder_helper_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        storageFile = File(tempDir, "events.json")
        val storage = AtomicFileEventStorage(JvmAtomicFileWriter(storageFile))
        repository = DefaultEventRepository(storage)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    private fun createSampleEvent(id: String, title: String, isPinned: Boolean = false): CountdownEvent =
        CountdownEvent(
            id = id,
            title = title,
            eventDate = EventDate.Solar(SolarDate(2026, 1, 1)),
            isPinned = isPinned,
        )

    @Test
    fun `reorder moves element downwards correctly`() {
        val list = listOf("A", "B", "C", "D")
        val result = EventReorderHelper.reorder(list, 0, 2)
        assertEquals(listOf("B", "C", "A", "D"), result)
    }

    @Test
    fun `reorder moves element upwards correctly`() {
        val list = listOf("A", "B", "C", "D")
        val result = EventReorderHelper.reorder(list, 3, 1)
        assertEquals(listOf("A", "D", "B", "C"), result)
    }

    @Test
    fun `reorder with same index is a no-op`() {
        val list = listOf("A", "B", "C")
        val result = EventReorderHelper.reorder(list, 1, 1)
        assertEquals(list, result)
    }

    @Test
    fun `reorder with out of bounds index returns original list`() {
        val list = listOf("A", "B", "C")
        assertEquals(list, EventReorderHelper.reorder(list, -1, 1))
        assertEquals(list, EventReorderHelper.reorder(list, 1, 5))
    }

    @Test
    fun `reorder preserves pinned flags and event integrity`() {
        val e1 = createSampleEvent("1", "Event 1", isPinned = true)
        val e2 = createSampleEvent("2", "Event 2", isPinned = false)
        val e3 = createSampleEvent("3", "Event 3", isPinned = true)
        val original = listOf(e1, e2, e3)

        val reordered = EventReorderHelper.reorder(original, 0, 2)
        assertEquals(listOf("2", "3", "1"), reordered.map { it.id })
        assertTrue(reordered[0].id == "2" && !reordered[0].isPinned)
        assertTrue(reordered[1].id == "3" && reordered[1].isPinned)
        assertTrue(reordered[2].id == "1" && reordered[2].isPinned)
        assertTrue(EventReorderHelper.validateReorderIntegrity(original, reordered))
    }

    @Test
    fun `calculateTargetIndex calculates correct indices with 50 percent threshold`() {
        val itemHeight = 60f
        val total = 5

        // Dragging starting at index 1:
        // Tiny drag down (< 30px): stays at 1
        assertEquals(1, EventReorderHelper.calculateTargetIndex(1, 15f, itemHeight, total))
        // Drag down crosses 30px (e.g. 35px): snaps to 2
        assertEquals(2, EventReorderHelper.calculateTargetIndex(1, 35f, itemHeight, total))
        // Drag down by 80px: snaps to 2 (1 + round(80/60) = 1 + 1 = 2)
        assertEquals(2, EventReorderHelper.calculateTargetIndex(1, 80f, itemHeight, total))
        // Drag down by 100px: snaps to 3 (1 + round(100/60) = 1 + 2 = 3)
        assertEquals(3, EventReorderHelper.calculateTargetIndex(1, 100f, itemHeight, total))

        // Drag up crosses -30px (e.g. -35px): snaps to 0
        assertEquals(0, EventReorderHelper.calculateTargetIndex(1, -35f, itemHeight, total))

        // Clamping at boundaries:
        assertEquals(0, EventReorderHelper.calculateTargetIndex(0, -200f, itemHeight, total))
        assertEquals(4, EventReorderHelper.calculateTargetIndex(3, 500f, itemHeight, total))
    }

    @Test
    fun `drag reorder operations correctly commit to repository and persist to disk`() {
        val a = createSampleEvent("A", "Birthday", isPinned = true)
        val b = createSampleEvent("B", "Anniversary", isPinned = false)
        val c = createSampleEvent("C", "New Year", isPinned = true)
        val d = createSampleEvent("D", "Vacation", isPinned = false)

        repository.add(a)
        repository.add(b)
        repository.add(c)
        repository.add(d)

        assertEquals(listOf("A", "B", "C", "D"), repository.getAll().map { it.id })

        // Simulate dragging B (index 1) to end (index 3)
        repository.reorder(1, 3)
        assertEquals(listOf("A", "C", "D", "B"), repository.getAll().map { it.id })

        // Reload fresh from disk storage to ensure persistence
        val reloadedStorage = AtomicFileEventStorage(JvmAtomicFileWriter(storageFile))
        val diskEvents = reloadedStorage.load()
        assertEquals(listOf("A", "C", "D", "B"), diskEvents.map { it.id })
        assertTrue(diskEvents[0].isPinned) // A
        assertTrue(diskEvents[1].isPinned) // C
        assertFalse(diskEvents[2].isPinned) // D
        assertFalse(diskEvents[3].isPinned) // B

        // Simulate dragging D (now index 2) to first position (index 0)
        repository.reorder(2, 0)
        assertEquals(listOf("D", "A", "C", "B"), repository.getAll().map { it.id })
        val reloadedAfterSecondMove = reloadedStorage.load()
        assertEquals(listOf("D", "A", "C", "B"), reloadedAfterSecondMove.map { it.id })
    }
}
