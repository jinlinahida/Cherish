package com.cherish.app.event.model

import com.cherish.app.date.model.SolarDate
import com.cherish.app.storage.EventJsonSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CountdownEventCompatibilityTest {

    @Test
    fun `legacy json without color field defaults to EventColor Default and resolves legacy background color`() {
        // This simulates a persistent JSON file written by previous Cherish versions
        val legacyJson = """
            {
                "id": "legacy-birthday",
                "title": "妈妈生日",
                "emoji": "🎂",
                "category": "BIRTHDAY",
                "eventDate": {
                    "type": "Solar",
                    "date": {
                        "year": 2026,
                        "month": 10,
                        "day": 15
                    }
                },
                "repeatRule": {
                    "type": "Yearly"
                },
                "isPinned": true,
                "background": {
                    "type": "Color",
                    "argb": 4293467747
                },
                "notes": "买礼物"
            }
        """.trimIndent()

        val event = EventJsonSerializer.deserializeEvent(legacyJson)

        assertEquals("legacy-birthday", event.id)
        assertEquals("妈妈生日", event.title)
        // Explicit color is Default because it was absent in legacy payload
        assertEquals(EventColor.Default, event.color)
        // Background was preserved
        assertEquals(EventBackground.Color(4293467747L), event.background)
        // resolvedColor() provides seamless fallback to legacy card color
        assertEquals(EventColor.Single(4293467747L), event.resolvedColor())
    }

    @Test
    fun `legacy json with gradient background resolves to gradient EventColor`() {
        val legacyJson = """
            {
                "id": "legacy-gradient",
                "title": "日落时刻",
                "emoji": "🌅",
                "category": "LIFE",
                "eventDate": {
                    "type": "Solar",
                    "date": {
                        "year": 2026,
                        "month": 10,
                        "day": 5
                    }
                },
                "repeatRule": {
                    "type": "None"
                },
                "isPinned": false,
                "background": {
                    "type": "Gradient",
                    "startColor": 4294615040,
                    "endColor": 4293467747,
                    "angle": 45.0
                },
                "notes": ""
            }
        """.trimIndent()

        val event = EventJsonSerializer.deserializeEvent(legacyJson)

        assertEquals(EventColor.Default, event.color)
        assertEquals(
            EventColor.Gradient(4294615040L, 4293467747L, 45.0f),
            event.resolvedColor(),
        )
    }

    @Test
    fun `modern event with distinct color and background image serializes and deserializes cleanly`() {
        val event = CountdownEvent(
            id = "custom-bg-1",
            title = "旅行倒计时",
            emoji = "✈️",
            category = EventCategory.LIFE,
            eventDate = EventDate.Solar(SolarDate(2027, 4, 1)),
            color = EventColor.Ocean,
            background = EventBackground.Image(
                path = "backgrounds/trip_kyoto.jpg",
                dimAlpha = 0.4f,
            ),
            notes = "去京都看樱花",
        )

        val json = EventJsonSerializer.serializeEvent(event)
        val deserialized = EventJsonSerializer.deserializeEvent(json)

        assertEquals(event, deserialized)
        assertEquals(EventColor.Ocean, deserialized.color)
        assertEquals(EventColor.Ocean, deserialized.resolvedColor())
        assertTrue(deserialized.background is EventBackground.Image)

        val imageBg = deserialized.background as EventBackground.Image
        assertEquals("backgrounds/trip_kyoto.jpg", imageBg.path)
        assertEquals(0.4f, imageBg.dimAlpha, 0.001f)
    }
}
