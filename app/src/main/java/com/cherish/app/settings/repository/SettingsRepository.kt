package com.cherish.app.settings.repository

import com.cherish.app.settings.model.AppSettings
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface managing global user preferences.
 */
interface SettingsRepository {

    /**
     * Observable reactive stream of current [AppSettings].
     */
    val settings: StateFlow<AppSettings>

    /**
     * Returns a snapshot of current [AppSettings].
     */
    fun getSettings(): AppSettings

    /**
     * Updates settings via [transform].
     * Persists to storage before updating in-memory state.
     *
     * @throws com.cherish.app.settings.storage.SettingsStorageException if persistence fails.
     */
    fun update(transform: (AppSettings) -> AppSettings)

    /**
     * Reloads settings from underlying storage.
     */
    fun reload()
}
