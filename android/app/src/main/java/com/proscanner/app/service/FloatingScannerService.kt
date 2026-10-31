package com.proscanner.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.proscanner.app.MainActivity
import com.proscanner.app.R
import com.proscanner.app.data.model.ScanResult
import com.proscanner.app.data.repository.ScannerRepository
import com.proscanner.app.ui.components.FloatingBubbleOverlayContent
import com.proscanner.app.ui.components.FloatingPanelOverlayContent
import com.proscanner.app.ui.theme.FloatingPROScannerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class FloatingScannerService : Service(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

    companion object {
        private const val TAG = "FloatingScannerService"
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_CAPTURE_WITH_CONSENT = "ACTION_CAPTURE_WITH_CONSENT"
        const val ACTION_CAPTURE_CANCELLED = "ACTION_CAPTURE_CANCELLED"
        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_DATA = "EXTRA_DATA"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "pro_scanner_channel"
    }

    private val mViewModelStore = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = mViewModelStore

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var scanJob: Job? = null

    private lateinit var windowManager: WindowManager
    private var screenCaptureManager: ScreenCaptureManager? = null
    private val repository = ScannerRepository()

    // Overlay Views
    private var bubbleView: View? = null
    private var panelView: View? = null

    // Layout Params
    private lateinit var bubbleParams: WindowManager.LayoutParams
    private lateinit var panelParams: WindowManager.LayoutParams

    // Observable Compose State
    private var isExpanded by mutableStateOf(false)
    private var isScanning by mutableStateOf(false)
    private var currentResult by mutableStateOf<ScanResult?>(null)
    private var errorMessage by mutableStateOf<String?>(null)

    // Custom Lifecycle for Compose in Service
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Creating FloatingScannerService...")
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        screenCaptureManager = ScreenCaptureManager(this)

        setupOverlayViews()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand action: ${intent?.action}")
        when (intent?.action) {
            ACTION_START -> {
                startForegroundSafely(isCapturing = false)
                showBubble()
            }
            ACTION_CAPTURE_WITH_CONSENT -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_DATA)
                }

                if (resultCode != 0 && data != null) {
                    executeScreenScan(resultCode, data)
                } else {
                    isScanning = false
                    errorMessage = "Screen capture permission was invalid."
                }
            }
            ACTION_CAPTURE_CANCELLED -> {
                isScanning = false
                errorMessage = "Screen capture was cancelled."
            }
            ACTION_STOP -> {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundSafely(isCapturing: Boolean) {
        try {
            val notification = createNotification(
                if (isCapturing) "Scanning screen with Gemini AI..." else "Floating PRO Scanner is Active",
                isCapturing
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val fgsType = if (isCapturing) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                } else {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                }
                startForeground(NOTIFICATION_ID, notification, fgsType)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (isCapturing) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground service: ${e.message}", e)
        }
    }

    private fun setupOverlayViews() {
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // 1. Draggable Bubble Layout Params
        bubbleParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 320
        }

        // 2. Expanded Panel Layout Params
        panelParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            x = 0
            y = 0
        }

        buildBubbleView()
        buildPanelView()
    }

    private fun buildBubbleView() {
        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setViewTreeLifecycleOwner(this@FloatingScannerService)
            setViewTreeSavedStateRegistryOwner(this@FloatingScannerService)
            setViewTreeViewModelStoreOwner(this@FloatingScannerService)
            setContent {
                FloatingPROScannerTheme {
                    FloatingBubbleOverlayContent(
                        isScanning = isScanning,
                        onBubbleClick = {
                            expandPanel()
                        }
                    )
                }
            }
        }

        // Drag listener for the bubble
        composeView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isClick = true

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = bubbleParams.x
                        initialY = bubbleParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isClick = true
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            isClick = false
                        }
                        bubbleParams.x = initialX + dx
                        bubbleParams.y = initialY + dy
                        try {
                            if (bubbleView?.isAttachedToWindow == true) {
                                windowManager.updateViewLayout(bubbleView, bubbleParams)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error updating bubble layout: ${e.message}")
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isClick) {
                            expandPanel()
                        }
                        return true
                    }
                }
                return false
            }
        })

        bubbleView = composeView
    }

    private fun buildPanelView() {
        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setViewTreeLifecycleOwner(this@FloatingScannerService)
            setViewTreeSavedStateRegistryOwner(this@FloatingScannerService)
            setViewTreeViewModelStoreOwner(this@FloatingScannerService)
            setContent {
                FloatingPROScannerTheme {
                    FloatingPanelOverlayContent(
                        isScanning = isScanning,
                        scanResult = currentResult,
                        errorMessage = errorMessage,
                        onScanClick = { triggerScanRequest() },
                        onStopClick = { stopScan() },
                        onCollapseClick = { collapsePanel() },
                        onCloseAppClick = { stopSelf() }
                    )
                }
            }
        }

        composeView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                collapsePanel()
                true
            } else {
                false
            }
        }

        panelView = composeView
    }

    private fun showBubble() {
        try {
            if (bubbleView != null && bubbleView?.isAttachedToWindow != true) {
                windowManager.addView(bubbleView, bubbleParams)
                Log.d(TAG, "Floating bubble added to WindowManager")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing bubble: ${e.message}", e)
        }
    }

    private fun expandPanel() {
        isExpanded = true
        try {
            if (bubbleView != null && bubbleView?.isAttachedToWindow == true) {
                windowManager.removeView(bubbleView)
            }
            if (panelView != null && panelView?.isAttachedToWindow != true) {
                windowManager.addView(panelView, panelParams)
                Log.d(TAG, "Floating panel expanded")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error expanding panel: ${e.message}", e)
        }
    }

    private fun collapsePanel() {
        isExpanded = false
        try {
            if (panelView != null && panelView?.isAttachedToWindow == true) {
                windowManager.removeView(panelView)
            }
            if (bubbleView != null && bubbleView?.isAttachedToWindow != true) {
                windowManager.addView(bubbleView, bubbleParams)
                Log.d(TAG, "Floating panel collapsed to bubble")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error collapsing panel: ${e.message}", e)
        }
    }

    private fun triggerScanRequest() {
        if (isScanning) return
        isScanning = true
        errorMessage = null
        currentResult = null

        try {
            val intent = Intent(this, CaptureConsentActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            Log.d(TAG, "Launched CaptureConsentActivity for fresh MediaProjection consent")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch CaptureConsentActivity: ${e.message}", e)
            isScanning = false
            errorMessage = "Could not request screen capture permission."
        }
    }

    private fun executeScreenScan(resultCode: Int, data: Intent) {
        scanJob?.cancel()
        scanJob = serviceScope.launch {
            try {
                isScanning = true
                errorMessage = null

                // Promote service to mediaProjection FGS BEFORE calling getMediaProjection
                startForegroundSafely(isCapturing = true)

                val base64Image = screenCaptureManager?.captureScreenBase64(resultCode, data)

                // Revert FGS type back to standard overlay
                startForegroundSafely(isCapturing = false)

                if (base64Image == null) {
                    errorMessage = "Could not capture chart screenshot. Please retry."
                    isScanning = false
                    return@launch
                }

                // Send chart screenshot to secure Gemini AI backend proxy
                val analysisResult = repository.analyzeChartScreenshot(base64Image)
                if (analysisResult.isSuccess) {
                    currentResult = analysisResult.getOrThrow()
                    Log.d(TAG, "Scan analysis succeeded: ${currentResult?.signal} (${currentResult?.confidence}%)")
                } else {
                    errorMessage = analysisResult.exceptionOrNull()?.localizedMessage
                        ?: "Failed to analyze chart with AI."
                    Log.e(TAG, "Scan analysis failed: $errorMessage")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during executeScreenScan: ${e.message}", e)
                errorMessage = e.localizedMessage ?: "Unknown scanning error occurred."
            } finally {
                isScanning = false
                startForegroundSafely(isCapturing = false)
            }
        }
    }

    private fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        isScanning = false
        startForegroundSafely(isCapturing = false)
    }

    private fun createNotification(contentText: String, isCapturing: Boolean): Notification {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Floating PRO Scanner",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active floating bubble and screen scanner"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(if (isCapturing) "Scanning Chart with Gemini AI" else "Floating PRO Scanner is Active")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Destroying FloatingScannerService...")
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        serviceScope.cancel()
        mViewModelStore.clear()

        try {
            if (bubbleView != null && bubbleView?.isAttachedToWindow == true) {
                windowManager.removeView(bubbleView)
            }
            if (panelView != null && panelView?.isAttachedToWindow == true) {
                windowManager.removeView(panelView)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing views on destroy: ${e.message}", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
