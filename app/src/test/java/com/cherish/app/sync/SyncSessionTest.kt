package com.cherish.app.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncSessionTest {

    @Test
    fun `session creates unguessable token and valid connection URL`() {
        val session = SyncSession(port = 8080)
        assertTrue(session.token.isNotBlank())
        assertEquals(16, session.token.length)

        val url = session.buildConnectionUrl("192.168.1.100")
        assertEquals("http://192.168.1.100:8080/?token=${session.token}", url)
    }

    @Test
    fun `session token validation accepts matching token and rejects mismatch`() {
        val session = SyncSession()
        assertTrue(session.validateToken(session.token))
        assertFalse(session.validateToken("wrong-token"))
        assertFalse(session.validateToken(null))
        assertFalse(session.validateToken(""))
    }

    @Test
    fun `session disconnect invalidates token`() {
        val session = SyncSession()
        assertTrue(session.validateToken(session.token))

        session.disconnect()
        assertEquals(SyncConnectionState.DISCONNECTED, session.state)
        assertTrue(session.isExpired())
        assertFalse(session.validateToken(session.token))
    }

    @Test
    fun `session timeout expiration marks expired state`() {
        // Create session with 1ms timeout
        val session = SyncSession(timeoutMillis = 1L)
        Thread.sleep(5)
        assertTrue(session.isExpired())
        assertFalse(session.validateToken(session.token))
        assertEquals(SyncConnectionState.EXPIRED, session.state)
    }
}
