package com.cherish.app.settings.storage

import com.cherish.app.home.model.HomeViewMode
import com.cherish.app.settings.model.AppContentScale
import com.cherish.app.settings.model.AppSettings
import com.cherish.app.storage.AtomicFileWriter
import com.cherish.app.storage.JvmAtomicFileWriter
import java.io.File
import java.io.IOException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class AtomicFileSettingsStorageTest {

    private lateinit var tempDir: File
    private lateinit var settingsFile: File
    private lateinit var storage: AtomicFileSettingsStorage

    @Before
    fun setUp() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "cherish_settings_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        settingsFile = File(tempDir, "settings.json")
        storage = AtomicFileSettingsStorage(JvmAtomicFileWriter(settingsFile))
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `reading non-existent file returns default settings without error`() {
        assertFalse(settingsFile.exists())
        val settings = storage.load()
        assertEquals(AppSettings(), settings)
        assertEquals(HomeViewMode.LIST, settings.homeViewMode)
        assertTrue(settings.hapticsEnabled)
        assertEquals(AppContentScale.STANDARD, settings.contentScale)
    }

    @Test
    fun `saving and reloading custom settings preserves values`() {
        val custom = AppSettings(
            homeViewMode = HomeViewMode.GRID,
            hapticsEnabled = false,
            contentScale = AppContentScale.LARGE,
        )

        storage.save(custom)
        assertTrue(settingsFile.exists())

        val reloaded = storage.load()
        assertEquals(custom, reloaded)
        assertEquals(HomeViewMode.GRID, reloaded.homeViewMode)
        assertFalse(reloaded.hapticsEnabled)
        assertEquals(AppContentScale.LARGE, reloaded.contentScale)
    }

    @Test
    fun `corrupted json syntax throws CorruptedStorageException`() {
        settingsFile.writeText("{ malformed json ::: }", Charsets.UTF_8)

        try {
            storage.load()
            fail("Expected CorruptedStorageException for malformed json")
        } catch (e: SettingsStorageException.CorruptedStorageException) {
            assertTrue(e.message?.contains("Invalid or corrupted JSON") == true)
        }
    }

    @Test
    fun `empty file (0 bytes) throws CorruptedStorageException`() {
        settingsFile.writeText("", Charsets.UTF_8)

        try {
            storage.load()
            fail("Expected CorruptedStorageException for empty file")
        } catch (e: SettingsStorageException.CorruptedStorageException) {
            assertTrue(e.message?.contains("empty") == true)
        }
    }

    @Test
    fun `write failure does not destroy previous valid settings file`() {
        val original = AppSettings(
            homeViewMode = HomeViewMode.GRID,
            hapticsEnabled = true,
            contentScale = AppContentScale.SMALL,
        )
        storage.save(original)

        val failingWriter = object : AtomicFileWriter {
            override val file: File = settingsFile
            override fun exists(): Boolean = settingsFile.exists()
            override fun readFully(): ByteArray = settingsFile.readBytes()
            override fun writeBytes(bytes: ByteArray) {
                throw IOException("Simulated disk error during save")
            }
            override fun delete() {}
        }

        val failingStorage = AtomicFileSettingsStorage(failingWriter)

        try {
            failingStorage.save(original.copy(hapticsEnabled = false))
            fail("Expected StorageWriteException")
        } catch (e: SettingsStorageException.StorageWriteException) {
            assertTrue(e.message?.contains("Failed to persist settings") == true)
        }

        val preserved = storage.load()
        assertEquals(original, preserved)
    }
}
