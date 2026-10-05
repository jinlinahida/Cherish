package com.cherish.app.storage

import com.cherish.app.event.model.CountdownEvent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Standard Json configuration for Cherish persistence.
 *
 * Configured with:
 * - prettyPrint: for human-readable JSON files and clean diffs
 * - ignoreUnknownKeys: for forward compatibility across schema evolution
 * - isLenient: for tolerant parsing
 * - encodeDefaults: so all fields are explicitly preserved
 * - coerceInputValues: for graceful fallback on unknown enum values
 */
val CherishJson: Json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    coerceInputValues = true
}

/**
 * Pure Kotlin serializer for countdown events.
 */
object EventJsonSerializer {

    fun serialize(events: List<CountdownEvent>): String =
        CherishJson.encodeToString(events)

    fun deserialize(json: String): List<CountdownEvent> =
        CherishJson.decodeFromString(json)

    fun serializeEvent(event: CountdownEvent): String =
        CherishJson.encodeToString(event)

    fun deserializeEvent(json: String): CountdownEvent =
        CherishJson.decodeFromString(json)
}
