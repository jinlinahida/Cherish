package com.cherish.app.event.repository

import com.cherish.app.event.model.CountdownEvent
import com.cherish.app.settings.model.EventReorderHelper
import com.cherish.app.storage.EventStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Default implementation of [EventRepository].
 *
 * Guarantees:
 * 1. Synchronous atomic persistence: mutations are persisted to [EventStorage] before updating memory.
 * 2. In-memory state remains clean if persistence throws an exception.
 * 3. Thread-safe operations via synchronization on an internal lock.
 * 4. User ordering preservation: the order of elements in [events] directly reflects custom ordering.
 */
class DefaultEventRepository(
    private val storage: EventStorage,
) : EventRepository {

    private val lock = Any()
    private val _events = MutableStateFlow<List<CountdownEvent>>(emptyList())
    override val events: StateFlow<List<CountdownEvent>> = _events.asStateFlow()

    init {
        reload()
    }

    override fun reload() = synchronized(lock) {
        val loaded = storage.load()
        _events.value = loaded
    }

    override fun getAll(): List<CountdownEvent> = synchronized(lock) {
        _events.value
    }

    override fun getById(id: String): CountdownEvent? = synchronized(lock) {
        _events.value.firstOrNull { it.id == id }
    }

    override fun add(event: CountdownEvent) = synchronized(lock) {
        require(_events.value.none { it.id == event.id }) {
            "Event with ID '${event.id}' already exists"
        }
        val newList = _events.value + event
        storage.save(newList)
        _events.value = newList
    }

    override fun update(event: CountdownEvent) = synchronized(lock) {
        val current = _events.value
        val index = current.indexOfFirst { it.id == event.id }
        if (index == -1) {
            throw NoSuchElementException("Event with ID '${event.id}' not found")
        }
        val newList = current.toMutableList().apply {
            this[index] = event
        }.toList()
        storage.save(newList)
        _events.value = newList
    }

    override fun delete(id: String): Boolean = synchronized(lock) {
        val current = _events.value
        val index = current.indexOfFirst { it.id == id }
        if (index == -1) return false
        val newList = current.toMutableList().apply {
            removeAt(index)
        }.toList()
        storage.save(newList)
        _events.value = newList
        return true
    }

    override fun reorder(fromIndex: Int, toIndex: Int) = synchronized(lock) {
        val current = _events.value
        if (fromIndex !in current.indices) {
            throw IndexOutOfBoundsException("fromIndex $fromIndex out of bounds (size: ${current.size})")
        }
        if (toIndex !in current.indices) {
            throw IndexOutOfBoundsException("toIndex $toIndex out of bounds (size: ${current.size})")
        }
        if (fromIndex == toIndex) return

        val mutable = current.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        val newList = mutable.toList()

        storage.save(newList)
        _events.value = newList
    }

    override fun reorderAll(orderedEvents: List<CountdownEvent>) = synchronized(lock) {
        val current = _events.value
        require(EventReorderHelper.validateReorderIntegrity(current, orderedEvents, allowPinChange = true)) {
            "reorderAll must contain the exact same set of events as current repository state"
        }
        storage.save(orderedEvents)
        _events.value = orderedEvents
    }
}
