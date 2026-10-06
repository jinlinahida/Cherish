package com.cherish.app.event.repository

import com.cherish.app.event.model.CountdownEvent
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface managing countdown events.
 *
 * Responsibilities:
 * - Maintains in-memory state of events in user-defined order
 * - Orchestrates atomic persistence via EventStorage
 * - Exposes reactive StateFlow for UI / consumers
 * - Supports add, update, delete, reorder operations
 */
interface EventRepository {

    /**
     * Observable reactive stream of events, emitting the current list
     * in user-defined order whenever persistence succeeds.
     */
    val events: StateFlow<List<CountdownEvent>>

    /**
     * Returns a snapshot of all events in user-defined order.
     */
    fun getAll(): List<CountdownEvent>

    /**
     * Looks up an event by its unique ID.
     */
    fun getById(id: String): CountdownEvent?

    /**
     * Adds a new event to the end of the list.
     * Persists to storage before updating in-memory state.
     *
     * @throws IllegalArgumentException if an event with the same ID already exists.
     * @throws com.cherish.app.storage.EventStorageException if persistence fails.
     */
    fun add(event: CountdownEvent)

    /**
     * Updates an existing event.
     * Persists to storage before updating in-memory state.
     *
     * @throws NoSuchElementException if the event is not found.
     * @throws com.cherish.app.storage.EventStorageException if persistence fails.
     */
    fun update(event: CountdownEvent)

    /**
     * Deletes an event by ID.
     * Persists to storage before updating in-memory state.
     *
     * @return true if an event was deleted, false if no event matched the ID.
     * @throws com.cherish.app.storage.EventStorageException if persistence fails.
     */
    fun delete(id: String): Boolean

    /**
     * Reorders an event from [fromIndex] to [toIndex].
     * Persists to storage before updating in-memory state.
     *
     * @throws IndexOutOfBoundsException if either index is out of bounds.
     * @throws com.cherish.app.storage.EventStorageException if persistence fails.
     */
    fun reorder(fromIndex: Int, toIndex: Int)

    /**
     * Updates the full order of events in the repository.
     * Persists to storage before updating in-memory state.
     *
     * @throws IllegalArgumentException if the provided list does not contain the exact same set of events.
     * @throws com.cherish.app.storage.EventStorageException if persistence fails.
     */
    fun reorderAll(orderedEvents: List<CountdownEvent>)

    /**
     * Reloads events from the underlying storage.
     */
    fun reload()
}
