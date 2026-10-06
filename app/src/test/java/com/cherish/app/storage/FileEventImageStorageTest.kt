package com.cherish.app.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File

class FileEventImageStorageTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var storageDir: File
    private lateinit var imageStorage: FileEventImageStorage

    @Before
    fun setUp() {
        storageDir = tempFolder.newFolder("backgrounds")
        imageStorage = FileEventImageStorage(storageDir)
    }

    @Test
    fun `saveImage with byte array creates file in base directory`() {
        val dummyBytes = byteArrayOf(1, 2, 3, 4, 5)
        val relativePath = imageStorage.saveImage("event-1", dummyBytes, "png")

        assertTrue(relativePath.startsWith("bg_event-1_"))
        assertTrue(relativePath.endsWith(".png"))

        val file = imageStorage.getImageFile(relativePath)
        assertNotNull(file)
        assertTrue(file!!.exists())
        assertEquals(5, file.length())
    }

    @Test
    fun `saveImage with input stream writes identical content`() {
        val dummyText = "sample image payload"
        val inputStream = ByteArrayInputStream(dummyText.toByteArray())
        val relativePath = imageStorage.saveImage("event-2", inputStream, "jpg")

        val file = imageStorage.getImageFile(relativePath)
        assertNotNull(file)
        assertEquals(dummyText, file!!.readText())
    }

    @Test
    fun `getImageFile resolves both relative and absolute paths`() {
        val dummyBytes = byteArrayOf(10, 20, 30)
        val relativePath = imageStorage.saveImage("event-3", dummyBytes, "jpg")

        // Relative lookup
        val relFile = imageStorage.getImageFile(relativePath)
        assertNotNull(relFile)

        // Absolute lookup
        val absFile = imageStorage.getImageFile(relFile!!.absolutePath)
        assertNotNull(absFile)
        assertEquals(relFile.absolutePath, absFile!!.absolutePath)

        // Non-existent lookup
        assertNull(imageStorage.getImageFile("non_existent.jpg"))
        assertNull(imageStorage.getImageFile(""))
    }

    @Test
    fun `deleteImage removes file and returns true`() {
        val dummyBytes = byteArrayOf(99)
        val relativePath = imageStorage.saveImage("event-4", dummyBytes, "jpg")

        assertTrue(imageStorage.deleteImage(relativePath))
        assertNull(imageStorage.getImageFile(relativePath))
        assertFalse(imageStorage.deleteImage(relativePath))
    }

    @Test
    fun `cleanupOrphanedImages removes unreferenced files and temp files`() {
        val bytes = byteArrayOf(42)
        val activePath1 = imageStorage.saveImage("active-1", bytes, "jpg")
        val activePath2 = imageStorage.saveImage("active-2", bytes, "jpg")
        val orphanPath1 = imageStorage.saveImage("orphan-1", bytes, "jpg")
        val orphanPath2 = imageStorage.saveImage("orphan-2", bytes, "jpg")

        // Create a stray .tmp file
        val strayTmp = File(storageDir, "bg_test.tmp")
        strayTmp.writeBytes(bytes)

        val activeSet = setOf(activePath1, activePath2)
        val deletedCount = imageStorage.cleanupOrphanedImages(activeSet)

        assertEquals(2, deletedCount) // 2 orphaned images deleted
        assertNotNull(imageStorage.getImageFile(activePath1))
        assertNotNull(imageStorage.getImageFile(activePath2))
        assertNull(imageStorage.getImageFile(orphanPath1))
        assertNull(imageStorage.getImageFile(orphanPath2))
        assertFalse(strayTmp.exists()) // temp file was also cleaned up
    }
}
