package com.proscanner.app.data.api

import com.proscanner.app.BuildConfig
import com.proscanner.app.data.model.AnalysisRequest
import com.proscanner.app.data.model.ScanResult
import com.proscanner.app.data.model.SignalType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class GeminiAnalysisService(
    private var baseUrl: String = try {
        BuildConfig.DEFAULT_BACKEND_URL
    } catch (_: Exception) {
        "https://ais-dev-msuwv3xuyna6ensh5lmjdf-987225211015.asia-southeast1.run.app"
    }
) {
    // Disable automatic redirect following to prevent silently turning 302 auth redirects into 200 HTML responses
    private val client = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    fun setBaseUrl(url: String) {
        baseUrl = url.trimEnd('/')
    }

    fun getBaseUrl(): String = baseUrl

    suspend fun analyzeChart(base64Image: String, language: String = "en"): Result<ScanResult> = withContext(Dispatchers.IO) {
        try {
            val reqBody = json.encodeToString(
                AnalysisRequest.serializer(),
                AnalysisRequest(
                    imageBase64 = base64Image,
                    mimeType = "image/jpeg",
                    language = language
                )
            ).toRequestBody("application/json".toMediaType())

            val targetUrl = "$baseUrl/api/analyze"
            val request = Request.Builder()
                .url(targetUrl)
                .post(reqBody)
                .header("User-Agent", "FloatingPROScanner-Android/1.0")
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                // 1. Detect HTTP Redirects (e.g. Google Cloud preview cookie wall or captive portal)
                if (response.code in 300..399 || response.priorResponse != null) {
                    val location = response.header("Location") ?: "login or auth page"
                    return@withContext Result.failure(
                        IOException(
                            "Backend redirected (HTTP ${response.code}) to: $location. " +
                            "Direct API access to development preview URLs is blocked by session authentication. " +
                            "Please configure a public or local backend URL in Settings."
                        )
                    )
                }

                val bodyStr = response.body?.string()?.trim()
                    ?: return@withContext Result.failure(IOException("Empty response from backend server"))

                // 2. Safe check for HTML response bodies (prevents JSON parser crash)
                val contentType = response.header("Content-Type")?.lowercase() ?: ""
                val isHtml = contentType.contains("text/html") || bodyStr.startsWith("<")

                if (isHtml) {
                    val preview = bodyStr.take(75).replace(Regex("\\s+"), " ")
                    return@withContext Result.failure(
                        IOException(
                            "Backend returned HTML instead of JSON: '$preview...'. " +
                            "The server URL ($baseUrl) points to a web frontend or redirect page. " +
                            "Please enter your backend API URL in Settings."
                        )
                    )
                }

                // 3. Handle non-successful HTTP status codes
                if (!response.isSuccessful) {
                    val code = response.code
                    val serverError = try {
                        val elem = json.parseToJsonElement(bodyStr)
                        elem.jsonObject["error"]?.jsonPrimitive?.content
                    } catch (_: Exception) {
                        null
                    }
                    val msg = serverError ?: "Server returned error code $code: $bodyStr"
                    return@withContext Result.failure(IOException(msg))
                }

                // 4. Safely decode JSON payload
                try {
                    val result = json.decodeFromString(ScanResult.serializer(), bodyStr)
                    Result.success(result)
                } catch (e: Exception) {
                    Result.failure(
                        IOException("Failed to parse backend ScanResult JSON: ${e.localizedMessage}")
                    )
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkHealth(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/health")
                .get()
                .header("User-Agent", "FloatingPROScanner-Android/1.0")
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful || response.code in 300..399 || response.priorResponse != null) {
                    return@withContext false
                }
                val contentType = response.header("Content-Type")?.lowercase() ?: ""
                if (contentType.contains("text/html")) {
                    return@withContext false
                }
                val body = response.body?.string()?.trim() ?: return@withContext false
                if (body.startsWith("<")) {
                    return@withContext false
                }
                // Verify response contains valid online health status
                body.contains("\"status\"") && (body.contains("\"online\"") || body.contains("\"connected\":true"))
            }
        } catch (_: Exception) {
            false
        }
    }
}
