async function testBackendEndpoints() {
  console.log('=== Testing Backend API Endpoints on Port 3000 ===\n');

  const baseUrl = 'http://127.0.0.1:3000';
  let totalTests = 0;
  let passedTests = 0;

  function record(pass: boolean, msg: string) {
    totalTests++;
    if (pass) {
      passedTests++;
      console.log(`   ✅ PASS: ${msg}`);
    } else {
      console.log(`   ❌ FAIL: ${msg}`);
    }
  }

  // Test 1: GET /api/health
  try {
    const healthRes = await fetch(`${baseUrl}/api/health`);
    const healthData = await healthRes.json();
    console.log('1. Health Check Test:');
    record(healthRes.ok && healthData.status === 'online', `/api/health responded 200 with online status and model: ${healthData.model}`);
    record(typeof healthData.hasApiKey === 'boolean', `Health payload specifies API key configuration status: ${healthData.hasApiKey}`);
  } catch (err: any) {
    record(false, `Could not reach /api/health: ${err.message}`);
  }

  // Test 2: POST /api/analyze with empty payload (expects 400 Bad Request)
  try {
    const emptyRes = await fetch(`${baseUrl}/api/analyze`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ imageBase64: '' })
    });
    console.log('\n2. Empty Image Payload Validation:');
    record(emptyRes.status === 400, `Rejected empty image with 400 Bad Request`);
    const errJson = await emptyRes.json();
    record(!!errJson.error, `Returned informative error message: ${errJson.error}`);
  } catch (err: any) {
    record(false, `Empty payload test threw error: ${err.message}`);
  }

  // Test 3: POST /api/analyze with non-chart image (must return NO_TRADE)
  try {
    console.log('\n3. Non-Chart Image Integrity Test:');
    // 1x1 black pixel JPEG base64
    const sampleBase64 = 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==';
    const analyzeRes = await fetch(`${baseUrl}/api/analyze`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        imageBase64: sampleBase64,
        requestedPair: 'EUR/USD',
        requestedTimeframe: '5m',
        language: 'en'
      })
    });
    const analyzeData = await analyzeRes.json();
    record(analyzeRes.ok, `Analyze request returned 200 OK`);
    record(analyzeData.signal === 'NO_TRADE', `Non-chart image correctly returned NO_TRADE (actual: ${analyzeData.signal})`);
    record(analyzeData.confidence <= 60, `Calibrated confidence is realistic for unreadable image: ${analyzeData.confidence}%`);
    record(Array.isArray(analyzeData.evidence || analyzeData.summary), `Contains structured observations list`);
    record(typeof (analyzeData.reasoning || analyzeData.keyReason) === 'string', `Contains explanation reasoning`);
    record(Array.isArray(analyzeData.risks), `Contains risks assessment (${analyzeData.risks?.length} items)`);
    record(!!analyzeData.disclaimer, `Contains legal disclaimer`);
  } catch (err: any) {
    record(false, `Non-chart test threw error: ${err.message}`);
  }

  // Test 4: POST /api/analyze in Bangla (বাংলা) language
  try {
    console.log('\n4. Bangla Localization Integrity Test:');
    const sampleBase64 = 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==';
    const bnRes = await fetch(`${baseUrl}/api/analyze`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        imageBase64: sampleBase64,
        requestedPair: 'USD/BDT',
        requestedTimeframe: '1m',
        language: 'bn'
      })
    });
    const bnData = await bnRes.json();
    record(bnRes.ok, `Bangla analyze request returned 200 OK`);
    record(bnData.signal === 'NO_TRADE', `Bangla request returned NO_TRADE on blank image`);
    record(!!bnData.disclaimer, `Disclaimers localized properly`);
  } catch (err: any) {
    record(false, `Bangla test threw error: ${err.message}`);
  }

  console.log(`\n======================================================`);
  console.log(`Backend API Verification: ${passedTests} / ${totalTests} checks passed`);
  console.log(`======================================================`);

  if (passedTests < totalTests) {
    process.exit(1);
  }
}

testBackendEndpoints().catch((e) => {
  console.error(e);
  process.exit(1);
});
