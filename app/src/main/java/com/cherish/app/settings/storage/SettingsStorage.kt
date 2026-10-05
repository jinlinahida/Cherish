package com.cherish.app.settings.storage

import com.cherish.app.settings.model.AppSettings

/**
 * Storage interface for loading and persisting [AppSettings].
 */
interface SettingsStorage {
    /**
     * Loads settings from disk. Returns default [AppSettings] if file does not exist.
     *
     * @throws SettingsStorageException.CorruptedStorageException if file exists but is corrupted.
     */
    fun load(): AppSettings

    /**
     * Persists [settings] to disk.
     *
     * @throws SettingsStorageException.StorageWriteException if write operation fails.
     */
    fun save(settings: AppSettings)
}
