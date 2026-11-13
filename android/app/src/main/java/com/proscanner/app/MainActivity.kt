package com.proscanner.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.proscanner.app.service.FloatingScannerService
import com.proscanner.app.ui.screens.ErrorType
import com.proscanner.app.ui.screens.FriendlyErrorScreen
import com.proscanner.app.ui.screens.HistoryScreen
import com.proscanner.app.ui.screens.HomeScreen
import com.proscanner.app.ui.screens.SettingsScreen
import com.proscanner.app.ui.screens.SplashScreen
import com.proscanner.app.ui.theme.FloatingPROScannerTheme
import com.proscanner.app.viewmodel.ScannerViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ScannerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.initPersistence(applicationContext)

        val destination = intent.getStringExtra("destination")
        if (!destination.isNullOrBlank()) {
            viewModel.setTargetDestination(destination)
        }

        setContent {
            FloatingPROScannerTheme {
                MainAppNavHost(viewModel = viewModel, activity = this)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val destination = intent.getStringExtra("destination")
        if (!destination.isNullOrBlank()) {
            viewModel.setTargetDestination(destination)
        }
    }

    override fun onResume() {
        super.onResume()
        // Check overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            viewModel.setOverlayGranted(Settings.canDrawOverlays(this))
        } else {
            viewModel.setOverlayGranted(true)
        }
        viewModel.refreshBackendStatus()
    }
}

@Composable
fun MainAppNavHost(
    viewModel: ScannerViewModel,
    activity: ComponentActivity
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    val isOverlayGranted by viewModel.isOverlayGranted.collectAsState()
    val isCaptureGranted by viewModel.isCaptureGranted.collectAsState()
    val isBackendOnline by viewModel.isBackendConnected.collectAsState()
    val isBackendConfigured by viewModel.isBackendConfigured.collectAsState()
    val backendStatusDetail by viewModel.backendStatusDetail.collectAsState()
    val recentScans by viewModel.recentScans.collectAsState()
    val currentLang by viewModel.currentLanguage.collectAsState()
    val activeError by viewModel.activeError.collectAsState()
    val backendUrl by viewModel.backendUrl.collectAsState()
    val targetDestination by viewModel.targetDestination.collectAsState()

    // Handle deep navigation when requested from the floating overlay
    LaunchedEffect(targetDestination) {
        targetDestination?.let { dest ->
            navController.navigate(dest) {
                launchSingleTop = true
            }
            viewModel.clearTargetDestination()
        }
    }

    // Notification permission launcher for Android 13+ (API 33+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Notification permission handled
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Activity Result Launcher for MediaProjection Screen Capture (optional test/pre-grant)
    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            viewModel.setCaptureGranted(true)
            Toast.makeText(context, "Screen capture permission granted", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.setCaptureGranted(false)
            Toast.makeText(context, context.getString(R.string.error_capture_cancelled), Toast.LENGTH_SHORT).show()
        }
    }

    // Overlay Permission launcher
    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val granted = Settings.canDrawOverlays(context)
            viewModel.setOverlayGranted(granted)
            if (!granted) {
                viewModel.showError(ErrorType.PERMISSION_DENIED)
            }
        }
    }

    fun requestOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            overlayPermissionLauncher.launch(intent)
        } else {
            viewModel.setOverlayGranted(true)
        }
    }

    fun requestScreenCaptureConsent() {
        val projectionManager =
            context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
        if (projectionManager != null) {
            mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
        }
    }

    fun startFloatingBubbleOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            requestOverlay()
            return
        }

        // Start floating overlay service independently without requiring MediaProjection upfront
        val serviceIntent = Intent(context, FloatingScannerService::class.java).apply {
            action = FloatingScannerService.ACTION_START
        }
        try {
            ContextCompat.startForegroundService(context, serviceIntent)
            Toast.makeText(
                context,
                "Starting Floating PRO Scanner...",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            e.printStackTrace()
            viewModel.showError(ErrorType.UNKNOWN)
        }
    }

    if (activeError != null) {
        FriendlyErrorScreen(
            errorType = activeError!!,
            onPrimaryAction = {
                val err = activeError
                viewModel.clearError()
                if (err == ErrorType.PERMISSION_DENIED) {
                    requestOverlay()
                } else {
                    startFloatingBubbleOverlay()
                }
            },
            onDismiss = { viewModel.clearError() }
        )
        return
    }

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeScreen(
                isOverlayGranted = isOverlayGranted,
                isCaptureGranted = isCaptureGranted,
                isBackendOnline = isBackendOnline,
                isBackendConfigured = isBackendConfigured,
                backendStatusDetail = backendStatusDetail,
                recentScans = recentScans,
                onStartScannerClick = {
                    startFloatingBubbleOverlay()
                },
                onRequestOverlayClick = { requestOverlay() },
                onRequestCaptureClick = { requestScreenCaptureConsent() },
                onNavigateSettings = { navController.navigate("settings") },
                onNavigateHistory = { navController.navigate("history") },
                onBackendCardClick = { navController.navigate("settings") }
            )
        }

        composable("settings") {
            SettingsScreen(
                isOverlayGranted = isOverlayGranted,
                isCaptureGranted = isCaptureGranted,
                isBackendConnected = isBackendOnline,
                isBackendConfigured = isBackendConfigured,
                backendStatusDetail = backendStatusDetail,
                currentLanguage = currentLang,
                backendUrl = backendUrl,
                onUpdateBackendUrl = { viewModel.updateBackendUrl(it) },
                onRefreshBackendStatus = { viewModel.refreshBackendStatus() },
                onGrantOverlay = { requestOverlay() },
                onGrantCapture = { requestScreenCaptureConsent() },
                onToggleLanguage = { viewModel.toggleLanguage() },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("history") {
            HistoryScreen(
                scans = recentScans,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
