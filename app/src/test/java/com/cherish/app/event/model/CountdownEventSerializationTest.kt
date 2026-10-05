package com.cherish.app.event.model

import com.cherish.app.date.model.LunarDate
import com.cherish.app.date.model.RepeatRule
import com.cherish.app.date.model.RepeatUnit
import com.cherish.app.date.model.SolarDate
import com.cherish.app.storage.CherishJson
import com.cherish.app.storage.EventJsonSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CountdownEventSerializationTest {

    @Test
    fun `case 1 - standard solar event round-trip serialization`() {
        val event = CountdownEvent(
            id = "solar-1",
            title = "New Year 2027",
            emoji = "🎆",
            category = EventCategory.HOLIDAY,
            eventDate = EventDate.Solar(SolarDate(2027, 1, 1)),
            repeatRule = RepeatRule.None,
            isPinned = false,
            background = EventBackground.Default,
        )

        val json = EventJsonSerializer.serializeEvent(event)
        val deserialized = EventJsonSerializer.deserializeEvent(json)

        assertEquals(event, deserialized)
    }

    @Test
    fun `case 2 - regular lunar event round-trip serialization`() {
        val event = CountdownEvent(
            id = "lunar-1",
            title = "Mid-Autumn Festival",
            emoji = "🥮",
            category = EventCategory.HOLIDAY,
            eventDate = EventDate.Lunar(LunarDate(2026, 8, 15, isLeapMonth = false)),
            repeatRule = RepeatRule.Yearly,
            isPinned = true,
            background = EventBackground.Color(0xFFFFA000),
        )

        val json = EventJsonSerializer.serializeEvent(event)
        val deserialized = EventJsonSerializer.deserializeEvent(json)

        assertEquals(event, deserialized)
    }

    @Test
    fun `case 3 - leap month lunar event round-trip serialization`() {
        val event = CountdownEvent(
            id = "lunar-leap-1",
            title = "Leap Month Birthday",
            emoji = "🎂",
            category = EventCategory.BIRTHDAY,
            eventDate = EventDate.Lunar(LunarDate(2028, 5, 12, isLeapMonth = true)),
            repeatRule = RepeatRule.Yearly,
            isPinned = false,
        )

        val json = EventJsonSerializer.serializeEvent(event)
        val deserialized = EventJsonSerializer.deserializeEvent(json)

        assertEquals(event, deserialized)
        assertTrue((deserialized.eventDate as EventDate.Lunar).date.isLeapMonth)
    }

    @Test
    fun `case 4 - daily recurrence rule serialization`() {
        val event = CountdownEvent(
            id = "daily-1",
            title = "Daily Workout",
            emoji = "🏃",
            category = EventCategory.LIFE,
            eventDate = EventDate.Solar(SolarDate(2026, 1, 1)),
            repeatRule = RepeatRule.Daily,
        )

        val json = EventJsonSerializer.serializeEvent(event)
        val deserialized = EventJsonSerializer.deserializeEvent(json)

        assertEquals(event, deserialized)
        assertEquals(RepeatRule.Daily, deserialized.repeatRule)
    }

    @Test
    fun `case 5 - monthly recurrence rule serialization`() {
        val event = CountdownEvent(
            id = "monthly-1",
            title = "Rent Payment",
            emoji = "🏠",
            category = EventCategory.LIFE,
            eventDate = EventDate.Solar(SolarDate(2026, 1, 15)),
            repeatRule = RepeatRule.Monthly,
        )

        val json = EventJsonSerializer.serializeEvent(event)
        val deserialized = EventJsonSerializer.deserializeEvent(json)

        assertEquals(event, deserialized)
        assertEquals(RepeatRule.Monthly, deserialized.repeatRule)
    }

    @Test
    fun `case 6 - yearly recurrence rule serialization`() {
        val event = CountdownEvent(
            id = "yearly-1",
            title = "Anniversary",
            emoji = "💍",
            category = EventCategory.ANNIVERSARY,
            eventDate = EventDate.Solar(SolarDate(2025, 6, 18)),
            repeatRule = RepeatRule.Yearly,
        )

        val json = EventJsonSerializer.serializeEvent(event)
        val deserialized = EventJsonSerializer.deserializeEvent(json)

        assertEquals(event, deserialized)
        assertEquals(RepeatRule.Yearly, deserialized.repeatRule)
    }

    @Test
    fun `case 7 - custom recurrence rule serialization with various units`() {
        val units = listOf(RepeatUnit.DAY, RepeatUnit.WEEK, RepeatUnit.MONTH, RepeatUnit.YEAR)
        for (unit in units) {
            val event = CountdownEvent(
                id = "custom-$unit",
                title = "Recurrence Test $unit",
                eventDate = EventDate.Solar(SolarDate(2026, 3, 10)),
                repeatRule = RepeatRule.Custom(interval = 4, unit = unit),
            )

            val json = EventJsonSerializer.serializeEvent(event)
            val deserialized = EventJsonSerializer.deserializeEvent(json)

            assertEquals(event, deserialized)
            val rule = deserialized.repeatRule as RepeatRule.Custom
            assertEquals(4, rule.interval)
            assertEquals(unit, rule.unit)
        }
    }

    @Test
    fun `case 8 - pinned state preservation`() {
        val pinned = CountdownEvent(
            id = "pinned-1",
            title = "Pinned Event",
            eventDate = EventDate.Solar(SolarDate(2026, 10, 1)),
            isPinned = true,
        )
        val unpinned = pinned.copy(id = "unpinned-1", isPinned = false)

        val pinnedJson = EventJsonSerializer.serializeEvent(pinned)
        val unpinnedJson = EventJsonSerializer.serializeEvent(unpinned)

        assertTrue(EventJsonSerializer.deserializeEvent(pinnedJson).isPinned)
        assertFalse(EventJsonSerializer.deserializeEvent(unpinnedJson).isPinned)
    }

    @Test
    fun `case 9 - unicode and emoji fidelity`() {
        val event = CountdownEvent(
            id = "emoji-1",
            title = "Special Day ✨ 庆祝日 💖",
            emoji = "🎉",
            eventDate = EventDate.Solar(SolarDate(2026, 7, 7)),
            notes = "Multi-line\nNotes with emojis: 🌟🚀🎈",
        )

        val json = EventJsonSerializer.serializeEvent(event)
        val deserialized = EventJsonSerializer.deserializeEvent(json)

        assertEquals(event, deserialized)
        assertEquals("Special Day ✨ 庆祝日 💖", deserialized.title)
        assertEquals("🎉", deserialized.emoji)
        assertEquals("Multi-line\nNotes with emojis: 🌟🚀🎈", deserialized.notes)
    }

    @Test
    fun `case 10 - all category enum variants`() {
        for (category in EventCategory.entries) {
            val event = CountdownEvent(
                id = "cat-${category.name}",
                title = "Category ${category.name}",
                category = category,
                eventDate = EventDate.Solar(SolarDate(2026, 5, 20)),
            )

            val json = EventJsonSerializer.serializeEvent(event)
            val deserialized = EventJsonSerializer.deserializeEvent(json)

            assertEquals(category, deserialized.category)
        }
    }

    @Test
    fun `case 11 - all background variants`() {
        val backgrounds = listOf(
            EventBackground.Default,
            EventBackground.Color(0xFF123456L),
            EventBackground.Gradient(0xFF112233L, 0xFF445566L, 45f),
            EventBackground.Pattern("stripes_subtle"),
        )

        for ((index, bg) in backgrounds.withIndex()) {
            val event = CountdownEvent(
                id = "bg-$index",
                title = "Background $index",
                eventDate = EventDate.Solar(SolarDate(2026, 4, 1)),
                background = bg,
            )

            val json = EventJsonSerializer.serializeEvent(event)
            val deserialized = EventJsonSerializer.deserializeEvent(json)

            assertEquals(bg, deserialized.background)
        }
    }

    @Test
    fun `case 12 - multi-event list serialization preserves exact ordering`() {
        val events = listOf(
            CountdownEvent(
                id = "event-3",
                title = "Third Event",
                eventDate = EventDate.Solar(SolarDate(2026, 12, 1)),
                isPinned = false,
            ),
            CountdownEvent(
                id = "event-1",
                title = "First Event",
                eventDate = EventDate.Lunar(LunarDate(2026, 1, 1)),
                isPinned = true,
            ),
            CountdownEvent(
                id = "event-2",
                title = "Second Event",
                eventDate = EventDate.Solar(SolarDate(2026, 6, 1)),
                isPinned = false,
            ),
        )

        val json = EventJsonSerializer.serialize(events)
        val deserialized = EventJsonSerializer.deserialize(json)

        assertEquals(3, deserialized.size)
        assertEquals("event-3", deserialized[0].id)
        assertEquals("event-1", deserialized[1].id)
        assertEquals("event-2", deserialized[2].id)
        assertEquals(events, deserialized)
    }

    @Test
    fun `forward compatibility - ignores unknown keys gracefully`() {
        val jsonWithExtraFields = """
            {
                "id": "compat-1",
                "title": "Future Event",
                "emoji": "🚀",
                "category": "WORK",
                "eventDate": {
                    "type": "Solar",
                    "date": {
                        "year": 2026,
                        "month": 11,
                        "day": 11
                    }
                },
                "repeatRule": {
                    "type": "None"
                },
                "isPinned": false,
                "background": {
                    "type": "Default"
                },
                "notes": "",
                "unknownField": "should_be_ignored",
                "extraNumericField": 42,
                "nestedObject": { "foo": "bar" }
            }
        """.trimIndent()

        val event = EventJsonSerializer.deserializeEvent(jsonWithExtraFields)
        assertEquals("compat-1", event.id)
        assertEquals("Future Event", event.title)
    }
}
