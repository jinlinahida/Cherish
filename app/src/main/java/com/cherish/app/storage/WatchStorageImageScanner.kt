package com.cherish.app.storage

import android.content.Context
import android.os.Environment
import java.io.File

/**
 * Metadata for a local image file discovered on wearable flash storage.
 */
data class LocalWatchImage(
    val file: File,
    val displayName: String,
    val sizeBytes: Long,
)

/**
 * Local storage scanner for Wear OS devices.
 *
 * Allows users to import pictures transferred to the watch via ADB, Bluetooth,
 * or watch file transfer tools into Cherish without requiring a third-party photo picker.
 */
object WatchStorageImageScanner {

    private val SUPPORTED_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "bmp")

    /**
     * Scans standard public and private storage locations on the watch for candidate image files.
     */
    fun scanLocalImages(context: Context): List<LocalWatchImage> {
        val candidates = mutableListOf<File>()

        runCatching {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)?.let {
                candidates.add(it)
            }
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.let {
                candidates.add(it)
            }
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.let {
                candidates.add(it)
            }
            candidates.add(File(context.filesDir, "pictures"))
        }

        val results = mutableListOf<LocalWatchImage>()
        for (dir in candidates) {
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { file ->
                    if (file.isFile && file.extension.lowercase() in SUPPORTED_EXTENSIONS) {
                        results.add(
                            LocalWatchImage(
                                file = file,
                                displayName = file.name,
                                sizeBytes = file.length(),
                            ),
                        )
                    }
                }
            }
        }

        return results.distinctBy { it.file.absolutePath }.sortedByDescending { it.file.lastModified() }
    }

    /**
     * Imports a discovered [image] into sandboxed [imageStorage] for [eventId].
     */
    fun importImage(
        image: LocalWatchImage,
        eventId: String,
        imageStorage: EventImageStorage,
    ): String {
        return image.file.inputStream().use { inputStream ->
            imageStorage.saveImage(
                eventId = eventId,
                inputStream = inputStream,
                extension = image.file.extension.ifBlank { "jpg" },
            )
        }
    }
}
