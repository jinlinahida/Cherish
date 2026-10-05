package com.cherish.app.storage

import com.cherish.app.date.model.LunarDate
import com.cherish.app.date.model.RepeatRule
import com.cherish.app.date.model.RepeatUnit
import com.cherish.app.date.model.SolarDate
import com.cherish.app.event.model.CountdownEvent
import com.cherish.app.event.model.EventBackground
import com.cherish.app.event.model.EventCategory
import com.cherish.app.event.model.EventDate
import java.io.File
import java.io.IOException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class AtomicFileEventStorageTest {

    private lateinit var tempDir: File
    private lateinit var storageFile: File
    private lateinit var storage: AtomicFileEventStorage

    @Before
    fun setUp() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "cherish_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        storageFile = File(tempDir, "events.json")
        storage = AtomicFileEventStorage(JvmAtomicFileWriter(storageFile))
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `reading non-existent file returns empty list without error`() {
        assertFalse(storageFile.exists())
        val events = storage.load()
        assertTrue(events.isEmpty())
    }

    @Test
    fun `saving empty list persists empty json array`() {
        storage.save(emptyList())
        assertTrue(storageFile.exists())

        val reloaded = storage.load()
        assertTrue(reloaded.isEmpty())
    }

    @Test
    fun `saving and reloading a single event preserves all fields`() {
        val event = CountdownEvent(
            id = "single-1",
            title = "Birthday Celebration 🎂",
            emoji = "🎉",
            category = EventCategory.BIRTHDAY,
            eventDate = EventDate.Solar(SolarDate(2026, 12, 25)),
            repeatRule = RepeatRule.Yearly,
            isPinned = true,
            background = EventBackground.Color(0xFFE91E63),
            notes = "Remember the cake!",
        )

        storage.save(listOf(event))
        val reloaded = storage.load()

        assertEquals(1, reloaded.size)
        assertEquals(event, reloaded[0])
    }

    @Test
    fun `saving multiple events preserves exact list ordering`() {
        val events = listOf(
            CountdownEvent(
                id = "id-c",
                title = "Event C",
                eventDate = EventDate.Solar(SolarDate(2027, 3, 1)),
                isPinned = false,
            ),
            CountdownEvent(
                id = "id-a",
                title = "Event A",
                eventDate = EventDate.Solar(SolarDate(2026, 1, 1)),
                isPinned = true,
            ),
            CountdownEvent(
                id = "id-b",
                title = "Event B",
                eventDate = EventDate.Solar(SolarDate(2026, 5, 1)),
                isPinned = false,
            ),
        )

        storage.save(events)
        val reloaded = storage.load()

        assertEquals(3, reloaded.size)
        assertEquals("id-c", reloaded[0].id)
        assertEquals("id-a", reloaded[1].id)
        assertEquals("id-b", reloaded[2].id)
        assertEquals(events, reloaded)
    }

    @Test
    fun `multiple consecutive saves overwrite state cleanly without corruption`() {
        val event1 = CountdownEvent(
            id = "ev-1",
            title = "Version 1",
            eventDate = EventDate.Solar(SolarDate(2026, 6, 1)),
        )
        storage.save(listOf(event1))
        assertEquals("Version 1", storage.load()[0].title)

        val event2 = CountdownEvent(
            id = "ev-2",
            title = "Version 2",
            eventDate = EventDate.Solar(SolarDate(2026, 7, 1)),
        )
        storage.save(listOf(event1, event2))
        val reloaded2 = storage.load()
        assertEquals(2, reloaded2.size)
        assertEquals("Version 2", reloaded2[1].title)

        // Third save removes event1
        storage.save(listOf(event2))
        val reloaded3 = storage.load()
        assertEquals(1, reloaded3.size)
        assertEquals("ev-2", reloaded3[0].id)
    }

    @Test
    fun `special data - supports multilingual, complex emojis, lunar leap months, and custom rules`() {
        val complexEvent = CountdownEvent(
            id = "complex-1",
            title = "中秋节 🏮 & National Holiday 🇨🇳",
            emoji = "🥮",
            category = EventCategory.HOLIDAY,
            eventDate = EventDate.Lunar(LunarDate(2028, 5, 15, isLeapMonth = true)),
            repeatRule = RepeatRule.Custom(interval = 2, unit = RepeatUnit.WEEK),
            isPinned = true,
            background = EventBackground.Gradient(0xFF123456, 0xFF654321, 135f),
            notes = "Special notes with emoji 🚀 and newline\nSecond line",
        )

        storage.save(listOf(complexEvent))
        val reloaded = storage.load()

        assertEquals(1, reloaded.size)
        val loaded = reloaded[0]
        assertEquals("中秋节 🏮 & National Holiday 🇨🇳", loaded.title)
        assertEquals("🥮", loaded.emoji)
        assertTrue(loaded.eventDate.isLunar)
        assertTrue((loaded.eventDate as EventDate.Lunar).date.isLeapMonth)
        assertEquals(RepeatRule.Custom(2, RepeatUnit.WEEK), loaded.repeatRule)
        assertTrue(loaded.isPinned)
        assertEquals(EventBackground.Gradient(0xFF123456, 0xFF654321, 135f), loaded.background)
    }

    @Test
    fun `corrupted json syntax throws CorruptedStorageException`() {
        storageFile.writeText("{ malformed json :: [ }", Charsets.UTF_8)

        try {
            storage.load()
            fail("Expected CorruptedStorageException for malformed json")
        } catch (e: EventStorageException.CorruptedStorageException) {
            assertTrue(e.message?.contains("Invalid or corrupted JSON") == true)
        }
    }

    @Test
    fun `empty file (0 bytes) throws CorruptedStorageException`() {
        storageFile.writeText("", Charsets.UTF_8)

        try {
            storage.load()
            fail("Expected CorruptedStorageException for empty file")
        } catch (e: EventStorageException.CorruptedStorageException) {
            assertTrue(e.message?.contains("empty") == true)
        }
    }

    @Test
    fun `json object instead of array throws CorruptedStorageException`() {
        storageFile.writeText("""{"id": "not-an-array"}""", Charsets.UTF_8)

        try {
            storage.load()
            fail("Expected CorruptedStorageException when root is not an array")
        } catch (e: EventStorageException.CorruptedStorageException) {
            assertTrue(e.message?.contains("Invalid or corrupted JSON") == true)
        }
    }

    @Test
    fun `write failure does not destroy previous valid file`() {
        val originalEvent = CountdownEvent(
            id = "preserved-1",
            title = "Preserved Event",
            eventDate = EventDate.Solar(SolarDate(2026, 1, 1)),
        )
        storage.save(listOf(originalEvent))

        // Create a failing writer that simulates crash during write
        val failingWriter = object : AtomicFileWriter {
            override val file: File = storageFile
            override fun exists(): Boolean = storageFile.exists()
            override fun readFully(): ByteArray = storageFile.readBytes()
            override fun writeBytes(bytes: ByteArray) {
                throw IOException("Simulated disk write failure")
            }
            override fun delete() {}
        }

        val failingStorage = AtomicFileEventStorage(failingWriter)

        try {
            failingStorage.save(listOf(originalEvent.copy(title = "Should Fail")))
            fail("Expected StorageWriteException")
        } catch (e: EventStorageException.StorageWriteException) {
            assertTrue(e.message?.contains("Failed to persist") == true)
        }

        // Original file must remain intact and valid
        val preserved = storage.load()
        assertEquals(1, preserved.size)
        assertEquals("Preserved Event", preserved[0].title)
    }
}
