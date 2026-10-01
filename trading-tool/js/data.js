// Scaricamento dati storici. Le chiavi API restano solo nel browser dell'utente (localStorage).

const CACHE_HOURS = 12;

function storageGet(key) {
  try { return localStorage.getItem(key); } catch { return null; }
}
function storageSet(key, value) {
  try { localStorage.setItem(key, value); } catch { /* storage pieno o bloccato: si lavora senza cache */ }
}

export const settings = {
  get provider() { return storageGet('tt.provider') || 'alphavantage'; },
  set provider(v) { storageSet('tt.provider', v); },
  get avKey() { return storageGet('tt.avKey') || ''; },
  set avKey(v) { storageSet('tt.avKey', v.trim()); },
  get tdKey() { return storageGet('tt.tdKey') || ''; },
  set tdKey(v) { storageSet('tt.tdKey', v.trim()); },
  get claudeKey() { return storageGet('tt.claudeKey') || ''; },
  set claudeKey(v) { storageSet('tt.claudeKey', v.trim()); },
};

function cached(key) {
  const raw = storageGet('tt.cache.' + key);
  if (!raw) return null;
  try {
    const { t, data } = JSON.parse(raw);
    if (Date.now() - t > CACHE_HOURS * 3600000) return null;
    return data;
  } catch { return null; }
}
function store(key, data) {
  storageSet('tt.cache.' + key, JSON.stringify({ t: Date.now(), data }));
}

const isForexPair = (s) => /^[A-Z]{3}\/[A-Z]{3}$/.test(s);

async function getJson(url) {
  const res = await fetch(url);
  if (!res.ok) throw new Error(`Errore di rete (${res.status}).`);
  return res.json();
}

// ---------- Alpha Vantage ----------

function avError(json) {
  const msg = json['Error Message'] || json['Note'] || json['Information'];
  if (!msg) return null;
  if (/rate limit|frequency|premium|25 requests/i.test(msg)) return 'Limite gratuito di Alpha Vantage raggiunto (25 richieste al giorno) o funzione a pagamento. Riprova domani o usa Twelve Data. Dettaglio: ' + msg;
  if (/invalid api call/i.test(msg)) return 'Simbolo non trovato su Alpha Vantage. Usa la ricerca per trovare il codice esatto (es. VWCE.DEX per Xetra, SWDA.LON per Londra).';
  return msg;
}

async function alphaVantageSeries(symbol, key) {
  let url, seriesKey, closeField;
  if (isForexPair(symbol)) {
    const [from, to] = symbol.split('/');
    url = `https://www.alphavantage.co/query?function=FX_WEEKLY&from_symbol=${from}&to_symbol=${to}&apikey=${key}`;
    seriesKey = 'Time Series FX (Weekly)';
    closeField = '4. close';
  } else {
    url = `https://www.alphavantage.co/query?function=TIME_SERIES_WEEKLY_ADJUSTED&symbol=${encodeURIComponent(symbol)}&apikey=${key}`;
    seriesKey = 'Weekly Adjusted Time Series';
    closeField = '5. adjusted close'; // corretto per dividendi e frazionamenti
  }
  const json = await getJson(url);
  const err = avError(json);
  if (err) throw new Error(err);
  const raw = json[seriesKey];
  if (!raw) throw new Error('Risposta inattesa da Alpha Vantage.');
  const meta = json['Meta Data'] || {};
  const series = Object.entries(raw)
    .map(([d, v]) => ({ date: new Date(d + 'T00:00:00Z'), close: parseFloat(v[closeField] ?? v['4. close']) }))
    .filter((p) => p.close > 0)
    .sort((a, b) => a.date - b.date);
  return { series, name: meta['2. Symbol'] || symbol, currency: isForexPair(symbol) ? symbol.split('/')[1] : '' };
}

export async function alphaVantageFundamentals(symbol, key) {
  if (isForexPair(symbol)) return null;
  // Prima prova il profilo ETF (TER, dividendi, settori), poi la scheda società (P/E ecc.).
  try {
    const etf = await getJson(`https://www.alphavantage.co/query?function=ETF_PROFILE&symbol=${encodeURIComponent(symbol)}&apikey=${key}`);
    if (!avError(etf) && etf.net_assets) {
      return {
        kind: 'ETF',
        netAssets: parseFloat(etf.net_assets) || null,
        expenseRatio: parseFloat(etf.net_expense_ratio) || null,
        dividendYield: parseFloat(etf.dividend_yield) || null,
        inception: etf.inception_date || null,
        sectors: (etf.sectors || []).slice(0, 5).map((s) => ({ name: s.sector, weight: parseFloat(s.weight) })),
        holdings: (etf.holdings || []).slice(0, 5).map((h) => ({ name: h.description || h.symbol, weight: parseFloat(h.weight) })),
      };
    }
  } catch { /* non è un ETF o non disponibile */ }
  try {
    const ov = await getJson(`https://www.alphavantage.co/query?function=OVERVIEW&symbol=${encodeURIComponent(symbol)}&apikey=${key}`);
    if (!avError(ov) && ov.Symbol) {
      const num = (v) => (v && v !== 'None' && v !== '-' ? parseFloat(v) : null);
      return {
        kind: 'Azione',
        name: ov.Name,
        pe: num(ov.PERatio),
        dividendYield: num(ov.DividendYield),
        marketCap: num(ov.MarketCapitalization),
        sector: ov.Sector,
      };
    }
  } catch { /* nessun dato fondamentale */ }
  return null;
}

export async function alphaVantageSearch(query, key) {
  const json = await getJson(`https://www.alphavantage.co/query?function=SYMBOL_SEARCH&keywords=${encodeURIComponent(query)}&apikey=${key}`);
  const err = avError(json);
  if (err) throw new Error(err);
  return (json.bestMatches || []).map((m) => ({
    symbol: m['1. symbol'], name: m['2. name'], type: m['3. type'], region: m['4. region'], currency: m['8. currency'],
  }));
}

// ---------- Twelve Data ----------

async function twelveDataSeries(symbol, key) {
  const json = await getJson(`https://api.twelvedata.com/time_series?symbol=${encodeURIComponent(symbol)}&interval=1week&outputsize=5000&apikey=${key}`);
  if (json.status === 'error') {
    if (json.code === 429) throw new Error('Limite di richieste Twelve Data raggiunto (8 al minuto / 800 al giorno nel piano gratuito). Attendi un minuto.');
    if (/plan|upgrade|pro|grow/i.test(json.message || '')) throw new Error('Questo strumento non è incluso nel piano gratuito di Twelve Data (spesso le borse europee). Prova con Alpha Vantage. Dettaglio: ' + json.message);
    throw new Error(json.message || 'Errore Twelve Data.');
  }
  const series = (json.values || [])
    .map((v) => ({ date: new Date(v.datetime + 'T00:00:00Z'), close: parseFloat(v.close) }))
    .filter((p) => p.close > 0)
    .sort((a, b) => a.date - b.date);
  return { series, name: json.meta?.symbol || symbol, currency: json.meta?.currency || '' };
}

export async function twelveDataSearch(query) {
  const json = await getJson(`https://api.twelvedata.com/symbol_search?symbol=${encodeURIComponent(query)}`);
  return (json.data || []).map((m) => ({
    symbol: m.symbol, name: m.instrument_name, type: m.instrument_type, region: m.exchange, currency: m.currency,
  }));
}

// ---------- Interfaccia comune ----------

export async function fetchSeries(symbol, { withFundamentals = true } = {}) {
  symbol = symbol.trim().toUpperCase();
  const provider = settings.provider;
  const key = provider === 'twelvedata' ? settings.tdKey : settings.avKey;
  if (!key) throw new Error('Inserisci prima la chiave API gratuita nelle Impostazioni (icona ⚙).');
  const cacheKey = `${provider}.${symbol}`;
  let result = cached(cacheKey);
  if (result) {
    result.series = result.series.map((p) => ({ date: new Date(p.date), close: p.close }));
    result.fromCache = true;
  } else {
    result = provider === 'twelvedata' ? await twelveDataSeries(symbol, key) : await alphaVantageSeries(symbol, key);
    // I fondamentali richiedono richieste extra: solo con Alpha Vantage (Twelve Data li offre solo a pagamento).
    result.fundamentals = withFundamentals && provider === 'alphavantage' ? await alphaVantageFundamentals(symbol, key) : null;
    store(cacheKey, result);
  }
  if (result.series.length < 60) throw new Error(`Storico troppo corto (${result.series.length} settimane). Servono almeno ~60 settimane.`);
  return { ...result, symbol, provider };
}

export async function searchSymbols(query) {
  return settings.provider === 'twelvedata' ? twelveDataSearch(query) : alphaVantageSearch(query, settings.avKey);
}

// Serie dimostrativa (dati inventati) per provare il tool senza chiave API.
export function demoSeries() {
  let seed = 11;
  const rand = () => { seed = (seed * 16807) % 2147483647; return seed / 2147483647; };
  const gauss = () => Math.sqrt(-2 * Math.log(rand())) * Math.cos(2 * Math.PI * rand());
  const series = [];
  let p = 50;
  const weeks = 52 * 16;
  const start = Date.now() - weeks * 7 * 86400000; // la serie demo finisce sempre a oggi
  for (let i = 0; i < weeks; i++) {
    let drift = 0.0024;
    if (i > 520 && i < 545) drift = -0.018; // crollo stile 2020
    if (i > 640 && i < 680) drift = -0.006; // fase ribassista stile 2022
    p *= Math.exp(drift + 0.021 * gauss());
    series.push({ date: new Date(start + i * 7 * 86400000), close: p });
  }
  return { series, name: 'DEMO (dati simulati)', symbol: 'DEMO', currency: 'EUR', fundamentals: { kind: 'ETF', expenseRatio: 0.0022, dividendYield: 0.016 }, provider: 'demo' };
}
