package com.cherish.app.detail

import com.cherish.app.detail.model.DetailCountdownTypographyTier
import com.cherish.app.detail.model.resolveDetailCountdownTypographyTier
import com.cherish.app.home.model.CountdownDisplayStatus
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests verifying typography sizing resolution for the Detail Hero Countdown card:
 * - 0, 1, 9, 99, 999 (HERO_LARGE, displayMedium)
 * - 1000, 9999 (HERO_MEDIUM, displaySmall)
 * - 10000, 10000+, 50000 (HERO_COMPACT, titleLarge)
 * - TODAY ("今天", CountdownDisplayStatus.TODAY)
 * - CountedPast (short past, large past, extreme past)
 */
class DetailCountdownTypographyTest {

    @Test
    fun `small numbers resolve to HERO_LARGE tier`() {
        val testCases = listOf(0, 1, 9, 99, 999)
        for (count in testCases) {
            val tier = resolveDetailCountdownTypographyTier(daysCount = count, isToday = false)
            assertEquals("Day count $count should resolve to HERO_LARGE", DetailCountdownTypographyTier.HERO_LARGE, tier)

            val stringTier = resolveDetailCountdownTypographyTier(heroText = "$count", status = CountdownDisplayStatus.COUNTDOWN)
            assertEquals("Hero text '$count' should resolve to HERO_LARGE", DetailCountdownTypographyTier.HERO_LARGE, stringTier)
        }
    }

    @Test
    fun `four digit numbers resolve to HERO_MEDIUM tier`() {
        val testCases = listOf(1000, 1234, 9999)
        for (count in testCases) {
            val tier = resolveDetailCountdownTypographyTier(daysCount = count, isToday = false)
            assertEquals("Day count $count should resolve to HERO_MEDIUM", DetailCountdownTypographyTier.HERO_MEDIUM, tier)

            val stringTier = resolveDetailCountdownTypographyTier(heroText = "$count", status = CountdownDisplayStatus.COUNTDOWN)
            assertEquals("Hero text '$count' should resolve to HERO_MEDIUM", DetailCountdownTypographyTier.HERO_MEDIUM, stringTier)
        }
    }

    @Test
    fun `five digit and larger numbers resolve to HERO_COMPACT tier`() {
        val testCases = listOf(10000, 12345, 99999)
        for (count in testCases) {
            val tier = resolveDetailCountdownTypographyTier(daysCount = count, isToday = false)
            assertEquals("Day count $count should resolve to HERO_COMPACT", DetailCountdownTypographyTier.HERO_COMPACT, tier)

            val stringTier = resolveDetailCountdownTypographyTier(heroText = "$count", status = CountdownDisplayStatus.COUNTDOWN)
            assertEquals("Hero text '$count' should resolve to HERO_COMPACT", DetailCountdownTypographyTier.HERO_COMPACT, stringTier)
        }
    }

    @Test
    fun `formatted string 10000 plus resolves to HERO_COMPACT tier`() {
        val tier = resolveDetailCountdownTypographyTier(heroText = "10000+", status = CountdownDisplayStatus.COUNTDOWN)
        assertEquals(DetailCountdownTypographyTier.HERO_COMPACT, tier)
    }

    @Test
    fun `today status resolves to HERO_MEDIUM tier`() {
        // Via daysCount + isToday flag
        val tierFromFlag = resolveDetailCountdownTypographyTier(daysCount = 0, isToday = true)
        assertEquals(DetailCountdownTypographyTier.HERO_MEDIUM, tierFromFlag)

        // Via "今天" hero text and TODAY status
        val tierFromStatus = resolveDetailCountdownTypographyTier(heroText = "今天", status = CountdownDisplayStatus.TODAY)
        assertEquals(DetailCountdownTypographyTier.HERO_MEDIUM, tierFromStatus)

        // Case insensitivity check for english "TODAY"
        val tierFromEnglish = resolveDetailCountdownTypographyTier(heroText = "TODAY", status = CountdownDisplayStatus.TODAY)
        assertEquals(DetailCountdownTypographyTier.HERO_MEDIUM, tierFromEnglish)
    }

    @Test
    fun `counted past resolves based on absolute days count`() {
        // Small past (e.g. 5 days ago, -5) -> HERO_LARGE
        val smallPast = resolveDetailCountdownTypographyTier(daysCount = -5L, isToday = false)
        assertEquals(DetailCountdownTypographyTier.HERO_LARGE, smallPast)

        val smallPastText = resolveDetailCountdownTypographyTier(heroText = "5", status = CountdownDisplayStatus.PAST)
        assertEquals(DetailCountdownTypographyTier.HERO_LARGE, smallPastText)

        // 4-digit past (e.g. 1500 days ago, -1500) -> HERO_MEDIUM
        val mediumPast = resolveDetailCountdownTypographyTier(daysCount = -1500L, isToday = false)
        assertEquals(DetailCountdownTypographyTier.HERO_MEDIUM, mediumPast)

        val mediumPastText = resolveDetailCountdownTypographyTier(heroText = "1500", status = CountdownDisplayStatus.PAST)
        assertEquals(DetailCountdownTypographyTier.HERO_MEDIUM, mediumPastText)

        // 5-digit past (e.g. 20000 days ago) -> HERO_COMPACT
        val largePast = resolveDetailCountdownTypographyTier(daysCount = -20000L, isToday = false)
        assertEquals(DetailCountdownTypographyTier.HERO_COMPACT, largePast)

        val largePastText = resolveDetailCountdownTypographyTier(heroText = "20000", status = CountdownDisplayStatus.PAST)
        assertEquals(DetailCountdownTypographyTier.HERO_COMPACT, largePastText)
    }
}
