package com.cherish.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CherishRouteTest {

    @Test
    fun `Home route has correct routeKey, depth, and backKey`() {
        assertEquals("home", CherishRoute.Home.routeKey)
        assertEquals(0, CherishRoute.Home.depth)
        assertNull(CherishRoute.Home.backKey)
    }

    @Test
    fun `Detail route reflects eventId and points back to home`() {
        val route = CherishRoute.Detail("evt-123")
        assertEquals("detail_evt-123", route.routeKey)
        assertEquals(1, route.depth)
        assertEquals("home", route.backKey)
    }

    @Test
    fun `Editor route handles creation and editing depths and backKeys`() {
        val createRoute = CherishRoute.Editor(null)
        assertEquals("editor_new", createRoute.routeKey)
        assertEquals(1, createRoute.depth)
        assertEquals("home", createRoute.backKey)

        val editRoute = CherishRoute.Editor("evt-456")
        assertEquals("editor_evt-456", editRoute.routeKey)
        assertEquals(2, editRoute.depth)
        assertEquals("detail_evt-456", editRoute.backKey)
    }

    @Test
    fun `Settings route has correct hierarchy and points back to home`() {
        assertEquals("settings", CherishRoute.Settings.routeKey)
        assertEquals(0, CherishRoute.Settings.depth)
        assertEquals("home", CherishRoute.Settings.backKey)
    }

    @Test
    fun `EventOrder and About routes are children of settings`() {
        assertEquals("event_order", CherishRoute.EventOrder.routeKey)
        assertEquals(1, CherishRoute.EventOrder.depth)
        assertEquals("settings", CherishRoute.EventOrder.backKey)

        assertEquals("about", CherishRoute.About.routeKey)
        assertEquals(1, CherishRoute.About.depth)
        assertEquals("settings", CherishRoute.About.backKey)
    }

    @Test
    fun `resolveBackRoute returns null for Home meaning system back exits application`() {
        assertNull(resolveBackRoute(CherishRoute.Home))
    }

    @Test
    fun `resolveBackRoute pops Detail and Settings back to Home`() {
        assertEquals(CherishRoute.Home, resolveBackRoute(CherishRoute.Detail("ev-1")))
        assertEquals(CherishRoute.Home, resolveBackRoute(CherishRoute.Settings))
    }

    @Test
    fun `resolveBackRoute pops Editor creation to Home and Editor edit to Detail`() {
        // Creation mode (null eventId) -> Home
        assertEquals(CherishRoute.Home, resolveBackRoute(CherishRoute.Editor(null)))

        // Edit mode (specific eventId) -> Detail(eventId)
        assertEquals(CherishRoute.Detail("ev-99"), resolveBackRoute(CherishRoute.Editor("ev-99")))
    }

    @Test
    fun `resolveBackRoute pops EventOrder and About back to Settings`() {
        assertEquals(CherishRoute.Settings, resolveBackRoute(CherishRoute.EventOrder))
        assertEquals(CherishRoute.Settings, resolveBackRoute(CherishRoute.About))
    }

    @Test
    fun `Home and Settings are parallel top-level spaces with depth 0`() {
        assertEquals(0, CherishRoute.Home.depth)
        assertEquals(0, CherishRoute.Settings.depth)
        assertEquals(CherishRoute.Home.depth, CherishRoute.Settings.depth)
    }

    @Test
    fun `child routes under Home and Settings branch appropriately`() {
        // Under Home:
        assertEquals("home", CherishRoute.Detail("1").backKey)
        assertEquals("home", CherishRoute.Editor(null).backKey)
        // Under Settings:
        assertEquals("settings", CherishRoute.EventOrder.backKey)
        assertEquals("settings", CherishRoute.About.backKey)
    }
}
