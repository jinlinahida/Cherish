package com.cherish.app.settings.model

import com.cherish.app.event.model.CountdownEvent

/**
 * Pure Kotlin helper functions for list reordering and drag calculations.
 *
 * Fully decoupled from Android UI/Framework classes to ensure 100% JVM unit testability.
 */
object EventReorderHelper {

    /**
     * Reorders an item within a list from [fromIndex] to [toIndex].
     * Returns a new list with the item placed at [toIndex].
     *
     * If [fromIndex] == [toIndex] or either index is out of bounds, returns the original list.
     */
    fun <T> reorder(list: List<T>, fromIndex: Int, toIndex: Int): List<T> {
        if (list.size <= 1) return list
        if (fromIndex !in list.indices || toIndex !in list.indices) return list
        if (fromIndex == toIndex) return list

        val mutable = list.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        return mutable.toList()
    }

    /**
     * Calculates the target index when an item is dragged vertically by [dragOffsetY] pixels,
     * given an effective [itemHeightPx] (including item spacing).
     *
     * Uses a 50% threshold hysteresis to ensure natural, responsive snapping without jitter.
     */
    fun calculateTargetIndex(
        startIndex: Int,
        dragOffsetY: Float,
        itemHeightPx: Float,
        totalItems: Int,
    ): Int {
        if (totalItems <= 1 || itemHeightPx <= 0f) return startIndex
        val offsetSteps = Math.round(dragOffsetY / itemHeightPx)
        val target = startIndex + offsetSteps
        return target.coerceIn(0, totalItems - 1)
    }

    /**
     * Reorders an item with Pin consistency guarantee.
     *
     * In Cherish, pinned events represent top-priority events on the Home screen.
     * When an event is reordered across the pinned boundary:
     * - Moving an unpinned event above any pinned event promotes it to pinned (`isPinned = true`).
     * - Moving a pinned event below any unpinned event demotes it to unpinned (`isPinned = false`).
     * - Moving within the pinned group or within the unpinned group preserves the event's pin state.
     *
     * This guarantees that the order displayed in EventOrderScreen and the order displayed
     * on HomeScreen are always 100% synchronized and consistent.
     */
    fun reorderWithPinConsistency(
        list: List<CountdownEvent>,
        fromIndex: Int,
        toIndex: Int,
    ): List<CountdownEvent> {
        if (list.size <= 1) return list
        if (fromIndex !in list.indices || toIndex !in list.indices) return list
        if (fromIndex == toIndex) return list

        val moved = reorder(list, fromIndex, toIndex)
        val targetItem = moved[toIndex]

        val hasPinnedAfter = (toIndex + 1 until moved.size).any { moved[it].isPinned }
        val hasUnpinnedBefore = (0 until toIndex).any { !moved[it].isPinned }

        val newPinnedStatus = when {
            hasPinnedAfter -> true
            hasUnpinnedBefore -> false
            else -> targetItem.isPinned
        }

        if (newPinnedStatus == targetItem.isPinned) {
            return moved
        }

        val updated = moved.toMutableList()
        updated[toIndex] = targetItem.copy(isPinned = newPinnedStatus)
        return updated.toList()
    }

    /**
     * Checks if moving items from [original] to [reordered] maintains all event properties,
     * ensuring IDs, dates, titles, and categories are strictly preserved.
     *
     * If [allowPinChange] is true, pin state transitions caused by crossing the pinned
     * boundary are permitted.
     */
    fun validateReorderIntegrity(
        original: List<CountdownEvent>,
        reordered: List<CountdownEvent>,
        allowPinChange: Boolean = false,
    ): Boolean {
        if (original.size != reordered.size) return false
        val originalIds = original.map { it.id }.toSet()
        val reorderedIds = reordered.map { it.id }.toSet()
        if (originalIds != reorderedIds) return false

        val originalMap = original.associateBy { it.id }
        return reordered.all { event ->
            val orig = originalMap[event.id] ?: return false
            orig.title == event.title &&
                orig.category == event.category &&
                orig.eventDate == event.eventDate &&
                (allowPinChange || orig.isPinned == event.isPinned)
        }
    }
}
