package com.cherish.app.sync

import java.util.Base64
import java.time.LocalDate
import com.cherish.app.date.calc.CountdownCalculator
import com.cherish.app.date.calc.EventCountdownStatus
import com.cherish.app.date.calc.RecurrenceCalculator
import com.cherish.app.date.lunar.LunarCalendar
import com.cherish.app.date.model.LunarDate
import com.cherish.app.date.model.RepeatRule
import com.cherish.app.date.model.RepeatUnit
import com.cherish.app.date.model.SolarDate
import com.cherish.app.event.model.CountdownEvent
import com.cherish.app.event.model.EventBackground
import com.cherish.app.event.model.EventCategory
import com.cherish.app.event.model.EventColor
import com.cherish.app.event.model.EventDate
import com.cherish.app.event.repository.EventRepository
import com.cherish.app.storage.EventImageStorage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Embedded local HTTP server for Cherish on Wear OS.
 *
 * Hosts the zero-dependency mobile web editor and REST API for real-time
 * peer-to-peer event synchronisation and visual photo editing.
 */
class CherishHttpServer(
    val session: SyncSession,
    private val repository: EventRepository,
    private val imageStorage: EventImageStorage?,
    private val calendar: LunarCalendar,
    private val onEventSynced: () -> Unit = {},
) {
    private val recurrenceCalculator = RecurrenceCalculator(calendar)
    private var serverSocket: ServerSocket? = null
    private val isRunning = AtomicBoolean(false)
    private val executor = Executors.newCachedThreadPool()
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Starts listening on [session.port] (or falls back to an ephemeral port if occupied).
     * Returns the actual bound port.
     */
    fun start(): Int {
        if (isRunning.get()) return serverSocket?.localPort ?: session.port

        val socket = try {
            ServerSocket(session.port)
        } catch (e: Exception) {
            // Ephemeral port fallback
            ServerSocket(0)
        }
        serverSocket = socket
        isRunning.set(true)

        executor.execute {
            while (isRunning.get() && !socket.isClosed) {
                try {
                    val client = socket.accept()
                    executor.execute { handleClient(client) }
                } catch (e: Exception) {
                    if (!isRunning.get()) break
                }
            }
        }

        return socket.localPort
    }

    /**
     * Gracefully stops the HTTP server and terminates active client threads.
     */
    fun stop() {
        if (!isRunning.compareAndSet(true, false)) return
        session.disconnect()
        runCatching { serverSocket?.close() }
        serverSocket = null
        executor.shutdownNow()
    }

    private fun handleClient(client: Socket) {
        try {
            client.soTimeout = 10000 // 10s socket read timeout
            val input = client.getInputStream()
            val output = client.getOutputStream()

            val requestLine = readLineFromStream(input) ?: return client.close()
            val parts = requestLine.split(" ")
            if (parts.size < 2) return client.close()

            val method = parts[0].uppercase()
            val fullPath = parts[1]
            val path = fullPath.substringBefore("?")
            val queryString = fullPath.substringAfter("?", "")
            val queryParams = parseQueryParams(queryString)

            // Read HTTP Headers
            val headers = mutableMapOf<String, String>()
            var line: String? = readLineFromStream(input)
            var contentLength = 0
            while (!line.isNullOrBlank()) {
                val colon = line.indexOf(':')
                if (colon > 0) {
                    val key = line.substring(0, colon).trim().lowercase()
                    val value = line.substring(colon + 1).trim()
                    headers[key] = value
                    if (key == "content-length") {
                        contentLength = value.toIntOrNull() ?: 0
                    }
                }
                line = readLineFromStream(input)
            }

            // CORS Pre-flight check
            if (method == "OPTIONS") {
                sendResponse(output, 204, "No Content", "text/plain", ByteArray(0))
                return client.close()
            }

            // Read request body if present
            val bodyBytes = if (contentLength > 0) {
                readBodyBytes(input, contentLength)
            } else {
                ByteArray(0)
            }

            // Validate Session Token
            val token = queryParams["token"] ?: headers["authorization"]?.removePrefix("Bearer ")?.trim()
            val isAuthorized = session.validateToken(token)

            if (!isAuthorized) {
                val errorHtml = """
                    <!DOCTYPE html><html><body style="background:#0e1117;color:#fff;font-family:sans-serif;text-align:center;padding:50px 20px;">
                    <h2 style="color:#e07a5f;">会话已过期或无效</h2>
                    <p style="color:#888;margin-top:10px;">请在手表 Cherish 的「设置 → 手机编辑」中重新扫码连接。</p>
                    </body></html>
                """.trimIndent()
                sendResponse(output, 403, "Forbidden", "text/html; charset=utf-8", errorHtml.toByteArray())
                return client.close()
            }

            session.recordActivity()

            // Route dispatch
            when {
                method == "GET" && path == "/" -> {
                    val html = WebEditorHtmlProvider.getHtml(session.token, "Cherish Watch")
                    sendResponse(output, 200, "OK", "text/html; charset=utf-8", html.toByteArray())
                }

                method == "GET" && path == "/api/status" -> {
                    val res = """{"status":"ok","state":"${session.state}"}"""
                    sendResponse(output, 200, "OK", "application/json", res.toByteArray())
                }

                method == "GET" && path == "/api/events" -> {
                    handleGetEvents(output)
                }

                method == "POST" && path == "/api/events" -> {
                    handleCreateEvent(output, bodyBytes)
                }

                method == "PUT" && path.startsWith("/api/events/") -> {
                    val eventId = path.removePrefix("/api/events/")
                    handleUpdateEvent(output, eventId, bodyBytes)
                }

                method == "DELETE" && path.startsWith("/api/events/") -> {
                    val eventId = path.removePrefix("/api/events/")
                    handleDeleteEvent(output, eventId)
                }

                method == "POST" && path == "/api/reorder" -> {
                    handleReorder(output, bodyBytes)
                }

                method == "POST" && path == "/api/upload-image" -> {
                    handleUploadImage(output, bodyBytes)
                }

                else -> {
                    sendResponse(output, 404, "Not Found", "text/plain", "Endpoint not found".toByteArray())
                }
            }
        } catch (e: Exception) {
            // Client disconnect or parse error
        } finally {
            runCatching { client.close() }
        }
    }

    private fun handleGetEvents(output: OutputStream) {
        val today = SolarDate.fromLocalDate(LocalDate.now())
        val allEvents = repository.getAll()
        val eventListJson = StringBuilder("[")

        allEvents.forEachIndexed { index, event ->
            val (baseSolar, baseLunar) = when (val eventDate = event.eventDate) {
                is EventDate.Solar -> Pair(eventDate.date, null)
                is EventDate.Lunar -> Pair(calendar.lunarToSolar(eventDate.date), eventDate.date)
            }

            val targetSolarDate: SolarDate = if (event.repeatRule == RepeatRule.None) {
                baseSolar
            } else {
                recurrenceCalculator.nextOccurrence(
                    baseSolar = baseSolar,
                    baseLunar = baseLunar,
                    referenceDate = today,
                    rule = event.repeatRule,
                ) ?: baseSolar
            }

            val countdownStatus = CountdownCalculator.calculate(
                referenceDate = today,
                targetDate = targetSolarDate,
            )

            val (daysCount, statusStr) = when (countdownStatus) {
                is EventCountdownStatus.Countdown -> Pair(countdownStatus.days.toInt(), "COUNTDOWN")
                is EventCountdownStatus.Today -> Pair(0, "TODAY")
                is EventCountdownStatus.CountedPast -> Pair(countdownStatus.days.toInt(), "PAST")
            }

            val (isLunar, solarDateStr, lunarObj) = when (val d = event.eventDate) {
                is EventDate.Solar -> Triple(false, d.date.toString(), "null")
                is EventDate.Lunar -> Triple(
                    true,
                    "",
                    """{"year":${d.date.year},"month":${d.date.month},"day":${d.date.day},"isLeap":${d.date.isLeapMonth}}""",
                )
            }

            val targetDateStr = when (val d = event.eventDate) {
                is EventDate.Solar -> "${d.date.year}年${d.date.month}月${d.date.day}日"
                is EventDate.Lunar -> "农历 ${d.date.year}年${if (d.date.isLeapMonth) "闰" else ""}${d.date.month}月${d.date.day}日"
            }

            val repeatRuleJson = when (val r = event.repeatRule) {
                RepeatRule.None -> """{"type":"NONE"}"""
                RepeatRule.Daily -> """{"type":"DAILY"}"""
                RepeatRule.Monthly -> """{"type":"MONTHLY"}"""
                RepeatRule.Yearly -> """{"type":"YEARLY"}"""
                is RepeatRule.Custom -> """{"type":"CUSTOM","interval":${r.interval},"unit":"${r.unit.name}"}"""
            }

            val repeatDesc = when (val r = event.repeatRule) {
                RepeatRule.None -> "不重复"
                RepeatRule.Daily -> "每天重复"
                RepeatRule.Monthly -> "每月重复"
                RepeatRule.Yearly -> "每年重复"
                is RepeatRule.Custom -> "每 ${r.interval} ${when(r.unit) { RepeatUnit.DAY -> "天"; RepeatUnit.WEEK -> "周"; RepeatUnit.MONTH -> "月"; RepeatUnit.YEAR -> "年" }}"
            }

            val (colorType, colorHex) = when (val c = event.color) {
                EventColor.Default -> Pair("Default", "#1e222b")
                EventColor.Amber -> Pair("Amber", "#ffa000")
                EventColor.Emerald -> Pair("Emerald", "#009688")
                EventColor.Ocean -> Pair("Ocean", "#1e88e5")
                EventColor.Rose -> Pair("Rose", "#e91e63")
                EventColor.Violet -> Pair("Violet", "#9c27b0")
                EventColor.Coral -> Pair("Coral", "#ff5722")
                EventColor.Sunset -> Pair("Sunset", "#ffa000")
                EventColor.Aurora -> Pair("Aurora", "#009688")
                EventColor.Lavender -> Pair("Lavender", "#1e88e5")
                is EventColor.Single -> Pair("Custom", "#" + (c.argb and 0xFFFFFFL).toString(16).padStart(6, '0'))
                is EventColor.Gradient -> Pair("Gradient", "#" + (c.startColor and 0xFFFFFFL).toString(16).padStart(6, '0'))
            }

            val backgroundJson = when (val bg = event.background) {
                EventBackground.Default -> """{"type":"DEFAULT","presetKey":"DEFAULT"}"""
                is EventBackground.Color -> """{"type":"COLOR","argb":${bg.argb}}"""
                is EventBackground.Gradient -> """{"type":"GRADIENT","startColor":${bg.startColor},"endColor":${bg.endColor},"angle":${bg.angle}}"""
                is EventBackground.Pattern -> """{"type":"PATTERN","patternId":"${escapeJson(bg.patternId)}"}"""
                is EventBackground.Image -> """{"type":"IMAGE","path":"${escapeJson(bg.path)}","dimAlpha":${bg.dimAlpha}}"""
            }

            val categoryDesc = when (event.category) {
                EventCategory.GENERAL -> "常规"
                EventCategory.ANNIVERSARY -> "纪念日"
                EventCategory.BIRTHDAY -> "生日"
                EventCategory.HOLIDAY -> "节日"
                EventCategory.WORK -> "工作"
                EventCategory.LIFE -> "生活"
                EventCategory.OTHER -> "其他"
            }

            if (index > 0) eventListJson.append(",")
            eventListJson.append("""
                {
                  "id":"${escapeJson(event.id)}",
                  "title":"${escapeJson(event.title)}",
                  "emoji":"${escapeJson(event.emoji)}",
                  "notes":"${escapeJson(event.notes)}",
                  "isPinned":${event.isPinned},
                  "isLunar":$isLunar,
                  "solarDateStr":"$solarDateStr",
                  "lunarDate":$lunarObj,
                  "targetDateStr":"${escapeJson(targetDateStr)}",
                  "repeatRule":$repeatRuleJson,
                  "repeatDesc":"${escapeJson(repeatDesc)}",
                  "category":"${event.category.name}",
                  "categoryDesc":"${escapeJson(categoryDesc)}",
                  "colorType":"$colorType",
                  "colorHex":"$colorHex",
                  "background":$backgroundJson,
                  "daysCount":$daysCount,
                  "status":"$statusStr"
                }
            """.trimIndent())
        }
        eventListJson.append("]")

        val responseJson = """{"events":$eventListJson}"""
        sendResponse(output, 200, "OK", "application/json; charset=utf-8", responseJson.toByteArray())
    }

    private fun handleCreateEvent(output: OutputStream, bodyBytes: ByteArray) {
        val bodyStr = String(bodyBytes, StandardCharsets.UTF_8)
        val jsonEl = json.parseToJsonElement(bodyStr).jsonObject
        val event = parseEventFromJson(jsonEl, null)

        repository.add(event)
        onEventSynced()

        val responseJson = """{"success":true,"id":"${event.id}"}"""
        sendResponse(output, 200, "OK", "application/json", responseJson.toByteArray())
    }

    private fun handleUpdateEvent(output: OutputStream, eventId: String, bodyBytes: ByteArray) {
        val existing = repository.getById(eventId)
        if (existing == null) {
            sendResponse(output, 404, "Not Found", "application/json", """{"error":"Event not found"}""".toByteArray())
            return
        }

        val bodyStr = String(bodyBytes, StandardCharsets.UTF_8)
        val jsonEl = json.parseToJsonElement(bodyStr).jsonObject
        val updatedEvent = parseEventFromJson(jsonEl, eventId)

        // Clean previous image if background changed
        val oldBg = existing.background
        val newBg = updatedEvent.background
        if (oldBg is EventBackground.Image && (newBg !is EventBackground.Image || oldBg.path != newBg.path)) {
            imageStorage?.deleteImage(oldBg.path)
        }

        repository.update(updatedEvent)
        onEventSynced()

        sendResponse(output, 200, "OK", "application/json", """{"success":true}""".toByteArray())
    }

    private fun handleDeleteEvent(output: OutputStream, eventId: String) {
        val existing = repository.getById(eventId)
        if (existing != null) {
            val bg = existing.background
            if (bg is EventBackground.Image) {
                imageStorage?.deleteImage(bg.path)
            }
            repository.delete(eventId)
            onEventSynced()
        }
        sendResponse(output, 200, "OK", "application/json", """{"success":true}""".toByteArray())
    }

    private fun handleReorder(output: OutputStream, bodyBytes: ByteArray) {
        val bodyStr = String(bodyBytes, StandardCharsets.UTF_8)
        val jsonEl = json.parseToJsonElement(bodyStr).jsonObject
        val idsArray = jsonEl["eventIds"]?.jsonArray ?: JsonArray(emptyList())
        val ids = idsArray.map { it.jsonPrimitive.content }

        val allEventsMap = repository.getAll().associateBy { it.id }
        val reorderedList = ids.mapNotNull { allEventsMap[it] }

        if (reorderedList.isNotEmpty()) {
            repository.reorderAll(reorderedList)
            onEventSynced()
        }

        sendResponse(output, 200, "OK", "application/json", """{"success":true}""".toByteArray())
    }

    private fun handleUploadImage(output: OutputStream, bodyBytes: ByteArray) {
        val bodyStr = String(bodyBytes, StandardCharsets.UTF_8)
        val jsonEl = json.parseToJsonElement(bodyStr).jsonObject
        val base64Raw = jsonEl["base64"]?.jsonPrimitive?.contentOrNull ?: ""
        val eventId = jsonEl["eventId"]?.jsonPrimitive?.contentOrNull ?: "phone_draft"
        val dimAlpha = jsonEl["dimAlpha"]?.jsonPrimitive?.doubleOrNull?.toFloat() ?: 0.45f

        val cleanBase64 = base64Raw.substringAfter("base64,").trim()
        val imageBytes = Base64.getDecoder().decode(cleanBase64)

        val filename = imageStorage?.saveImage(
            eventId = eventId,
            bytes = imageBytes,
            extension = "jpg",
        ) ?: "uploaded_${System.currentTimeMillis()}.jpg"

        val responseJson = """{"success":true,"path":"${escapeJson(filename)}","dimAlpha":$dimAlpha}"""
        sendResponse(output, 200, "OK", "application/json", responseJson.toByteArray())
    }

    private fun parseEventFromJson(jsonEl: JsonObject, existingId: String?): CountdownEvent {
        val id = existingId ?: UUID.randomUUID().toString()
        val title = jsonEl["title"]?.jsonPrimitive?.contentOrNull?.trim()?.ifBlank { "新建倒数日" } ?: "新建倒数日"
        val emoji = jsonEl["emoji"]?.jsonPrimitive?.contentOrNull?.trim() ?: "🎂"
        val notes = jsonEl["notes"]?.jsonPrimitive?.contentOrNull ?: ""
        val isPinned = jsonEl["isPinned"]?.jsonPrimitive?.booleanOrNull ?: false

        // Event Date
        val isLunar = jsonEl["isLunar"]?.jsonPrimitive?.booleanOrNull ?: false
        val eventDate: EventDate = if (isLunar) {
            val lunarObj = jsonEl["lunarDate"]?.jsonObject
            val y = lunarObj?.get("year")?.jsonPrimitive?.intOrNull ?: 2026
            val m = lunarObj?.get("month")?.jsonPrimitive?.intOrNull ?: 1
            val d = lunarObj?.get("day")?.jsonPrimitive?.intOrNull ?: 1
            val isLeap = lunarObj?.get("isLeap")?.jsonPrimitive?.booleanOrNull ?: false
            EventDate.Lunar(LunarDate(y, m, d, isLeap))
        } else {
            val today = SolarDate.fromLocalDate(LocalDate.now())
            val solarStr = jsonEl["solarDateStr"]?.jsonPrimitive?.contentOrNull ?: today.toString()
            val parts = solarStr.split("-").mapNotNull { it.toIntOrNull() }
            if (parts.size == 3) {
                EventDate.Solar(SolarDate(parts[0], parts[1], parts[2]))
            } else {
                EventDate.Solar(today)
            }
        }

        // Repeat Rule
        val repeatObj = jsonEl["repeatRule"]?.jsonObject
        val repeatType = repeatObj?.get("type")?.jsonPrimitive?.contentOrNull ?: "NONE"
        val repeatRule: RepeatRule = when (repeatType) {
            "DAILY" -> RepeatRule.Daily
            "MONTHLY" -> RepeatRule.Monthly
            "YEARLY" -> RepeatRule.Yearly
            "CUSTOM" -> {
                val interval = repeatObj?.get("interval")?.jsonPrimitive?.intOrNull ?: 1
                val unitStr = repeatObj?.get("unit")?.jsonPrimitive?.contentOrNull ?: "DAY"
                val unit = runCatching { RepeatUnit.valueOf(unitStr) }.getOrDefault(RepeatUnit.DAY)
                RepeatRule.Custom(interval, unit)
            }
            else -> RepeatRule.None
        }

        // Category
        val categoryStr = jsonEl["category"]?.jsonPrimitive?.contentOrNull ?: "GENERAL"
        val category = runCatching { EventCategory.valueOf(categoryStr) }.getOrDefault(EventCategory.GENERAL)

        // Event Color
        val colorType = jsonEl["colorType"]?.jsonPrimitive?.contentOrNull ?: "Default"
        val colorHex = jsonEl["colorHex"]?.jsonPrimitive?.contentOrNull ?: ""
        val color: EventColor = when (colorType) {
            "Amber" -> EventColor.Amber
            "Emerald" -> EventColor.Emerald
            "Ocean", "Sapphire" -> EventColor.Ocean
            "Rose" -> EventColor.Rose
            "Violet", "Amethyst" -> EventColor.Violet
            "Coral" -> EventColor.Coral
            "Sunset" -> EventColor.Sunset
            "Aurora" -> EventColor.Aurora
            "Lavender" -> EventColor.Lavender
            "Custom" -> {
                val cleanHex = colorHex.trimStart('#')
                val parsed = cleanHex.toLongOrNull(16)?.let { 0xFF000000L or it } ?: 0xFF1E222BL
                EventColor.Single(parsed)
            }
            else -> EventColor.Default
        }

        // Background
        val bgObj = jsonEl["background"]?.jsonObject
        val bgType = bgObj?.get("type")?.jsonPrimitive?.contentOrNull ?: "DEFAULT"
        val background: EventBackground = when (bgType) {
            "IMAGE" -> {
                val path = bgObj?.get("path")?.jsonPrimitive?.contentOrNull ?: ""
                val dim = bgObj?.get("dimAlpha")?.jsonPrimitive?.doubleOrNull?.toFloat() ?: 0.45f
                EventBackground.Image(path = path, dimAlpha = dim)
            }
            "PRESET" -> {
                when (bgObj?.get("presetKey")?.jsonPrimitive?.contentOrNull) {
                    "COLOR_PINK" -> EventBackground.Color(0xFFE91E63L)
                    "COLOR_GOLD" -> EventBackground.Color(0xFFFFA000L)
                    "COLOR_TEAL" -> EventBackground.Color(0xFF009688L)
                    "COLOR_BLUE" -> EventBackground.Color(0xFF1E88E5L)
                    "COLOR_PURPLE" -> EventBackground.Color(0xFF9C27B0L)
                    "GRAD_SUNSET" -> EventBackground.Gradient(0xFFFFA000L, 0xFFE91E63L)
                    "GRAD_AURORA" -> EventBackground.Gradient(0xFF009688L, 0xFF1E88E5L)
                    "GRAD_NEBULA" -> EventBackground.Gradient(0xFF1E88E5L, 0xFF9C27B0L)
                    else -> EventBackground.Default
                }
            }
            else -> EventBackground.Default
        }

        return CountdownEvent(
            id = id,
            title = title,
            emoji = emoji,
            eventDate = eventDate,
            repeatRule = repeatRule,
            category = category,
            isPinned = isPinned,
            notes = notes,
            color = color,
            background = background,
        )
    }

    private fun sendResponse(
        output: OutputStream,
        statusCode: Int,
        statusText: String,
        contentType: String,
        content: ByteArray,
    ) {
        val header = StringBuilder()
            .append("HTTP/1.1 $statusCode $statusText\r\n")
            .append("Content-Type: $contentType\r\n")
            .append("Content-Length: ${content.size}\r\n")
            .append("Connection: close\r\n")
            .append("Access-Control-Allow-Origin: *\r\n")
            .append("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS\r\n")
            .append("Access-Control-Allow-Headers: Content-Type, Authorization\r\n")
            .append("\r\n")

        output.write(header.toString().toByteArray(StandardCharsets.UTF_8))
        if (content.isNotEmpty()) {
            output.write(content)
        }
        output.flush()
    }

    private fun readLineFromStream(input: InputStream): String? {
        val baos = ByteArrayOutputStream()
        var b = input.read()
        if (b == -1) return null
        while (b != -1) {
            if (b == '\n'.code) {
                break
            }
            if (b != '\r'.code) {
                baos.write(b)
            }
            b = input.read()
        }
        return baos.toString(StandardCharsets.UTF_8.name())
    }

    private fun readBodyBytes(input: InputStream, length: Int): ByteArray {
        val buffer = ByteArray(length)
        var totalRead = 0
        while (totalRead < length) {
            val read = input.read(buffer, totalRead, length - totalRead)
            if (read == -1) break
            totalRead += read
        }
        return if (totalRead == length) buffer else buffer.copyOf(totalRead)
    }

    private fun parseQueryParams(queryString: String): Map<String, String> {
        if (queryString.isBlank()) return emptyMap()
        val params = mutableMapOf<String, String>()
        for (pair in queryString.split("&")) {
            val idx = pair.indexOf("=")
            if (idx > 0) {
                val key = URLDecoder.decode(pair.substring(0, idx), "UTF-8")
                val value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                params[key] = value
            }
        }
        return params
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
