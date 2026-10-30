# Floating PRO Scanner — Native Android Project

A complete, production-grade native Android application built with **Kotlin** and **Jetpack Compose**. It provides an AI-powered candlestick chart scanner with a floating draggable overlay bubble over third-party trading apps (such as Quotex demo charts), real-time radar scanning animation, MediaProjection user-consented screen capture, and secure backend Gemini vision analysis.

---

## 📱 Features & Architecture

1. **Jetpack Compose UI**:
   - Recreates all 9 screens from the reference design.
   - Futuristic `#00F59B` neon-green and pitch-black `#05080A` trading aesthetic.
   - Animated glowing circular `PRO` emblem with radial sweep.
   - Bilingual support (**English** & **বাংলা** Bangla) configured in `res/values/strings.xml` and `res/values-bn/strings.xml`.

2. **Overlay & Foreground Service (Android 14+ Ready)**:
   - `FloatingScannerService` manages `TYPE_APPLICATION_OVERLAY` floating bubble and expandable panel.
   - Declares and promotes to `FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION` when screen capture starts.
   - Registers `MediaProjection.Callback` before creating `VirtualDisplay` to strictly comply with Android 14 (API 34) and Android 15 (API 35) security rules.
   - Draggable gesture tracking with `MotionEvent.ACTION_MOVE` and `WindowManager`.

3. **Screen Capture & AI Analysis**:
   - `ScreenCaptureManager` uses Android `MediaProjection` with explicit user consent.
   - Captures only when the user taps **Scan**.
   - Includes **Stop** and **Close** controls.
   - Secure backend proxy (`/api/analyze`) using Gemini 3.8 Flash (`gemini-3.8-flash`) via the modern `@google/genai` TypeScript SDK.
   - **Zero Secret Leaks**: No private Gemini API key is hardcoded or bundled inside the APK.
   - Returns structured `UP`, `DOWN`, or `NO_TRADE` assessments with confidence %, summary points, support/resistance, uncertainty, and risk warnings.
   - Explicit disclaimer that assessments are observational and not financial advice. No trading automation or login scraping.

---

## 🛠️ How to Build & Download the APK on an Android Phone

### Method 1: Automated 1-Click Cloud APK Build via GitHub Actions (Recommended)

1. Push this repository to your GitHub account.
2. The workflow file `.github/workflows/build-apk.yml` is already included in this repository.
3. On your Android phone, open Chrome or your preferred browser and navigate to your GitHub repository.
4. Tap the **"Actions"** tab.
5. Tap the **"Build Floating PRO Scanner APK"** workflow run (or trigger manually via `workflow_dispatch`).
6. Once the build completes (~2 minutes) with a green checkmark:
   - Scroll down to the **"Artifacts"** section.
   - Tap **`FloatingPROScanner-v1.0.0-debug`** to download the zip file containing `app-debug.apk`.
7. Open your phone's File Manager &gt; Downloads, extract the zip, and tap `app-debug.apk` to install!

---

### Method 2: Build Directly on Phone via Termux (No PC Required)

1. Install **Termux** from F-Droid (do not use Google Play Store version).
2. Install OpenJDK 17, Git, and build tools:
   ```bash
   pkg update && pkg upgrade -y
   pkg install openjdk-17 git wget unzip -y
   export JAVA_HOME=/data/data/com.termux/files/usr/lib/jvm/java-17-openjdk
   ```
3. Navigate to the `android` folder:
   ```bash
   cd ~/floating-pro-scanner/android
   chmod +x gradlew
   ./gradlew assembleDebug
   ```
4. Copy the generated APK to your phone's Downloads folder:
   ```bash
   cp app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/FloatingPROScanner.apk
   ```
5. Open your File Manager and tap `FloatingPROScanner.apk` to install!

---

### Method 3: Android Studio (Standard Developer Build)

1. Open Android Studio (Koala / Ladybug or newer).
2. Select **File > Open** and choose the `/android` directory.
3. Allow Gradle sync to complete (uses Gradle 8.7 and Kotlin 2.0.20).
4. Connect your Android phone via USB with **USB Debugging** enabled (or use an emulator).
5. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)** or press **Run (Shift + F10)**.

---

## 🔒 Permissions & Setup on Android Device

Once installed on your phone:
1. Open **Floating PRO Scanner**.
2. Tap **"Display over other apps"** in Settings or Home dashboard:
   - Android will open system settings. Toggle **Allow display over other apps** to ON.
3. Tap **"START SCANNER"**:
   - Android will prompt: *"Start recording or casting with Floating PRO Scanner?"* Tap **"Start now"**.
4. The circular glowing **PRO** bubble will appear floating over your screen!
5. Open your preferred trading or chart application (e.g. Quotex demo account).
6. Move the floating bubble anywhere you like. Tap it to expand the scan panel, and tap **"Scan"** to analyze the active chart with Gemini AI!

---

## ⚠️ Integrity & Limitation Notes

- **Real Source Code**: The entire Kotlin and Jetpack Compose project files are real, structured, and compliant with Android 14/15 standards.
- **APK Testing Status**: In this container environment without Android SDK build-tools pre-installed, local `gradlew assembleDebug` cannot run unless Android SDK packages (`platforms;android-35`, `build-tools;35.0.0`) are downloaded. The APK has not been pre-compiled in this container; use Method 1, 2, or 3 above to produce the signed/debug binary.
- **Fair Play Notice**: This application does **never** automate trading, clicks, or orders on any platform. It is an AI visual chart interpreter only.
