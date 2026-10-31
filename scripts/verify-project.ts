import fs from 'fs';
import path from 'path';

interface TestResult {
  name: string;
  category: string;
  status: 'PASS' | 'FAIL';
  details: string;
}

const results: TestResult[] = [];

function assert(condition: boolean, name: string, category: string, details: string) {
  if (condition) {
    results.push({ name, category, status: 'PASS', details });
  } else {
    results.push({ name, category, status: 'FAIL', details });
  }
}

async function runVerification() {
  console.log('=== Running Automated Project & Backend Verification ===\n');

  const rootDir = process.cwd();

  // Test 1: Android Manifest Permissions & Structure
  const manifestPath = path.join(rootDir, 'android/app/src/main/AndroidManifest.xml');
  const manifestExists = fs.existsSync(manifestPath);
  assert(manifestExists, 'AndroidManifest.xml exists', 'Android Manifest', 'Path: ' + manifestPath);

  if (manifestExists) {
    const manifestContent = fs.readFileSync(manifestPath, 'utf-8');
    assert(
      manifestContent.includes('android.permission.SYSTEM_ALERT_WINDOW'),
      'Overlay Permission SYSTEM_ALERT_WINDOW declared',
      'Android Manifest',
      'Required for floating PRO bubble'
    );
    assert(
      manifestContent.includes('android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION'),
      'FOREGROUND_SERVICE_MEDIA_PROJECTION declared',
      'Android Manifest',
      'Android 14+ requirement for screen capture FGS'
    );
    assert(
      manifestContent.includes('mediaProjection') && manifestContent.includes('specialUse'),
      'FloatingScannerService foregroundServiceType set (specialUse|mediaProjection)',
      'Android Manifest',
      'Required for MediaProjection and overlay in service'
    );
    assert(
      manifestContent.includes('android.permission.POST_NOTIFICATIONS'),
      'POST_NOTIFICATIONS permission declared',
      'Android Manifest',
      'Required for Android 13+ notifications'
    );
  }

  // Test 2: Resource and Icon Assets
  const iconPaths = [
    'android/app/src/main/res/drawable/ic_launcher_background.xml',
    'android/app/src/main/res/drawable/ic_launcher_foreground.xml',
    'android/app/src/main/res/drawable/ic_notification.xml',
    'android/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml',
    'android/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml',
  ];

  for (const p of iconPaths) {
    const fullP = path.join(rootDir, p);
    assert(fs.existsSync(fullP), `Asset ${path.basename(p)} exists`, 'Resources', 'Avoids AAPT2 build failures');
  }

  // Test 3: Bilingual Strings Localization
  const enStringsPath = path.join(rootDir, 'android/app/src/main/res/values/strings.xml');
  const bnStringsPath = path.join(rootDir, 'android/app/src/main/res/values-bn/strings.xml');
  assert(fs.existsSync(enStringsPath), 'English strings.xml exists', 'Localization', 'res/values/strings.xml');
  assert(fs.existsSync(bnStringsPath), 'Bangla values-bn/strings.xml exists', 'Localization', 'res/values-bn/strings.xml');

  if (fs.existsSync(enStringsPath) && fs.existsSync(bnStringsPath)) {
    const en = fs.readFileSync(enStringsPath, 'utf-8');
    const bn = fs.readFileSync(bnStringsPath, 'utf-8');
    assert(
      en.includes('signal_up') && bn.includes('signal_up'),
      'signal_up localized in English & Bangla',
      'Localization',
      'Bilingual trading signals verified'
    );
    assert(
      en.includes('signal_no_trade') && bn.includes('signal_no_trade'),
      'signal_no_trade localized in English & Bangla',
      'Localization',
      'NO TRADE key verified in both files'
    );
    assert(
      en.includes('evidence_title') && bn.includes('evidence_title'),
      'evidence_title localized in English & Bangla',
      'Localization',
      'Visible evidence header verified in both files'
    );
    assert(
      en.includes('uncertainty_title') && bn.includes('uncertainty_title'),
      'uncertainty_title localized in English & Bangla',
      'Localization',
      'Uncertainty & missing data header verified in both files'
    );
  }

  // Test 3.1: Kotlin Data Model Integrity
  const modelPath = path.join(rootDir, 'android/app/src/main/java/com/proscanner/app/data/model/ScanResult.kt');
  if (fs.existsSync(modelPath)) {
    const modelContent = fs.readFileSync(modelPath, 'utf-8');
    assert(
      modelContent.includes('val evidence: List<String>') &&
      modelContent.includes('val reasoning: String') &&
      modelContent.includes('val uncertainty: List<String>'),
      'ScanResult.kt contains structured evidence, reasoning, uncertainty',
      'Data Model',
      'Clean schema alignment with decision engine'
    );
  }

  // Test 4: Kotlin Architecture & Android 14 Compliance
  const servicePath = path.join(rootDir, 'android/app/src/main/java/com/proscanner/app/service/FloatingScannerService.kt');
  if (fs.existsSync(servicePath)) {
    const serviceContent = fs.readFileSync(servicePath, 'utf-8');
    assert(
      serviceContent.includes('ViewModelStoreOwner') && serviceContent.includes('setViewTreeViewModelStoreOwner'),
      'FloatingScannerService implements ViewModelStoreOwner',
      'Compose in Service',
      'Prevents Compose runtime crash in Android Service window'
    );
    assert(
      serviceContent.includes('ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION'),
      'startForeground specifies FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION',
      'Android 14+ FGS',
      'Complies with Android 14 MediaProjection requirements'
    );
  }

  const capturePath = path.join(rootDir, 'android/app/src/main/java/com/proscanner/app/service/ScreenCaptureManager.kt');
  if (fs.existsSync(capturePath)) {
    const captureContent = fs.readFileSync(capturePath, 'utf-8');
    assert(
      captureContent.includes('registerCallback'),
      'ScreenCaptureManager registers MediaProjection.Callback',
      'Android 14+ Capture',
      'Avoids IllegalStateException in createVirtualDisplay on Android 14'
    );
  }

  // Test 5: Secret Protection & Backend Proxy
  const apiServicePath = path.join(rootDir, 'android/app/src/main/java/com/proscanner/app/data/api/GeminiAnalysisService.kt');
  if (fs.existsSync(apiServicePath)) {
    const apiContent = fs.readFileSync(apiServicePath, 'utf-8');
    assert(
      !apiContent.includes('AIzaSy') && !apiContent.includes('GEMINI_API_KEY = "'),
      'No hardcoded API keys in GeminiAnalysisService.kt',
      'Security',
      'API key remains server-side only'
    );
    assert(
      apiContent.includes('/api/analyze'),
      'GeminiAnalysisService calls backend /api/analyze proxy',
      'Security',
      'Calls backend proxy endpoint'
    );
  }

  // Test 6: Gradle Scripts & GitHub Actions Workflow
  const gradlewPath = path.join(rootDir, 'android/gradlew');
  assert(fs.existsSync(gradlewPath), 'android/gradlew shell script exists', 'Gradle Build', 'Required for CI/CD and Termux');

  const workflowPath = path.join(rootDir, '.github/workflows/build-apk.yml');
  const workflowExists = fs.existsSync(workflowPath);
  assert(workflowExists, 'GitHub Actions workflow exists', 'CI/CD', '.github/workflows/build-apk.yml');

  if (workflowExists) {
    const workflowContent = fs.readFileSync(workflowPath, 'utf-8');
    assert(
      workflowContent.includes('assembleDebug'),
      'Workflow builds assembleDebug target',
      'CI/CD',
      'Builds app-debug.apk'
    );
    assert(
      workflowContent.includes('upload-artifact'),
      'Workflow uploads app-debug.apk artifact',
      'CI/CD',
      'Enables direct phone download from GitHub Actions'
    );
  }

  // Print Summary Table
  console.log('----------------------------------------------------------------------------------------');
  console.log('| Status | Category               | Test Name                                            |');
  console.log('----------------------------------------------------------------------------------------');
  let passCount = 0;
  let failCount = 0;
  for (const r of results) {
    const statusFormatted = r.status === 'PASS' ? '✅ PASS' : '❌ FAIL';
    console.log(`| ${statusFormatted} | ${r.category.padEnd(22)} | ${r.name.padEnd(52)} |`);
    if (r.status === 'PASS') passCount++;
    else failCount++;
  }
  console.log('----------------------------------------------------------------------------------------');
  console.log(`\nTotal: ${results.length} | Passed: ${passCount} | Failed: ${failCount}`);

  if (failCount > 0) {
    process.exit(1);
  }
}

runVerification().catch(err => {
  console.error('Test runner failure:', err);
  process.exit(1);
});
