package com.example.data.ai

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

object TravelAssistantService {
    private const val TAG = "TravelAssistant"
    private const val MODEL = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun askAssistant(userPrompt: String, contextInfo: String = ""): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getExpertOfflineHighwayResponse(userPrompt, contextInfo)
        }

        val systemPrompt = """
            You are TollMate AI Travel Assistant, an expert Indian highway and expressway navigator.
            You help travelers with toll plazas, FASTag guidelines, NHAI emergency services (Call 1033 / 112),
            EV charging stations, highway amenities, rest stops, speed limits, and vehicle roadside safety.
            Current journey context: $contextInfo.
            Keep answers concise, actionable, and friendly with highway safety tips.
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "$systemPrompt\n\nUser Question: $userPrompt")
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("maxOutputTokens", 500)
            })
        }

        val request = Request.Builder()
            .url("$BASE_URL$MODEL:generateContent?key=$apiKey")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val root = JSONObject(body)
                    val candidates = root.optJSONArray("candidates")
                    val firstCandidate = candidates?.optJSONObject(0)
                    val content = firstCandidate?.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    val text = parts?.optJSONObject(0)?.optString("text")
                    if (!text.isNullOrBlank()) {
                        return@withContext text.trim()
                    }
                }
                Log.w(TAG, "Gemini API HTTP ${response.code}: $body")
                getExpertOfflineHighwayResponse(userPrompt, contextInfo)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini call failed", e)
            getExpertOfflineHighwayResponse(userPrompt, contextInfo)
        }
    }

    private fun getExpertOfflineHighwayResponse(query: String, context: String): String {
        val q = query.lowercase()
        return when {
            q.contains("toll") || q.contains("rate") || q.contains("fastag") ->
                "Toll plazas typically charge standard car rates (approx ₹60-₹160 per plaza depending on distance). Ensure your FASTag has a minimum balance of ₹200 to avoid double cash toll penalties. In TollMate, in-app deductions are for estimation and expense tracking."

            q.contains("emergency") || q.contains("accident") || q.contains("help") || q.contains("breakdown") ->
                "EMERGENCY ASSISTANCE:\n• National Highway Helpline: Dial 1033 (Free 24x7 NHAI ambulance, crane & patrol)\n• Universal Emergency: Dial 112 (Police & Medical)\n• Park vehicle safely behind the crash barrier, turn on hazard warning lights, and deploy the red reflective triangle 50m behind."

            q.contains("ev") || q.contains("charging") || q.contains("charger") ->
                "Major expressways feature 60kW-120kW CCS2 fast chargers at food courts and plazas (such as Khalapur on Mumbai-Pune NE-1 and Shahpura on Delhi-Jaipur NH-48). Use the Toll Facilities menu (+) to locate nearest chargers."

            q.contains("speed") || q.contains("limit") ->
                "National Expressways allow up to 120 km/h for cars (100 km/h on ghat / hilly sections). 4-lane National Highways have an 80-100 km/h limit. Speed cameras are automated across gantries."

            q.contains("puncture") || q.contains("repair") || q.contains("fuel") ->
                "Way-side amenities (WSA) with authorized puncture, tyre air, and fuel stations are situated roughly every 40-50 km along toll corridors. You can request tyre/puncture assistance directly in TollMate."

            else ->
                "Highway Travel Tip: Stay in lane, take a 15-minute break every 2 hours at toll plaza rest areas, keep headlights aligned, and keep vehicle documents handy. How else can I assist your journey?"
        }
    }
}
