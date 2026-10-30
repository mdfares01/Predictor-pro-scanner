export type SignalType = 'UP' | 'DOWN' | 'NO_TRADE';

export interface ScanResult {
  id: string;
  signal: SignalType;
  confidence: number;
  pair: string;
  timeframe: string;
  evidence?: string[];
  reasoning?: string;
  uncertainty?: string[];
  summary: string[];
  risks: string[];
  keyReason: string;
  timestamp: number;
  disclaimer: string;
  modelUsed?: string;
}

export type AppScreen =
  | 'splash'
  | 'home'
  | 'settings'
  | 'history'
  | 'trading_overlay'
  | 'error_permission'
  | 'error_no_chart'
  | 'error_network'
  | 'error_ai';

export type Language = 'en' | 'bn';

export type FloatingState = 'bubble' | 'collapsed_pill' | 'expanded_panel';
