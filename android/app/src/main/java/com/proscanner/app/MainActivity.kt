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

        setContent {
            FloatingPROScannerTheme {
                MainAppNavHost(viewModel = viewModel, activity = this)
            }
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
    val recentScans by viewModel.recentScans.collectAsState()
    val currentLang by viewModel.currentLanguage.collectAsState()
    val activeError by viewModel.activeError.collectAsState()
    val backendUrl by viewModel.backendUrl.collectAsState()

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

    // Activity Result Launcher for MediaProjection Screen Capture
    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            viewModel.setCaptureGranted(true)
            startFloatingService(context, result.resultCode, result.data!!)
        } else {
            viewModel.setCaptureGranted(false)
            viewModel.showError(ErrorType.PERMISSION_DENIED)
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

    fun requestScreenCaptureAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            requestOverlay()
            return
        }

        val projectionManager =
            context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
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
                    requestScreenCaptureAndStart()
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
                recentScans = recentScans,
                onStartScannerClick = {
                    requestScreenCaptureAndStart()
                },
                onRequestOverlayClick = { requestOverlay() },
                onRequestCaptureClick = { requestScreenCaptureAndStart() },
                onNavigateSettings = { navController.navigate("settings") },
                onNavigateHistory = { navController.navigate("history") }
            )
        }

        composable("settings") {
            SettingsScreen(
                isOverlayGranted = isOverlayGranted,
                isCaptureGranted = isCaptureGranted,
                isBackendConnected = isBackendOnline,
                currentLanguage = currentLang,
                backendUrl = backendUrl,
                onUpdateBackendUrl = { viewModel.updateBackendUrl(it) },
                onGrantOverlay = { requestOverlay() },
                onGrantCapture = { requestScreenCaptureAndStart() },
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

private fun startFloatingService(context: Context, resultCode: Int, data: Intent) {
    val serviceIntent = Intent(context, FloatingScannerService::class.java).apply {
        action = FloatingScannerService.ACTION_START
        putExtra(FloatingScannerService.EXTRA_RESULT_CODE, resultCode)
        putExtra(FloatingScannerService.EXTRA_DATA, data)
    }
    ContextCompat.startForegroundService(context, serviceIntent)
}
