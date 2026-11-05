package com.proscanner.app.service

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class CaptureConsentActivity : ComponentActivity() {

    companion object {
        private const val TAG = "CaptureConsentActivity"
    }

    private val captureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG, "[ConsentHandoff] MediaProjection consent result: resultCode=${result.resultCode}, hasData=${result.data != null}")
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val serviceIntent = Intent(this, FloatingScannerService::class.java).apply {
                action = FloatingScannerService.ACTION_CAPTURE_WITH_CONSENT
                putExtra(FloatingScannerService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(FloatingScannerService.EXTRA_DATA, result.data)
            }
            try {
                Log.d(TAG, "[ConsentHandoff] Delivering capture intent to FloatingScannerService via ContextCompat.startForegroundService")
                ContextCompat.startForegroundService(this, serviceIntent)
                Log.i(TAG, "[ConsentHandoff] Successfully initiated foreground service transition")
            } catch (e: Exception) {
                Log.e(TAG, "[ConsentHandoff] Failed to startForegroundService: ${e.javaClass.simpleName} - ${e.message}", e)
            }
        } else {
            Log.w(TAG, "[ConsentHandoff] User cancelled or denied MediaProjection consent")
            notifyCancelledAndFinish()
            return@registerForActivityResult
        }
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val projectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
        if (projectionManager != null) {
            try {
                captureLauncher.launch(projectionManager.createScreenCaptureIntent())
            } catch (e: Exception) {
                Log.e(TAG, "Error launching screen capture intent: ${e.message}", e)
                notifyCancelledAndFinish()
            }
        } else {
            Log.e(TAG, "MediaProjectionManager unavailable on this device")
            notifyCancelledAndFinish()
        }
    }

    private fun notifyCancelledAndFinish() {
        val serviceIntent = Intent(this, FloatingScannerService::class.java).apply {
            action = FloatingScannerService.ACTION_CAPTURE_CANCELLED
        }
        try {
            startService(serviceIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send capture cancellation to service: ${e.message}", e)
        }
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}
