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

    private var resultDispatched = false

    private val captureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG, "[ConsentHandoff] MediaProjection consent result: resultCode=${result.resultCode}, hasData=${result.data != null}")
        resultDispatched = true
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
                notifyFailureAndFinish("Could not start foreground capture service: ${e.localizedMessage ?: e.javaClass.simpleName}")
                return@registerForActivityResult
            }
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        } else {
            Log.w(TAG, "[ConsentHandoff] User cancelled or denied MediaProjection consent")
            notifyCancelledAndFinish("Screen capture was cancelled or denied. No screenshot was captured.")
        }
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
                notifyFailureAndFinish("Could not launch Android screen capture prompt: ${e.localizedMessage ?: "Unknown error"}")
            }
        } else {
            Log.e(TAG, "MediaProjectionManager unavailable on this device")
            notifyFailureAndFinish("Screen capture is unavailable on this device.")
        }
    }

    override fun onDestroy() {
        if (isFinishing && !resultDispatched) {
            resultDispatched = true
            val cancelIntent = Intent(this, FloatingScannerService::class.java).apply {
                action = FloatingScannerService.ACTION_CAPTURE_CANCELLED
                putExtra(
                    FloatingScannerService.EXTRA_ERROR_MESSAGE,
                    "Screen capture was cancelled before consent completed."
                )
            }
            try {
                startService(cancelIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to notify cancellation on destroy: ${e.message}", e)
            }
        }
        super.onDestroy()
    }

    private fun notifyCancelledAndFinish(message: String? = null) {
        resultDispatched = true
        val serviceIntent = Intent(this, FloatingScannerService::class.java).apply {
            action = FloatingScannerService.ACTION_CAPTURE_CANCELLED
            if (!message.isNullOrBlank()) {
                putExtra(FloatingScannerService.EXTRA_ERROR_MESSAGE, message)
            }
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

    private fun notifyFailureAndFinish(message: String) {
        resultDispatched = true
        val serviceIntent = Intent(this, FloatingScannerService::class.java).apply {
            action = FloatingScannerService.ACTION_CAPTURE_FAILED
            putExtra(FloatingScannerService.EXTRA_ERROR_MESSAGE, message)
        }
        try {
            startService(serviceIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send capture failure to service: ${e.message}", e)
        }
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}
