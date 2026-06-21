package com.example.data.remote

import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * §694 — Centralised, user-friendly error translation.
 *
 * Every backend 4xx/5xx on the Nikhat Glow surface uses the frozen envelope
 * `{"error":{"code":"...","message":"..."}}`. Retrofit, however, throws a bare
 * [HttpException] whose `.message()` is only the status line ("HTTP 401
 * Unauthorized") — which is what the UI used to show. This object parses the
 * real response body and returns the server's human message, falling back to
 * clear, code-aware copy for network failures and unparseable bodies.
 *
 * Use [friendlyMessage] everywhere an error is surfaced (toasts, inline text)
 * so the whole app speaks the same language.
 */
object ApiErrors {

    /** Turn any Throwable from the data layer into a message safe to show a user. */
    fun friendlyMessage(t: Throwable): String {
        return when (t) {
            is HttpException -> httpMessage(t)
            is SocketTimeoutException ->
                "The server took too long to respond. Please try again."
            is UnknownHostException, is ConnectException ->
                "No internet connection. Check your network and try again."
            is IOException ->
                "Couldn't reach the server. Check your connection and try again."
            else -> t.message?.takeIf { it.isNotBlank() }
                ?: "Something went wrong. Please try again."
        }
    }

    private fun httpMessage(e: HttpException): String {
        // Read the error body ONCE (ResponseBody.string() can only be consumed
        // once), then derive both the code and the message from it.
        val raw = runCatching { e.response()?.errorBody()?.string() }.getOrNull()

        // §713 — code-aware copy for geofencing refusals when the server sent only
        // a code (no human message), so the customer understands WHY. Checked
        // before the server message so the copy is consistent + actionable.
        when (raw?.let { codeOf(it) }) {
            "PARTNER_OUT_OF_AREA" ->
                return "This professional doesn't serve your area. Try another expert, or update your location."
            "LOCATION_REQUIRED" ->
                return "Please set your location first so we can match you with nearby professionals."
        }

        // Otherwise prefer the server's own message.
        val parsed = raw?.takeIf { it.isNotBlank() }?.let { runCatching { extractMessage(it) }.getOrNull() }
        if (!parsed.isNullOrBlank()) return parsed

        // No parseable body — fall back to status-aware defaults.
        return when (e.code()) {
            401 -> "Your session has expired. Please log in again."
            403 -> "You don't have access to this. Please log in again."
            404 -> "We couldn't find what you were looking for."
            408, 504 -> "The request timed out. Please try again."
            409 -> "That action conflicts with the current state. Please refresh and retry."
            422 -> "Some details look invalid. Please check and try again."
            429 -> "Too many attempts. Please wait a moment and try again."
            in 500..599 -> "Something went wrong on our end. Please try again."
            else -> "Something went wrong. Please try again."
        }
    }

    /**
     * Pull a message out of either shape the backend can emit:
     *   { "error": { "code": "...", "message": "..." } }   (mobile envelope)
     *   { "detail": "..." }                                  (FastAPI default)
     *   { "detail": { "error": { "message": "..." } } }      (HTTPException w/ envelope)
     */
    private fun extractMessage(raw: String): String? {
        val json = runCatching { JSONObject(raw) }.getOrNull() ?: return null
        json.optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
            ?.let { return it }
        val detail = json.opt("detail")
        when (detail) {
            is String -> if (detail.isNotBlank()) return detail
            is JSONObject -> detail.optJSONObject("error")?.optString("message")
                ?.takeIf { it.isNotBlank() }?.let { return it }
        }
        return null
    }

    /** The structured error code, when present — for code-specific UI branching.
     *  NOTE: consumes the error body, so don't also call [friendlyMessage] on the
     *  same throwable (the body can only be read once). */
    fun errorCode(t: Throwable): String? {
        if (t !is HttpException) return null
        val raw = runCatching { t.response()?.errorBody()?.string() }.getOrNull() ?: return null
        return codeOf(raw)
    }

    /** Pull the structured error code out of an already-read body string. */
    private fun codeOf(raw: String): String? {
        if (raw.isBlank()) return null
        return runCatching {
            val json = JSONObject(raw)
            json.optJSONObject("error")?.optString("code")?.takeIf { it.isNotBlank() }
                ?: (json.opt("detail") as? JSONObject)
                    ?.optJSONObject("error")?.optString("code")?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }
}
