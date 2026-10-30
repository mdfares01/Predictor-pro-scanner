package com.proscanner.app.data.repository

import com.proscanner.app.data.api.GeminiAnalysisService
import com.proscanner.app.data.model.ScanResult
import com.proscanner.app.data.model.SignalType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ScannerRepository(
    private val apiService: GeminiAnalysisService = GeminiAnalysisService()
) {
    private val _recentScans = MutableStateFlow<List<ScanResult>>(
        listOf(
            ScanResult(
                id = "1",
                signal = SignalType.UP,
                confidence = 78,
                pair = "EUR/USD",
                timeframe = "5m",
                summary = listOf(
                    "Uptrend is visible",
                    "Strong bullish candles near support",
                    "Resistance at 1.0855",
                    "Volume looks increasing"
                ),
                risks = listOf(
                    "Market can be volatile",
                    "False breakout possible"
                ),
                keyReason = "Bullish momentum bounce from support",
                timestamp = System.currentTimeMillis() - 15 * 60 * 1000
            ),
            ScanResult(
                id = "2",
                signal = SignalType.DOWN,
                confidence = 72,
                pair = "GBP/CHF",
                timeframe = "15m",
                summary = listOf(
                    "Downtrend sequence intact",
                    "Bearish engulfing candle formed",
                    "Support breach at 1.1070"
                ),
                risks = listOf("Sudden reversal possible", "High impact news pending"),
                keyReason = "Bearish rejection at resistance",
                timestamp = System.currentTimeMillis() - 60 * 60 * 1000
            ),
            ScanResult(
                id = "3",
                signal = SignalType.NO_TRADE,
                confidence = 56,
                pair = "USD/JPY",
                timeframe = "1m",
                summary = listOf(
                    "Sideways movement within tight box",
                    "Doji candlesticks prevailing",
                    "Unclear momentum direction"
                ),
                risks = listOf("False signals possible", "Choppy market condition"),
                keyReason = "Sideways movement",
                timestamp = System.currentTimeMillis() - 150 * 60 * 1000
            )
        )
    )
    val recentScans: StateFlow<List<ScanResult>> = _recentScans.asStateFlow()

    private val _isBackendConnected = MutableStateFlow(true)
    val isBackendConnected: StateFlow<Boolean> = _isBackendConnected.asStateFlow()

    suspend fun analyzeChartScreenshot(base64Image: String, language: String = "en"): Result<ScanResult> {
        val result = apiService.analyzeChart(base64Image, language)
        if (result.isSuccess) {
            val scan = result.getOrThrow()
            _recentScans.value = listOf(scan) + _recentScans.value.take(20)
            _isBackendConnected.value = true
        } else {
            _isBackendConnected.value = false
        }
        return result
    }

    fun setBackendUrl(url: String) {
        apiService.setBaseUrl(url)
    }

    fun getBackendUrl(): String = apiService.getBaseUrl()

    suspend fun checkBackendHealth(): Boolean {
        val ok = apiService.checkHealth()
        _isBackendConnected.value = ok
        return ok
    }
}
