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
            screenDensity = context.resources.configuration.densityDpi
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
            screenDensity = metrics.densityDpi
            screenWidth = metrics.widthPixels.coerceAtLeast(480)
            screenHeight = metrics.heightPixels.coerceAtLeast(800)
        }
    }

    suspend fun captureScreenBase64(resultCode: Int, data: Intent): String? = withContext(Dispatchers.IO) {
        updateScreenDimensions()
        val projectionManager =
            context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
        if (projectionManager == null) {
            Log.e(TAG, "MediaProjectionManager is null")
            return@withContext null
        }

        var mediaProjection: MediaProjection? = null
        var virtualDisplay: VirtualDisplay? = null
        var imageReader: ImageReader? = null
        var projectionCallback: MediaProjection.Callback? = null

        try {
            Log.d(TAG, "[ScreenCapture] Calling projectionManager.getMediaProjection(resultCode, data)...")
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)
            if (mediaProjection == null) {
                Log.e(TAG, "[ScreenCapture] projectionManager.getMediaProjection returned null!")
                return@withContext null
            }
            Log.i(TAG, "[ScreenCapture] getMediaProjection succeeded, registering callback...")

            // Android 14+ requires registering callback BEFORE creating any VirtualDisplay
            val mainHandler = Handler(Looper.getMainLooper())
            projectionCallback = object : MediaProjection.Callback() {
                override fun onStop() {
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
            Log.i(TAG, "[ScreenCapture] VirtualDisplay created successfully")

            // Short frame render pause for surface buffer swap
            delay(350)

            var acquiredImage: Image? = null
            for (attempt in 1..3) {
                acquiredImage = imageReader.acquireLatestImage()
                if (acquiredImage != null) break
                delay(150)
            }

            if (acquiredImage != null) {
                try {
                    val planes = acquiredImage.planes
                    val buffer: ByteBuffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding = rowStride - pixelStride * screenWidth

                    val bitmap = Bitmap.createBitmap(
                        screenWidth + rowPadding / pixelStride,
                        screenHeight,
                        Bitmap.Config.ARGB_8888
                    )
                    bitmap.copyPixelsFromBuffer(buffer)

                    // Crop to exact screen dimensions
                    val cropped = Bitmap.createBitmap(bitmap, 0, 0, screenWidth, screenHeight)

                    // Compress to JPEG for high-efficiency Gemini multimodal transmission
                    val outStream = ByteArrayOutputStream()
                    cropped.compress(Bitmap.CompressFormat.JPEG, 85, outStream)
                    val byteArray = outStream.toByteArray()

                    if (bitmap != cropped) {
                        bitmap.recycle()
                    }
                    cropped.recycle()

                    Log.i(TAG, "[ScreenCapture] Screenshot captured successfully: ${byteArray.size} bytes encoded")
                    Base64.encodeToString(byteArray, Base64.NO_WRAP)
                } finally {
                    acquiredImage.close()
                }
            } else {
                Log.e(TAG, "[ScreenCapture] No image frame acquired from ImageReader after 3 attempts")
                null
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "[ScreenCapture] SecurityException during screen capture: ${e.message}", e)
            null
        } catch (e: IllegalStateException) {
            Log.e(TAG, "[ScreenCapture] IllegalStateException during screen capture: ${e.message}", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "[ScreenCapture] Unexpected exception during screen capture: ${e.message}", e)
            null
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
