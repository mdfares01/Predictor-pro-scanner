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
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FloatingScannerService : Service(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
    private val mViewModelStore = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = mViewModelStore

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_DATA = "EXTRA_DATA"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "pro_scanner_channel"
    }

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

    // State
    private var isExpanded = false
    private var isScanning = false
    private var currentResult: ScanResult? = null
    private var errorMessage: String? = null

    // Custom Lifecycle for Compose in Service
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        screenCaptureManager = ScreenCaptureManager(this)

        setupOverlayViews()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_DATA)
                }

                if (resultCode != 0 && data != null) {
                    screenCaptureManager?.setMediaProjection(resultCode, data)
                }

                // Promote to foreground with mediaProjection service type on Android 10+ and 14+
                startForegroundSafely()
                showBubble()
            }
            ACTION_STOP -> {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundSafely() {
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
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
            y = 300
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
                            windowManager.updateViewLayout(bubbleView, bubbleParams)
                        } catch (_: Exception) {}
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
            setViewTreeLifecycleOwner(this@FloatingScannerService)
            setViewTreeSavedStateRegistryOwner(this@FloatingScannerService)
            setViewTreeViewModelStoreOwner(this@FloatingScannerService)
            setContent {
                FloatingPROScannerTheme {
                    FloatingPanelOverlayContent(
                        isScanning = isScanning,
                        scanResult = currentResult,
                        errorMessage = errorMessage,
                        onScanClick = { startScan() },
                        onStopClick = { stopScan() },
                        onCollapseClick = { collapsePanel() },
                        onCloseAppClick = { stopSelf() }
                    )
                }
            }
        }
        panelView = composeView
    }

    private fun showBubble() {
        try {
            if (bubbleView?.windowToken == null) {
                windowManager.addView(bubbleView, bubbleParams)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun expandPanel() {
        isExpanded = true
        try {
            if (panelView?.windowToken == null) {
                windowManager.addView(panelView, panelParams)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun collapsePanel() {
        isExpanded = false
        try {
            if (panelView?.windowToken != null) {
                windowManager.removeView(panelView)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startScan() {
        if (isScanning) return
        isScanning = true
        errorMessage = null
        currentResult = null

        scanJob = serviceScope.launch {
            try {
                // Simulate capture delay for user screen stabilization
                delay(400)
                val base64Image = screenCaptureManager?.captureScreenBase64()

                if (base64Image == null) {
                    errorMessage = "Could not capture chart screenshot. Check permissions."
                    isScanning = false
                    return@launch
                }

                // Call secure backend
                val analysisResult = repository.analyzeChartScreenshot(base64Image)
                if (analysisResult.isSuccess) {
                    currentResult = analysisResult.getOrThrow()
                } else {
                    errorMessage = analysisResult.exceptionOrNull()?.localizedMessage
                        ?: "Failed to analyze chart."
                }
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Unknown scanning error occurred."
            } finally {
                isScanning = false
            }
        }
    }

    private fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        isScanning = false
    }

    private fun createNotification(): Notification {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Floating PRO Scanner Service",
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
            .setContentTitle("Floating PRO Scanner is Active")
            .setContentText("Tap to open main dashboard or use floating bubble")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        serviceScope.cancel()
        mViewModelStore.clear()
        screenCaptureManager?.release()

        try {
            if (bubbleView?.windowToken != null) {
                windowManager.removeView(bubbleView)
            }
            if (panelView?.windowToken != null) {
                windowManager.removeView(panelView)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
