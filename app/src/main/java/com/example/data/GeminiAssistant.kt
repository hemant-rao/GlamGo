package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiAssistant {
    private const val TAG = "GeminiAssistant"
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun generateBeautyAdvise(userMessage: String): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API key is not configured. Falling back to local helper.")
            return@withContext getLocalFallbackAdvice(userMessage)
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
            val systemInstruction = "You are Nikhat Glow's Smart Beauty and Wellness AI Assistant. " +
                    "Your role is to guide users to choose the best beauty services like haircut, makeup, facial, spa, and massage. " +
                    "Be friendly, professional, concise (under 3-4 sentences), and propose relevant categories or treatments like hair oiling, M3 facial, deep tissue massage, etc."

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "$systemInstruction\n\nUser asked: $userMessage")
                            })
                        })
                    })
                })
            }

            val body = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Unsuccessful response from Gemini API: ${response.code}")
                    return@withContext getLocalFallbackAdvice(userMessage)
                }

                val responseBody = response.body?.string() ?: return@withContext getLocalFallbackAdvice(userMessage)
                val jsonResponse = JSONObject(responseBody)
                val text = jsonResponse
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                return@withContext text.trim()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API call failed with exception", e)
            return@withContext getLocalFallbackAdvice(userMessage)
        }
    }

    private fun getLocalFallbackAdvice(prompt: String): String {
        val p = prompt.lowercase()
        return when {
            p.contains("hair") || p.contains("cut") || p.contains("spa") -> {
                "For beautiful hair, I highly recommend our Luxury Haircut & Beard Grooming or our Keratin-infused Intense Nourishing Hair Spa! These will replenish hydration and refresh your structure style perfectly."
            }
            p.contains("face") || p.contains("glow") || p.contains("tan") -> {
                "To get a beautiful radiance, our M3 Premium Brightening Facial with active face serum or the organic Anti-Tan Clay Cleanup is ideal. They deeply detoxify and eliminate tan instantly!"
            }
            p.contains("massage") || p.contains("pain") || p.contains("relax") -> {
                "To relieve tension, nothing beats our Deep Tissue Healing Massage. Our certified therapists bring a professional table and use warmed sesame-herbal oils for full body relaxation."
            }
            p.contains("wedding") || p.contains("makeup") || p.contains("party") -> {
                "Our Signature Party Glam Makeup is popular! It uses luxury products like MAC/Huda and is applied by expert artists for a gorgeous long-lasting finish."
            }
            else -> {
                "Welcome to Nikhat Glow! I can recommend haircuts, facials, bridal makeups, and professional massages. Feel free to let me know any skin or muscle concerns so I can guide you!"
            }
        }
    }
}
