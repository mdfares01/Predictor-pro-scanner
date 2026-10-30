package com.proscanner.app.data.api

import com.proscanner.app.BuildConfig
import com.proscanner.app.data.model.AnalysisRequest
import com.proscanner.app.data.model.ScanResult
import com.proscanner.app.data.model.SignalType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
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
    private val client = OkHttpClient.Builder()
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

            val request = Request.Builder()
                .url("$baseUrl/api/analyze")
                .post(reqBody)
                .header("User-Agent", "FloatingPROScanner-Android/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    val errorBody = response.body?.string() ?: ""
                    return@withContext Result.failure(
                        IOException("Backend returned error code $code: $errorBody")
                    )
                }

                val bodyStr = response.body?.string()
                    ?: return@withContext Result.failure(IOException("Empty response from backend"))

                val result = json.decodeFromString(ScanResult.serializer(), bodyStr)
                Result.success(result)
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
                .build()
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (_: Exception) {
            false
        }
    }
}
