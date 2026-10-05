package com.cherish.app.storage

/**
 * Base exception for storage operations in Cherish.
 */
sealed class EventStorageException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    /**
     * Thrown when the storage file exists on disk but contains invalid or corrupted data
     * (e.g. empty file, truncated JSON, non-array root, malformed syntax).
     *
     * This is strictly distinguished from a non-existent file to prevent silent data loss.
     */
    class CorruptedStorageException(
        message: String,
        cause: Throwable? = null,
    ) : EventStorageException(message, cause)

    /**
     * Thrown when writing data to disk fails (e.g. I/O error, permissions, disk full).
     */
    class StorageWriteException(
        message: String,
        cause: Throwable? = null,
    ) : EventStorageException(message, cause)
}
