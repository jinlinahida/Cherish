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
        assertEquals(1, CherishRoute.Settings.depth)
        assertEquals("home", CherishRoute.Settings.backKey)
    }

    @Test
    fun `EventOrder and About routes are children of settings`() {
        assertEquals("event_order", CherishRoute.EventOrder.routeKey)
        assertEquals(2, CherishRoute.EventOrder.depth)
        assertEquals("settings", CherishRoute.EventOrder.backKey)

        assertEquals("about", CherishRoute.About.routeKey)
        assertEquals(2, CherishRoute.About.depth)
        assertEquals("settings", CherishRoute.About.backKey)
    }
}
