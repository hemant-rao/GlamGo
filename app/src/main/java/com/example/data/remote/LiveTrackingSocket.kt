package com.example.data.remote

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * §690 — minimal live-tracking socket over the existing Nikhat chat WebSocket
 * (`/ws/nikhatglow/chat`). The backend relays {type:"location", role, lat, lon}
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

    private fun wsUrl(): String? {
        val token = tokenStore.accessToken() ?: return null
        val root = NetworkConfig.baseUrl.substringBefore("/api/")  // https://host
        val ws = root.replaceFirst("https://", "wss://").replaceFirst("http://", "ws://")
        return "$ws/ws/nikhatglow/chat?token=$token&booking_id=$bookingId"
    }

    fun connect() {
        if (socket != null) return
        val url = wsUrl() ?: return
        val req = Request.Builder().url(url).build()
        socket = client.newWebSocket(req, object : WebSocketListener() {
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
        })
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
        try {
            socket?.close(1000, null)
        } catch (_: Exception) {
        }
        socket = null
    }
}
