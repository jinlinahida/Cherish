package com.cherish.app.settings.repository

import com.cherish.app.settings.model.AppSettings
import com.cherish.app.settings.storage.SettingsStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Thread-safe default implementation of [SettingsRepository].
 *
 * Guarantees:
 * 1. Synchronous atomic persistence: mutations are persisted to [SettingsStorage] before updating memory.
 * 2. In-memory state remains clean if persistence throws an exception.
 * 3. Thread-safe operations via synchronization on an internal lock.
 */
class DefaultSettingsRepository(
    private val storage: SettingsStorage,
) : SettingsRepository {

    private val lock = Any()
    private val _settings = MutableStateFlow(AppSettings())
    override val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    init {
        reload()
    }

    override fun reload() = synchronized(lock) {
        val loaded = storage.load()
        _settings.value = loaded
    }

    override fun getSettings(): AppSettings = synchronized(lock) {
        _settings.value
    }

    override fun update(transform: (AppSettings) -> AppSettings) = synchronized(lock) {
        val current = _settings.value
        val updated = transform(current)
        storage.save(updated)
        _settings.value = updated
    }
}
