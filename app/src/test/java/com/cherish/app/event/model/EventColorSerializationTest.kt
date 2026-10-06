package com.cherish.app.event.model

import com.cherish.app.storage.CherishJson
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventColorSerializationTest {

    @Test
    fun `default color round-trip serialization`() {
        val color: EventColor = EventColor.Default
        val json = CherishJson.encodeToString(color)
        val deserialized = CherishJson.decodeFromString<EventColor>(json)

        assertEquals(color, deserialized)
        assertTrue(json.contains("\"type\": \"Default\""))
    }

    @Test
    fun `single color round-trip serialization`() {
        val color: EventColor = EventColor.Single(0xFFE91E63L)
        val json = CherishJson.encodeToString(color)
        val deserialized = CherishJson.decodeFromString<EventColor>(json)

        assertEquals(color, deserialized)
        assertTrue(json.contains("\"type\": \"Single\""))
        assertTrue(json.contains("4293467747"))
    }

    @Test
    fun `gradient color round-trip serialization`() {
        val color: EventColor = EventColor.Gradient(
            startColor = 0xFFFFA000L,
            endColor = 0xFFE91E63L,
            angle = 90f,
        )
        val json = CherishJson.encodeToString(color)
        val deserialized = CherishJson.decodeFromString<EventColor>(json)

        assertEquals(color, deserialized)
        assertTrue(json.contains("\"type\": \"Gradient\""))
        val grad = deserialized as EventColor.Gradient
        assertEquals(0xFFFFA000L, grad.startColor)
        assertEquals(0xFFE91E63L, grad.endColor)
        assertEquals(90f, grad.angle, 0.001f)
    }

    @Test
    fun `companion presets match expected values`() {
        assertEquals(0xFFE91E63L, EventColor.Rose.argb)
        assertEquals(0xFFFFA000L, EventColor.Amber.argb)
        assertEquals(0xFF009688L, EventColor.Emerald.argb)
        assertEquals(0xFF1E88E5L, EventColor.Ocean.argb)
        assertEquals(0xFF9C27B0L, EventColor.Violet.argb)
        assertEquals(0xFFFF5722L, EventColor.Coral.argb)

        assertEquals(0xFFFFA000L, EventColor.Sunset.startColor)
        assertEquals(0xFFE91E63L, EventColor.Sunset.endColor)
        assertEquals(0xFF009688L, EventColor.Aurora.startColor)
        assertEquals(0xFF1E88E5L, EventColor.Aurora.endColor)
    }
}
