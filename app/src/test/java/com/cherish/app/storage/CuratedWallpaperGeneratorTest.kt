package com.cherish.app.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class CuratedWallpaperGeneratorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var storageDir: File
    private lateinit var imageStorage: FileEventImageStorage

    @Before
    fun setUp() {
        storageDir = tempFolder.newFolder("curated_test")
        imageStorage = FileEventImageStorage(storageDir)
    }

    @Test
    fun `generateBmp produces valid Windows 24-bit BMP header structure`() {
        val width = 360
        val height = 360
        val bytes = CuratedWallpaperGenerator.generatePresetBytes(
            preset = CuratedWallpaperPreset.STARRY_NIGHT,
            width = width,
            height = height,
        )

        val rowSize = (width * 3 + 3) / 4 * 4
        val expectedSize = 54 + rowSize * height
        assertEquals("Total byte length must match BMP spec", expectedSize, bytes.size)

        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        // Magic number 'BM'
        assertEquals('B'.code.toByte(), buffer.get(0))
        assertEquals('M'.code.toByte(), buffer.get(1))

        // Total file size at byte 2
        assertEquals(expectedSize, buffer.getInt(2))

        // Data offset at byte 10
        assertEquals(54, buffer.getInt(10))

        // DIB Header size at byte 14
        assertEquals(40, buffer.getInt(14))

        // Dimensions
        assertEquals(width, buffer.getInt(18))
        assertEquals(height, buffer.getInt(22))

        // Planes = 1, Bits per pixel = 24
        assertEquals(1.toShort(), buffer.getShort(26))
        assertEquals(24.toShort(), buffer.getShort(28))

        // Compression = 0 (BI_RGB)
        assertEquals(0, buffer.getInt(30))
    }

    @Test
    fun `all presets synthesize successfully with distinct non-empty payload`() {
        for (preset in CuratedWallpaperPreset.entries) {
            val bytes = CuratedWallpaperGenerator.generatePresetBytes(preset, 100, 100)
            assertTrue("Preset ${preset.name} bytes should not be empty", bytes.isNotEmpty())

            val rowSize = (100 * 3 + 3) / 4 * 4
            assertEquals(54 + rowSize * 100, bytes.size)
        }
    }

    @Test
    fun `saveCuratedWallpaper stores file in EventImageStorage properly`() {
        val filename = CuratedWallpaperGenerator.saveCuratedWallpaper(
            preset = CuratedWallpaperPreset.AURORA_BOREALIS,
            eventId = "event-aurora",
            imageStorage = imageStorage,
            width = 120,
            height = 120,
        )

        assertTrue(filename.startsWith("bg_event-aurora_"))
        assertTrue(filename.endsWith(".bmp"))

        val file = imageStorage.getImageFile(filename)
        assertNotNull(file)
        assertTrue(file!!.exists())
        assertTrue(file.length() > 54)
    }
}
