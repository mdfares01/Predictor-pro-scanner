package com.proscanner.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
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
        const val ACTION_CAPTURE_FAILED = "ACTION_CAPTURE_FAILED"
        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_DATA = "EXTRA_DATA"
        const val EXTRA_ERROR_MESSAGE = "EXTRA_ERROR_MESSAGE"
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
    private val repository = ScannerRepository.shared
    private var isForegroundStarted = false

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
    private var isBubbleAttached by mutableStateOf(false)

    // Custom Lifecycle for Compose in Service
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "[Service-Lifecycle] Creating FloatingScannerService...")
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        repository.initPersistence(applicationContext)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        screenCaptureManager = ScreenCaptureManager(this)

        setupOverlayParams()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "[FGS] onStartCommand action: ${intent?.action}")
        when (intent?.action) {
            ACTION_START -> {
                Log.d(TAG, "[FGS] Received ACTION_START. Initializing foreground service in SPECIAL_USE mode.")
                val started = startForegroundSafely(isCapturing = false)
                if (started) {
                    showBubble()
                } else {
                    Log.e(TAG, "[FGS] Failed to start foreground service on ACTION_START; stopping service.")
                    stopSelf()
                }
            }
            ACTION_CAPTURE_WITH_CONSENT -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_DATA)
                }

                Log.d(TAG, "[MediaProjection-FGS] onStartCommand ACTION_CAPTURE_WITH_CONSENT. resultCode=$resultCode, hasData=${data != null}")

                if (resultCode == android.app.Activity.RESULT_OK && data != null) {
                    // Synchronously promote FGS to MEDIA_PROJECTION on main thread while consent is active
                    val promoted = startForegroundSafely(isCapturing = true)
                    Log.i(TAG, "[MediaProjection-FGS] Synchronous FGS promotion to MEDIA_PROJECTION: success=$promoted")
                    if (promoted) {
                        executeScreenScan(resultCode, data)
                    } else {
                        Log.e(TAG, "[MediaProjection-FGS] FGS promotion failed. Keeping overlay active without crashing.")
                        isScanning = false
                        errorMessage = getString(R.string.error_fgs_start_failed)
                        // Revert back to overlay mode safely without stopping service
                        startForegroundSafely(isCapturing = false)
                    }
                } else {
                    Log.e(TAG, "[MediaProjection-FGS] Received invalid capture permission data: resultCode=$resultCode")
                    isScanning = false
                    errorMessage = "Screen capture permission was denied or invalid. No screenshot was captured."
                    if (!isForegroundStarted) {
                        startForegroundSafely(isCapturing = false)
                    }
                }
            }
            ACTION_CAPTURE_CANCELLED -> {
                Log.d(TAG, "[MediaProjection-FGS] Screen capture was cancelled by user.")
                isScanning = false
                errorMessage = intent.getStringExtra(EXTRA_ERROR_MESSAGE)
                    ?: getString(R.string.error_capture_cancelled)
                if (!isForegroundStarted) {
                    stopSelf()
                }
            }
            ACTION_CAPTURE_FAILED -> {
                Log.e(TAG, "[MediaProjection-FGS] Screen capture consent failed: ${intent.getStringExtra(EXTRA_ERROR_MESSAGE)}")
                isScanning = false
                errorMessage = intent.getStringExtra(EXTRA_ERROR_MESSAGE)
                    ?: "Screen capture could not be started. Please try again."
                if (!isForegroundStarted) {
                    stopSelf()
                }
            }
            ACTION_STOP -> {
                Log.d(TAG, "[FGS] Received ACTION_STOP. Terminating service.")
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundSafely(isCapturing: Boolean): Boolean {
        val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (isCapturing) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && isCapturing) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        } else {
            0
        }

        Log.d(TAG, "[FGS] Executing startForeground: isCapturing=$isCapturing, targetType=$fgsType, sdk=${Build.VERSION.SDK_INT}")

        return try {
            val notification = createNotification(
                if (isCapturing) "Scanning screen with Gemini AI..." else "Floating PRO Scanner is Active",
                isCapturing
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
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
            isForegroundStarted = true
            Log.i(TAG, "[FGS] startForeground SUCCEEDED (isCapturing=$isCapturing, type=$fgsType)")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "[FGS] SecurityException in startForeground (isCapturing=$isCapturing, type=$fgsType): ${e.message}", e)
            false
        } catch (e: IllegalStateException) {
            Log.e(TAG, "[FGS] IllegalStateException in startForeground (isCapturing=$isCapturing, type=$fgsType): ${e.message}", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "[FGS] Unexpected exception in startForeground (isCapturing=$isCapturing, type=$fgsType): ${e.javaClass.simpleName}: ${e.message}", e)
            false
        }
    }

    private fun setupOverlayParams() {
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val metrics = resources.displayMetrics
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels

        // 1. Draggable Bubble Layout Params: WRAP_CONTENT, non-focusable, non-modal so touches to other apps are never blocked
        bubbleParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = (screenHeight * 0.35f).toInt()
        }

        // 2. Expanded Panel Layout Params: Constrained width (max 380dp) with FLAG_LAYOUT_IN_SCREEN
        val maxPanelWidthPx = (380 * metrics.density).toInt()
        val panelWidth = (screenWidth * 0.92f).toInt().coerceAtMost(maxPanelWidthPx)

        panelParams = WindowManager.LayoutParams(
            panelWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = ((screenWidth - panelWidth) / 2).coerceAtLeast(0)
            y = (screenHeight * 0.12f).toInt()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val metrics = resources.displayMetrics
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels
        val maxPanelWidthPx = (380 * metrics.density).toInt()
        val newPanelWidth = (screenWidth * 0.92f).toInt().coerceAtMost(maxPanelWidthPx)

        panelParams.width = newPanelWidth
        panelParams.x = panelParams.x.coerceIn(0, (screenWidth - newPanelWidth).coerceAtLeast(0))
        panelParams.y = panelParams.y.coerceIn(0, (screenHeight - (220 * metrics.density).toInt()).coerceAtLeast(0))

        val bubbleDim = (64 * metrics.density).toInt()
        bubbleParams.x = bubbleParams.x.coerceIn(0, (screenWidth - bubbleDim).coerceAtLeast(0))
        bubbleParams.y = bubbleParams.y.coerceIn(0, (screenHeight - bubbleDim).coerceAtLeast(0))

        try {
            if (panelView?.isAttachedToWindow == true) {
                windowManager.updateViewLayout(panelView, panelParams)
            }
            if (bubbleView?.isAttachedToWindow == true) {
                windowManager.updateViewLayout(bubbleView, bubbleParams)
            }
        } catch (e: Exception) {
            Log.w(TAG, "[Overlay] Error updating layout on configuration change: ${e.message}")
        }
    }

    private fun buildBubbleView(): View {
        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(this@FloatingScannerService)
            setViewTreeSavedStateRegistryOwner(this@FloatingScannerService)
            setViewTreeViewModelStoreOwner(this@FloatingScannerService)
            setContent {
                FloatingPROScannerTheme {
                    FloatingBubbleOverlayContent(
                        isScanning = isScanning,
                        onBubbleClick = { expandPanel() }
                    )
                }
            }
        }

        // Add attach state change listener to reliably track when the bubble is actually attached to the WindowManager
        composeView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                Log.i(TAG, "[Overlay] SUCCESS: Floating bubble is attached to WindowManager")
                isBubbleAttached = true
            }

            override fun onViewDetachedFromWindow(v: View) {
                Log.i(TAG, "[Overlay] Floating bubble detached from WindowManager")
                isBubbleAttached = false
            }
        })

        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop

        // Reliable drag vs tap detection for the floating bubble
        composeView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isDragging = false
            private var touchDownTime = 0L

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = bubbleParams.x
                        initialY = bubbleParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        touchDownTime = System.currentTimeMillis()
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (!isDragging && (Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop)) {
                            isDragging = true
                        }
                        if (isDragging) {
                            val metrics = resources.displayMetrics
                            val maxX = (metrics.widthPixels - v.width).coerceAtLeast(0)
                            val maxY = (metrics.heightPixels - v.height).coerceAtLeast(0)
                            bubbleParams.x = (initialX + dx).coerceIn(0, maxX)
                            bubbleParams.y = (initialY + dy).coerceIn(0, maxY)
                            try {
                                if (bubbleView?.isAttachedToWindow == true) {
                                    windowManager.updateViewLayout(bubbleView, bubbleParams)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "[Overlay] Error updating bubble layout: ${e.message}")
                            }
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val duration = System.currentTimeMillis() - touchDownTime
                        val dx = Math.abs(event.rawX - initialTouchX)
                        val dy = Math.abs(event.rawY - initialTouchY)
                        if (!isDragging && dx < touchSlop && dy < touchSlop && duration < 600) {
                            v.performClick()
                            expandPanel()
                        } else if (isDragging) {
                            snapBubbleToEdge(v.width)
                        }
                        isDragging = false
                        return true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        if (isDragging) {
                            snapBubbleToEdge(v.width)
                        }
                        isDragging = false
                        return true
                    }
                }
                return false
            }
        })

        return composeView
    }

    private fun snapBubbleToEdge(bubbleWidth: Int) {
        val metrics = resources.displayMetrics
        val screenWidth = metrics.widthPixels
        val middle = screenWidth / 2
        val finalWidth = if (bubbleWidth > 0) bubbleWidth else (64 * metrics.density).toInt()
        bubbleParams.x = if (bubbleParams.x + (finalWidth / 2) < middle) {
            16
        } else {
            (screenWidth - finalWidth - 16).coerceAtLeast(0)
        }
        try {
            if (bubbleView?.isAttachedToWindow == true) {
                windowManager.updateViewLayout(bubbleView, bubbleParams)
            }
        } catch (e: Exception) {
            Log.e(TAG, "[Overlay] Error snapping bubble to screen edge: ${e.message}")
        }
    }

    private fun openSettingsScreen() {
        collapsePanel()
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("destination", "settings")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "[Navigation] Error opening settings screen: ${e.message}", e)
        }
    }

    private fun buildPanelView() {
        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
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
                        onCloseAppClick = { stopSelf() },
                        onSettingsClick = { openSettingsScreen() }
                    )
                }
            }
        }

        // DraggablePanelLayout intercepts drag gestures strictly in the header area via MotionEvent rawX/rawY
        // and dispatches all other touches (including button clicks) immediately to Compose without delay
        val container = DraggablePanelLayout(
            context = this,
            windowManager = windowManager,
            panelParams = panelParams
        ).apply {
            setViewTreeLifecycleOwner(this@FloatingScannerService)
            setViewTreeSavedStateRegistryOwner(this@FloatingScannerService)
            setViewTreeViewModelStoreOwner(this@FloatingScannerService)
            addView(composeView)
        }

        panelView = container
    }

    private fun showBubble() {
        val canDraw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.provider.Settings.canDrawOverlays(this)
        } else {
            true
        }
        Log.d(TAG, "[Overlay] showBubble called. canDrawOverlays=$canDraw, isExpanded=$isExpanded, isAttached=${bubbleView?.isAttachedToWindow}")

        if (!canDraw) {
            Log.e(TAG, "[Overlay] Cannot add bubble: SYSTEM_ALERT_WINDOW permission is missing!")
            errorMessage = "Overlay permission is not granted. Please enable 'Appear on top'."
            return
        }

        // Prevent duplicate overlay if expanded panel is already visible
        if (isExpanded || panelView?.isAttachedToWindow == true) {
            Log.d(TAG, "[Overlay] Expanded panel already active; keeping single overlay without duplicate bubble")
            return
        }

        try {
            if (bubbleView?.isAttachedToWindow != true) {
                bubbleView = buildBubbleView()
                windowManager.addView(bubbleView, bubbleParams)
                Log.i(TAG, "[Overlay] windowManager.addView succeeded for fresh bubble view")
            } else {
                Log.d(TAG, "[Overlay] Bubble view already attached")
            }
        } catch (e: Exception) {
            Log.e(TAG, "[Overlay] WindowManager.addView FAILED for bubble: ${e.javaClass.simpleName} - ${e.message}", e)
            bubbleView = null
            errorMessage = "Overlay error: ${e.localizedMessage}"
        }
    }

    private fun expandPanel() {
        val canDraw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.provider.Settings.canDrawOverlays(this)
        } else {
            true
        }
        if (!canDraw) {
            Log.e(TAG, "[Overlay] Cannot expand panel: SYSTEM_ALERT_WINDOW permission missing")
            return
        }

        isExpanded = true
        Log.d(TAG, "[Overlay] Expanding scanner panel...")
        try {
            if (bubbleView != null) {
                if (bubbleView?.isAttachedToWindow == true) {
                    windowManager.removeView(bubbleView)
                }
                bubbleView = null
                Log.d(TAG, "[Overlay] Bubble view removed and cleared for panel expansion")
            }
        } catch (e: Exception) {
            Log.w(TAG, "[Overlay] Exception removing bubbleView: ${e.message}")
            bubbleView = null
        }

        try {
            if (panelView != null) {
                if (panelView?.isAttachedToWindow == true) {
                    windowManager.removeView(panelView)
                }
                panelView = null
            }
            // Always build a fresh panel view so ComposeView, its ViewTree owners,
            // and input event channels are freshly initialized for WindowManager
            buildPanelView()
            windowManager.addView(panelView, panelParams)
            Log.i(TAG, "[Overlay] Floating panel view added to WindowManager")
        } catch (e: Exception) {
            Log.e(TAG, "[Overlay] Error expanding panel: ${e.javaClass.simpleName} - ${e.message}", e)
            panelView = null
        }
    }

    private fun collapsePanel() {
        isExpanded = false
        Log.d(TAG, "[Overlay] Collapsing scanner panel to bubble...")
        try {
            if (panelView != null) {
                if (panelView?.isAttachedToWindow == true) {
                    windowManager.removeView(panelView)
                }
                panelView = null
                Log.d(TAG, "[Overlay] Panel view removed and cleanly disposed")
            }
        } catch (e: Exception) {
            Log.w(TAG, "[Overlay] Exception removing panelView: ${e.message}")
            panelView = null
        }

        try {
            if (bubbleView?.isAttachedToWindow != true) {
                bubbleView = buildBubbleView()
                windowManager.addView(bubbleView, bubbleParams)
                Log.i(TAG, "[Overlay] Fresh bubble view restored to WindowManager")
            }
        } catch (e: Exception) {
            Log.e(TAG, "[Overlay] Error restoring bubble view: ${e.javaClass.simpleName} - ${e.message}", e)
            bubbleView = null
        }
    }

    private fun triggerScanRequest() {
        Log.i(TAG, "[Touch] triggerScanRequest called. Current isScanning=$isScanning")
        if (isScanning) return

        if (!repository.isBackendConfigured()) {
            Log.w(TAG, "[Backend] Scan blocked: backend URL is not configured")
            currentResult = null
            errorMessage = getString(R.string.error_backend_not_configured_detail)
            return
        }

        isScanning = true
        errorMessage = null
        currentResult = null

        try {
            val intent = Intent(this, CaptureConsentActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            }
            val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                try {
                    val bOptions = android.app.ActivityOptions.makeBasic()
                    bOptions.setPendingIntentBackgroundActivityStartMode(
                        android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                    )
                    bOptions.toBundle()
                } catch (e: Throwable) {
                    Log.w(TAG, "[MediaProjection] setPendingIntentBackgroundActivityStartMode warning: ${e.message}")
                    null
                }
            } else {
                null
            }

            val pendingIntent = PendingIntent.getActivity(
                this,
                101,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            try {
                if (options != null) {
                    pendingIntent.send(this, 0, null, null, null, null, options)
                } else {
                    pendingIntent.send()
                }
                Log.i(TAG, "[MediaProjection] Dispatched CaptureConsentActivity via PendingIntent with BAL opt-in")
            } catch (e: Exception) {
                Log.w(TAG, "[MediaProjection] PendingIntent send failed; fallback to startActivity: ${e.message}")
                if (options != null) {
                    startActivity(intent, options)
                } else {
                    startActivity(intent)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[MediaProjection] Failed to launch CaptureConsentActivity: ${e.message}", e)
            isScanning = false
            errorMessage = "Could not request screen capture permission: ${e.localizedMessage ?: "Unknown error"}"
        }
    }

    private fun executeScreenScan(resultCode: Int, data: Intent) {
        scanJob?.cancel()
        scanJob = serviceScope.launch {
            try {
                isScanning = true
                errorMessage = null

                Log.d(TAG, "[MediaProjection-FGS] Initiating screen capture through ScreenCaptureManager...")
                val captureResult = screenCaptureManager?.captureScreenBase64(resultCode, data)
                    ?: Result.failure(IllegalStateException("ScreenCaptureManager is not initialized."))

                // Revert FGS type back to standard overlay mode after capture completes
                startForegroundSafely(isCapturing = false)

                if (captureResult.isFailure) {
                    val captureErr = captureResult.exceptionOrNull()?.localizedMessage
                        ?: "Could not capture chart screenshot. Please retry."
                    Log.e(TAG, "[MediaProjection-FGS] Screenshot capture failed: $captureErr")
                    errorMessage = captureErr
                    return@launch
                }

                val base64Image = captureResult.getOrThrow()

                // Send chart screenshot to secure Gemini AI backend proxy
                Log.d(TAG, "[Analysis] Sending chart screenshot to Gemini backend proxy...")
                val analysisResult = repository.analyzeChartScreenshot(base64Image)
                if (analysisResult.isSuccess) {
                    currentResult = analysisResult.getOrThrow()
                    Log.i(TAG, "[Analysis] Scan analysis succeeded: ${currentResult?.signal} (${currentResult?.confidence}%)")
                } else {
                    errorMessage = analysisResult.exceptionOrNull()?.localizedMessage
                        ?: "Failed to analyze chart with AI."
                    Log.e(TAG, "[Analysis] Scan analysis failed: $errorMessage")
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "[MediaProjection-FGS] SecurityException during executeScreenScan: ${e.message}", e)
                errorMessage = "Screen capture permission denied or restricted: ${e.localizedMessage ?: "SecurityException"}"
                // DO NOT stopSelf()! Revert FGS type safely and keep overlay bubble alive
                startForegroundSafely(isCapturing = false)
            } catch (e: Exception) {
                Log.e(TAG, "[MediaProjection-FGS] Error during executeScreenScan: ${e.message}", e)
                errorMessage = e.localizedMessage ?: "Unknown scanning error occurred."
                startForegroundSafely(isCapturing = false)
            } finally {
                isScanning = false
            }
        }
    }

    private fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        isScanning = false
        try {
            startForegroundSafely(isCapturing = false)
        } catch (e: Exception) {
            Log.w(TAG, "Error resetting foreground service in stopScan: ${e.message}")
        }
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
            if (bubbleView != null) {
                if (bubbleView?.isAttachedToWindow == true) {
                    windowManager.removeViewImmediate(bubbleView)
                }
                bubbleView = null
            }
            if (panelView != null) {
                if (panelView?.isAttachedToWindow == true) {
                    windowManager.removeViewImmediate(panelView)
                }
                panelView = null
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing views on destroy: ${e.message}", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
