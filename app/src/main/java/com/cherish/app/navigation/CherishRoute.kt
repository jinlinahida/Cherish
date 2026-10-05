package com.cherish.app.navigation

import io.github.jinlinahida.shirokowear.navigation.ShirokoWearRoute

/**
 * Type-safe navigation routes for Cherish.
 *
 * Implements [ShirokoWearRoute] to leverage the spatial transition engine
 * in ShirokoWearUI (forward slides, backward pop parallax, and bezel easing).
 */
sealed class CherishRoute : ShirokoWearRoute {

    /**
     * Home screen displaying the list / grid of countdown events.
     */
    data object Home : CherishRoute() {
        override val routeKey: String = "home"
        override val depth: Int = 0
        override val backKey: String? = null
    }

    /**
     * Detail screen for viewing a specific countdown event.
     *
     * @property eventId The persistent identifier of the event.
     */
    data class Detail(val eventId: String) : CherishRoute() {
        override val routeKey: String = "detail_$eventId"
        override val depth: Int = 1
        override val backKey: String = "home"
    }

    /**
     * Editor screen for creating or modifying a countdown event.
     *
     * @property eventId The event identifier to edit, or null to create a new event.
     */
    data class Editor(val eventId: String? = null) : CherishRoute() {
        override val routeKey: String = "editor_${eventId ?: "new"}"
        override val depth: Int = if (eventId != null) 2 else 1
        override val backKey: String = if (eventId != null) "detail_$eventId" else "home"
    }

    /**
     * Settings screen managing display preferences, haptics, and about.
     */
    data object Settings : CherishRoute() {
        override val routeKey: String = "settings"
        override val depth: Int = 1
        override val backKey: String = "home"
    }

    /**
     * Event ordering screen to manually adjust repository order.
     */
    data object EventOrder : CherishRoute() {
        override val routeKey: String = "event_order"
        override val depth: Int = 2
        override val backKey: String = "settings"
    }
}
