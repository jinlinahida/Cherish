package com.cherish.app.home.model

import kotlin.math.abs

/**
 * Typography sizing tier for countdown numbers and status on Wear OS screens.
 *
 * Designed to ensure large 4+ digit numbers (e.g. 1000, 9999) and "TODAY" labels
 * scale down gracefully on small round smartwatch displays without text truncation,
 * clipping, or multi-line wrapping under various content scale settings (SMALL 90%,
 * STANDARD 100%, LARGE 110%).
 */
enum class CountdownTypographyTier {
    /**
     * Standard List view primary tier (displayMedium, ~40sp).
     * Used for 1..3 digit numbers (0..999) in single-column List mode.
     */
    DISPLAY_MEDIUM,

    /**
     * Standard List view compact tier (displaySmall, ~34sp).
     * Used for 4+ digit numbers (>= 1000) and "TODAY" in single-column List mode.
     */
    DISPLAY_SMALL,

    /**
     * Grid view primary tier (titleLarge, ~20sp).
     * Used for 1..3 digit numbers (0..999) in double-column Grid mode.
     */
    TITLE_LARGE,

    /**
     * Grid view compact tier (titleMedium, ~16sp).
     * Used for 4+ digit numbers (>= 1000) and "TODAY" in double-column Grid mode.
     */
    TITLE_MEDIUM,
}

/**
 * Pure calculation resolving the appropriate [CountdownTypographyTier] based on presentation mode,
 * status, and countdown days magnitude.
 *
 * @param isCompact true for 2-column Grid mode, false for 1-column List mode.
 * @param isToday true if the event occurs today.
 * @param daysCount magnitude of days (0 or positive).
 */
fun resolveCountdownTypographyTier(
    isCompact: Boolean,
    isToday: Boolean,
    daysCount: Long,
): CountdownTypographyTier {
    val count = abs(daysCount)
    return if (isCompact) {
        if (isToday || count >= 1000L) {
            CountdownTypographyTier.TITLE_MEDIUM
        } else {
            CountdownTypographyTier.TITLE_LARGE
        }
    } else {
        if (isToday || count >= 1000L) {
            CountdownTypographyTier.DISPLAY_SMALL
        } else {
            CountdownTypographyTier.DISPLAY_MEDIUM
        }
    }
}

/**
 * Overload of [resolveCountdownTypographyTier] accepting [Int] for convenient model mapping.
 */
fun resolveCountdownTypographyTier(
    isCompact: Boolean,
    isToday: Boolean,
    daysCount: Int,
): CountdownTypographyTier = resolveCountdownTypographyTier(isCompact, isToday, daysCount.toLong())
