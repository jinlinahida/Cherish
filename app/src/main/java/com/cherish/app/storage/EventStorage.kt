package com.cherish.app.storage

import com.cherish.app.event.model.CountdownEvent

/**
 * Storage contract for persisting and retrieving countdown events.
 *
 * Implementations are responsible strictly for serialization and I/O persistence.
 * Business rules (such as ordering semantics, pin status handling, recalculation)
 * belong in the repository layer.
 */
interface EventStorage {
    /**
     * Loads the stored events from disk.
     *
     * @return List of persisted events, or an empty list if the storage file does not exist yet.
     * @throws EventStorageException.CorruptedStorageException if the storage file exists but is corrupted.
     */
    fun load(): List<CountdownEvent>

    /**
     * Persists the given list of events atomically to disk.
     *
     * @throws EventStorageException.StorageWriteException if the write operation fails.
     */
    fun save(events: List<CountdownEvent>)
}
