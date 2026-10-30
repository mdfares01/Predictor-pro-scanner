package com.proscanner.app.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class SignalType {
    UP,
    DOWN,
    NO_TRADE
}

@Serializable
data class ScanResult(
    val id: String = System.currentTimeMillis().toString(),
    val signal: SignalType,
    val confidence: Int,
    val pair: String = "EUR/USD",
    val timeframe: String = "5m",
    val evidence: List<String> = emptyList(),
    val reasoning: String = "",
    val uncertainty: List<String> = emptyList(),
    val summary: List<String> = emptyList(),
    val risks: List<String> = emptyList(),
    val keyReason: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val disclaimer: String = "This is an AI analysis based on screenshot only. Not financial advice.",
    val modelUsed: String = "gemini-3.8-flash"
)

@Serializable
data class AnalysisRequest(
    val imageBase64: String,
    val mimeType: String = "image/jpeg",
    val requestedPair: String? = null,
    val requestedTimeframe: String? = null,
    val language: String? = "en"
)

@Serializable
data class BackendStatus(
    val connected: Boolean,
    val serverStatus: String,
    val model: String = "gemini-3.8-flash"
)
