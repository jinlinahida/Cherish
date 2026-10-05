package com.cherish.app.storage

import com.cherish.app.event.model.CountdownEvent
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import kotlinx.serialization.SerializationException

/**
 * High-reliability event storage backed by an atomic file and Kotlinx Serialization.
 *
 * Guarantees:
 * 1. Atomic updates: writes are written to a temporary buffer/file first, then atomically swapped.
 * 2. Corruption detection: malformed, truncated, or empty files throw [EventStorageException.CorruptedStorageException]
 *    rather than silently destroying user data.
 * 3. Fresh state: non-existent files return an empty list.
 * 4. UTF-8 encoded, pretty-printed JSON format.
 */
class AtomicFileEventStorage(
    private val fileWriter: AtomicFileWriter,
    private val serializer: EventJsonSerializer = EventJsonSerializer,
) : EventStorage {

    /**
     * Standard constructor for Android runtime, backed by [AndroidAtomicFileWriter].
     */
    constructor(file: File) : this(AndroidAtomicFileWriter(file))

    override fun load(): List<CountdownEvent> {
        if (!fileWriter.exists()) {
            return emptyList()
        }

        val bytes = try {
            fileWriter.readFully()
        } catch (e: FileNotFoundException) {
            return emptyList()
        } catch (e: IOException) {
            throw EventStorageException.CorruptedStorageException(
                "I/O error reading storage file: ${fileWriter.file.path}",
                e,
            )
        }

        val jsonString = bytes.toString(Charsets.UTF_8).trim()
        if (jsonString.isEmpty()) {
            throw EventStorageException.CorruptedStorageException(
                "Storage file exists at ${fileWriter.file.path} but is empty",
            )
        }

        return try {
            serializer.deserialize(jsonString)
        } catch (e: SerializationException) {
            throw EventStorageException.CorruptedStorageException(
                "Invalid or corrupted JSON in ${fileWriter.file.path}: ${e.message}",
                e,
            )
        } catch (e: IllegalArgumentException) {
            throw EventStorageException.CorruptedStorageException(
                "Data constraint violation while deserializing ${fileWriter.file.path}: ${e.message}",
                e,
            )
        }
    }

    override fun save(events: List<CountdownEvent>) {
        try {
            val jsonString = serializer.serialize(events)
            val bytes = jsonString.toByteArray(Charsets.UTF_8)
            fileWriter.writeBytes(bytes)
        } catch (e: Throwable) {
            throw EventStorageException.StorageWriteException(
                "Failed to persist ${events.size} events to ${fileWriter.file.path}: ${e.message}",
                e,
            )
        }
    }
}
