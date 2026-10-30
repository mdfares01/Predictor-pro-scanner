import React from 'react';
import {
  Sparkles,
  ArrowUp,
  ArrowDown,
  Minus,
  CheckCircle,
  Play,
  Settings,
  Layers,
  Camera,
  Wifi,
  Battery,
  Shield,
  ChevronRight,
  ExternalLink
} from 'lucide-react';
import { AppScreen } from '../types';

interface ScreensShowcaseProps {
  onSelectScreen: (screen: AppScreen) => void;
}

export const ScreensShowcase: React.FC<ScreensShowcaseProps> = ({ onSelectScreen }) => {
  return (
    <div className="space-y-6">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-[#0F151A] border border-[#1E2830] p-4 rounded-2xl">
        <div>
          <h2 className="text-lg font-bold text-white flex items-center gap-2">
            <span className="w-2.5 h-2.5 rounded-full bg-[#00F59B] animate-pulse" />
            9 Screens UI Architecture Reference
          </h2>
          <p className="text-xs text-slate-400 mt-1">
            Exact 1:1 recreation of all visual screens from the reference design. Tap any card to test in the interactive simulator.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <span className="text-xs bg-[#0E3023] text-[#00F59B] px-3 py-1 rounded-full border border-[#00F59B]/30 font-semibold">
            ✦ Theme: Neon Green (#00F59B) & Pitch Black
          </span>
        </div>
      </div>

      {/* Grid of 9 Screens */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">

        {/* 1. Launch Screen */}
        <div
          onClick={() => onSelectScreen('splash')}
          className="group bg-[#090D10] border border-[#1E2830] hover:border-[#00F59B] transition-all rounded-3xl p-4 flex flex-col justify-between cursor-pointer overflow-hidden relative shadow-lg"
        >
          <div className="flex items-center justify-between text-xs text-slate-400 mb-3">
            <span className="font-bold text-[#00F59B]">1. Launch Screen</span>
            <span className="text-[10px] bg-slate-800 px-2 py-0.5 rounded text-slate-300 group-hover:bg-[#00F59B] group-hover:text-black transition">Test Screen</span>
          </div>

          <div className="h-64 bg-[#05080A] rounded-2xl border border-slate-800 flex flex-col items-center justify-center p-4 text-center">
            <div className="w-20 h-20 rounded-full border-2 border-[#00F59B] bg-[#0c2e22] flex items-center justify-center shadow-[0_0_25px_#00F59B55] mb-4">
              <span className="text-lg font-black text-[#00F59B]">PRO</span>
            </div>
            <div className="text-sm font-bold text-white mb-0.5">
              Floating <span className="text-[#00F59B]">PRO</span> Scanner
            </div>
            <div className="text-[10px] text-slate-400">AI Powered Trading Analysis</div>
            <div className="text-[9px] text-slate-500 mb-4">Smart • Fast • Accurate</div>
            <div className="w-28 h-1 bg-slate-800 rounded-full overflow-hidden">
              <div className="w-20 h-full bg-[#00F59B]" />
            </div>
          </div>
          <div className="text-center text-xs text-slate-400 mt-2 font-medium">
            App starting animation & splash initializing
          </div>
        </div>

        {/* 2. Home Screen */}
        <div
          onClick={() => onSelectScreen('home')}
          className="group bg-[#090D10] border border-[#1E2830] hover:border-[#00F59B] transition-all rounded-3xl p-4 flex flex-col justify-between cursor-pointer overflow-hidden relative shadow-lg"
        >
          <div className="flex items-center justify-between text-xs text-slate-400 mb-3">
            <span className="font-bold text-[#00F59B]">2. Home Screen</span>
            <span className="text-[10px] bg-slate-800 px-2 py-0.5 rounded text-slate-300 group-hover:bg-[#00F59B] group-hover:text-black transition">Test Screen</span>
          </div>

          <div className="h-64 bg-[#070B0E] rounded-2xl border border-slate-800 p-3 space-y-2 overflow-hidden flex flex-col justify-between">
            <div className="flex items-center justify-between bg-[#0F151A] p-2 rounded-xl border border-[#1E2830]">
              <div className="flex items-center gap-1.5 text-xs text-white">
                <Sparkles className="w-3.5 h-3.5 text-[#00F59B]" />
                <span className="font-bold text-[11px]">Gemini AI</span>
              </div>
              <span className="text-[9px] text-[#00F59B] bg-[#0E3023] px-2 py-0.5 rounded-full font-bold">
                ✦ Connected
              </span>
            </div>

            <div className="bg-[#0F151A] p-2 rounded-xl border border-[#1E2830] space-y-1.5 text-[10px]">
              <div className="flex justify-between items-center text-slate-300">
                <span>Floating Button</span>
                <span className="text-[#00F59B] bg-[#0F261E] px-1.5 py-0.5 rounded">Active</span>
              </div>
              <div className="flex justify-between items-center text-slate-300">
                <span>Screen Capture</span>
                <span className="text-[#00F59B] bg-[#0F261E] px-1.5 py-0.5 rounded">Granted</span>
              </div>
            </div>

            <div className="bg-[#00F59B] text-black text-xs font-black py-2 rounded-xl text-center">
              ▶ START SCANNER
            </div>

            <div className="text-[10px] text-slate-400">
              <span className="font-bold text-white">Recent:</span> EUR/USD 5m (UP), GBP/CHF (DOWN)
            </div>
          </div>
          <div className="text-center text-xs text-slate-400 mt-2 font-medium">
            Main dashboard with permissions & recent analysis
          </div>
        </div>

        {/* 3. Settings */}
        <div
          onClick={() => onSelectScreen('settings')}
          className="group bg-[#090D10] border border-[#1E2830] hover:border-[#00F59B] transition-all rounded-3xl p-4 flex flex-col justify-between cursor-pointer overflow-hidden relative shadow-lg"
        >
          <div className="flex items-center justify-between text-xs text-slate-400 mb-3">
            <span className="font-bold text-[#00F59B]">3. Settings</span>
            <span className="text-[10px] bg-slate-800 px-2 py-0.5 rounded text-slate-300 group-hover:bg-[#00F59B] group-hover:text-black transition">Test Screen</span>
          </div>

          <div className="h-64 bg-[#070B0E] rounded-2xl border border-slate-800 p-3 space-y-2 flex flex-col justify-between">
            <div className="text-xs font-bold text-white">Settings</div>
            <div className="bg-[#0F151A] p-2 rounded-xl space-y-2 border border-[#1E2830] text-[10px]">
              <div className="flex justify-between items-center">
                <span className="text-white">Display over other apps</span>
                <span className="text-[#00F59B] font-bold">Grant</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-white">Screen capture</span>
                <span className="text-[#00F59B] font-bold">Grant</span>
              </div>
            </div>

            <div className="bg-[#0F151A] p-2 rounded-xl space-y-1.5 border border-[#1E2830] text-[10px]">
              <div className="flex justify-between items-center">
                <span className="text-slate-300">Language</span>
                <span className="text-[#00F59B] font-bold">English / বাংলা</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-slate-300">Button Style</span>
                <span className="text-[#00F59B]">Neon Green</span>
              </div>
            </div>

            <div className="text-[9px] text-slate-500 text-center">Floating PRO Scanner v1.0.0</div>
          </div>
          <div className="text-center text-xs text-slate-400 mt-2 font-medium">
            Overlay permissions, language, and AI connection
          </div>
        </div>

        {/* 4. Floating Button */}
        <div
          onClick={() => onSelectScreen('trading_overlay')}
          className="group bg-[#090D10] border border-[#1E2830] hover:border-[#00F59B] transition-all rounded-3xl p-4 flex flex-col justify-between cursor-pointer overflow-hidden relative shadow-lg"
        >
          <div className="flex items-center justify-between text-xs text-slate-400 mb-3">
            <span className="font-bold text-[#00F59B]">4. Floating Button</span>
            <span className="text-[10px] bg-slate-800 px-2 py-0.5 rounded text-slate-300 group-hover:bg-[#00F59B] group-hover:text-black transition">Test Screen</span>
          </div>

          <div className="relative h-64 bg-[#0A1017] rounded-2xl border border-slate-800 p-3 overflow-hidden flex flex-col justify-center items-center">
            {/* Background simulated trading chart */}
            <div className="absolute inset-0 opacity-30 flex items-center justify-center gap-2">
              <div className="w-1.5 h-16 bg-[#00F59B]" />
              <div className="w-1.5 h-28 bg-[#FF3B56]" />
              <div className="w-1.5 h-20 bg-[#00F59B]" />
              <div className="w-1.5 h-36 bg-[#00F59B]" />
            </div>

            {/* Glowing PRO bubble floating over chart */}
            <div className="relative z-10 w-14 h-14 rounded-full border-2 border-[#00F59B] bg-gradient-to-br from-[#0c2e22] to-[#05080A] flex items-center justify-center shadow-[0_0_25px_#00F59B] animate-bounce">
              <span className="text-xs font-black text-[#00F59B]">PRO</span>
            </div>
            <div className="absolute bottom-2 text-[9px] text-[#00F59B] font-semibold bg-black/60 px-2 py-0.5 rounded">
              Draggable over Quotex / Any Chart App
            </div>
          </div>
          <div className="text-center text-xs text-slate-400 mt-2 font-medium">
            Floating button over other apps
          </div>
        </div>

        {/* 5. Floating Panel (Collapsed) */}
        <div
          onClick={() => onSelectScreen('trading_overlay')}
          className="group bg-[#090D10] border border-[#1E2830] hover:border-[#00F59B] transition-all rounded-3xl p-4 flex flex-col justify-between cursor-pointer overflow-hidden relative shadow-lg"
        >
          <div className="flex items-center justify-between text-xs text-slate-400 mb-3">
            <span className="font-bold text-[#00F59B]">5. Floating Panel (Collapsed)</span>
            <span className="text-[10px] bg-slate-800 px-2 py-0.5 rounded text-slate-300 group-hover:bg-[#00F59B] group-hover:text-black transition">Test Screen</span>
          </div>

          <div className="h-64 bg-[#0A1017] rounded-2xl border border-slate-800 p-3 flex flex-col justify-center items-center">
            {/* Collapsed Pill mode */}
            <div className="bg-[#0F151A] border border-[#00F59B] rounded-full px-4 py-2 flex items-center gap-2.5 shadow-xl">
              <div className="w-6 h-6 rounded-full border border-[#00F59B] bg-[#0c2e22] flex items-center justify-center">
                <span className="text-[8px] font-black text-[#00F59B]">PRO</span>
              </div>
              <div className="text-left">
                <div className="text-[11px] font-bold text-white">PRO Scanner</div>
                <div className="text-[9px] text-slate-400">Tap to open</div>
              </div>
              <div className="flex items-center gap-1.5 text-slate-400 ml-2">
                <Settings className="w-3.5 h-3.5" />
                <span>✕</span>
              </div>
            </div>
          </div>
          <div className="text-center text-xs text-slate-400 mt-2 font-medium">
            Small collapsed pill mode
          </div>
        </div>

        {/* 6. Floating Panel (Expanded - Scanning) */}
        <div
          onClick={() => onSelectScreen('trading_overlay')}
          className="group bg-[#090D10] border border-[#1E2830] hover:border-[#00F59B] transition-all rounded-3xl p-4 flex flex-col justify-between cursor-pointer overflow-hidden relative shadow-lg"
        >
          <div className="flex items-center justify-between text-xs text-slate-400 mb-3">
            <span className="font-bold text-[#00F59B]">6. Floating Panel (Expanded)</span>
            <span className="text-[10px] bg-slate-800 px-2 py-0.5 rounded text-slate-300 group-hover:bg-[#00F59B] group-hover:text-black transition">Test Screen</span>
          </div>

          <div className="h-64 bg-[#070B0E] rounded-2xl border border-[#00F59B]/50 p-3 flex flex-col justify-between">
            <div className="flex justify-between items-center text-xs">
              <span className="font-black text-[#00F59B]">PRO SCANNER</span>
              <span className="text-slate-400 text-xs">− ✕</span>
            </div>

            <div className="flex flex-col items-center justify-center py-2">
              <div className="relative w-20 h-20 rounded-full border border-[#00F59B]/30 flex items-center justify-center mb-2">
                <div
                  className="absolute inset-0 rounded-full animate-spin"
                  style={{
                    background: 'conic-gradient(from 0deg, transparent 270deg, rgba(0,245,155,0.4) 360deg)'
                  }}
                />
                <div className="w-2 h-2 rounded-full bg-[#00F59B]" />
              </div>
              <div className="text-xs font-black text-[#00F59B]">SCANNING...</div>
              <div className="text-[9px] text-slate-400 text-center mt-0.5">
                Analyzing chart patterns, trend, support & resistance...
              </div>
            </div>

            <div className="border border-[#FF3B56] text-[#FF3B56] text-xs font-bold py-1 rounded-lg text-center">
              STOP
            </div>
          </div>
          <div className="text-center text-xs text-slate-400 mt-2 font-medium">
            Active scanning mode with radar animation
          </div>
        </div>

        {/* 7. Analysis Result (UP Signal) */}
        <div
          onClick={() => onSelectScreen('trading_overlay')}
          className="group bg-[#090D10] border border-[#1E2830] hover:border-[#00F59B] transition-all rounded-3xl p-4 flex flex-col justify-between cursor-pointer overflow-hidden relative shadow-lg"
        >
          <div className="flex items-center justify-between text-xs text-slate-400 mb-3">
            <span className="font-bold text-[#00F59B]">7. Analysis Result (UP)</span>
            <span className="text-[10px] bg-slate-800 px-2 py-0.5 rounded text-slate-300 group-hover:bg-[#00F59B] group-hover:text-black transition">Test Screen</span>
          </div>

          <div className="h-64 bg-[#070B0E] rounded-2xl border border-[#00F59B] p-3 space-y-2 overflow-y-auto">
            <div className="flex items-center gap-1.5 text-xs text-[#00F59B]">
              <CheckCircle className="w-3.5 h-3.5" />
              <span className="font-bold">SIGNAL READY</span>
            </div>

            <div className="bg-[#0E261D] border border-[#00F59B] rounded-xl p-2 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <div className="w-7 h-7 rounded-full bg-[#00F59B]/20 border border-[#00F59B] flex items-center justify-center text-[#00F59B]">
                  <ArrowUp className="w-4 h-4 stroke-[3]" />
                </div>
                <div>
                  <div className="text-sm font-black text-[#00F59B]">UP</div>
                  <div className="text-[9px] text-slate-300">Confidence: 78%</div>
                </div>
              </div>
              <span className="text-[10px] font-bold text-white">EUR/USD 5m</span>
            </div>

            <div className="text-[9px] text-slate-300 space-y-0.5">
              <div>• Uptrend is visible</div>
              <div>• Strong bullish candles near support</div>
              <div>• Volume looks increasing</div>
            </div>

            <div className="text-[8px] text-[#FFB800]">⚠ Risks: Market can be volatile</div>
          </div>
          <div className="text-center text-xs text-slate-400 mt-2 font-medium">
            UP signal with confidence and summary
          </div>
        </div>

        {/* 8. Other Results (DOWN / NO TRADE) */}
        <div
          onClick={() => onSelectScreen('history')}
          className="group bg-[#090D10] border border-[#1E2830] hover:border-[#00F59B] transition-all rounded-3xl p-4 flex flex-col justify-between cursor-pointer overflow-hidden relative shadow-lg"
        >
          <div className="flex items-center justify-between text-xs text-slate-400 mb-3">
            <span className="font-bold text-[#00F59B]">8. Other Results</span>
            <span className="text-[10px] bg-slate-800 px-2 py-0.5 rounded text-slate-300 group-hover:bg-[#00F59B] group-hover:text-black transition">Test Screen</span>
          </div>

          <div className="h-64 bg-[#070B0E] rounded-2xl border border-slate-800 p-3 space-y-2.5 flex flex-col justify-between">
            {/* DOWN Card */}
            <div className="bg-[#2E1217] border border-[#FF3B56] rounded-xl p-2 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <div className="w-7 h-7 rounded-full bg-[#FF3B56]/20 border border-[#FF3B56] flex items-center justify-center text-[#FF3B56]">
                  <ArrowDown className="w-4 h-4 stroke-[3]" />
                </div>
                <div>
                  <div className="text-xs font-black text-[#FF3B56]">DOWN</div>
                  <div className="text-[9px] text-slate-300">Confidence: 72%</div>
                </div>
              </div>
              <span className="text-[9px] text-slate-400">Rejection at resistance</span>
            </div>

            {/* NO TRADE Card */}
            <div className="bg-[#172026] border border-slate-600 rounded-xl p-2 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <div className="w-7 h-7 rounded-full bg-slate-700 border border-slate-400 flex items-center justify-center text-slate-300">
                  <Minus className="w-4 h-4 stroke-[3]" />
                </div>
                <div>
                  <div className="text-xs font-black text-slate-300">NO TRADE</div>
                  <div className="text-[9px] text-slate-400">Confidence: 56%</div>
                </div>
              </div>
              <span className="text-[9px] text-slate-400">Sideways movement</span>
            </div>

            <div className="text-[8px] text-slate-500 text-center">
              Always use your own analysis. This is not financial advice.
            </div>
          </div>
          <div className="text-center text-xs text-slate-400 mt-2 font-medium">
            DOWN and NO TRADE conditional evaluations
          </div>
        </div>

        {/* 9. Live Trading Overlay Demo */}
        <div
          onClick={() => onSelectScreen('trading_overlay')}
          className="group bg-[#090D10] border border-[#1E2830] hover:border-[#00F59B] transition-all rounded-3xl p-4 flex flex-col justify-between cursor-pointer overflow-hidden relative shadow-lg"
        >
          <div className="flex items-center justify-between text-xs text-slate-400 mb-3">
            <span className="font-bold text-[#00F59B]">9. Live Trading Overlay Demo</span>
            <span className="text-[10px] bg-slate-800 px-2 py-0.5 rounded text-slate-300 group-hover:bg-[#00F59B] group-hover:text-black transition">Test Screen</span>
          </div>

          <div className="h-64 bg-[#090E14] rounded-2xl border border-slate-800 p-2 flex flex-col justify-between relative overflow-hidden">
            <div className="flex items-center justify-between text-[10px] text-white">
              <span className="font-bold text-[#FF3B56]">QUOTEX $10,000</span>
              <span className="text-[#00F59B]">Deposit</span>
            </div>

            <div className="flex-1 flex items-center justify-center">
              <div className="w-12 h-12 rounded-full border-2 border-[#00F59B] bg-[#0c2e22] flex items-center justify-center shadow-[0_0_20px_#00F59B]">
                <span className="text-[10px] font-black text-[#00F59B]">PRO</span>
              </div>
            </div>

            <div className="bg-[#121B24] p-1.5 rounded-lg flex justify-between items-center text-[10px]">
              <span className="text-[#00F59B]">Analyzing... 00:12</span>
              <span className="bg-[#FF3B56] text-white px-2 py-0.5 rounded font-bold">DOWN</span>
            </div>
          </div>
          <div className="text-center text-xs text-slate-400 mt-2 font-medium">
            Full screen overlay working directly on Quotex
          </div>
        </div>

      </div>
    </div>
  );
};
