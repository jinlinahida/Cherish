package com.cherish.app.sync

import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.UUID

/**
 * Lifecycle connection state of the phone-to-watch sync session.
 */
enum class SyncConnectionState {
    WAITING_FOR_CONNECTION,
    CONNECTED,
    DISCONNECTED,
    EXPIRED,
}

/**
 * Manages the temporary session credentials and network addressing for QR sync.
 *
 * Enforces:
 * - Temporary unguessable token generation
 * - 10-minute inactivity expiration
 * - Local Wi-Fi / Hotspot IPv4 resolution
 * - Safe invalidation on exit
 */
class SyncSession(
    val port: Int = 8080,
    val token: String = UUID.randomUUID().toString().replace("-", "").take(16),
    val timeoutMillis: Long = 10 * 60 * 1000L, // 10 minutes
) {
    private val createdAt = System.currentTimeMillis()
    private var lastActivityAt = System.currentTimeMillis()

    var state: SyncConnectionState = SyncConnectionState.WAITING_FOR_CONNECTION
        private set

    /**
     * Updates the last activity timestamp and transitions to [SyncConnectionState.CONNECTED].
     */
    fun recordActivity() {
        if (!isExpired()) {
            lastActivityAt = System.currentTimeMillis()
            state = SyncConnectionState.CONNECTED
        }
    }

    /**
     * Validates whether the provided [requestToken] matches this session and is active.
     */
    fun validateToken(requestToken: String?): Boolean {
        if (isExpired()) {
            state = SyncConnectionState.EXPIRED
            return false
        }
        return requestToken != null && requestToken == token
    }

    /**
     * Checks if the session has expired due to inactivity.
     */
    fun isExpired(): Boolean {
        if (state == SyncConnectionState.DISCONNECTED || state == SyncConnectionState.EXPIRED) {
            return true
        }
        val elapsed = System.currentTimeMillis() - lastActivityAt
        return elapsed > timeoutMillis
    }

    /**
     * Marks the session as disconnected and invalidates the session token.
     */
    fun disconnect() {
        state = SyncConnectionState.DISCONNECTED
    }

    /**
     * Resolves the primary local IPv4 address (e.g., 192.168.x.x, 10.x.x.x)
     * of the watch's active network interface (Wi-Fi, Bluetooth tethering, hotspot).
     */
    fun resolveLocalIpAddress(): String? {
        return resolveDeviceLocalIp()
    }

    /**
     * Constructs the full HTTP connection URL to encode in the QR code.
     */
    fun buildConnectionUrl(ipAddress: String): String {
        return "http://$ipAddress:$port/?token=$token"
    }

    companion object {
        fun resolveDeviceLocalIp(): String? {
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
                var fallbackIp: String? = null

                for (intf in interfaces) {
                    if (intf.isLoopback || !intf.isUp) continue
                    val addresses = intf.inetAddresses
                    for (addr in addresses) {
                        if (addr is Inet4Address && !addr.isLoopbackAddress) {
                            val hostAddress = addr.hostAddress ?: continue
                            // Prefer Wi-Fi / wlan or tethering ap interfaces
                            val name = intf.name.lowercase()
                            if (name.contains("wlan") || name.contains("ap") || name.contains("rndis") || name.contains("bt-pan")) {
                                return hostAddress
                            }
                            if (fallbackIp == null) {
                                fallbackIp = hostAddress
                            }
                        }
                    }
                }
                return fallbackIp
            } catch (e: Exception) {
                return null
            }
        }
    }
}
