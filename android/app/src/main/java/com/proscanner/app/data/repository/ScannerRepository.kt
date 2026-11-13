package com.proscanner.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.proscanner.app.data.api.GeminiAnalysisService
import com.proscanner.app.data.model.ScanResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException

class ScannerRepository(
    private val apiService: GeminiAnalysisService = GeminiAnalysisService()
) {
    companion object {
        private const val PREFS_NAME = "pro_scanner_prefs"
        private const val KEY_BACKEND_URL = "backend_url"

        val shared: ScannerRepository by lazy { ScannerRepository() }
    }

    private var prefs: SharedPreferences? = null

    private val _recentScans = MutableStateFlow<List<ScanResult>>(emptyList())
    val recentScans: StateFlow<List<ScanResult>> = _recentScans.asStateFlow()

    private val _isBackendConnected = MutableStateFlow(false)
    val isBackendConnected: StateFlow<Boolean> = _isBackendConnected.asStateFlow()

    private val _backendUrl = MutableStateFlow(apiService.getBaseUrl())
    val backendUrl: StateFlow<String> = _backendUrl.asStateFlow()

    private val _isBackendConfigured = MutableStateFlow(apiService.isConfigured())
    val isBackendConfigured: StateFlow<Boolean> = _isBackendConfigured.asStateFlow()

    private val _backendStatusDetail = MutableStateFlow<String?>(apiService.validateConfigurationError())
    val backendStatusDetail: StateFlow<String?> = _backendStatusDetail.asStateFlow()

    @Synchronized
    fun initPersistence(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedUrl = prefs?.getString(KEY_BACKEND_URL, null)
            if (savedUrl != null) {
                if (GeminiAnalysisService.isValidBackendUrl(savedUrl)) {
                    apiService.setBaseUrl(savedUrl)
                } else {
                    apiService.setBaseUrl("")
                    prefs?.edit()?.remove(KEY_BACKEND_URL)?.apply()
                }
            }
            syncConfigState()
        }
    }

    private fun syncConfigState() {
        _backendUrl.value = apiService.getBaseUrl()
        _isBackendConfigured.value = apiService.isConfigured()
        if (!apiService.isConfigured()) {
            _isBackendConnected.value = false
            _backendStatusDetail.value = apiService.validateConfigurationError()
        }
    }

    fun isBackendConfigured(): Boolean = apiService.isConfigured()

    suspend fun analyzeChartScreenshot(base64Image: String, language: String = "en"): Result<ScanResult> {
        val configError = apiService.validateConfigurationError()
        if (configError != null) {
            _isBackendConnected.value = false
            _isBackendConfigured.value = false
            _backendStatusDetail.value = configError
            return Result.failure(IOException(configError))
        }

        val result = apiService.analyzeChart(base64Image, language)
        if (result.isSuccess) {
            val scan = result.getOrThrow()
            _recentScans.value = listOf(scan) + _recentScans.value.take(20)
            _isBackendConnected.value = true
            _backendStatusDetail.value = null
        } else {
            _isBackendConnected.value = false
            _backendStatusDetail.value = result.exceptionOrNull()?.localizedMessage
        }
        return result
    }

    fun setBackendUrl(url: String) {
        val normalized = GeminiAnalysisService.normalizeUrl(url)
        if (normalized.isEmpty()) {
            apiService.setBaseUrl("")
            prefs?.edit()?.remove(KEY_BACKEND_URL)?.apply()
        } else {
            apiService.setBaseUrl(normalized)
            if (GeminiAnalysisService.isValidBackendUrl(normalized)) {
                prefs?.edit()?.putString(KEY_BACKEND_URL, normalized)?.apply()
            } else {
                prefs?.edit()?.remove(KEY_BACKEND_URL)?.apply()
            }
        }
        _isBackendConnected.value = false
        syncConfigState()
    }

    fun getBackendUrl(): String = apiService.getBaseUrl()

    suspend fun checkBackendHealth(): Boolean {
        val health = apiService.checkHealthDetailed()
        _isBackendConfigured.value = apiService.isConfigured()
        _isBackendConnected.value = health.isConnected
        _backendStatusDetail.value = health.errorDetail
        return health.isConnected
    }
}
