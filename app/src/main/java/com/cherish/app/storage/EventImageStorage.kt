package com.cherish.app.storage

import java.io.File
import java.io.InputStream

/**
 * Storage manager for custom event background images on Wear OS.
 *
 * Enforces local sandboxing, clean file naming, relative path resolution,
 * and orphaned image cleanup to conserve limited wearable flash storage.
 */
interface EventImageStorage {

    /**
     * Stores an image file from a byte array for the specified [eventId].
     * Returns the relative path suitable for storage in [com.cherish.app.event.model.EventBackground.Image].
     */
    fun saveImage(eventId: String, bytes: ByteArray, extension: String = "jpg"): String

    /**
     * Stores an image file from an [InputStream] for the specified [eventId].
     * Returns the relative path suitable for storage in [com.cherish.app.event.model.EventBackground.Image].
     */
    fun saveImage(eventId: String, inputStream: InputStream, extension: String = "jpg"): String

    /**
     * Resolves a stored image path (relative or absolute) to a readable [File],
     * or returns null if not found.
     */
    fun getImageFile(relativePathOrPath: String): File?

    /**
     * Deletes a stored image.
     */
    fun deleteImage(relativePathOrPath: String): Boolean

    /**
     * Scans the storage directory and removes any files not included in [activeReferencedPaths].
     * Returns the count of deleted files.
     */
    fun cleanupOrphanedImages(activeReferencedPaths: Set<String>): Int
}

/**
 * Standard file-backed implementation of [EventImageStorage].
 */
class FileEventImageStorage(
    private val baseDirectory: File,
) : EventImageStorage {

    init {
        if (!baseDirectory.exists()) {
            baseDirectory.mkdirs()
        }
    }

    override fun saveImage(eventId: String, bytes: ByteArray, extension: String): String {
        val sanitizedExt = extension.trimStart('.').ifBlank { "jpg" }
        val filename = "bg_${eventId}_${System.currentTimeMillis()}.$sanitizedExt"
        val targetFile = File(baseDirectory, filename)
        val tempFile = File(baseDirectory, "$filename.tmp")
        tempFile.writeBytes(bytes)
        if (targetFile.exists()) {
            targetFile.delete()
        }
        if (!tempFile.renameTo(targetFile)) {
            targetFile.writeBytes(tempFile.readBytes())
            tempFile.delete()
        }
        return filename
    }

    override fun saveImage(eventId: String, inputStream: InputStream, extension: String): String {
        return saveImage(eventId, inputStream.readBytes(), extension)
    }

    override fun getImageFile(relativePathOrPath: String): File? {
        if (relativePathOrPath.isBlank()) return null
        val directFile = File(relativePathOrPath)
        if (directFile.isAbsolute && directFile.exists()) {
            return directFile
        }
        val fileInDir = File(baseDirectory, relativePathOrPath)
        return if (fileInDir.exists()) fileInDir else null
    }

    override fun deleteImage(relativePathOrPath: String): Boolean {
        val file = getImageFile(relativePathOrPath) ?: return false
        return file.delete()
    }

    override fun cleanupOrphanedImages(activeReferencedPaths: Set<String>): Int {
        val normalizedReferences = activeReferencedPaths.map { ref ->
            File(ref).name
        }.toSet()

        var deletedCount = 0
        val files = baseDirectory.listFiles() ?: return 0
        for (file in files) {
            if (file.isFile && !file.name.endsWith(".tmp") && file.name !in normalizedReferences) {
                if (file.delete()) {
                    deletedCount++
                }
            } else if (file.isFile && file.name.endsWith(".tmp")) {
                file.delete()
            }
        }
        return deletedCount
    }
}
