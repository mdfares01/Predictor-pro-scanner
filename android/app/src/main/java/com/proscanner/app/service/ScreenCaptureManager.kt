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
import android.view.WindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class ScreenCaptureManager(private val context: Context) {
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var isCallbackRegistered = false

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

    fun setMediaProjection(resultCode: Int, data: Intent) {
        val projectionManager =
            context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)

        // Android 14+ requires registering a callback before creating any VirtualDisplay
        if (!isCallbackRegistered) {
            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    release()
                }
            }, Handler(Looper.getMainLooper()))
            isCallbackRegistered = true
        }
    }

    suspend fun captureScreenBase64(): String? = withContext(Dispatchers.IO) {
        val projection = mediaProjection ?: return@withContext null
        updateScreenDimensions()

        try {
            imageReader = ImageReader.newInstance(
                screenWidth,
                screenHeight,
                PixelFormat.RGBA_8888,
                2
            )

            virtualDisplay = projection.createVirtualDisplay(
                "ScreenScannerCapture",
                screenWidth,
                screenHeight,
                screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                null
            )

            // Allow short frame capture delay for buffer swap
            kotlinx.coroutines.delay(250)

            var acquiredImage: Image? = null
            try {
                acquiredImage = imageReader?.acquireLatestImage()
                if (acquiredImage != null) {
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

                    Base64.encodeToString(byteArray, Base64.NO_WRAP)
                } else {
                    null
                }
            } finally {
                acquiredImage?.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            virtualDisplay?.release()
            virtualDisplay = null
            imageReader?.close()
            imageReader = null
        }
    }

    fun release() {
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        mediaProjection?.stop()
        mediaProjection = null
        isCallbackRegistered = false
    }
}
