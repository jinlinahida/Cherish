package com.cherish.app.home

import com.cherish.app.date.model.SolarDate
import com.cherish.app.event.model.CountdownEvent
import com.cherish.app.event.model.EventDate
import com.cherish.app.home.mapper.HomeEventMapper
import com.cherish.app.home.model.CountdownDisplayStatus
import com.cherish.app.home.preview.DemoEvents
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying Home Screen presentation logic:
 * - Demo events mapping & presentation sanity
 * - Pinned presentation ordering preservation
 * - 2-column Grid chunking behavior (odd, even, single, empty)
 * - Empty state edge cases
 */
class HomeScreenPresentationTest {

    private val referenceDate = SolarDate(2026, 10, 5)

    @Test
    fun `demo events mapping produces valid and complete ui models`() {
        val samples = DemoEvents.samples(referenceDate)
        assertEquals(9, samples.size)

        val uiModels = samples.map { HomeEventMapper.toUiModel(it, referenceDate) }
        assertEquals(9, uiModels.size)

        // Pinned status verification
        assertTrue(uiModels[0].isPinned) // demo-midautumn
        assertTrue(uiModels[1].isPinned) // demo-birthday
        assertFalse(uiModels[2].isPinned) // demo-today
        assertFalse(uiModels[8].isPinned) // demo-past

        // Status mapping correctness
        assertEquals(CountdownDisplayStatus.COUNTDOWN, uiModels[0].status)
        assertEquals(CountdownDisplayStatus.COUNTDOWN, uiModels[1].status)
        assertEquals(CountdownDisplayStatus.TODAY, uiModels[2].status)
        assertEquals(CountdownDisplayStatus.COUNTDOWN, uiModels[3].status)
        assertEquals(CountdownDisplayStatus.PAST, uiModels[8].status)

        // Non-empty presentation labels
        uiModels.forEach { model ->
            assertTrue(model.event.title.isNotBlank())
            assertTrue(model.targetDateFormatted.isNotBlank())
            assertTrue(model.daysCount >= 0)
            assertTrue(model.unitLabel.isNotBlank())
        }
    }

    @Test
    fun `pinned ordering partition maintains relative ordering within groups`() {
        val events = listOf(
            CountdownEvent(id = "u1", title = "Unpinned 1", eventDate = EventDate.Solar(SolarDate(2026, 11, 1)), isPinned = false),
            CountdownEvent(id = "p1", title = "Pinned 1", eventDate = EventDate.Solar(SolarDate(2026, 11, 2)), isPinned = true),
            CountdownEvent(id = "u2", title = "Unpinned 2", eventDate = EventDate.Solar(SolarDate(2026, 11, 3)), isPinned = false),
            CountdownEvent(id = "p2", title = "Pinned 2", eventDate = EventDate.Solar(SolarDate(2026, 11, 4)), isPinned = true),
            CountdownEvent(id = "u3", title = "Unpinned 3", eventDate = EventDate.Solar(SolarDate(2026, 11, 5)), isPinned = false),
            CountdownEvent(id = "p3", title = "Pinned 3", eventDate = EventDate.Solar(SolarDate(2026, 11, 6)), isPinned = true),
        )

        val (pinned, unpinned) = events.partition { it.isPinned }
        val displayedEvents = pinned + unpinned

        val displayedIds = displayedEvents.map { it.id }
        assertEquals(listOf("p1", "p2", "p3", "u1", "u2", "u3"), displayedIds)
    }

    @Test
    fun `pinned ordering when all events are pinned maintains identical sequence`() {
        val events = (1..5).map { index ->
            CountdownEvent(
                id = "p$index",
                title = "Pinned $index",
                eventDate = EventDate.Solar(SolarDate(2026, 12, index)),
                isPinned = true,
            )
        }

        val (pinned, unpinned) = events.partition { it.isPinned }
        val displayedEvents = pinned + unpinned

        assertEquals(events.map { it.id }, displayedEvents.map { it.id })
    }

    @Test
    fun `pinned ordering when no events are pinned maintains identical sequence`() {
        val events = (1..5).map { index ->
            CountdownEvent(
                id = "u$index",
                title = "Unpinned $index",
                eventDate = EventDate.Solar(SolarDate(2026, 12, index)),
                isPinned = false,
            )
        }

        val (pinned, unpinned) = events.partition { it.isPinned }
        val displayedEvents = pinned + unpinned

        assertEquals(events.map { it.id }, displayedEvents.map { it.id })
    }

    @Test
    fun `grid chunking splits items into pairs correctly for even item counts`() {
        val items = (1..4).map { "item-$it" }
        val rows = items.chunked(2)

        assertEquals(2, rows.size)
        assertEquals(listOf("item-1", "item-2"), rows[0])
        assertEquals(listOf("item-3", "item-4"), rows[1])
    }

    @Test
    fun `grid chunking splits items into pairs and single trailing item for odd item counts`() {
        val items = (1..5).map { "item-$it" }
        val rows = items.chunked(2)

        assertEquals(3, rows.size)
        assertEquals(listOf("item-1", "item-2"), rows[0])
        assertEquals(listOf("item-3", "item-4"), rows[1])
        assertEquals(listOf("item-5"), rows[2])
    }

    @Test
    fun `grid chunking handles single item correctly`() {
        val items = listOf("item-1")
        val rows = items.chunked(2)

        assertEquals(1, rows.size)
        assertEquals(listOf("item-1"), rows[0])
    }

    @Test
    fun `grid chunking handles empty list correctly`() {
        val items = emptyList<String>()
        val rows = items.chunked(2)

        assertTrue(rows.isEmpty())
    }
}
