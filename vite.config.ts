import tailwindcss from '@tailwindcss/vite';
import react from '@vitejs/plugin-react';
import path from 'path';
import {defineConfig, Plugin} from 'vite';
import {GoogleGenAI, Type, ThinkingLevel} from '@google/genai';

function geminiScannerBackendPlugin(): Plugin {
  return {
    name: 'gemini-scanner-backend',
    configureServer(server) {
      server.middlewares.use(async (req, res, next) => {
        // Health check endpoint
        if (req.url === '/api/health' && req.method === 'GET') {
          res.setHeader('Content-Type', 'application/json');
          res.end(
            JSON.stringify({
              status: 'online',
              connected: true,
              hasApiKey: !!process.env.GEMINI_API_KEY,
              model: 'gemini-3.1-flash-lite (resilient fallback: gemini-3.8-flash)',
              timestamp: Date.now(),
            })
          );
          return;
        }

        // Real chart analysis endpoint
        if (req.url === '/api/analyze' && req.method === 'POST') {
          let body = '';
          req.on('data', chunk => {
            body += chunk;
          });

          req.on('end', async () => {
            let reqLanguage = 'en';
            try {
              const parsed = body ? JSON.parse(body) : {};
              const {imageBase64, requestedPair, requestedTimeframe, language} = parsed;
              reqLanguage = language || 'en';
              const isBn = reqLanguage === 'bn';

              // Validate request payload
              if (!imageBase64 || typeof imageBase64 !== 'string' || imageBase64.trim().length < 50) {
                res.statusCode = 400;
                res.setHeader('Content-Type', 'application/json');
                res.end(
                  JSON.stringify({
                    error: isBn
                      ? 'কোনো চার্ট স্ক্রিনশট পাওয়া যায়নি। অনুগ্রহ করে ক্যান্ডেলস্টিক চার্ট ক্যাপচার বা আপলোড করুন।'
                      : 'No chart screenshot provided. Capture or upload a visible candlestick chart.',
                  })
                );
                return;
              }

              const apiKey = process.env.GEMINI_API_KEY;

              // If GEMINI_API_KEY is not configured on the server, return an honest NO_TRADE without fabricating random signals
              if (!apiKey) {
                res.setHeader('Content-Type', 'application/json');
                res.end(
                  JSON.stringify({
                    id: String(Date.now()),
                    signal: 'NO_TRADE',
                    confidence: 50,
                    pair: requestedPair || 'EUR/USD',
                    timeframe: requestedTimeframe || '5m',
                    summary: isBn
                      ? [
                          'সার্ভারে জেমিনি এআই কি (API Key) কনফিগার করা নেই',
                          'সরাসরি এআই ভিশন বিশ্লেষণ ছাড়া সিগন্যাল তৈরি কঠোরভাবে নিষিদ্ধ',
                          'আসল চার্ট মূল্যায়নের জন্য ব্যাকএন্ডে API কি যোগ করুন',
                        ]
                      : [
                          'Gemini AI API key is not configured on the backend server',
                          'Fabricating simulated trading signals without live vision evaluation is prohibited',
                          'Add GEMINI_API_KEY in the server secrets to enable live vision chart scanning',
                        ],
                    risks: isBn
                      ? [
                          'লাইভ এআই মডেল অ্যাক্সেস ছাড়া কোনো দিকনির্দেশনা নির্ধারণ সম্ভব নয়',
                          'কাল্পনিক সিগন্যালের ভিত্তিতে ট্রেডিং অত্যন্ত ঝুঁকিপূর্ণ',
                        ]
                      : [
                          'Chart direction cannot be determined without live model access',
                          'Trading on unverified assumptions carries high risk of financial loss',
                        ],
                    keyReason: isBn
                      ? 'সার্ভারে এআই কি অনুপস্থিত — কোনো সিগন্যাল অনুমান করা হয়নি'
                      : 'Backend AI credentials unconfigured — strictly refusing fabricated signals',
                    timestamp: Date.now(),
                    disclaimer: isBn
                      ? 'এটি শুধুমাত্র ভিজ্যুয়াল পর্যবেক্ষণ। কোনো আর্থিক পরামর্শ বা জয়ের নিশ্চয়তা নয়।'
                      : 'Observational visual interpretation only. Not financial advice. No profit guarantee.',
                  })
                );
                return;
              }

              // Multimodal Vision Analysis with multi-model fallback
              const ai = new GoogleGenAI({
                apiKey,
                httpOptions: {
                  headers: {
                    'User-Agent': 'aistudio-build',
                  },
                },
              });

              const cleanBase64 = imageBase64.replace(/^data:image\/[a-z]+;base64,/, '');

              const prompt = `You are a professional, objective technical chart analyst examining a user-authorized trading screenshot.
CRITICAL INTEGRITY INSTRUCTIONS:
1. DETECT CHART: First determine if a valid candlestick chart (or bar chart) is visible in the image. If the image is blank, blurry, cropped, dark, decorative simulator candles, or missing price/candlestick context, you MUST return signal "NO_TRADE" and state that no readable chart was detected.
2. OBSERVABLE EVIDENCE: State concrete visual observations visible in the image (e.g. "Series of consecutive green bullish candles testing 1.0855 level", "Long upper shadow rejection candle formed at resistance", "Moving averages flat and tangled in narrow range").
3. DIRECTIONAL BIAS:
   - Only return "UP" if there is unambiguous bullish momentum bouncing from verified support with bullish price action.
   - Only return "DOWN" if there is unambiguous bearish rejection at verified resistance with bearish confirmation.
   - If market is in a tight range, consolidating, choppy, alternating candle colors, or evidence is insufficient, YOU MUST return "NO_TRADE".
4. OTC MARKETS: If the market is OTC (e.g., includes "(OTC)" or marked as OTC), explicitly state in uncertainties and risks that future price movement cannot be reliably determined from a screenshot alone.
5. CALIBRATED CONFIDENCE: Set confidence score between 50% and 78%. This reflects visual assessment clarity, NEVER a probability of winning or financial guarantee. Never output 100%, 90%, or promise profit.
6. UNCERTAINTIES & RISKS: Explicitly list missing or conflicting evidence in the uncertainty field, and 2 market risks in the risks field.
7. Language: Output text strictly in ${isBn ? 'Bangla (বাংলা)' : 'English'}.
8. Pair and timeframe: Identify from chart markings if visible, or fallback to "${requestedPair || 'EUR/USD'}" and "${requestedTimeframe || '5m'}".
9. DISCLAIMER: Emphasize that this is an observational AI assessment based on screenshot only, not financial advice, and trading carries high financial risk.`;

              const responseSchema = {
                type: Type.OBJECT,
                properties: {
                  signal: {
                    type: Type.STRING,
                    description: 'Strictly "UP", "DOWN", or "NO_TRADE"',
                  },
                  confidence: {
                    type: Type.INTEGER,
                    description: 'Calibrated score between 50 and 78 reflecting visual clarity',
                  },
                  pair: {
                    type: Type.STRING,
                    description: 'Asset pair symbol e.g. EUR/USD',
                  },
                  timeframe: {
                    type: Type.STRING,
                    description: 'Timeframe e.g. 5m, 1m',
                  },
                  evidence: {
                    type: Type.ARRAY,
                    items: {type: Type.STRING},
                    description: 'Factual visual observations supported by the screenshot',
                  },
                  reasoning: {
                    type: Type.STRING,
                    description: 'Why evidence supports or fails to support a directional assessment',
                  },
                  uncertainty: {
                    type: Type.ARRAY,
                    items: {type: Type.STRING},
                    description: 'Missing, unreadable, or conflicting chart evidence',
                  },
                  summary: {
                    type: Type.ARRAY,
                    items: {type: Type.STRING},
                    description: '3 to 4 bullet points of observable chart conditions',
                  },
                  risks: {
                    type: Type.ARRAY,
                    items: {type: Type.STRING},
                    description: '2 key uncertainties or market risks',
                  },
                  keyReason: {
                    type: Type.STRING,
                    description: 'Concise reason for assessment',
                  },
                  disclaimer: {
                    type: Type.STRING,
                    description: 'Standard risk disclaimer',
                  },
                },
                required: [
                  'signal',
                  'confidence',
                  'pair',
                  'timeframe',
                  'evidence',
                  'reasoning',
                  'uncertainty',
                  'summary',
                  'risks',
                  'keyReason',
                  'disclaimer',
                ],
              };

              // Multi-model pipeline: gemini-3.1-flash-lite (fast, minimal latency, avoids deep thinking stalls)
              // followed by gemini-3.8-flash (configured with ThinkingLevel.LOW to avoid latency spikes)
              const candidateModels: Array<{name: string; timeoutMs: number; thinkingLevel?: ThinkingLevel}> = [
                {name: 'gemini-3.1-flash-lite', timeoutMs: 15000},
                {name: 'gemini-3.8-flash', timeoutMs: 20000, thinkingLevel: ThinkingLevel.LOW},
              ];

              let parsedResult: any = null;
              let usedModel = 'gemini-3.1-flash-lite';
              let lastError: any = null;

              for (const candidate of candidateModels) {
                try {
                  const timeoutPromise = new Promise((_, reject) =>
                    setTimeout(
                      () => reject(new Error(`Model ${candidate.name} exceeded ${candidate.timeoutMs / 1000}s timeout threshold`)),
                      candidate.timeoutMs
                    )
                  );

                  const modelConfig: any = {
                    responseMimeType: 'application/json',
                    responseSchema,
                  };

                  if (candidate.thinkingLevel) {
                    modelConfig.thinkingConfig = {
                      thinkingLevel: candidate.thinkingLevel,
                    };
                  }

                  const geminiPromise = ai.models.generateContent({
                    model: candidate.name,
                    contents: {
                      parts: [
                        {
                          inlineData: {
                            mimeType: 'image/jpeg',
                            data: cleanBase64,
                          },
                        },
                        {
                          text: prompt,
                        },
                      ],
                    },
                    config: modelConfig,
                  });

                  const response: any = await Promise.race([geminiPromise, timeoutPromise]);
                  const text = response?.text;

                  if (text) {
                    parsedResult = JSON.parse(text);
                    usedModel = candidate.name;
                    break;
                  }
                } catch (err: any) {
                  console.warn(`Vision model ${candidate.name} fallback trigger:`, err?.message || err);
                  lastError = err;
                }
              }

              if (parsedResult) {
                parsedResult.id = String(Date.now());
                parsedResult.timestamp = Date.now();
                parsedResult.modelUsed = usedModel;

                // Sync fallback fields if empty
                if (!parsedResult.summary || parsedResult.summary.length === 0) {
                  parsedResult.summary = parsedResult.evidence || [];
                }
                if (!parsedResult.keyReason) {
                  parsedResult.keyReason = parsedResult.reasoning || '';
                }

                res.setHeader('Content-Type', 'application/json');
                res.end(JSON.stringify(parsedResult));
                return;
              }

              throw lastError || new Error('Empty response from Gemini vision model');
            } catch (err: any) {
              console.error('Analysis error:', err?.message || err);
              const isBn = reqLanguage === 'bn';
              let cleanErrorMessage = isBn
                ? 'এআই দৃষ্টি বিশ্লেষণ প্রক্রিয়ায় একটি ত্রুটি দেখা দিয়েছে। অনুগ্রহ করে পরিষ্কার চার্ট সহ আবার চেষ্টা করুন।'
                : 'AI vision analysis encountered an error. Please try again with a clear screenshot.';
              let statusCode = 500;

              if (err?.message) {
                const msg = String(err.message);
                if (msg.includes('timeout') || msg.includes('timed out') || msg.includes('exceeded')) {
                  statusCode = 504;
                  cleanErrorMessage = isBn
                    ? 'এআই মডেলের প্রতিক্রিয়ার সময়সীমা অতিক্রান্ত হয়েছে। অনুগ্রহ করে কয়েক সেকেন্ড পর আবার স্ক্যান করুন।'
                    : 'The AI vision model timed out under heavy load. Please retry your scan in a few moments.';
                } else {
                  try {
                    const parsedErr = JSON.parse(msg);
                    if (parsedErr?.error?.message) {
                      cleanErrorMessage = parsedErr.error.message;
                    }
                    if (parsedErr?.error?.code) {
                      statusCode = Number(parsedErr.error.code) || 500;
                    }
                  } catch {
                    cleanErrorMessage = msg;
                  }
                }
              }

              res.statusCode = statusCode >= 400 && statusCode < 600 ? statusCode : 500;
              res.setHeader('Content-Type', 'application/json');
              res.end(
                JSON.stringify({
                  error: cleanErrorMessage,
                })
              );
            }
          });
          return;
        }

        next();
      });
    },
  };
}

export default defineConfig(() => {
  return {
    plugins: [react(), tailwindcss(), geminiScannerBackendPlugin()],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, '.'),
      },
    },
    server: {
      hmr: process.env.DISABLE_HMR !== 'true',
      watch: process.env.DISABLE_HMR === 'true' ? null : {},
    },
  };
});
