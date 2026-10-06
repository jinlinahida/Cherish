package com.cherish.app.event.model

import com.cherish.app.storage.CherishJson
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventBackgroundSerializationTest {

    @Test
    fun `default background round-trip serialization`() {
        val bg: EventBackground = EventBackground.Default
        val json = CherishJson.encodeToString(bg)
        val deserialized = CherishJson.decodeFromString<EventBackground>(json)

        assertEquals(bg, deserialized)
        assertTrue(json.contains("\"type\": \"Default\""))
    }

    @Test
    fun `color background round-trip serialization`() {
        val bg: EventBackground = EventBackground.Color(0xFF1E88E5L)
        val json = CherishJson.encodeToString(bg)
        val deserialized = CherishJson.decodeFromString<EventBackground>(json)

        assertEquals(bg, deserialized)
        assertTrue(json.contains("\"type\": \"Color\""))
    }

    @Test
    fun `gradient background round-trip serialization`() {
        val bg: EventBackground = EventBackground.Gradient(
            startColor = 0xFF009688L,
            endColor = 0xFF1E88E5L,
            angle = 135f,
        )
        val json = CherishJson.encodeToString(bg)
        val deserialized = CherishJson.decodeFromString<EventBackground>(json)

        assertEquals(bg, deserialized)
        assertTrue(json.contains("\"type\": \"Gradient\""))
    }

    @Test
    fun `pattern background round-trip serialization`() {
        val bg: EventBackground = EventBackground.Pattern("stars_nebula")
        val json = CherishJson.encodeToString(bg)
        val deserialized = CherishJson.decodeFromString<EventBackground>(json)

        assertEquals(bg, deserialized)
        assertTrue(json.contains("\"type\": \"Pattern\""))
    }

    @Test
    fun `image background round-trip serialization`() {
        val bg: EventBackground = EventBackground.Image(
            path = "backgrounds/bg_event_123.jpg",
            dimAlpha = 0.45f,
        )
        val json = CherishJson.encodeToString(bg)
        val deserialized = CherishJson.decodeFromString<EventBackground>(json)

        assertEquals(bg, deserialized)
        assertTrue(json.contains("\"type\": \"Image\""))
        val img = deserialized as EventBackground.Image
        assertEquals("backgrounds/bg_event_123.jpg", img.path)
        assertEquals(0.45f, img.dimAlpha, 0.001f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `image background rejects blank path`() {
        EventBackground.Image(path = "   ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `image background rejects invalid dimAlpha above 1`() {
        EventBackground.Image(path = "bg.jpg", dimAlpha = 1.2f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `image background rejects negative dimAlpha`() {
        EventBackground.Image(path = "bg.jpg", dimAlpha = -0.1f)
    }
}
