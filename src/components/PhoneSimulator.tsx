import React, { useState, useEffect, useRef } from 'react';
import {
  Wifi,
  Battery,
  Shield,
  Layers,
  Camera,
  Play,
  RotateCcw,
  Settings as SettingsIcon,
  History as HistoryIcon,
  Home as HomeIcon,
  ArrowLeft,
  ChevronRight,
  CheckCircle,
  X,
  Minus,
  AlertTriangle,
  ArrowUp,
  ArrowDown,
  Globe,
  Radio,
  Sparkles,
  Maximize2,
  Upload,
  AlertOctagon,
  CloudOff,
  ImageOff
} from 'lucide-react';
import { AppScreen, Language, ScanResult, SignalType, FloatingState } from '../types';

interface PhoneSimulatorProps {
  currentScreen: AppScreen;
  setScreen: (screen: AppScreen) => void;
  language: Language;
  setLanguage: (lang: Language) => void;
  recentScans: ScanResult[];
  onAddScan: (scan: ScanResult) => void;
}

export const PhoneSimulator: React.FC<PhoneSimulatorProps> = ({
  currentScreen,
  setScreen,
  language,
  setLanguage,
  recentScans,
  onAddScan
}) => {
  // Permission state
  const [overlayGranted, setOverlayGranted] = useState(true);
  const [captureGranted, setCaptureGranted] = useState(true);
  const [backendOnline, setBackendOnline] = useState(true);

  // Floating widget state
  const [floatingState, setFloatingState] = useState<FloatingState>('bubble');
  const [bubblePos, setBubblePos] = useState({ x: 260, y: 160 });
  const [isDragging, setIsDragging] = useState(false);
  const [dragOffset, setDragOffset] = useState({ x: 0, y: 0 });

  // Scanner state
  const [isScanning, setIsScanning] = useState(false);
  const [activeResult, setActiveResult] = useState<ScanResult | null>(null);
  const [scanProgress, setScanProgress] = useState(0);
  const [scanMessage, setScanMessage] = useState('Analyzing chart patterns, trend, support & resistance...');
  const [chartPair, setChartPair] = useState('EUR/USD');
  const [chartTimeframe, setChartTimeframe] = useState('5m');
  const [activeTab, setActiveTab] = useState<'home' | 'history' | 'settings'>('home');

  // Quotex chart candlestick simulation
  const [candles, setCandles] = useState([
    { open: 1.0842, close: 1.0848, high: 1.0850, low: 1.0840, isGreen: true },
    { open: 1.0848, close: 1.0844, high: 1.0852, low: 1.0842, isGreen: false },
    { open: 1.0844, close: 1.0851, high: 1.0854, low: 1.0843, isGreen: true },
    { open: 1.0851, close: 1.0847, high: 1.0855, low: 1.0845, isGreen: false },
    { open: 1.0847, close: 1.0856, high: 1.0858, low: 1.0846, isGreen: true },
    { open: 1.0856, close: 1.0859, high: 1.0862, low: 1.0854, isGreen: true },
    { open: 1.0859, close: 1.0853, high: 1.0861, low: 1.0850, isGreen: false },
    { open: 1.0853, close: 1.0858, high: 1.0860, low: 1.0851, isGreen: true },
    { open: 1.0858, close: 1.0864, high: 1.0867, low: 1.0856, isGreen: true },
  ]);

  const fileInputRef = useRef<HTMLInputElement>(null);

  // Translations
  const t = {
    en: {
      appName: 'Floating PRO Scanner',
      tagline: 'AI Powered Trading Analysis',
      subTagline: 'Smart • Fast • Accurate',
      initializing: 'Initializing...',
      geminiAi: 'Gemini AI',
      secureBackend: 'Secure Backend',
      connected: 'Connected',
      floatingButton: 'Floating Button',
      floatingButtonDesc: 'Enable to show PRO button over other apps',
      screenCapture: 'Screen Capture',
      screenCaptureDesc: 'Allow to capture screen for analysis',
      active: 'Active',
      notActive: 'Not Active',
      granted: 'Granted',
      notGranted: 'Not Granted',
      startScanner: 'START SCANNER',
      recentAnalysis: 'Recent Analysis',
      viewAll: 'View All',
      home: 'Home',
      history: 'History',
      settings: 'Settings',
      permissions: 'Permissions',
      displayOverApps: 'Display over other apps',
      displayDesc: 'Required for floating button',
      grant: 'Grant',
      aiBackend: 'AI & Backend',
      geminiConnection: 'Gemini AI Connection',
      backendServer: 'Backend Server',
      online: 'Online',
      appPreferences: 'App Preferences',
      floatingStyle: 'Floating button style',
      neonGreen: 'Neon Green',
      notification: 'Notification',
      language: 'Language',
      about: 'About',
      proScanner: 'PRO SCANNER',
      tapToOpen: 'Tap to open',
      scan: 'Scan',
      stop: 'STOP',
      close: 'Close',
      scanningStatus: 'SCANNING...',
      scanningDesc: 'Analyzing chart patterns, trend, support & resistance...',
      poweredByGemini: 'Powered by Gemini AI',
      signalReady: 'SIGNAL READY',
      analysisCompleted: 'Analysis completed',
      up: 'UP',
      down: 'DOWN',
      noTrade: 'NO TRADE',
      confidence: 'Confidence',
      analysisSummary: 'Analysis Summary',
      evidenceTitle: 'Visible Evidence',
      uncertaintyTitle: 'Uncertainties & Missing Data',
      risks: 'Risks',
      keyReason: 'Key reason',
      disclaimer: 'This is an AI analysis based on screenshot only. Not financial advice.',
      errPermTitle: 'Permission Denied',
      errPermDesc: 'Screen capture or overlay permission is required to analyze trading charts.',
      errNoChartTitle: 'No Chart Detected',
      errNoChartDesc: 'Could not clearly detect candlestick chart or market timeframe. Please ensure the chart is fully visible.',
      errNetTitle: 'Network Failure',
      errNetDesc: 'Unable to reach the AI backend server. Please verify your internet connection.',
      errAiTitle: 'AI Analysis Error',
      errAiDesc: 'The AI vision model encountered an issue parsing this screenshot. Please retry with a clear view.',
      retry: 'Retry',
    },
    bn: {
      appName: 'ফ্লোটিং প্রো স্ক্যানার',
      tagline: 'এআই চালিত ট্রেডিং বিশ্লেষণ',
      subTagline: 'স্মার্ট • দ্রুত • নির্ভুল',
      initializing: 'শুরু হচ্ছে...',
      geminiAi: 'জেমিনি এআই',
      secureBackend: 'নিরাপদ ব্যাকএন্ড',
      connected: 'সংযুক্ত',
      floatingButton: 'ফ্লোটিং বোতাম',
      floatingButtonDesc: 'অন্য অ্যাপের উপরে প্রো বাটন চালু করুন',
      screenCapture: 'স্ক্রিন ক্যাপচার',
      screenCaptureDesc: 'চার্ট বিশ্লেষণের জন্য স্ক্রিন ক্যাপচারের অনুমতি দিন',
      active: 'সক্রিয়',
      notActive: 'নিষ্ক্রিয়',
      granted: 'অনুমোদিত',
      notGranted: 'অনুমতি নেই',
      startScanner: 'স্ক্যানার চালু করুন',
      recentAnalysis: 'সাম্প্রতিক বিশ্লেষণ',
      viewAll: 'সবগুলো দেখুন',
      home: 'হোম',
      history: 'ইতিহাস',
      settings: 'সেটিংস',
      permissions: 'অনুমতিসমূহ',
      displayOverApps: 'অন্যান্য অ্যাপের উপরে দেখান',
      displayDesc: 'ফ্লোটিং বাটনটির জন্য প্রয়োজনীয়',
      grant: 'অনুমতি দিন',
      aiBackend: 'এআই ও ব্যাকএন্ড',
      geminiConnection: 'জেমিনি এআই সংযোগ',
      backendServer: 'ব্যাকএন্ড সার্ভার',
      online: 'অনলাইন',
      appPreferences: 'অ্যাপ পছন্দসমূহ',
      floatingStyle: 'ফ্লোটিং বাটন স্টাইল',
      neonGreen: 'নিয়ন গ্রিন',
      notification: 'বিজ্ঞপ্তি',
      language: 'ভাষা',
      about: 'সম্পর্কে',
      proScanner: 'প্রো স্ক্যানার',
      tapToOpen: 'খুলতে ট্যাপ করুন',
      scan: 'স্ক্যান',
      stop: 'বন্ধ করুন',
      close: 'বন্ধ',
      scanningStatus: 'স্ক্যান চলছে...',
      scanningDesc: 'চার্ট প্যাটার্ন, ট্রেন্ড, সাপোর্ট ও রেজিস্ট্যান্স বিশ্লেষণ হচ্ছে...',
      poweredByGemini: 'জেমিনি এআই দ্বারা চালিত',
      signalReady: 'সিগন্যাল প্রস্তুত',
      analysisCompleted: 'বিশ্লেষণ সম্পন্ন হয়েছে',
      up: 'UP (উর্ধ্বমুখী)',
      down: 'DOWN (নিম্নমুখী)',
      noTrade: 'NO TRADE (ট্রেড নেই)',
      confidence: 'সম্ভাব্যতা',
      analysisSummary: 'বিশ্লেষণের সারসংক্ষেপ',
      evidenceTitle: 'দৃশ্যমান পর্যবেক্ষণ ও প্রমাণ',
      uncertaintyTitle: 'অনিশ্চয়তা ও অনুপস্থিত তথ্য',
      risks: 'ঝুঁকিসমূহ',
      keyReason: 'মূল কারণ',
      disclaimer: 'এটি শুধুমাত্র স্ক্রিনশটের উপর ভিত্তি করে এআই বিশ্লেষণ। আর্থিক পরামর্শ নয়।',
      errPermTitle: 'অনুমতি বাতিল হয়েছে',
      errPermDesc: 'ট্রেডিং চার্ট বিশ্লেষণের জন্য স্ক্রিন ক্যাপচার ও ওভারলে অনুমতি প্রয়োজন।',
      errNoChartTitle: 'কোন চার্ট সনাক্ত হয়নি',
      errNoChartDesc: 'ক্যান্ডেলস্টিক চার্ট স্পষ্টভাবে চিহ্নিত করা যায়নি। অনুগ্রহ করে সম্পূর্ণ চার্ট দৃশ্যমান রাখুন।',
      errNetTitle: 'নেটওয়ার্ক ত্রুটি',
      errNetDesc: 'এআই ব্যাকএন্ড সার্ভারের সাথে সংযোগ করা সম্ভব হয়নি। ইন্টারনেট সংযোগ পরীক্ষা করুন।',
      errAiTitle: 'এআই বিশ্লেষণ ত্রুটি',
      errAiDesc: 'স্ক্রিনশট বিশ্লেষণের সময় সমস্যা দেখা দিয়েছে। পরিষ্কার চার্ট সহ পুনরায় চেষ্টা করুন।',
      retry: 'পুনরায় চেষ্টা করুন',
    }
  }[language];

  // Drag handlers for floating bubble
  const handleMouseDown = (e: React.MouseEvent) => {
    setIsDragging(true);
    setDragOffset({
      x: e.clientX - bubblePos.x,
      y: e.clientY - bubblePos.y
    });
  };

  const handleMouseMove = (e: React.MouseEvent) => {
    if (!isDragging) return;
    const newX = Math.max(10, Math.min(290, e.clientX - dragOffset.x));
    const newY = Math.max(50, Math.min(520, e.clientY - dragOffset.y));
    setBubblePos({ x: newX, y: newY });
  };

  const handleMouseUp = () => {
    setIsDragging(false);
  };

  // Generate real visual candlestick snapshot from canvas for genuine vision evaluation
  const generateChartSnapshotBase64 = (): string => {
    try {
      const canvas = document.createElement('canvas');
      canvas.width = 440;
      canvas.height = 320;
      const ctx = canvas.getContext('2d');
      if (!ctx) return '';

      // Dark trading background
      ctx.fillStyle = '#090E14';
      ctx.fillRect(0, 0, canvas.width, canvas.height);

      // Grid lines
      ctx.strokeStyle = '#18232F';
      ctx.lineWidth = 1;
      for (let y = 40; y < canvas.height; y += 40) {
        ctx.beginPath();
        ctx.moveTo(0, y);
        ctx.lineTo(canvas.width, y);
        ctx.stroke();
      }

      // Title & Pair
      ctx.fillStyle = '#FFFFFF';
      ctx.font = 'bold 14px sans-serif';
      ctx.fillText(`${chartPair} • ${chartTimeframe} Quotex Demo Chart`, 16, 26);

      // Draw Candlesticks
      const candleWidth = 14;
      const spacing = (canvas.width - 60) / candles.length;
      const minPrice = 1.0838;
      const maxPrice = 1.0870;
      const priceRange = maxPrice - minPrice;

      candles.forEach((c, i) => {
        const x = 24 + i * spacing;
        const highY = canvas.height - 40 - ((c.high - minPrice) / priceRange) * (canvas.height - 80);
        const lowY = canvas.height - 40 - ((c.low - minPrice) / priceRange) * (canvas.height - 80);
        const openY = canvas.height - 40 - ((c.open - minPrice) / priceRange) * (canvas.height - 80);
        const closeY = canvas.height - 40 - ((c.close - minPrice) / priceRange) * (canvas.height - 80);

        const isGreen = c.close >= c.open;
        ctx.strokeStyle = isGreen ? '#00F59B' : '#FF3B56';
        ctx.fillStyle = isGreen ? '#00F59B' : '#FF3B56';

        // Wick
        ctx.lineWidth = 2;
        ctx.beginPath();
        ctx.moveTo(x + candleWidth / 2, highY);
        ctx.lineTo(x + candleWidth / 2, lowY);
        ctx.stroke();

        // Candle Body
        const bodyTop = Math.min(openY, closeY);
        const bodyHeight = Math.max(Math.abs(closeY - openY), 4);
        ctx.fillRect(x, bodyTop, candleWidth, bodyHeight);
      });

      return canvas.toDataURL('image/jpeg', 0.85);
    } catch {
      return '';
    }
  };

  // Perform AI scan using real server endpoint
  const executeScan = async (customBase64?: string) => {
    setIsScanning(true);
    setFloatingState('expanded_panel');
    setActiveResult(null);
    setScanProgress(15);
    setScanMessage(language === 'bn' ? 'স্ক্রিন ক্যাপচার ও প্রক্রিয়াকরণ হচ্ছে...' : 'Capturing chart and processing frames...');

    const payloadImage = customBase64 || generateChartSnapshotBase64();

    try {
      const timer = setInterval(() => {
        setScanProgress((prev) => (prev < 85 ? prev + 15 : prev));
      }, 350);

      const res = await fetch('/api/analyze', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          imageBase64: payloadImage,
          requestedPair: chartPair,
          requestedTimeframe: chartTimeframe,
          language
        })
      });

      clearInterval(timer);
      setScanProgress(100);

      if (!res.ok) {
        const errorData = await res.json().catch(() => ({}));
        throw new Error(errorData.error || 'Backend failed');
      }

      const data: ScanResult = await res.json();
      setActiveResult(data);
      onAddScan(data);
    } catch (err: any) {
      console.error(err);
      const msg = err?.message || '';
      if (msg.includes('No chart') || msg.includes('কোনো চার্ট') || msg.includes('candlestick')) {
        setScreen('error_no_chart');
      } else if (msg.includes('Gemini') || msg.includes('vision') || msg.includes('AI') || msg.includes('model') || msg.includes('503') || msg.includes('demand')) {
        setScreen('error_ai');
      } else {
        setScreen('error_network');
      }
    } finally {
      setIsScanning(false);
    }
  };

  // Handle local user image upload for analysis
  const handleImageUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (uploadEvent) => {
      const base64 = uploadEvent.target?.result as string;
      executeScan(base64);
    };
    reader.readAsDataURL(file);
  };

  return (
    <div
      className="flex flex-col items-center select-none"
      onMouseMove={handleMouseMove}
      onMouseUp={handleMouseUp}
    >
      {/* Hidden file input for uploading custom chart screenshots */}
      <input
        type="file"
        ref={fileInputRef}
        onChange={handleImageUpload}
        accept="image/*"
        className="hidden"
      />

      {/* Phone Hardware Frame */}
      <div className="relative w-[360px] h-[720px] bg-[#05080A] rounded-[48px] p-3 shadow-2xl shadow-[#00F59B]/10 border-4 border-[#1A262F] flex flex-col overflow-hidden">
        {/* Outer glowing rim */}
        <div className="absolute inset-0 rounded-[44px] pointer-events-none border border-[#00F59B]/20" />

        {/* Screen inner container */}
        <div className="relative w-full h-full bg-[#070B0E] rounded-[38px] overflow-hidden flex flex-col font-sans">
          
          {/* Top Status Bar (9:41, icons, camera notch) */}
          <div className="relative z-50 flex items-center justify-between px-6 pt-3 pb-1 text-xs text-slate-400">
            <span className="font-semibold text-white tracking-wider text-[11px]">9:41</span>
            
            {/* Camera Punch Hole */}
            <div className="absolute left-1/2 -translate-x-1/2 top-2.5 w-4 h-4 bg-black rounded-full border border-slate-800 flex items-center justify-center">
              <div className="w-1.5 h-1.5 bg-[#00F59B]/40 rounded-full" />
            </div>

            <div className="flex items-center gap-1.5 text-slate-300">
              <Wifi className="w-3.5 h-3.5" />
              <Battery className="w-3.5 h-3.5" />
            </div>
          </div>

          {/* SCREEN CONTENT SWITCHER */}
          <div className="flex-1 flex flex-col overflow-hidden relative">

            {/* 1. SPLASH SCREEN */}
            {currentScreen === 'splash' && (
              <div className="flex-1 flex flex-col items-center justify-center px-6 text-center animate-fadeIn">
                {/* Glowing PRO Logo */}
                <div className="relative flex items-center justify-center mb-8">
                  <div className="absolute w-36 h-36 rounded-full bg-[#00F59B]/15 animate-ping opacity-30" />
                  <div className="w-28 h-28 rounded-full border-2 border-[#00F59B] bg-gradient-to-br from-[#0c2e22] to-[#05080A] flex items-center justify-center shadow-[0_0_35px_#00F59B55]">
                    <div className="w-20 h-20 rounded-full border border-[#00F59B]/60 flex items-center justify-center">
                      <span className="text-2xl font-black text-[#00F59B] tracking-widest drop-shadow-[0_0_10px_#00F59B]">
                        PRO
                      </span>
                    </div>
                  </div>
                </div>

                <div className="flex items-center gap-1.5 text-xl font-bold text-white mb-2">
                  <span>Floating</span>
                  <span className="text-[#00F59B] font-extrabold drop-shadow-[0_0_8px_#00F59B]">PRO</span>
                  <span>Scanner</span>
                </div>
                <p className="text-xs text-slate-400 mb-1">{t.tagline}</p>
                <p className="text-[10px] text-slate-500 tracking-wider mb-8">{t.subTagline}</p>

                {/* Progress bar */}
                <div className="w-40 h-1 bg-slate-900 rounded-full overflow-hidden mb-3">
                  <div className="h-full bg-[#00F59B] rounded-full animate-pulse w-3/4" />
                </div>
                <span className="text-[10px] text-slate-500">{t.initializing}</span>

                <button
                  onClick={() => setScreen('home')}
                  className="mt-8 px-4 py-1.5 rounded-full bg-[#00F59B]/20 text-[#00F59B] text-xs font-semibold hover:bg-[#00F59B]/30 transition-all border border-[#00F59B]/40"
                >
                  Enter App →
                </button>
              </div>
            )}

            {/* 2. HOME SCREEN */}
            {currentScreen === 'home' && (
              <div className="flex-1 flex flex-col justify-between overflow-y-auto px-4 py-2 space-y-3 pb-16">
                {/* Header with mini PRO logo */}
                <div className="flex flex-col items-center text-center pt-2">
                  <div className="w-12 h-12 rounded-full border border-[#00F59B] bg-gradient-to-b from-[#0c2e22] to-[#05080A] flex items-center justify-center mb-1 shadow-[0_0_15px_#00F59B44]">
                    <span className="text-xs font-black text-[#00F59B]">PRO</span>
                  </div>
                  <div className="text-sm font-bold text-white flex items-center gap-1">
                    <span>Floating</span>
                    <span className="text-[#00F59B]">PRO</span>
                    <span>Scanner</span>
                  </div>
                  <span className="text-[10px] text-slate-400">{t.tagline}</span>
                </div>

                {/* Status Card: Gemini AI */}
                <div className="bg-[#0F151A] border border-[#1E2830] rounded-2xl p-3 flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-lg bg-[#00F59B]/15 flex items-center justify-center text-[#00F59B]">
                      <Sparkles className="w-4 h-4" />
                    </div>
                    <div>
                      <div className="text-xs font-bold text-white">{t.geminiAi}</div>
                      <div className="text-[10px] text-slate-400">{t.secureBackend}</div>
                    </div>
                  </div>
                  <div className="flex items-center gap-1 bg-[#0E3023] border border-[#00F59B]/30 px-2 py-0.5 rounded-full text-[10px] font-semibold text-[#00F59B]">
                    <span>✦</span> {t.connected}
                  </div>
                </div>

                {/* Permissions & Feature Cards */}
                <div className="bg-[#0F151A] border border-[#1E2830] rounded-2xl divide-y divide-[#172026]">
                  {/* Floating button item */}
                  <div
                    onClick={() => setOverlayGranted(!overlayGranted)}
                    className="p-3 flex items-center justify-between cursor-pointer hover:bg-slate-800/20 transition"
                  >
                    <div className="flex items-center gap-2.5">
                      <div className="w-7 h-7 rounded-lg bg-[#14201A] flex items-center justify-center text-[#00F59B]">
                        <Layers className="w-3.5 h-3.5" />
                      </div>
                      <div>
                        <div className="text-xs font-semibold text-white">{t.floatingButton}</div>
                        <div className="text-[10px] text-slate-400">{t.floatingButtonDesc}</div>
                      </div>
                    </div>
                    <div className="flex items-center gap-1">
                      <span className={`text-[10px] px-2 py-0.5 rounded-full font-semibold ${overlayGranted ? 'bg-[#0F261E] text-[#00F59B]' : 'bg-[#291719] text-[#FF3B56]'}`}>
                        {overlayGranted ? t.active : t.notActive}
                      </span>
                      <ChevronRight className="w-3.5 h-3.5 text-slate-600" />
                    </div>
                  </div>

                  {/* Screen capture item */}
                  <div
                    onClick={() => setCaptureGranted(!captureGranted)}
                    className="p-3 flex items-center justify-between cursor-pointer hover:bg-slate-800/20 transition"
                  >
                    <div className="flex items-center gap-2.5">
                      <div className="w-7 h-7 rounded-lg bg-[#14201A] flex items-center justify-center text-[#00F59B]">
                        <Camera className="w-3.5 h-3.5" />
                      </div>
                      <div>
                        <div className="text-xs font-semibold text-white">{t.screenCapture}</div>
                        <div className="text-[10px] text-slate-400">{t.screenCaptureDesc}</div>
                      </div>
                    </div>
                    <div className="flex items-center gap-1">
                      <span className={`text-[10px] px-2 py-0.5 rounded-full font-semibold ${captureGranted ? 'bg-[#0F261E] text-[#00F59B]' : 'bg-[#291719] text-[#FF3B56]'}`}>
                        {captureGranted ? t.granted : t.notGranted}
                      </span>
                      <ChevronRight className="w-3.5 h-3.5 text-slate-600" />
                    </div>
                  </div>
                </div>

                {/* Big Action: START SCANNER */}
                <button
                  onClick={() => {
                    if (!overlayGranted) {
                      setScreen('error_permission');
                      return;
                    }
                    setScreen('trading_overlay');
                  }}
                  className="w-full py-3.5 rounded-xl bg-[#00F59B] hover:bg-[#00c97f] active:scale-[0.98] text-[#05080A] font-extrabold text-xs tracking-wider flex items-center justify-center gap-2 shadow-[0_4px_20px_#00F59B55] transition-all cursor-pointer"
                >
                  <Play className="w-4 h-4 fill-current" />
                  {t.startScanner}
                </button>

                {/* Recent Analysis Section */}
                <div className="space-y-2">
                  <div className="flex items-center justify-between text-xs">
                    <span className="font-bold text-white">{t.recentAnalysis}</span>
                    <button
                      onClick={() => setScreen('history')}
                      className="text-[#00F59B] text-[11px] font-semibold hover:underline"
                    >
                      {t.viewAll}
                    </button>
                  </div>

                  <div className="space-y-1.5">
                    {recentScans.slice(0, 3).map((scan) => (
                      <div
                        key={scan.id}
                        className="bg-[#0F151A] border border-[#172026] rounded-xl p-2.5 flex items-center justify-between"
                      >
                        <div className="flex items-center gap-2.5">
                          <div className="w-7 h-7 rounded-full bg-[#16232D] flex items-center justify-center text-[10px] font-bold text-white">
                            {scan.pair.substring(0, 2)}
                          </div>
                          <div>
                            <div className="text-xs font-semibold text-white">
                              {scan.pair} <span className="text-[10px] text-slate-400 font-normal">{scan.timeframe}</span>
                            </div>
                            <div className="text-[10px] text-slate-500">Today, 14:32</div>
                          </div>
                        </div>

                        <span
                          className={`text-[10px] font-extrabold px-2 py-0.5 rounded-md ${
                            scan.signal === 'UP'
                              ? 'bg-[#00F59B]/15 text-[#00F59B] border border-[#00F59B]/30'
                              : scan.signal === 'DOWN'
                              ? 'bg-[#FF3B56]/15 text-[#FF3B56] border border-[#FF3B56]/30'
                              : 'bg-slate-700/30 text-slate-300 border border-slate-700'
                          }`}
                        >
                          {scan.signal}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            )}

            {/* 3. SETTINGS SCREEN */}
            {currentScreen === 'settings' && (
              <div className="flex-1 overflow-y-auto px-4 py-2 space-y-4 pb-16">
                <div className="flex items-center gap-3 pt-2">
                  <button onClick={() => setScreen('home')} className="p-1 text-slate-300 hover:text-white">
                    <ArrowLeft className="w-4 h-4" />
                  </button>
                  <h2 className="text-base font-bold text-white">{t.settings}</h2>
                </div>

                {/* Permissions Section */}
                <div className="space-y-1.5">
                  <span className="text-xs font-semibold text-slate-400">{t.permissions}</span>
                  <div className="bg-[#0F151A] border border-[#1E2830] rounded-2xl divide-y divide-[#172026]">
                    <div className="p-3 flex items-center justify-between">
                      <div>
                        <div className="text-xs font-bold text-white">{t.displayOverApps}</div>
                        <div className="text-[10px] text-slate-400">{t.displayDesc}</div>
                      </div>
                      <button
                        onClick={() => setOverlayGranted(!overlayGranted)}
                        className={`text-[10px] font-bold px-3 py-1 rounded-lg ${overlayGranted ? 'bg-[#0E3023] text-[#00F59B]' : 'bg-[#00F59B] text-black'}`}
                      >
                        {overlayGranted ? t.granted : t.grant}
                      </button>
                    </div>

                    <div className="p-3 flex items-center justify-between">
                      <div>
                        <div className="text-xs font-bold text-white">{t.screenCapture}</div>
                        <div className="text-[10px] text-slate-400">{t.screenCaptureDesc}</div>
                      </div>
                      <button
                        onClick={() => setCaptureGranted(!captureGranted)}
                        className={`text-[10px] font-bold px-3 py-1 rounded-lg ${captureGranted ? 'bg-[#0E3023] text-[#00F59B]' : 'bg-[#00F59B] text-black'}`}
                      >
                        {captureGranted ? t.granted : t.grant}
                      </button>
                    </div>
                  </div>
                </div>

                {/* AI & Backend */}
                <div className="space-y-1.5">
                  <span className="text-xs font-semibold text-slate-400">{t.aiBackend}</span>
                  <div className="bg-[#0F151A] border border-[#1E2830] rounded-2xl divide-y divide-[#172026]">
                    <div className="p-3 flex items-center justify-between">
                      <div className="text-xs font-medium text-white">{t.geminiConnection}</div>
                      <span className="text-[10px] text-[#00F59B] bg-[#0E3023] px-2 py-0.5 rounded-full font-semibold">
                        ✦ {t.connected}
                      </span>
                    </div>
                    <div className="p-3 flex items-center justify-between">
                      <div className="text-xs font-medium text-white">{t.backendServer}</div>
                      <span className="text-[10px] text-[#00F59B] bg-[#0E3023] px-2 py-0.5 rounded-full font-semibold">
                        ✦ {t.online}
                      </span>
                    </div>
                  </div>
                </div>

                {/* App Preferences */}
                <div className="space-y-1.5">
                  <span className="text-xs font-semibold text-slate-400">{t.appPreferences}</span>
                  <div className="bg-[#0F151A] border border-[#1E2830] rounded-2xl divide-y divide-[#172026]">
                    <div className="p-3 flex items-center justify-between">
                      <div className="text-xs font-medium text-white">{t.floatingStyle}</div>
                      <span className="text-xs text-[#00F59B] font-semibold">{t.neonGreen}</span>
                    </div>
                    <div
                      onClick={() => setLanguage(language === 'en' ? 'bn' : 'en')}
                      className="p-3 flex items-center justify-between cursor-pointer hover:bg-slate-800/20"
                    >
                      <div className="text-xs font-medium text-white">{t.language}</div>
                      <div className="flex items-center gap-1 text-xs text-slate-300 font-semibold">
                        <span>{language === 'bn' ? 'বাংলা (Bangla)' : 'English'}</span>
                        <ChevronRight className="w-3.5 h-3.5 text-slate-500" />
                      </div>
                    </div>
                  </div>
                </div>

                {/* About */}
                <div className="space-y-1.5">
                  <span className="text-xs font-semibold text-slate-400">{t.about}</span>
                  <div className="bg-[#0F151A] border border-[#1E2830] rounded-2xl p-3 text-xs text-slate-300">
                    Floating PRO Scanner v1.0.0
                  </div>
                </div>
              </div>
            )}

            {/* 4. HISTORY SCREEN */}
            {currentScreen === 'history' && (
              <div className="flex-1 overflow-y-auto px-4 py-2 space-y-3 pb-16">
                <div className="flex items-center gap-3 pt-2">
                  <button onClick={() => setScreen('home')} className="p-1 text-slate-300 hover:text-white">
                    <ArrowLeft className="w-4 h-4" />
                  </button>
                  <h2 className="text-base font-bold text-white">{t.recentAnalysis}</h2>
                </div>

                <div className="space-y-2">
                  {recentScans.map((scan) => (
                    <div
                      key={scan.id}
                      className="bg-[#0F151A] border border-[#1E2830] rounded-2xl p-3 space-y-2"
                    >
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <span className="text-xs font-bold text-white">{scan.pair}</span>
                          <span className="text-[10px] text-slate-400 bg-slate-800 px-1.5 py-0.5 rounded">
                            {scan.timeframe}
                          </span>
                        </div>
                        <span
                          className={`text-[10px] font-black px-2 py-0.5 rounded-md ${
                            scan.signal === 'UP'
                              ? 'bg-[#00F59B]/20 text-[#00F59B]'
                              : scan.signal === 'DOWN'
                              ? 'bg-[#FF3B56]/20 text-[#FF3B56]'
                              : 'bg-slate-700/40 text-slate-300'
                          }`}
                        >
                          {scan.signal} • {scan.confidence}%
                        </span>
                      </div>

                      <div className="text-[11px] text-slate-300 font-medium">
                        {scan.keyReason}
                      </div>

                      <ul className="text-[10px] text-slate-400 space-y-0.5 list-disc pl-3">
                        {scan.summary.slice(0, 2).map((pt, i) => (
                          <li key={i}>{pt}</li>
                        ))}
                      </ul>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* 5. LIVE TRADING OVERLAY DEMO (Quotex chart background with Draggable PRO Widget) */}
            {currentScreen === 'trading_overlay' && (
              <div className="relative flex-1 bg-[#090E14] overflow-hidden flex flex-col justify-between">
                
                {/* Quotex App Header Mock */}
                <div className="bg-[#121B24] border-b border-[#1C2836] px-3 py-2 flex items-center justify-between text-xs text-white z-10">
                  <div className="flex items-center gap-2">
                    <div className="w-5 h-5 rounded bg-[#FF3B56] flex items-center justify-center font-bold text-[10px]">Q</div>
                    <span className="font-extrabold tracking-wider text-[11px]">QUOTEX</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <div className="text-right">
                      <div className="text-[9px] text-slate-400">Demo Account</div>
                      <div className="text-[11px] font-bold text-[#00F59B]">$10,000.00</div>
                    </div>
                    <button className="bg-[#00F59B] text-black text-[9px] font-bold px-2 py-1 rounded">
                      Deposit
                    </button>
                  </div>
                </div>

                {/* Sub-header with asset pair selector */}
                <div className="px-3 py-1.5 bg-[#0D141C] flex items-center justify-between text-[11px] border-b border-[#18232F] z-10">
                  <div className="flex items-center gap-2">
                    <span className="font-bold text-white">{chartPair}</span>
                    <span className="text-[9px] text-[#00F59B] bg-[#0E3023] px-1.5 rounded">89%</span>
                    <span className="text-[9px] text-slate-400">{chartTimeframe}</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => fileInputRef.current?.click()}
                      className="text-[9px] text-[#00F59B] hover:underline flex items-center gap-1 bg-[#16232D] px-2 py-0.5 rounded"
                      title="Upload custom screenshot for Gemini to analyze"
                    >
                      <Upload className="w-2.5 h-2.5" />
                      Analyze Real Chart
                    </button>
                    <button
                      onClick={() => setScreen('home')}
                      className="text-[10px] text-slate-400 hover:text-white"
                    >
                      Exit Demo
                    </button>
                  </div>
                </div>

                {/* Simulated Candlestick Trading Chart */}
                <div className="relative flex-1 px-4 py-4 flex flex-col justify-center overflow-hidden">
                  {/* Grid Lines */}
                  <div className="absolute inset-0 flex flex-col justify-between py-6 pointer-events-none opacity-20">
                    <div className="border-b border-dashed border-slate-500 w-full" />
                    <div className="border-b border-dashed border-slate-500 w-full" />
                    <div className="border-b border-dashed border-slate-500 w-full" />
                    <div className="border-b border-dashed border-slate-500 w-full" />
                  </div>

                  {/* Candlesticks view */}
                  <div className="relative h-48 w-full flex items-end justify-between px-2">
                    {candles.map((candle, idx) => {
                      const height = Math.abs(candle.close - candle.open) * 4000 + 20;
                      return (
                        <div key={idx} className="flex flex-col items-center group">
                          {/* Upper wick */}
                          <div
                            className={`w-0.5 ${candle.isGreen ? 'bg-[#00F59B]' : 'bg-[#FF3B56]'}`}
                            style={{ height: '14px' }}
                          />
                          {/* Candle body */}
                          <div
                            className={`w-3.5 rounded-xs transition-all ${
                              candle.isGreen ? 'bg-[#00F59B]' : 'bg-[#FF3B56]'
                            }`}
                            style={{ height: `${height}px` }}
                          />
                          {/* Lower wick */}
                          <div
                            className={`w-0.5 ${candle.isGreen ? 'bg-[#00F59B]' : 'bg-[#FF3B56]'}`}
                            style={{ height: '12px' }}
                          />
                        </div>
                      );
                    })}
                  </div>

                  {/* Horizontal price line */}
                  <div className="absolute right-0 top-1/2 -translate-y-1/2 flex items-center">
                    <div className="w-32 border-b border-dashed border-[#00F59B]" />
                    <span className="bg-[#00F59B] text-black text-[9px] font-bold px-1 rounded-l">
                      1.0858
                    </span>
                  </div>
                </div>

                {/* Quotex Trading Action Buttons at Bottom */}
                <div className="p-3 bg-[#121B24] border-t border-[#1C2836] flex items-center gap-2 z-10">
                  <div className="flex-1 bg-[#00F59B] text-black rounded-lg py-2 text-center font-black text-xs flex items-center justify-center gap-1 shadow-[0_0_10px_#00F59B44]">
                    <ArrowUp className="w-3.5 h-3.5 stroke-[3]" /> UP
                  </div>
                  <div className="flex-1 bg-[#FF3B56] text-white rounded-lg py-2 text-center font-black text-xs flex items-center justify-center gap-1 shadow-[0_0_10px_#FF3B5644]">
                    <ArrowDown className="w-3.5 h-3.5 stroke-[3]" /> DOWN
                  </div>
                </div>

                {/* --- FLOATING OVERLAY WIDGET (DRAGGABLE ANYWHERE) --- */}
                {/* 1. COLLAPSED BUBBLE MODE (Screen 4) */}
                {floatingState === 'bubble' && (
                  <div
                    style={{ left: `${bubblePos.x}px`, top: `${bubblePos.y}px` }}
                    className="absolute z-50 cursor-grab active:cursor-grabbing transition-transform active:scale-95"
                    onMouseDown={handleMouseDown}
                    onClick={() => {
                      if (!isDragging) setFloatingState('expanded_panel');
                    }}
                  >
                    <div className="relative group">
                      <div className="absolute -inset-1 rounded-full bg-[#00F59B]/40 blur-sm group-hover:bg-[#00F59B]/60 animate-pulse" />
                      <div className="relative w-12 h-12 rounded-full border-2 border-[#00F59B] bg-gradient-to-br from-[#0c2e22] to-[#05080A] flex items-center justify-center shadow-[0_0_20px_#00F59B]">
                        <span className="text-[11px] font-black text-[#00F59B] tracking-wider">PRO</span>
                      </div>
                    </div>
                  </div>
                )}

                {/* 2. COLLAPSED PILL BAR (Screen 5) */}
                {floatingState === 'collapsed_pill' && (
                  <div
                    style={{ left: `${Math.min(bubblePos.x, 100)}px`, top: `${bubblePos.y}px` }}
                    className="absolute z-50 bg-[#0F151A]/95 backdrop-blur-md border border-[#00F59B] rounded-full px-3 py-1.5 flex items-center gap-2.5 shadow-2xl cursor-grab"
                    onMouseDown={handleMouseDown}
                  >
                    <div className="w-6 h-6 rounded-full border border-[#00F59B] bg-[#0c2e22] flex items-center justify-center">
                      <span className="text-[8px] font-black text-[#00F59B]">PRO</span>
                    </div>
                    <div
                      onClick={() => setFloatingState('expanded_panel')}
                      className="cursor-pointer"
                    >
                      <div className="text-[11px] font-bold text-white leading-none">{t.proScanner}</div>
                      <div className="text-[9px] text-[#00F59B] leading-none mt-0.5">{t.tapToOpen}</div>
                    </div>
                    <button
                      onClick={() => setFloatingState('expanded_panel')}
                      className="text-slate-400 hover:text-white ml-1"
                    >
                      <SettingsIcon className="w-3.5 h-3.5" />
                    </button>
                    <button
                      onClick={() => setFloatingState('bubble')}
                      className="text-slate-400 hover:text-white"
                    >
                      <X className="w-3.5 h-3.5" />
                    </button>
                  </div>
                )}

                {/* 3. EXPANDED FLOATING PANEL (Screen 6, 7, 8) */}
                {floatingState === 'expanded_panel' && (
                  <div className="absolute inset-x-3 bottom-14 z-50 bg-[#070B0E]/95 backdrop-blur-xl border border-[#00F59B]/60 rounded-3xl p-4 shadow-2xl shadow-black/80 flex flex-col space-y-3 animate-scaleUp">
                    {/* Header: Title + Minimize + Close */}
                    <div className="flex items-center justify-between border-b border-[#1E2830] pb-2">
                      <div className="flex items-center gap-2">
                        <div className="w-6 h-6 rounded-full border border-[#00F59B] bg-[#0c2e22] flex items-center justify-center">
                          <span className="text-[8px] font-black text-[#00F59B]">PRO</span>
                        </div>
                        <span className="text-xs font-black text-[#00F59B] tracking-wider">
                          {t.proScanner}
                        </span>
                      </div>
                      <div className="flex items-center gap-2">
                        <button
                          onClick={() => setFloatingState('collapsed_pill')}
                          className="p-1 text-slate-400 hover:text-white"
                          title="Minimize to Pill"
                        >
                          <Minus className="w-3.5 h-3.5" />
                        </button>
                        <button
                          onClick={() => setFloatingState('bubble')}
                          className="p-1 text-slate-400 hover:text-white"
                          title="Close to Bubble"
                        >
                          <X className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </div>

                    {/* Toolbar: [Scan] [Settings] [History] */}
                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => executeScan()}
                        disabled={isScanning}
                        className="flex-1 py-1.5 bg-[#00F59B] hover:bg-[#00c97f] text-black font-extrabold text-[11px] rounded-lg flex items-center justify-center gap-1 cursor-pointer transition disabled:opacity-50"
                      >
                        <Play className="w-3 h-3 fill-current" />
                        {t.scan}
                      </button>

                      <button
                        onClick={() => setScreen('settings')}
                        className="flex-1 py-1.5 bg-[#172026] hover:bg-[#1E2830] text-slate-300 font-semibold text-[11px] rounded-lg flex items-center justify-center gap-1 border border-[#1E2830]"
                      >
                        <SettingsIcon className="w-3 h-3" />
                        {t.settings}
                      </button>
                    </div>

                    {/* DYNAMIC BODY: Scanning animation vs Result vs Idle */}
                    {isScanning ? (
                      /* Screen 6: Scanning Radar View */
                      <div className="flex flex-col items-center py-4 space-y-3">
                        {/* Circular Radar Sweep */}
                        <div className="relative w-28 h-28 flex items-center justify-center">
                          <div className="absolute inset-0 rounded-full border border-[#00F59B]/20" />
                          <div className="absolute inset-4 rounded-full border border-[#00F59B]/30" />
                          <div className="absolute inset-8 rounded-full border border-[#00F59B]/40" />
                          
                          {/* Rotating Radar Sweep Beam */}
                          <div
                            className="absolute inset-0 rounded-full animate-spin"
                            style={{
                              background: 'conic-gradient(from 0deg, transparent 270deg, rgba(0,245,155,0.4) 360deg)'
                            }}
                          />
                          <div className="w-2 h-2 rounded-full bg-[#00F59B] shadow-[0_0_8px_#00F59B]" />
                        </div>

                        <div className="text-center">
                          <div className="text-xs font-extrabold text-[#00F59B] tracking-wider mb-1">
                            {t.scanningStatus}
                          </div>
                          <div className="text-[10px] text-slate-400 max-w-[220px] leading-tight">
                            {scanMessage}
                          </div>
                        </div>

                        {/* STOP BUTTON */}
                        <button
                          onClick={() => setIsScanning(false)}
                          className="px-6 py-1 rounded-lg border border-[#FF3B56] text-[#FF3B56] hover:bg-[#FF3B56]/10 text-xs font-bold transition cursor-pointer"
                        >
                          {t.stop}
                        </button>
                      </div>
                    ) : activeResult ? (
                      /* Screen 7 & 8: Result Card (UP / DOWN / NO_TRADE) */
                      <div className="space-y-2.5 max-h-[280px] overflow-y-auto pr-1">
                        {/* Signal Ready Header */}
                        <div className="flex items-center gap-1.5 text-xs">
                          <CheckCircle className="w-3.5 h-3.5 text-[#00F59B]" />
                          <div>
                            <span className="font-bold text-[#00F59B]">{t.signalReady}</span>
                            <span className="text-[10px] text-slate-400 ml-1.5">• {t.analysisCompleted}</span>
                          </div>
                        </div>

                        {/* Big Signal Card */}
                        <div
                          className={`p-3 rounded-2xl border flex items-center justify-between ${
                            activeResult.signal === 'UP'
                              ? 'bg-[#0E261D] border-[#00F59B]'
                              : activeResult.signal === 'DOWN'
                              ? 'bg-[#2E1217] border-[#FF3B56]'
                              : 'bg-[#172026] border-slate-600'
                          }`}
                        >
                          <div className="flex items-center gap-3">
                            <div
                              className={`w-10 h-10 rounded-full flex items-center justify-center border-2 ${
                                activeResult.signal === 'UP'
                                  ? 'bg-[#00F59B]/20 border-[#00F59B] text-[#00F59B]'
                                  : activeResult.signal === 'DOWN'
                                  ? 'bg-[#FF3B56]/20 border-[#FF3B56] text-[#FF3B56]'
                                  : 'bg-slate-700/40 border-slate-400 text-slate-300'
                              }`}
                            >
                              {activeResult.signal === 'UP' && <ArrowUp className="w-5 h-5 stroke-[3]" />}
                              {activeResult.signal === 'DOWN' && <ArrowDown className="w-5 h-5 stroke-[3]" />}
                              {activeResult.signal === 'NO_TRADE' && <Minus className="w-5 h-5 stroke-[3]" />}
                            </div>

                            <div>
                              <div
                                className={`text-base font-black ${
                                  activeResult.signal === 'UP'
                                    ? 'text-[#00F59B]'
                                    : activeResult.signal === 'DOWN'
                                    ? 'text-[#FF3B56]'
                                    : 'text-slate-300'
                                }`}
                              >
                                {activeResult.signal === 'UP' ? t.up : activeResult.signal === 'DOWN' ? t.down : t.noTrade}
                              </div>
                              <div className="text-[10px] text-slate-400">
                                {t.confidence}: <span className="font-bold text-white">{activeResult.confidence}%</span>
                              </div>
                            </div>
                          </div>

                          <div className="text-right text-[10px] font-semibold text-slate-300">
                            {activeResult.pair} | {activeResult.timeframe}
                          </div>
                        </div>

                        {/* Analysis Summary & Evidence */}
                        {((activeResult.evidence && activeResult.evidence.length > 0) || (activeResult.summary && activeResult.summary.length > 0)) && (
                          <div className="space-y-1">
                            <span className="text-[10px] font-bold text-white uppercase tracking-wider">
                              {t.evidenceTitle || t.analysisSummary}
                            </span>
                            <ul className="text-[10px] text-slate-300 space-y-0.5">
                              {(activeResult.evidence || activeResult.summary).map((point, idx) => (
                                <li key={idx} className="flex items-start gap-1">
                                  <span className="text-[#00F59B]">•</span>
                                  <span>{point}</span>
                                </li>
                              ))}
                            </ul>
                          </div>
                        )}

                        {/* Reasoning / Key reason */}
                        {(activeResult.reasoning || activeResult.keyReason) && (
                          <div className="bg-[#121B24] p-2 rounded-xl text-[10px] border border-[#1A2633]">
                            <span className="font-bold text-white">{t.keyReason}: </span>
                            <span className="text-slate-300">{activeResult.reasoning || activeResult.keyReason}</span>
                          </div>
                        )}

                        {/* Uncertainty & Missing Data */}
                        {activeResult.uncertainty && activeResult.uncertainty.length > 0 && (
                          <div className="space-y-1">
                            <div className="flex items-center gap-1 text-[10px] font-bold text-[#64B5F6]">
                              <AlertTriangle className="w-3 h-3 text-[#64B5F6]" />
                              <span>{t.uncertaintyTitle}</span>
                            </div>
                            <ul className="text-[10px] text-slate-300 space-y-0.5">
                              {activeResult.uncertainty.map((item, idx) => (
                                <li key={idx} className="flex items-start gap-1">
                                  <span className="text-[#64B5F6]">•</span>
                                  <span>{item}</span>
                                </li>
                              ))}
                            </ul>
                          </div>
                        )}

                        {/* Risks */}
                        {activeResult.risks && activeResult.risks.length > 0 && (
                          <div className="space-y-1">
                            <div className="flex items-center gap-1 text-[10px] font-bold text-[#FFB800]">
                              <AlertTriangle className="w-3 h-3" />
                              <span>{t.risks}</span>
                            </div>
                            <ul className="text-[10px] text-slate-400 space-y-0.5">
                              {activeResult.risks.map((risk, idx) => (
                                <li key={idx} className="flex items-start gap-1">
                                  <span className="text-[#FFB800]">•</span>
                                  <span>{risk}</span>
                                </li>
                              ))}
                            </ul>
                          </div>
                        )}

                        {/* Model badge and Disclaimer */}
                        <div className="text-[8px] text-slate-500 pt-1 leading-tight border-t border-[#172026] flex items-center justify-between">
                          <span>{activeResult.disclaimer}</span>
                          {activeResult.modelUsed && (
                            <span className="text-[8px] text-[#00F59B] font-mono shrink-0 ml-1">
                              {activeResult.modelUsed}
                            </span>
                          )}
                        </div>
                      </div>
                    ) : (
                      /* Idle prompt */
                      <div className="text-center py-4 space-y-2">
                        <div className="w-10 h-10 rounded-full bg-[#14201A] text-[#00F59B] mx-auto flex items-center justify-center">
                          <Camera className="w-5 h-5" />
                        </div>
                        <p className="text-[11px] text-slate-300">
                          {language === 'bn' ? 'চার্ট বিশ্লেষণ করতে "Scan" এ চাপ দিন' : 'Tap "Scan" to capture live chart with Gemini'}
                        </p>
                      </div>
                    )}

                    {/* Footer */}
                    <div className="text-center text-[9px] text-slate-500 pt-1 border-t border-[#1E2830]">
                      ✦ {t.poweredByGemini}
                    </div>
                  </div>
                )}
              </div>
            )}

            {/* 6. ERROR SCREENS (Screen 9) */}
            {currentScreen.startsWith('error_') && (
              <div className="flex-1 flex flex-col items-center justify-center px-6 text-center space-y-4">
                <div className="w-16 h-16 rounded-full bg-[#291719] border-2 border-[#FF3B56] flex items-center justify-center text-[#FF3B56]">
                  {currentScreen === 'error_permission' && <AlertOctagon className="w-8 h-8" />}
                  {currentScreen === 'error_no_chart' && <ImageOff className="w-8 h-8" />}
                  {currentScreen === 'error_network' && <CloudOff className="w-8 h-8" />}
                  {currentScreen === 'error_ai' && <AlertTriangle className="w-8 h-8" />}
                </div>

                <div>
                  <h3 className="text-base font-bold text-white mb-1">
                    {currentScreen === 'error_permission' && t.errPermTitle}
                    {currentScreen === 'error_no_chart' && t.errNoChartTitle}
                    {currentScreen === 'error_network' && t.errNetTitle}
                    {currentScreen === 'error_ai' && t.errAiTitle}
                  </h3>
                  <p className="text-xs text-slate-400 leading-relaxed">
                    {currentScreen === 'error_permission' && t.errPermDesc}
                    {currentScreen === 'error_no_chart' && t.errNoChartDesc}
                    {currentScreen === 'error_network' && t.errNetDesc}
                    {currentScreen === 'error_ai' && t.errAiDesc}
                  </p>
                </div>

                <div className="flex flex-col w-full gap-2 pt-2">
                  <button
                    onClick={() => {
                      if (currentScreen === 'error_permission') {
                        setOverlayGranted(true);
                        setCaptureGranted(true);
                      }
                      setScreen('home');
                    }}
                    className="w-full py-2.5 bg-[#00F59B] text-black font-bold text-xs rounded-xl shadow-[0_0_15px_#00F59B44]"
                  >
                    {currentScreen === 'error_permission' ? t.grant : t.retry}
                  </button>

                  <button
                    onClick={() => setScreen('home')}
                    className="text-xs text-slate-400 hover:text-white py-1"
                  >
                    {t.close}
                  </button>
                </div>
              </div>
            )}

          </div>

          {/* Bottom Android Navigation Bar (Home, History, Settings) - only shown for main screens */}
          {['home', 'history', 'settings'].includes(currentScreen) && (
            <div className="bg-[#05080A] border-t border-[#172026] py-2 px-6 flex items-center justify-between text-slate-400">
              <button
                onClick={() => setScreen('home')}
                className={`flex flex-col items-center gap-0.5 ${currentScreen === 'home' ? 'text-[#00F59B]' : 'hover:text-slate-200'}`}
              >
                <HomeIcon className="w-4 h-4" />
                <span className="text-[9px] font-semibold">{t.home}</span>
              </button>

              <button
                onClick={() => setScreen('history')}
                className={`flex flex-col items-center gap-0.5 ${currentScreen === 'history' ? 'text-[#00F59B]' : 'hover:text-slate-200'}`}
              >
                <HistoryIcon className="w-4 h-4" />
                <span className="text-[9px] font-semibold">{t.history}</span>
              </button>

              <button
                onClick={() => setScreen('settings')}
                className={`flex flex-col items-center gap-0.5 ${currentScreen === 'settings' ? 'text-[#00F59B]' : 'hover:text-slate-200'}`}
              >
                <SettingsIcon className="w-4 h-4" />
                <span className="text-[9px] font-semibold">{t.settings}</span>
              </button>
            </div>
          )}

          {/* Android Bottom Home Gesture Pill Bar */}
          <div className="w-full bg-[#05080A] pb-2 pt-1 flex justify-center">
            <div className="w-28 h-1 bg-slate-600 rounded-full" />
          </div>

        </div>
      </div>
    </div>
  );
};
