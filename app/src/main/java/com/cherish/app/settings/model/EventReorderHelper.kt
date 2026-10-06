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
     * Checks if moving an item from [fromIndex] to [toIndex] maintains all event properties,
     * specifically ensuring [CountdownEvent.isPinned] and IDs are preserved.
     */
    fun validateReorderIntegrity(
        original: List<CountdownEvent>,
        reordered: List<CountdownEvent>,
    ): Boolean {
        if (original.size != reordered.size) return false
        val originalIds = original.map { it.id }.toSet()
        val reorderedIds = reordered.map { it.id }.toSet()
        if (originalIds != reorderedIds) return false

        val originalMap = original.associateBy { it.id }
        return reordered.all { event ->
            val orig = originalMap[event.id] ?: return false
            orig.isPinned == event.isPinned &&
                orig.title == event.title &&
                orig.category == event.category &&
                orig.eventDate == event.eventDate
        }
    }
}
