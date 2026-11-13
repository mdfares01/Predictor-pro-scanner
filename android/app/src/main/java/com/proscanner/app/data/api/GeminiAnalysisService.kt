package com.proscanner.app.data.api

import com.proscanner.app.BuildConfig
import com.proscanner.app.data.model.AnalysisRequest
import com.proscanner.app.data.model.ScanResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.URI
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

data class HealthCheckResult(
    val isConnected: Boolean,
    val errorDetail: String? = null
)

class GeminiAnalysisService(
    private var baseUrl: String = normalizeUrl(
        try {
            BuildConfig.DEFAULT_BACKEND_URL
        } catch (_: Exception) {
            ""
        }
    )
) {
    companion object {
        fun normalizeUrl(raw: String): String = raw.trim().trimEnd('/')

        fun isDevelopmentPreviewUrl(url: String): Boolean {
            val cleaned = normalizeUrl(url)
            if (cleaned.isBlank()) return false
            val host = try {
                URI.create(cleaned).host?.lowercase() ?: ""
            } catch (_: Exception) {
                ""
            }
            return host.startsWith("ais-dev-") || cleaned.contains("ais-dev-", ignoreCase = true)
        }

        fun isValidPublicHttpsUrl(url: String): Boolean {
            val cleaned = normalizeUrl(url)
            if (cleaned.isBlank()) return false
            if (isDevelopmentPreviewUrl(cleaned)) return false
            val uri = try {
                URI.create(cleaned)
            } catch (_: Exception) {
                return false
            }
            val scheme = uri.scheme?.lowercase() ?: return false
            val host = uri.host?.lowercase() ?: return false
            if (host.isBlank()) return false
            return scheme == "https"
        }

        fun isValidBackendUrl(url: String): Boolean {
            val cleaned = normalizeUrl(url)
            if (isValidPublicHttpsUrl(cleaned)) return true
            val uri = try {
                URI.create(cleaned)
            } catch (_: Exception) {
                return false
            }
            val scheme = uri.scheme?.lowercase() ?: return false
            val host = uri.host?.lowercase() ?: return false
            // Allow plain HTTP only for explicit local emulator/loopback development endpoints
            return scheme == "http" && (host == "10.0.2.2" || host == "127.0.0.1" || host == "localhost")
        }
    }

    init {
        if (!isValidBackendUrl(baseUrl)) {
            baseUrl = ""
        }
    }

    // Disable automatic redirect following to prevent silently turning 302 auth redirects into 200 HTML responses
    private val client = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
        encodeDefaults = true
    }

    fun setBaseUrl(url: String) {
        baseUrl = normalizeUrl(url)
    }

    fun getBaseUrl(): String = baseUrl

    fun isConfigured(): Boolean = isValidBackendUrl(baseUrl)

    fun validateConfigurationError(): String? {
        if (baseUrl.isBlank()) {
            return "Backend Not Configured. A real public HTTPS backend deployment is required before connection can succeed. Configure your backend URL in Settings."
        }
        if (isDevelopmentPreviewUrl(baseUrl)) {
            return "Google AI Studio development-preview URLs (ais-dev-...) cannot be used as a production backend. Deploy a real public HTTPS backend and configure its URL in Settings."
        }
        if (!isValidBackendUrl(baseUrl)) {
            return "Backend Not Configured. A valid public HTTPS backend URL is required (received: $baseUrl)."
        }
        return null
    }

    suspend fun analyzeChart(base64Image: String, language: String = "en"): Result<ScanResult> = withContext(Dispatchers.IO) {
        val configError = validateConfigurationError()
        if (configError != null) {
            return@withContext Result.failure(IOException(configError))
        }

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
                // 1. Detect HTTP Redirects (e.g. HTTP 302 auth redirect or captive portal)
                if (response.code in 300..399 || response.priorResponse != null) {
                    val location = response.header("Location") ?: "authentication or web portal"
                    return@withContext Result.failure(
                        IOException(
                            "Backend redirected (HTTP ${response.code}) to: $location. " +
                            "Redirects are blocked because session-gated preview URLs do not serve API JSON. " +
                            "Please configure a public HTTPS backend deployment in Settings."
                        )
                    )
                }

                val bodyStr = response.body?.string()?.trim()
                if (bodyStr.isNullOrBlank()) {
                    return@withContext Result.failure(IOException("Empty response from backend server ($baseUrl)."))
                }

                // 2. Safe check for HTML response bodies (prevents JSON parser crash)
                val contentType = response.header("Content-Type")?.lowercase() ?: ""
                val isHtml = contentType.contains("text/html") || bodyStr.startsWith("<")
                if (isHtml) {
                    val preview = bodyStr.take(75).replace(Regex("\\s+"), " ")
                    return@withContext Result.failure(
                        IOException(
                            "Backend returned HTML instead of JSON: '$preview...'. " +
                            "Verify that $baseUrl points to your deployed API backend, not a web page."
                        )
                    )
                }

                // 3. Handle non-successful HTTP status codes
                if (!response.isSuccessful) {
                    val code = response.code
                    val serverError = try {
                        val elem = json.parseToJsonElement(bodyStr)
                        elem.jsonObject["error"]?.jsonPrimitive?.contentOrNull
                    } catch (_: Exception) {
                        null
                    }
                    val msg = serverError ?: "Backend server returned HTTP $code: ${bodyStr.take(160)}"
                    return@withContext Result.failure(IOException(msg))
                }

                // 4. Safely decode JSON payload
                try {
                    val result = json.decodeFromString(ScanResult.serializer(), bodyStr)
                    Result.success(result)
                } catch (e: Exception) {
                    Result.failure(
                        IOException("Invalid JSON response from backend server: ${e.localizedMessage ?: "Malformed response"}")
                    )
                }
            }
        } catch (_: SocketTimeoutException) {
            Result.failure(IOException("Request timed out waiting for backend server ($baseUrl). Please check server status and retry."))
        } catch (_: UnknownHostException) {
            Result.failure(IOException("Backend server is offline or hostname could not be resolved ($baseUrl). Check your internet connection."))
        } catch (_: ConnectException) {
            Result.failure(IOException("Backend server is offline or refused connection ($baseUrl). Verify your backend deployment is running."))
        } catch (e: Exception) {
            Result.failure(IOException(e.localizedMessage ?: "Network error contacting backend server."))
        }
    }

    suspend fun checkHealthDetailed(): HealthCheckResult = withContext(Dispatchers.IO) {
        val configError = validateConfigurationError()
        if (configError != null) {
            return@withContext HealthCheckResult(isConnected = false, errorDetail = configError)
        }

        try {
            val request = Request.Builder()
                .url("$baseUrl/api/health")
                .get()
                .header("User-Agent", "FloatingPROScanner-Android/1.0")
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code in 300..399 || response.priorResponse != null) {
                    return@withContext HealthCheckResult(
                        isConnected = false,
                        errorDetail = "Backend redirected (HTTP ${response.code}); expected direct JSON health response."
                    )
                }
                if (!response.isSuccessful) {
                    return@withContext HealthCheckResult(
                        isConnected = false,
                        errorDetail = "Backend health check failed with HTTP ${response.code}."
                    )
                }
                val contentType = response.header("Content-Type")?.lowercase() ?: ""
                if (contentType.contains("text/html")) {
                    return@withContext HealthCheckResult(
                        isConnected = false,
                        errorDetail = "Backend returned HTML instead of JSON on /api/health."
                    )
                }
                val body = response.body?.string()?.trim()
                if (body.isNullOrBlank()) {
                    return@withContext HealthCheckResult(
                        isConnected = false,
                        errorDetail = "Backend returned an empty response on /api/health."
                    )
                }
                if (body.startsWith("<")) {
                    return@withContext HealthCheckResult(
                        isConnected = false,
                        errorDetail = "Backend returned HTML instead of JSON on /api/health."
                    )
                }

                // Strictly parse JSON to prevent false positives on invalid JSON
                try {
                    val obj = json.parseToJsonElement(body).jsonObject
                    val status = obj["status"]?.jsonPrimitive?.contentOrNull?.lowercase()
                    val connected = obj["connected"]?.jsonPrimitive?.booleanOrNull == true
                    if (status == "online" && connected) {
                        HealthCheckResult(isConnected = true, errorDetail = null)
                    } else {
                        HealthCheckResult(
                            isConnected = false,
                            errorDetail = "Backend /api/health did not report online status."
                        )
                    }
                } catch (_: Exception) {
                    HealthCheckResult(
                        isConnected = false,
                        errorDetail = "Backend returned invalid JSON on /api/health."
                    )
                }
            }
        } catch (_: SocketTimeoutException) {
            HealthCheckResult(isConnected = false, errorDetail = "Backend health check timed out.")
        } catch (_: UnknownHostException) {
            HealthCheckResult(isConnected = false, errorDetail = "Backend host not found or offline.")
        } catch (_: ConnectException) {
            HealthCheckResult(isConnected = false, errorDetail = "Backend server refused connection (offline).")
        } catch (e: Exception) {
            HealthCheckResult(isConnected = false, errorDetail = e.localizedMessage ?: "Backend connection failed.")
        }
    }

    suspend fun checkHealth(): Boolean = checkHealthDetailed().isConnected
}
