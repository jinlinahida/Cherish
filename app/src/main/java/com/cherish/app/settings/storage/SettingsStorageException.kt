package com.cherish.app.settings.storage

/**
 * Exceptions thrown during settings persistence.
 */
sealed class SettingsStorageException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    /**
     * Thrown when the settings storage file exists but contains corrupted, truncated, or empty data.
     */
    class CorruptedStorageException(
        message: String,
        cause: Throwable? = null,
    ) : SettingsStorageException(message, cause)

    /**
     * Thrown when persisting settings to disk fails.
     */
    class StorageWriteException(
        message: String,
        cause: Throwable? = null,
    ) : SettingsStorageException(message, cause)
}
