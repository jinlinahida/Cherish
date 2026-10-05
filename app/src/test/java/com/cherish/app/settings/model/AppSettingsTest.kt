package com.cherish.app.settings.model

import com.cherish.app.home.model.HomeViewMode
import com.cherish.app.storage.CherishJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {

    @Test
    fun `default values match requirements`() {
        val settings = AppSettings()
        assertEquals(HomeViewMode.LIST, settings.homeViewMode)
        assertTrue(settings.hapticsEnabled)
        assertEquals(AppContentScale.STANDARD, settings.contentScale)
    }

    @Test
    fun `serialization round trip preserves all fields`() {
        val custom = AppSettings(
            homeViewMode = HomeViewMode.GRID,
            hapticsEnabled = false,
            contentScale = AppContentScale.LARGE,
        )

        val json = CherishJson.encodeToString(AppSettings.serializer(), custom)
        val deserialized = CherishJson.decodeFromString(AppSettings.serializer(), json)

        assertEquals(custom, deserialized)
        assertEquals(HomeViewMode.GRID, deserialized.homeViewMode)
        assertFalse(deserialized.hapticsEnabled)
        assertEquals(AppContentScale.LARGE, deserialized.contentScale)
    }

    @Test
    fun `deserializing empty object populates default values`() {
        val emptyJson = "{}"
        val settings = CherishJson.decodeFromString(AppSettings.serializer(), emptyJson)

        assertEquals(HomeViewMode.LIST, settings.homeViewMode)
        assertTrue(settings.hapticsEnabled)
        assertEquals(AppContentScale.STANDARD, settings.contentScale)
    }

    @Test
    fun `forward compatibility - ignores unknown keys gracefully`() {
        val jsonWithUnknownKeys = """
            {
                "homeViewMode": "GRID",
                "hapticsEnabled": true,
                "contentScale": "SMALL",
                "futureThemeMode": "DARK_OLED",
                "futureKpopIntegration": true,
                "extraNumericField": 123
            }
        """.trimIndent()

        val settings = CherishJson.decodeFromString(AppSettings.serializer(), jsonWithUnknownKeys)
        assertEquals(HomeViewMode.GRID, settings.homeViewMode)
        assertTrue(settings.hapticsEnabled)
        assertEquals(AppContentScale.SMALL, settings.contentScale)
    }
}
