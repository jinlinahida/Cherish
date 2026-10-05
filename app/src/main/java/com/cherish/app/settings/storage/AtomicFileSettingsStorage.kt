package com.cherish.app.settings.storage

import com.cherish.app.settings.model.AppSettings
import com.cherish.app.storage.AndroidAtomicFileWriter
import com.cherish.app.storage.AtomicFileWriter
import com.cherish.app.storage.CherishJson
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString

/**
 * High-reliability settings storage backed by an atomic file and Kotlinx Serialization.
 *
 * Guarantees:
 * 1. Atomic updates: writes to temp buffer first, then atomically replaces destination file.
 * 2. Non-existent file returns default [AppSettings].
 * 3. Empty or malformed files throw [SettingsStorageException.CorruptedStorageException] to prevent silent corruption.
 * 4. Forward compatibility: unknown fields are safely ignored by [CherishJson].
 */
class AtomicFileSettingsStorage(
    private val fileWriter: AtomicFileWriter,
) : SettingsStorage {

    /**
     * Standard constructor for Android runtime backed by [AndroidAtomicFileWriter].
     */
    constructor(file: File) : this(AndroidAtomicFileWriter(file))

    override fun load(): AppSettings {
        if (!fileWriter.exists()) {
            return AppSettings()
        }

        val bytes = try {
            fileWriter.readFully()
        } catch (e: FileNotFoundException) {
            return AppSettings()
        } catch (e: IOException) {
            throw SettingsStorageException.CorruptedStorageException(
                "I/O error reading settings file: ${fileWriter.file.path}",
                e,
            )
        }

        val jsonString = bytes.toString(Charsets.UTF_8).trim()
        if (jsonString.isEmpty()) {
            throw SettingsStorageException.CorruptedStorageException(
                "Settings file exists at ${fileWriter.file.path} but is empty",
            )
        }

        return try {
            CherishJson.decodeFromString(AppSettings.serializer(), jsonString)
        } catch (e: SerializationException) {
            throw SettingsStorageException.CorruptedStorageException(
                "Invalid or corrupted JSON in ${fileWriter.file.path}: ${e.message}",
                e,
            )
        } catch (e: IllegalArgumentException) {
            throw SettingsStorageException.CorruptedStorageException(
                "Constraint violation deserializing ${fileWriter.file.path}: ${e.message}",
                e,
            )
        }
    }

    override fun save(settings: AppSettings) {
        try {
            val jsonString = CherishJson.encodeToString(AppSettings.serializer(), settings)
            val bytes = jsonString.toByteArray(Charsets.UTF_8)
            fileWriter.writeBytes(bytes)
        } catch (e: Throwable) {
            throw SettingsStorageException.StorageWriteException(
                "Failed to persist settings to ${fileWriter.file.path}: ${e.message}",
                e,
            )
        }
    }
}
