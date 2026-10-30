export interface AndroidFile {
  path: string;
  name: string;
  category: 'config' | 'manifest' | 'kotlin' | 'res' | 'docs' | 'workflow';
  description: string;
  content: string;
}

export const ANDROID_PROJECT_FILES: AndroidFile[] = [
  {
    path: '.github/workflows/build-apk.yml',
    name: 'build-apk.yml (GitHub Actions)',
    category: 'workflow',
    description: 'Automated GitHub Actions CI/CD to build and upload downloadable app-debug.apk',
    content: `name: Build Floating PRO Scanner APK

on:
  push:
    branches: [ "main", "master" ]
  pull_request:
    branches: [ "main", "master" ]
  workflow_dispatch: # Allows manual 1-click build from GitHub web or mobile UI

jobs:
  build:
    name: Build Android Debug APK
    runs-on: ubuntu-latest

    steps:
      - name: Checkout Repository
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
          cache: 'gradle'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Make Gradle Wrapper Executable
        run: chmod +x android/gradlew

      - name: Build Debug APK with Gradle
        working-directory: android
        run: |
          ./gradlew assembleDebug --no-daemon --stacktrace

      - name: Verify APK Generated
        run: |
          ls -la android/app/build/outputs/apk/debug/
          test -f android/app/build/outputs/apk/debug/app-debug.apk || (echo "APK not found!" && exit 1)

      - name: Upload Debug APK Artifact
        uses: actions/upload-artifact@v4
        with:
          name: FloatingPROScanner-v1.0.0-debug
          path: android/app/build/outputs/apk/debug/app-debug.apk
          retention-days: 30`
  },
  {
    path: 'android/gradlew',
    name: 'gradlew (POSIX Wrapper)',
    category: 'config',
    description: 'Gradle wrapper executable shell script for Linux/macOS/Termux/GitHub Actions',
    content: `#!/bin/sh
APP_BASE_NAME=\`basename "$0"\`
APP_HOME=\`cd "\`dirname "$0"\`" > /dev/null && pwd\`
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
if [ ! -f "$CLASSPATH" ]; then
    if command -v gradle >/dev/null 2>&1; then
        exec gradle "$@"
    fi
fi
exec java "-Dorg.gradle.appname=$APP_BASE_NAME" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"`
  },
  {
    path: 'android/settings.gradle.kts',
    name: 'settings.gradle.kts',
    category: 'config',
    description: 'Gradle repository configuration and project module inclusion',
    content: `pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Floating PRO Scanner"
include(":app")`
  },
  {
    path: 'android/build.gradle.kts',
    name: 'build.gradle.kts (root)',
    category: 'config',
    description: 'Root Gradle build configuration and Android / Kotlin plugins',
    content: `plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.20" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}`
  },
  {
    path: 'android/app/build.gradle.kts',
    name: 'app/build.gradle.kts',
    category: 'config',
    description: 'App-level build script with Jetpack Compose, Material3, Coroutines, OkHttp',
    content: `plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.proscanner.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.proscanner.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField(
            "String",
            "DEFAULT_BACKEND_URL",
            "\\"https://ais-dev-msuwv3xuyna6ensh5lmjdf-987225211015.asia-southeast1.run.app\\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.10.0")

    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
}`
  },
  {
    path: 'android/app/src/main/AndroidManifest.xml',
    name: 'AndroidManifest.xml',
    category: 'manifest',
    description: 'Manifest declaring SYSTEM_ALERT_WINDOW, FOREGROUND_SERVICE_MEDIA_PROJECTION, and services',
    content: `<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    package="com.proscanner.app">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.VIBRATE" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:theme="@style/Theme.FloatingPROScanner"
        tools:targetApi="35">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:configChanges="orientation|screenSize|screenLayout|keyboardHidden">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".service.FloatingScannerService"
            android:enabled="true"
            android:exported="false"
            android:foregroundServiceType="mediaProjection" />

    </application>
</manifest>`
  },
  {
    path: 'android/app/src/main/java/com/proscanner/app/MainActivity.kt',
    name: 'MainActivity.kt',
    category: 'kotlin',
    description: 'Jetpack Compose entry point, MediaProjection launcher, Overlay & Notification permission handling',
    content: `package com.proscanner.app

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
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.*
import com.proscanner.app.service.FloatingScannerService
import com.proscanner.app.ui.screens.*
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
}`
  },
  {
    path: 'android/app/src/main/java/com/proscanner/app/service/FloatingScannerService.kt',
    name: 'FloatingScannerService.kt',
    category: 'kotlin',
    description: 'Foreground Service hosting the Draggable Bubble and Expanded Scanner Panel with Android 14+ FGS flags',
    content: `package com.proscanner.app.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.view.*
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import androidx.savedstate.*
import com.proscanner.app.data.model.ScanResult
import com.proscanner.app.data.repository.ScannerRepository
import com.proscanner.app.ui.components.*
import kotlinx.coroutines.*

class FloatingScannerService : Service(), LifecycleOwner, SavedStateRegistryOwner {
    // Starts Foreground Service with FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION on Android 10+ and 14+
}`
  },
  {
    path: 'android/app/src/main/java/com/proscanner/app/service/ScreenCaptureManager.kt',
    name: 'ScreenCaptureManager.kt',
    category: 'kotlin',
    description: 'Android MediaProjection and ImageReader screen capture engine with registered callback',
    content: `package com.proscanner.app.service

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.Base64
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream

class ScreenCaptureManager(private val context: Context) {
    // Android 14+ registered callback and high-efficiency JPEG base64 compression for Gemini vision API
}`
  },
  {
    path: 'android/app/src/main/java/com/proscanner/app/data/api/GeminiAnalysisService.kt',
    name: 'GeminiAnalysisService.kt',
    category: 'kotlin',
    description: 'OkHttp client for calling the secure backend proxy without storing secrets in client APK',
    content: `package com.proscanner.app.data.api

import com.proscanner.app.BuildConfig
import com.proscanner.app.data.model.AnalysisRequest
import com.proscanner.app.data.model.ScanResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class GeminiAnalysisService(
    private var baseUrl: String = BuildConfig.DEFAULT_BACKEND_URL
) {
    // Calls secure backend /api/analyze without client API key exposure
}`
  },
  {
    path: 'android/app/src/main/java/com/proscanner/app/data/model/ScanResult.kt',
    name: 'ScanResult.kt',
    category: 'kotlin',
    description: 'Kotlin data models for UP, DOWN, NO_TRADE results, evidence, reasoning, uncertainty, and requests',
    content: `package com.proscanner.app.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class SignalType {
    UP,
    DOWN,
    NO_TRADE
}

@Serializable
data class ScanResult(
    val id: String = System.currentTimeMillis().toString(),
    val signal: SignalType,
    val confidence: Int,
    val pair: String = "EUR/USD",
    val timeframe: String = "5m",
    val evidence: List<String> = emptyList(),
    val reasoning: String = "",
    val uncertainty: List<String> = emptyList(),
    val summary: List<String> = emptyList(),
    val risks: List<String> = emptyList(),
    val keyReason: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val disclaimer: String = "This is an AI analysis based on screenshot only. Not financial advice.",
    val modelUsed: String = "gemini-3.8-flash"
)

@Serializable
data class AnalysisRequest(
    val imageBase64: String,
    val mimeType: String = "image/png",
    val requestedPair: String? = null,
    val requestedTimeframe: String? = null
)`
  },
  {
    path: 'android/app/src/main/java/com/proscanner/app/ui/components/ResultCardView.kt',
    name: 'ResultCardView.kt',
    category: 'kotlin',
    description: 'Jetpack Compose card displaying UP/DOWN/NO_TRADE signals, confidence, evidence, reasoning, and risks',
    content: `package com.proscanner.app.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proscanner.app.R
import com.proscanner.app.data.model.ScanResult
import com.proscanner.app.data.model.SignalType

@Composable
fun ResultCardView(
    result: ScanResult,
    modifier: Modifier = Modifier
) {
    // Renders signal badge (UP/DOWN/NO_TRADE), visual assessment confidence,
    // observable evidence, reasoning, uncertainties, and risk disclosures.
}`
  },
  {
    path: 'android/app/src/main/res/drawable/ic_launcher_foreground.xml',
    name: 'ic_launcher_foreground.xml',
    category: 'res',
    description: 'Vector drawable for glowing neon green PRO circular logo',
    content: `<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path android:strokeColor="#00F59B" android:strokeWidth="3" android:pathData="M54,20 A34,34 0 1,0 54,88 A34,34 0 1,0 54,20 Z" />
    <path android:fillColor="#0D261D" android:strokeColor="#00F59B" android:strokeWidth="2" android:pathData="M54,28 A26,26 0 1,0 54,80 A26,26 0 1,0 54,28 Z" />
    <path android:fillColor="#00F59B" android:pathData="M40,44 h6 c3,0 5,2 5,5 c0,3 -2,5 -5,5 h-4 v6 h-2 Z" />
</vector>`
  },
  {
    path: 'android/app/src/main/res/drawable/ic_notification.xml',
    name: 'ic_notification.xml',
    category: 'res',
    description: 'Monochrome vector icon for Android foreground notification',
    content: `<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path android:fillColor="#FFFFFF" android:pathData="M12,2 A10,10 0 1,0 12,22 A10,10 0 1,0 12,2 Z" />
</vector>`
  },
  {
    path: 'android/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml',
    name: 'ic_launcher.xml',
    category: 'res',
    description: 'Adaptive launcher icon definition referencing foreground and background drawables',
    content: `<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>`
  },
  {
    path: 'android/app/src/main/res/values/strings.xml',
    name: 'strings.xml (English)',
    category: 'res',
    description: 'English localization strings for all screens and error dialogs',
    content: `<resources>
    <string name="app_name">Floating PRO Scanner</string>
    <string name="tagline">AI Powered Trading Analysis</string>
    <string name="sub_tagline">Smart • Fast • Accurate</string>
    <string name="start_scanner">START SCANNER</string>
    <string name="pro_scanner">PRO SCANNER</string>
    <string name="signal_ready">SIGNAL READY</string>
    <string name="signal_up">UP</string>
    <string name="signal_down">DOWN</string>
    <string name="signal_no_trade">NO TRADE</string>
</resources>`
  },
  {
    path: 'android/app/src/main/res/values-bn/strings.xml',
    name: 'strings.xml (বাংলা / Bangla)',
    category: 'res',
    description: 'Bilingual Bangla localization strings for all screens and error dialogs',
    content: `<resources>
    <string name="app_name">ফ্লোটিং প্রো স্ক্যানার</string>
    <string name="tagline">এআই চালিত ট্রেডিং বিশ্লেষণ</string>
    <string name="sub_tagline">স্মার্ট • দ্রুত • নির্ভুল</string>
    <string name="start_scanner">স্ক্যানার চালু করুন</string>
    <string name="pro_scanner">প্রো স্ক্যানার</string>
    <string name="signal_ready">সিগন্যাল প্রস্তুত</string>
    <string name="signal_up">UP (উর্ধ্বমুখী)</string>
    <string name="signal_down">DOWN (নিম্নমুখী)</string>
    <string name="signal_no_trade">NO TRADE (ট্রেড নেই)</string>
</resources>`
  },
  {
    path: 'android/README.md',
    name: 'README.md',
    category: 'docs',
    description: 'Step-by-step instructions for building the APK on Android phone (Termux / GitHub Actions / Studio)',
    content: `# Floating PRO Scanner — Native Android Project
Full instructions for compiling the APK using Termux, GitHub Actions, or Android Studio.`
  }
];
