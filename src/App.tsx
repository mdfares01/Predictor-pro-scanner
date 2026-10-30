import React, { useState } from 'react';
import {
  Smartphone,
  LayoutGrid,
  Code2,
  BookOpen,
  Globe,
  Sparkles,
  Layers,
  ShieldAlert,
  Play,
  RotateCcw,
  CheckCircle2,
  ExternalLink
} from 'lucide-react';
import { PhoneSimulator } from './components/PhoneSimulator';
import { ScreensShowcase } from './components/ScreensShowcase';
import { AndroidCodeExplorer } from './components/AndroidCodeExplorer';
import { InstallationGuide } from './components/InstallationGuide';
import { AppScreen, Language, ScanResult } from './types';

export default function App() {
  const [activeTab, setActiveTab] = useState<'simulator' | 'screens' | 'code' | 'install'>('simulator');
  const [phoneScreen, setPhoneScreen] = useState<AppScreen>('trading_overlay');
  const [language, setLanguage] = useState<Language>('en');

  // History state for scans
  const [scans, setScans] = useState<ScanResult[]>([
    {
      id: '1',
      signal: 'UP',
      confidence: 78,
      pair: 'EUR/USD',
      timeframe: '5m',
      summary: [
        'Uptrend is visible with higher swing lows',
        'Strong bullish candles near dynamic support',
        'Resistance level at 1.0855 pending test',
        'Volume looks increasing on green candles'
      ],
      risks: [
        'Market can be volatile during news sessions',
        'False breakout possible near key resistance'
      ],
      keyReason: 'Bullish momentum bounce from support',
      timestamp: Date.now() - 15 * 60 * 1000,
      disclaimer: 'This is an AI analysis based on screenshot only. Not financial advice.'
    },
    {
      id: '2',
      signal: 'DOWN',
      confidence: 72,
      pair: 'GBP/CHF',
      timeframe: '15m',
      summary: [
        'Downtrend sequence intact with lower highs',
        'Bearish rejection candle formed at resistance',
        'Support breach pressure at 1.1070'
      ],
      risks: [
        'Sudden counter-trend reversal possible',
        'High impact economic news pending'
      ],
      keyReason: 'Bearish rejection at resistance',
      timestamp: Date.now() - 55 * 60 * 1000,
      disclaimer: 'This is an AI analysis based on screenshot only. Not financial advice.'
    },
    {
      id: '3',
      signal: 'NO_TRADE',
      confidence: 56,
      pair: 'USD/JPY',
      timeframe: '1m',
      summary: [
        'Sideways consolidation within tight range',
        'Alternating doji candles signaling indecision',
        'Lack of directional volume or breakout'
      ],
      risks: [
        'High risk of false whipsaws in choppy market',
        'Unclear momentum direction'
      ],
      keyReason: 'Sideways movement without trend confirmation',
      timestamp: Date.now() - 140 * 60 * 1000,
      disclaimer: 'Always use your own analysis. This is not financial advice.'
    }
  ]);

  const handleAddScan = (scan: ScanResult) => {
    setScans((prev) => [scan, ...prev]);
  };

  return (
    <div className="min-h-screen bg-[#05080A] text-slate-100 flex flex-col antialiased selection:bg-[#00F59B] selection:text-black">
      
      {/* Top Main Navigation Header */}
      <header className="border-b border-[#141C24] bg-[#070B0E]/90 backdrop-blur-md sticky top-0 z-50 px-4 lg:px-8 py-3">
        <div className="max-w-7xl mx-auto flex flex-col md:flex-row md:items-center justify-between gap-3">
          
          {/* Logo & App Branding */}
          <div className="flex items-center gap-3">
            <div className="relative w-9 h-9 rounded-full border border-[#00F59B] bg-gradient-to-br from-[#0c2e22] to-[#05080A] flex items-center justify-center shadow-[0_0_15px_#00F59B55]">
              <span className="text-[10px] font-black text-[#00F59B] tracking-wider">PRO</span>
            </div>
            <div>
              <div className="flex items-center gap-1.5 font-bold text-sm text-white">
                <span>Floating</span>
                <span className="text-[#00F59B] drop-shadow-[0_0_8px_#00F59B]">PRO</span>
                <span>Scanner</span>
                <span className="text-[10px] bg-[#0E3023] text-[#00F59B] font-mono px-2 py-0.5 rounded-full border border-[#00F59B]/30 ml-1">
                  Android Native
                </span>
              </div>
              <div className="text-[11px] text-slate-400">
                Kotlin & Jetpack Compose • Draggable Overlay • Gemini Vision AI
              </div>
            </div>
          </div>

          {/* Quick Language Toggle & Actions */}
          <div className="flex items-center gap-2">
            <button
              onClick={() => setLanguage(language === 'en' ? 'bn' : 'en')}
              className="px-3 py-1.5 rounded-xl bg-[#0F151A] hover:bg-[#172026] text-xs font-semibold text-slate-200 border border-[#1E2830] flex items-center gap-1.5 cursor-pointer transition"
              title="Toggle between English and Bangla (বাংলা)"
            >
              <Globe className="w-3.5 h-3.5 text-[#00F59B]" />
              <span>{language === 'en' ? 'English' : 'বাংলা (Bangla)'}</span>
            </button>

            <a
              href="#installation"
              onClick={(e) => {
                e.preventDefault();
                setActiveTab('install');
              }}
              className="px-3 py-1.5 rounded-xl bg-[#00F59B]/15 hover:bg-[#00F59B]/25 text-[#00F59B] text-xs font-bold border border-[#00F59B]/30 flex items-center gap-1.5 transition"
            >
              <Smartphone className="w-3.5 h-3.5" />
              <span>Install on Phone</span>
            </a>
          </div>

        </div>

        {/* Tab Switcher */}
        <div className="max-w-7xl mx-auto flex items-center gap-2 mt-3 overflow-x-auto pt-1">
          <button
            onClick={() => setActiveTab('simulator')}
            className={`px-4 py-2 rounded-xl text-xs font-bold flex items-center gap-2 transition cursor-pointer shrink-0 ${
              activeTab === 'simulator'
                ? 'bg-[#00F59B] text-black shadow-[0_0_15px_#00F59B44]'
                : 'bg-[#0F151A] text-slate-400 hover:text-white border border-[#1E2830]'
            }`}
          >
            <Smartphone className="w-4 h-4" />
            Interactive Phone Simulator
          </button>

          <button
            onClick={() => setActiveTab('screens')}
            className={`px-4 py-2 rounded-xl text-xs font-bold flex items-center gap-2 transition cursor-pointer shrink-0 ${
              activeTab === 'screens'
                ? 'bg-[#00F59B] text-black shadow-[0_0_15px_#00F59B44]'
                : 'bg-[#0F151A] text-slate-400 hover:text-white border border-[#1E2830]'
            }`}
          >
            <LayoutGrid className="w-4 h-4" />
            9 Screens UI Architecture
          </button>

          <button
            onClick={() => setActiveTab('code')}
            className={`px-4 py-2 rounded-xl text-xs font-bold flex items-center gap-2 transition cursor-pointer shrink-0 ${
              activeTab === 'code'
                ? 'bg-[#00F59B] text-black shadow-[0_0_15px_#00F59B44]'
                : 'bg-[#0F151A] text-slate-400 hover:text-white border border-[#1E2830]'
            }`}
          >
            <Code2 className="w-4 h-4" />
            Kotlin & Compose Android Code
          </button>

          <button
            onClick={() => setActiveTab('install')}
            className={`px-4 py-2 rounded-xl text-xs font-bold flex items-center gap-2 transition cursor-pointer shrink-0 ${
              activeTab === 'install'
                ? 'bg-[#00F59B] text-black shadow-[0_0_15px_#00F59B44]'
                : 'bg-[#0F151A] text-slate-400 hover:text-white border border-[#1E2830]'
            }`}
          >
            <BookOpen className="w-4 h-4" />
            APK Installation Instructions
          </button>
        </div>
      </header>

      {/* Main Container */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-4 lg:p-8">
        
        {/* TAB 1: INTERACTIVE PHONE SIMULATOR */}
        {activeTab === 'simulator' && (
          <div className="flex flex-col lg:flex-row items-start gap-8 justify-center">
            
            {/* Left: Quick screen controls & features guide */}
            <div className="w-full lg:w-80 space-y-4">
              <div className="bg-[#0F151A] border border-[#1E2830] rounded-2xl p-4 space-y-3">
                <div className="flex items-center justify-between">
                  <h3 className="text-xs font-bold text-white uppercase tracking-wider">
                    Quick Screen Switcher
                  </h3>
                  <span className="text-[10px] text-[#00F59B] font-mono">Real UI</span>
                </div>

                <div className="grid grid-cols-2 gap-2 text-xs">
                  <button
                    onClick={() => setPhoneScreen('splash')}
                    className={`p-2 rounded-xl text-left border transition ${
                      phoneScreen === 'splash'
                        ? 'bg-[#00F59B]/20 text-[#00F59B] border-[#00F59B]'
                        : 'bg-[#141C24] text-slate-300 border-[#1E2830] hover:border-slate-600'
                    }`}
                  >
                    1. Splash Logo
                  </button>

                  <button
                    onClick={() => setPhoneScreen('home')}
                    className={`p-2 rounded-xl text-left border transition ${
                      phoneScreen === 'home'
                        ? 'bg-[#00F59B]/20 text-[#00F59B] border-[#00F59B]'
                        : 'bg-[#141C24] text-slate-300 border-[#1E2830] hover:border-slate-600'
                    }`}
                  >
                    2. Home Screen
                  </button>

                  <button
                    onClick={() => setPhoneScreen('settings')}
                    className={`p-2 rounded-xl text-left border transition ${
                      phoneScreen === 'settings'
                        ? 'bg-[#00F59B]/20 text-[#00F59B] border-[#00F59B]'
                        : 'bg-[#141C24] text-slate-300 border-[#1E2830] hover:border-slate-600'
                    }`}
                  >
                    3. Settings
                  </button>

                  <button
                    onClick={() => setPhoneScreen('history')}
                    className={`p-2 rounded-xl text-left border transition ${
                      phoneScreen === 'history'
                        ? 'bg-[#00F59B]/20 text-[#00F59B] border-[#00F59B]'
                        : 'bg-[#141C24] text-slate-300 border-[#1E2830] hover:border-slate-600'
                    }`}
                  >
                    History
                  </button>

                  <button
                    onClick={() => setPhoneScreen('trading_overlay')}
                    className={`col-span-2 p-2.5 rounded-xl text-left border font-bold flex items-center justify-between transition ${
                      phoneScreen === 'trading_overlay'
                        ? 'bg-[#00F59B] text-black border-[#00F59B] shadow-[0_0_15px_#00F59B44]'
                        : 'bg-[#141C24] text-[#00F59B] border-[#00F59B]/40 hover:bg-[#1E2830]'
                    }`}
                  >
                    <span>4 & 9. Quotex Live Overlay Demo</span>
                    <Play className="w-3.5 h-3.5 fill-current" />
                  </button>
                </div>

                {/* Error Simulation Triggers */}
                <div className="pt-2 border-t border-[#1E2830] space-y-1.5">
                  <div className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                    Test Error Handling
                  </div>
                  <div className="grid grid-cols-2 gap-1.5 text-[11px]">
                    <button
                      onClick={() => setPhoneScreen('error_permission')}
                      className="p-1.5 bg-[#171920] hover:bg-[#1f232d] text-slate-300 rounded-lg text-left"
                    >
                      Denied Permission
                    </button>
                    <button
                      onClick={() => setPhoneScreen('error_no_chart')}
                      className="p-1.5 bg-[#171920] hover:bg-[#1f232d] text-slate-300 rounded-lg text-left"
                    >
                      No Chart Detected
                    </button>
                    <button
                      onClick={() => setPhoneScreen('error_network')}
                      className="p-1.5 bg-[#171920] hover:bg-[#1f232d] text-slate-300 rounded-lg text-left"
                    >
                      Network Failure
                    </button>
                    <button
                      onClick={() => setPhoneScreen('error_ai')}
                      className="p-1.5 bg-[#171920] hover:bg-[#1f232d] text-slate-300 rounded-lg text-left"
                    >
                      AI Failure
                    </button>
                  </div>
                </div>
              </div>

              {/* How to interact hint card */}
              <div className="bg-[#090D11] border border-[#1E2830] rounded-2xl p-4 text-xs space-y-2 text-slate-400">
                <div className="font-bold text-white flex items-center gap-1.5 text-xs">
                  <Sparkles className="w-3.5 h-3.5 text-[#00F59B]" />
                  Interactive Guide:
                </div>
                <ul className="space-y-1.5 text-[11px] list-disc pl-3">
                  <li><strong className="text-white">Drag Bubble:</strong> In Quotex Overlay mode, click & drag the glowing <code className="text-[#00F59B]">PRO</code> bubble anywhere on screen!</li>
                  <li><strong className="text-white">Tap to Open:</strong> Click the bubble to toggle between collapsed mode and the expanded scanner panel.</li>
                  <li><strong className="text-white">Scan Live Chart:</strong> Tap "Scan" to trigger the radar sweep and receive real Gemini AI analysis results.</li>
                  <li><strong className="text-white">Custom Chart:</strong> Click "Analyze Real Chart" to upload your own screenshot for Gemini to interpret!</li>
                </ul>
              </div>
            </div>

            {/* Center: The Realistic Android Phone Simulator */}
            <div className="flex justify-center">
              <PhoneSimulator
                currentScreen={phoneScreen}
                setScreen={setPhoneScreen}
                language={language}
                setLanguage={setLanguage}
                recentScans={scans}
                onAddScan={handleAddScan}
              />
            </div>

            {/* Right: Architectural details */}
            <div className="w-full lg:w-80 space-y-4">
              <div className="bg-[#0F151A] border border-[#1E2830] rounded-2xl p-4 space-y-3">
                <h3 className="text-xs font-bold text-white uppercase tracking-wider flex items-center gap-1.5">
                  <ShieldAlert className="w-4 h-4 text-[#00F59B]" />
                  Fair Play & Safety Architecture
                </h3>
                <p className="text-[11px] text-slate-400 leading-relaxed">
                  Floating PRO Scanner is strictly an observational visual tool powered by Gemini Multimodal AI.
                </p>
                <div className="space-y-2 text-[11px]">
                  <div className="flex items-start gap-2 bg-[#121820] p-2 rounded-xl">
                    <CheckCircle2 className="w-3.5 h-3.5 text-[#00F59B] shrink-0 mt-0.5" />
                    <span className="text-slate-300">
                      <strong>No Account Automation:</strong> Never prompts or stores Quotex credentials, never executes auto-clicks or trades.
                    </span>
                  </div>
                  <div className="flex items-start gap-2 bg-[#121820] p-2 rounded-xl">
                    <CheckCircle2 className="w-3.5 h-3.5 text-[#00F59B] shrink-0 mt-0.5" />
                    <span className="text-slate-300">
                      <strong>Explicit Consent:</strong> Uses official Android <code className="text-[#00F59B]">MediaProjection</code>. Captures only when you tap Scan.
                    </span>
                  </div>
                  <div className="flex items-start gap-2 bg-[#121820] p-2 rounded-xl">
                    <CheckCircle2 className="w-3.5 h-3.5 text-[#00F59B] shrink-0 mt-0.5" />
                    <span className="text-slate-300">
                      <strong>Secure Backend:</strong> API keys are kept strictly on the backend server.
                    </span>
                  </div>
                </div>
              </div>
            </div>

          </div>
        )}

        {/* TAB 2: 9 SCREENS REFERENCE GALLERY */}
        {activeTab === 'screens' && (
          <ScreensShowcase
            onSelectScreen={(screen) => {
              setPhoneScreen(screen);
              setActiveTab('simulator');
            }}
          />
        )}

        {/* TAB 3: ANDROID KOTLIN & COMPOSE CODE EXPLORER */}
        {activeTab === 'code' && <AndroidCodeExplorer />}

        {/* TAB 4: PHONE APK INSTALLATION GUIDE */}
        {activeTab === 'install' && <InstallationGuide />}

      </main>

      {/* Footer Disclaimer */}
      <footer className="border-t border-[#141C24] bg-[#070B0E] py-4 px-6 text-center text-xs text-slate-500">
        <div className="max-w-4xl mx-auto space-y-1">
          <p className="font-semibold text-slate-400">
            Floating PRO Scanner • Android Native Kotlin & Jetpack Compose
          </p>
          <p className="text-[10px] text-slate-600">
            DISCLAIMER: This application does not automate trading, clicks, or orders on any platform. All UP, DOWN, and NO TRADE signals are probabilistic visual interpretations for technical educational analysis. Trading financial contracts carries risk.
          </p>
        </div>
      </footer>

    </div>
  );
}
