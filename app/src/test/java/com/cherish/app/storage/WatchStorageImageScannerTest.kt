package com.cherish.app.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class WatchStorageImageScannerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var storageDir: File
    private lateinit var imageStorage: FileEventImageStorage

    @Before
    fun setUp() {
        storageDir = tempFolder.newFolder("scanner_storage")
        imageStorage = FileEventImageStorage(storageDir)
    }

    @Test
    fun `importImage copies local image file into managed EventImageStorage`() {
        val sampleLocalFile = tempFolder.newFile("sample_wallpaper.jpg")
        sampleLocalFile.writeBytes(byteArrayOf(10, 20, 30, 40, 50))

        val localImage = LocalWatchImage(
            file = sampleLocalFile,
            displayName = sampleLocalFile.name,
            sizeBytes = sampleLocalFile.length(),
        )

        val relativePath = WatchStorageImageScanner.importImage(
            image = localImage,
            eventId = "evt-import",
            imageStorage = imageStorage,
        )

        assertTrue(relativePath.startsWith("bg_evt-import_"))
        assertTrue(relativePath.endsWith(".jpg"))

        val storedFile = imageStorage.getImageFile(relativePath)
        assertNotNull(storedFile)
        assertTrue(storedFile!!.exists())
        assertEquals(5, storedFile.length())
        assertEquals(sampleLocalFile.readBytes().toList(), storedFile.readBytes().toList())
    }
}
