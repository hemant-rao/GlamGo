package com.example.data.remote

import android.content.Context
import android.os.Handler
import android.os.Looper
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * §690 — minimal live-tracking socket over the existing VedaDrop chat WebSocket
 * (`/ws/vedadrop/chat`). The backend relays {type:"location", role, lat, lon}
 * between the customer and the partner of a booking (server-stamps `role`), so we
 * reuse it for MUTUAL live tracking on the booking map. We push our own
 * {type:"location", lat, lon} and surface the counterparty's via [onLocation].
 *
 * Best-effort: never throws; if the socket can't connect the map simply shows the
 * last-known positions / route. Caller owns the lifecycle (start in the map tab,
 * [close] on dispose).
 */
class LiveTrackingSocket(
    context: Context,
    private val bookingId: String,
    private val onLocation: (role: String, lat: Double, lon: Double) -> Unit,
) {
    private val tokenStore = TokenStore(context)
    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .build()
    @Volatile private var socket: WebSocket? = null

    // §707 — reconnect with exponential backoff. `closed` guards against
    // reconnecting after a deliberate close(); `attempt` caps the delay.
    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile private var closed = false
    @Volatile private var attempt = 0
    private val reconnectRunnable = Runnable { reconnect() }

    /** Build the WS URL WITHOUT the token (token now travels in an Authorization header). */
    private fun wsUrl(): String {
        val root = NetworkConfig.baseUrl.substringBefore("/api/")  // https://host
        val ws = root.replaceFirst("https://", "wss://").replaceFirst("http://", "ws://")
        return "$ws/ws/vedadrop/chat?booking_id=$bookingId"
    }

    fun connect() {
        closed = false
        attempt = 0
        reconnect()
    }

    /** Open a fresh socket. Reused by [connect] and by the failure/close handlers. */
    private fun reconnect() {
        if (closed || socket != null) return
        // Token in the Authorization header — never in the URL query (avoids
        // leaking the JWT into proxy/access/device logs).
        val token = tokenStore.accessToken() ?: return
        val req = Request.Builder()
            .url(wsUrl())
            .header("Authorization", "Bearer $token")
            .build()
        socket = client.newWebSocket(req, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                attempt = 0  // healthy connection → reset backoff
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val obj = JSONObject(text)
                    if (obj.optString("type") == "location") {
                        val role = obj.optString("role", "")
                        val lat = obj.optDouble("lat", Double.NaN)
                        val lon = obj.optDouble("lon", Double.NaN)
                        if (!lat.isNaN() && !lon.isNaN()) onLocation(role, lat, lon)
                    }
                } catch (_: Exception) {
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                scheduleReconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                scheduleReconnect()
            }
        })
    }

    /** Drop the dead socket and queue a reconnect with capped exponential backoff. */
    private fun scheduleReconnect() {
        socket = null
        if (closed) return
        val delay = (1000L shl attempt.coerceAtMost(5)).coerceAtMost(30_000L)  // 1s,2s,4s,8s,16s,30s
        attempt++
        mainHandler.removeCallbacks(reconnectRunnable)
        mainHandler.postDelayed(reconnectRunnable, delay)
    }

    /** Push our own position so the counterparty sees us move (server stamps role). */
    fun sendLocation(lat: Double, lon: Double) {
        val s = socket ?: return
        try {
            s.send(JSONObject().put("type", "location").put("lat", lat).put("lon", lon).toString())
        } catch (_: Exception) {
        }
    }

    fun close() {
        closed = true
        mainHandler.removeCallbacks(reconnectRunnable)
        try {
            socket?.close(1000, null)
        } catch (_: Exception) {
        }
        socket = null
    }
}
