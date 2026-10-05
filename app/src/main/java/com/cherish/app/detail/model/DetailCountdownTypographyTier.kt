package com.cherish.app.detail.model

import com.cherish.app.home.model.CountdownDisplayStatus
import kotlin.math.abs

/**
 * Typography sizing tier specifically tailored for the Event Detail Hero Card
 * on Wear OS circular smartwatch displays.
 *
 * Ensures countdown digits and special statuses scale gracefully across
 * 0..999, 1000..9999, 10000+, TODAY, and CountedPast states under all
 * Wear OS content scaling settings (SMALL 90%, STANDARD 100%, LARGE 110%).
 */
enum class DetailCountdownTypographyTier {
    /**
     * Hero primary display tier (displayMedium, ~34-36sp).
     * Used for 1..3 digit numbers (0..999).
     */
    HERO_LARGE,

    /**
     * Hero medium display tier (displaySmall, ~28-30sp).
     * Used for 4-digit numbers (1000..9999) and "今天" (TODAY).
     */
    HERO_MEDIUM,

    /**
     * Hero compact display tier (titleLarge, ~20-22sp).
     * Used for 5+ digit numbers (>= 10000 or strings like "10000+").
     */
    HERO_COMPACT,
}

/**
 * Pure calculation resolving the appropriate [DetailCountdownTypographyTier] based on
 * numeric days count and today status.
 *
 * @param daysCount magnitude of days (0 or positive, negative handled via absolute value).
 * @param isToday true if the event occurs today.
 */
fun resolveDetailCountdownTypographyTier(
    daysCount: Long,
    isToday: Boolean,
): DetailCountdownTypographyTier {
    if (isToday) return DetailCountdownTypographyTier.HERO_MEDIUM
    val magnitude = abs(daysCount)
    return when {
        magnitude < 1000L -> DetailCountdownTypographyTier.HERO_LARGE
        magnitude < 10000L -> DetailCountdownTypographyTier.HERO_MEDIUM
        else -> DetailCountdownTypographyTier.HERO_COMPACT
    }
}

/**
 * Overload of [resolveDetailCountdownTypographyTier] accepting [Int] for convenient model mapping.
 */
fun resolveDetailCountdownTypographyTier(
    daysCount: Int,
    isToday: Boolean,
): DetailCountdownTypographyTier = resolveDetailCountdownTypographyTier(daysCount.toLong(), isToday)

/**
 * Resolves [DetailCountdownTypographyTier] from presentation text and status,
 * handling formatted strings like "10000+", "今天", etc.
 *
 * @param heroText the displayed number or status string.
 * @param status the display status.
 */
fun resolveDetailCountdownTypographyTier(
    heroText: String,
    status: CountdownDisplayStatus,
): DetailCountdownTypographyTier {
    if (status == CountdownDisplayStatus.TODAY || heroText == "今天" || heroText.equals("today", ignoreCase = true)) {
        return DetailCountdownTypographyTier.HERO_MEDIUM
    }
    val digitsOnly = heroText.filter { it.isDigit() }
    val count = digitsOnly.toLongOrNull()
    return if (count != null) {
        when {
            count < 1000L -> DetailCountdownTypographyTier.HERO_LARGE
            count < 10000L -> DetailCountdownTypographyTier.HERO_MEDIUM
            else -> DetailCountdownTypographyTier.HERO_COMPACT
        }
    } else {
        when {
            heroText.length <= 3 -> DetailCountdownTypographyTier.HERO_LARGE
            heroText.length <= 4 -> DetailCountdownTypographyTier.HERO_MEDIUM
            else -> DetailCountdownTypographyTier.HERO_COMPACT
        }
    }
}
