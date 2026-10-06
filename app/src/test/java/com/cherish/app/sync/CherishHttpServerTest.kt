package com.cherish.app.sync

import com.cherish.app.date.lunar.DefaultLunarCalendar
import com.cherish.app.date.model.SolarDate
import com.cherish.app.event.model.CountdownEvent
import com.cherish.app.event.model.EventDate
import com.cherish.app.event.repository.DefaultEventRepository
import com.cherish.app.storage.AtomicFileEventStorage
import com.cherish.app.storage.FileEventImageStorage
import com.cherish.app.storage.JvmAtomicFileWriter
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Base64

class CherishHttpServerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var storageFile: File
    private lateinit var imageDir: File
    private lateinit var repository: DefaultEventRepository
    private lateinit var imageStorage: FileEventImageStorage
    private val calendar = DefaultLunarCalendar()

    private lateinit var session: SyncSession
    private lateinit var server: CherishHttpServer
    private var port: Int = 0

    @Before
    fun setUp() {
        storageFile = File(tempFolder.root, "server_events.json")
        imageDir = tempFolder.newFolder("server_backgrounds")

        val eventStorage = AtomicFileEventStorage(JvmAtomicFileWriter(storageFile))
        repository = DefaultEventRepository(eventStorage)
        imageStorage = FileEventImageStorage(imageDir)

        session = SyncSession(port = 0) // ephemeral port
        server = CherishHttpServer(
            session = session,
            repository = repository,
            imageStorage = imageStorage,
            calendar = calendar,
        )
        port = server.start()
    }

    @After
    fun tearDown() {
        if (::server.isInitialized) {
            server.stop()
        }
    }

    private fun request(
        path: String,
        method: String = "GET",
        body: String? = null,
    ): Pair<Int, String> {
        val url = URL("http://127.0.0.1:$port$path")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = 3000
        conn.readTimeout = 3000

        if (body != null) {
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
        }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val response = stream?.bufferedReader()?.use { it.readText() } ?: ""
        return Pair(code, response)
    }

    @Test
    fun `GET root returns web editor HTML when token is valid`() {
        val (code, response) = request("/?token=${session.token}")
        assertEquals(200, code)
        assertTrue(response.contains("Cherish - 手机编辑"))
        assertTrue(response.contains("cropCanvas"))
    }

    @Test
    fun `GET root with invalid token returns 403 Forbidden`() {
        val (code, _) = request("/?token=invalid_token")
        assertEquals(403, code)
    }

    @Test
    fun `GET api status returns ok`() {
        val (code, response) = request("/api/status?token=${session.token}")
        assertEquals(200, code)
        assertTrue(response.contains("\"status\":\"ok\""))
    }

    @Test
    fun `CRUD flow via HTTP REST API creates, reads, updates, and deletes events`() {
        // 1. Initially empty
        val (codeList1, resList1) = request("/api/events?token=${session.token}")
        assertEquals(200, codeList1)
        assertTrue(resList1.contains("\"events\":[]"))

        // 2. Create Event
        val createJson = """
            {
              "title": "结婚十周年",
              "emoji": "💍",
              "notes": "订好法餐厅",
              "isPinned": true,
              "isLunar": false,
              "solarDateStr": "2026-10-15",
              "category": "ANNIVERSARY",
              "colorType": "Rose",
              "background": {"type": "PRESET", "presetKey": "COLOR_PINK"}
            }
        """.trimIndent()

        val (codeCreate, resCreate) = request("/api/events?token=${session.token}", "POST", createJson)
        assertEquals(200, codeCreate)
        assertTrue(resCreate.contains("\"success\":true"))

        val events = repository.getAll()
        assertEquals(1, events.size)
        val event = events.first()
        assertEquals("结婚十周年", event.title)
        assertTrue(event.isPinned)

        // 3. Get Events list
        val (codeList2, resList2) = request("/api/events?token=${session.token}")
        assertEquals(200, codeList2)
        assertTrue(resList2.contains("结婚十周年"))
        assertTrue(resList2.contains("ANNIVERSARY"))

        // 4. Update Event
        val updateJson = """
            {
              "title": "结婚十一周年",
              "emoji": "💍",
              "notes": "计划旅行",
              "isPinned": false,
              "isLunar": false,
              "solarDateStr": "2026-10-15",
              "category": "ANNIVERSARY",
              "colorType": "Amber",
              "background": {"type": "DEFAULT"}
            }
        """.trimIndent()

        val (codeUpdate, _) = request("/api/events/${event.id}?token=${session.token}", "PUT", updateJson)
        assertEquals(200, codeUpdate)

        val updated = repository.getById(event.id)
        assertNotNull(updated)
        assertEquals("结婚十一周年", updated!!.title)
        assertEquals("计划旅行", updated.notes)

        // 5. Delete Event
        val (codeDelete, _) = request("/api/events/${event.id}?token=${session.token}", "DELETE")
        assertEquals(200, codeDelete)
        assertTrue(repository.getAll().isEmpty())
    }

    @Test
    fun `upload-image endpoint decodes base64 and stores image file`() {
        val dummyBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7)
        val base64Data = Base64.getEncoder().encodeToString(dummyBytes)
        val uploadJson = """
            {
              "base64": "data:image/jpeg;base64,$base64Data",
              "eventId": "photo-test-1",
              "dimAlpha": 0.55
            }
        """.trimIndent()

        val (code, response) = request("/api/upload-image?token=${session.token}", "POST", uploadJson)
        assertEquals(200, code)
        assertTrue(response.contains("\"success\":true"))
        assertTrue(response.contains("\"path\":"))

        // Verify stored file
        val files = imageDir.listFiles() ?: emptyArray()
        assertTrue(files.any { it.name.startsWith("bg_photo-test-1_") })
    }

    @Test
    fun `reorder endpoint reorganises repository events according to request order`() {
        val evt1 = CountdownEvent(id = "e1", title = "Event 1", eventDate = EventDate.Solar(SolarDate(2026, 10, 1)))
        val evt2 = CountdownEvent(id = "e2", title = "Event 2", eventDate = EventDate.Solar(SolarDate(2026, 10, 2)))
        repository.add(evt1)
        repository.add(evt2)

        val reorderJson = """{"eventIds":["e2","e1"]}"""
        val (code, _) = request("/api/reorder?token=${session.token}", "POST", reorderJson)
        assertEquals(200, code)

        val reordered = repository.getAll()
        assertEquals("e2", reordered[0].id)
        assertEquals("e1", reordered[1].id)
    }

    @Test
    fun `GET api images endpoint serves stored image bytes and returns 404 for missing image`() {
        val testBytes = byteArrayOf(10, 20, 30, 40)
        val filename = imageStorage.saveImage("img-serve-test", testBytes)

        // 1. Fetch existing image
        val (codeOk, _) = request("/api/images/$filename?token=${session.token}")
        assertEquals(200, codeOk)

        // 2. Fetch nonexistent image
        val (codeMissing, _) = request("/api/images/nonexistent.jpg?token=${session.token}")
        assertEquals(404, codeMissing)
    }

    @Test
    fun `upload-image endpoint with malformed base64 returns 400 Bad Request`() {
        val badJson = """{"base64":"not_valid_base64!!!","eventId":"err-test"}"""
        val (code, response) = request("/api/upload-image?token=${session.token}", "POST", badJson)
        assertEquals(400, code)
        assertTrue(response.contains("\"success\":false"))
    }

    @Test
    fun `server stop invalidates old session and fresh session starts cleanly`() {
        server.stop()
        val newSession = SyncSession(port = 0)
        val newServer = CherishHttpServer(
            session = newSession,
            repository = repository,
            imageStorage = imageStorage,
            calendar = calendar,
        )
        val newPort = newServer.start()
        port = newPort

        try {
            // Old token from previous session is rejected (403)
            val (codeOld, _) = request("/api/status?token=${session.token}")
            assertEquals(403, codeOld)

            // New token from fresh session is accepted (200)
            val (codeNew, _) = request("/api/status?token=${newSession.token}")
            assertEquals(200, codeNew)
        } finally {
            newServer.stop()
        }
    }

    @Test
    fun `concurrent requests from simulated multiple clients execute thread-safely`() {
        val threads = (1..6).map { i ->
            Thread {
                val json = """
                    {
                      "title": "并发事件 $i",
                      "emoji": "🔥",
                      "isPinned": false,
                      "isLunar": false,
                      "solarDateStr": "2026-11-0$i",
                      "category": "WORK"
                    }
                """.trimIndent()
                request("/api/events?token=${session.token}", "POST", json)
            }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }

        val all = repository.getAll()
        assertEquals(6, all.size)
    }
}
