package com.proscanner.app.service

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

class ScreenCaptureManager(private val context: Context) {

    companion object {
        private const val TAG = "ScreenCaptureManager"
    }

    private var screenDensity = DisplayMetrics.DENSITY_DEFAULT
    private var screenWidth = 1080
    private var screenHeight = 2400

    init {
        updateScreenDimensions()
    }

    private fun updateScreenDimensions() {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            val bounds = metrics.bounds
            screenWidth = bounds.width().coerceAtLeast(480)
            screenHeight = bounds.height().coerceAtLeast(800)
            screenDensity = context.resources.displayMetrics.densityDpi.coerceAtLeast(DisplayMetrics.DENSITY_DEFAULT)
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
            screenDensity = metrics.densityDpi
            screenWidth = metrics.widthPixels.coerceAtLeast(480)
            screenHeight = metrics.heightPixels.coerceAtLeast(800)
        }
    }

    suspend fun captureScreenBase64(resultCode: Int, data: Intent): Result<String> = withContext(Dispatchers.IO) {
        updateScreenDimensions()
        val projectionManager =
            context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
        if (projectionManager == null) {
            Log.e(TAG, "MediaProjectionManager is null")
            return@withContext Result.failure(IllegalStateException("MediaProjection service is unavailable on this device."))
        }

        var mediaProjection: MediaProjection? = null
        var virtualDisplay: VirtualDisplay? = null
        var imageReader: ImageReader? = null
        var projectionCallback: MediaProjection.Callback? = null
        val projectionStopped = AtomicBoolean(false)

        try {
            Log.d(TAG, "[ScreenCapture] Calling projectionManager.getMediaProjection(resultCode, data)...")
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)
            if (mediaProjection == null) {
                Log.e(TAG, "[ScreenCapture] projectionManager.getMediaProjection returned null!")
                return@withContext Result.failure(SecurityException("Screen capture permission was denied or expired. Please grant capture permission again."))
            }
            Log.i(TAG, "[ScreenCapture] getMediaProjection succeeded, registering callback...")

            // Android 14+ requires registering callback BEFORE creating any VirtualDisplay
            val mainHandler = Handler(Looper.getMainLooper())
            projectionCallback = object : MediaProjection.Callback() {
                override fun onStop() {
                    projectionStopped.set(true)
                    Log.i(TAG, "[ScreenCapture] MediaProjection session stopped by system or callback")
                }
            }
            mediaProjection.registerCallback(projectionCallback, mainHandler)

            imageReader = ImageReader.newInstance(
                screenWidth,
                screenHeight,
                PixelFormat.RGBA_8888,
                2
            )

            Log.d(TAG, "[ScreenCapture] Creating virtualDisplay ${screenWidth}x${screenHeight} density=$screenDensity...")
            virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenScannerCapture",
                screenWidth,
                screenHeight,
                screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.surface,
                null,
                null
            )
            if (virtualDisplay == null) {
                return@withContext Result.failure(IllegalStateException("Failed to create VirtualDisplay for screen capture."))
            }
            Log.i(TAG, "[ScreenCapture] VirtualDisplay created successfully")

            // Short frame render pause for surface buffer swap
            delay(280)

            var acquiredImage: Image? = null
            for (attempt in 1..6) {
                if (projectionStopped.get()) {
                    return@withContext Result.failure(IllegalStateException("Screen capture session was stopped before a frame could be captured."))
                }
                acquiredImage = imageReader.acquireLatestImage()
                if (acquiredImage != null) break
                delay(120)
            }

            if (acquiredImage != null) {
                var bitmap: Bitmap? = null
                var cropped: Bitmap? = null
                try {
                    val planes = acquiredImage.planes
                    val buffer: ByteBuffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    if (pixelStride <= 0) {
                        return@withContext Result.failure(IllegalStateException("Invalid image buffer pixel stride received during capture."))
                    }
                    val rowPadding = (rowStride - pixelStride * screenWidth).coerceAtLeast(0)

                    bitmap = Bitmap.createBitmap(
                        screenWidth + rowPadding / pixelStride,
                        screenHeight,
                        Bitmap.Config.ARGB_8888
                    )
                    bitmap.copyPixelsFromBuffer(buffer)

                    // Crop to exact screen dimensions
                    cropped = Bitmap.createBitmap(bitmap, 0, 0, screenWidth, screenHeight)

                    // Compress to JPEG for multimodal transmission
                    val outStream = ByteArrayOutputStream()
                    val compressedOk = cropped.compress(Bitmap.CompressFormat.JPEG, 85, outStream)
                    val byteArray = outStream.toByteArray()
                    if (!compressedOk || byteArray.isEmpty()) {
                        return@withContext Result.failure(IllegalStateException("Captured screen frame could not be encoded."))
                    }

                    Log.i(TAG, "[ScreenCapture] Screenshot captured successfully: ${byteArray.size} bytes encoded")
                    Result.success(Base64.encodeToString(byteArray, Base64.NO_WRAP))
                } finally {
                    if (cropped != null && cropped != bitmap && !cropped.isRecycled) {
                        cropped.recycle()
                    }
                    if (bitmap != null && !bitmap.isRecycled) {
                        bitmap.recycle()
                    }
                    acquiredImage.close()
                }
            } else {
                Log.e(TAG, "[ScreenCapture] No image frame acquired from ImageReader after 6 attempts")
                Result.failure(IllegalStateException("No screen frame was produced before capture timed out. Please try again."))
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "[ScreenCapture] SecurityException during screen capture: ${e.message}", e)
            Result.failure(SecurityException("Screen capture permission denied or revoked by Android (${e.localizedMessage ?: "SecurityException"})."))
        } catch (e: IllegalStateException) {
            Log.e(TAG, "[ScreenCapture] IllegalStateException during screen capture: ${e.message}", e)
            Result.failure(IllegalStateException("Screen capture session error: ${e.localizedMessage ?: "IllegalStateException"}"))
        } catch (e: Exception) {
            Log.e(TAG, "[ScreenCapture] Unexpected exception during screen capture: ${e.message}", e)
            Result.failure(IllegalStateException("Screen capture failed: ${e.localizedMessage ?: e.javaClass.simpleName}"))
        } finally {
            Log.d(TAG, "[ScreenCapture] Cleaning up capture resources...")
            try {
                virtualDisplay?.release()
                Log.d(TAG, "[ScreenCapture] VirtualDisplay released")
            } catch (e: Exception) {
                Log.w(TAG, "[ScreenCapture] Error releasing virtualDisplay: ${e.message}")
            }
            try {
                imageReader?.close()
                Log.d(TAG, "[ScreenCapture] ImageReader closed")
            } catch (e: Exception) {
                Log.w(TAG, "[ScreenCapture] Error closing imageReader: ${e.message}")
            }
            try {
                if (projectionCallback != null) {
                    mediaProjection?.unregisterCallback(projectionCallback)
                }
                mediaProjection?.stop()
                Log.d(TAG, "[ScreenCapture] MediaProjection stopped and callback unregistered")
            } catch (e: Exception) {
                Log.w(TAG, "[ScreenCapture] Error stopping mediaProjection: ${e.message}")
            }
        }
    }
}
