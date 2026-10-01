// Motore di analisi: funzioni pure, nessun accesso al DOM.
// Tutte le serie sono array di { date: Date, close: number } ordinati dal più vecchio al più recente,
// campionati settimanalmente (il tool è pensato per il lungo periodo).

export const WEEKS_PER_YEAR = 52;

// ---------- Utilità ----------

export function toWeekly(series) {
  // Tiene l'ultimo prezzo di ogni settimana ISO: serve per serie giornaliere o irregolari (es. screenshot).
  const out = [];
  let lastKey = null;
  for (const p of series) {
    const d = new Date(p.date);
    const monday = new Date(Date.UTC(d.getUTCFullYear(), d.getUTCMonth(), d.getUTCDate() - ((d.getUTCDay() + 6) % 7)));
    const key = monday.getTime();
    if (key === lastKey) out[out.length - 1] = { date: d, close: p.close };
    else out.push({ date: d, close: p.close });
    lastKey = key;
  }
  return out;
}

export function resampleEvenly(points, stepDays = 7) {
  // Interpola linearmente punti sparsi (es. presi da uno screenshot) su una griglia regolare.
  const pts = [...points].sort((a, b) => a.date - b.date);
  if (pts.length < 2) return pts;
  const out = [];
  const step = stepDays * 86400000;
  let j = 0;
  for (let t = pts[0].date.getTime(); t <= pts[pts.length - 1].date.getTime(); t += step) {
    while (j < pts.length - 2 && pts[j + 1].date.getTime() < t) j++;
    const a = pts[j], b = pts[j + 1];
    const span = b.date - a.date || 1;
    const w = Math.min(1, Math.max(0, (t - a.date) / span));
    // interpolazione in scala logaritmica: più corretta per i prezzi
    out.push({ date: new Date(t), close: Math.exp(Math.log(a.close) * (1 - w) + Math.log(b.close) * w) });
  }
  const last = pts[pts.length - 1];
  if (out[out.length - 1].date < last.date) out.push({ date: last.date, close: last.close });
  return out;
}

const mean = (a) => a.reduce((s, x) => s + x, 0) / a.length;
const std = (a) => {
  const m = mean(a);
  return Math.sqrt(a.reduce((s, x) => s + (x - m) ** 2, 0) / Math.max(1, a.length - 1));
};
export function quantile(sorted, q) {
  const pos = (sorted.length - 1) * q;
  const lo = Math.floor(pos), hi = Math.ceil(pos);
  return sorted[lo] + (sorted[hi] - sorted[lo]) * (pos - lo);
}

// ---------- Indicatori tecnici ----------

export function sma(values, n) {
  const out = new Array(values.length).fill(null);
  let sum = 0;
  for (let i = 0; i < values.length; i++) {
    sum += values[i];
    if (i >= n) sum -= values[i - n];
    if (i >= n - 1) out[i] = sum / n;
  }
  return out;
}

export function ema(values, n) {
  const out = new Array(values.length).fill(null);
  const k = 2 / (n + 1);
  let prev = null;
  for (let i = 0; i < values.length; i++) {
    if (i === n - 1) prev = mean(values.slice(0, n));
    else if (i >= n) prev = values[i] * k + prev * (1 - k);
    out[i] = prev;
  }
  return out;
}

export function rsi(values, n = 14) {
  // RSI di Wilder
  const out = new Array(values.length).fill(null);
  if (values.length <= n) return out;
  let gain = 0, loss = 0;
  for (let i = 1; i <= n; i++) {
    const d = values[i] - values[i - 1];
    if (d >= 0) gain += d; else loss -= d;
  }
  gain /= n; loss /= n;
  out[n] = loss === 0 ? 100 : 100 - 100 / (1 + gain / loss);
  for (let i = n + 1; i < values.length; i++) {
    const d = values[i] - values[i - 1];
    gain = (gain * (n - 1) + Math.max(d, 0)) / n;
    loss = (loss * (n - 1) + Math.max(-d, 0)) / n;
    out[i] = loss === 0 ? 100 : 100 - 100 / (1 + gain / loss);
  }
  return out;
}

export function macd(values, fast = 12, slow = 26, signal = 9) {
  const ef = ema(values, fast), es = ema(values, slow);
  const line = values.map((_, i) => (ef[i] != null && es[i] != null ? ef[i] - es[i] : null));
  const start = line.findIndex((v) => v != null);
  const sig = new Array(values.length).fill(null);
  if (start >= 0) {
    const e = ema(line.slice(start), signal);
    e.forEach((v, i) => (sig[start + i] = v));
  }
  const hist = line.map((v, i) => (v != null && sig[i] != null ? v - sig[i] : null));
  return { line, signal: sig, hist };
}

export function drawdowns(values) {
  let peak = -Infinity, maxDd = 0, peakIdx = 0;
  const series = values.map((v, i) => {
    if (v > peak) { peak = v; peakIdx = i; }
    const dd = v / peak - 1;
    if (dd < maxDd) maxDd = dd;
    return dd;
  });
  return { series, current: series[series.length - 1], max: maxDd, athIndex: peakIdx };
}

// Supporti e resistenze: minimi/massimi locali raggruppati per vicinanza di prezzo.
export function supportResistance(series, { window = 6, lookbackWeeks = 260, tolerance = 0.035 } = {}) {
  const data = series.slice(-lookbackWeeks);
  const pivots = [];
  for (let i = window; i < data.length - window; i++) {
    const seg = data.slice(i - window, i + window + 1).map((p) => p.close);
    const v = data[i].close;
    if (v === Math.max(...seg)) pivots.push({ price: v, type: 'high' });
    if (v === Math.min(...seg)) pivots.push({ price: v, type: 'low' });
  }
  pivots.sort((a, b) => a.price - b.price);
  const clusters = [];
  for (const p of pivots) {
    const c = clusters[clusters.length - 1];
    if (c && p.price / c.avg - 1 < tolerance) {
      c.prices.push(p.price);
      c.avg = mean(c.prices);
    } else clusters.push({ prices: [p.price], avg: p.price });
  }
  const last = series[series.length - 1].close;
  const levels = clusters.map((c) => ({ price: c.avg, touches: c.prices.length }));
  return {
    supports: levels.filter((l) => l.price < last).sort((a, b) => b.price - a.price).slice(0, 3),
    resistances: levels.filter((l) => l.price > last).sort((a, b) => a.price - b.price).slice(0, 3),
  };
}

// Tendenza di lungo periodo: regressione lineare del log-prezzo nel tempo.
// La distanza dalla retta (in deviazioni standard) è un indicatore di "caro/economico" rispetto alla storia.
export function logTrend(series) {
  const x = series.map((_, i) => i / WEEKS_PER_YEAR);
  const y = series.map((p) => Math.log(p.close));
  const mx = mean(x), my = mean(y);
  let num = 0, den = 0;
  for (let i = 0; i < x.length; i++) { num += (x[i] - mx) * (y[i] - my); den += (x[i] - mx) ** 2; }
  const slope = num / den, intercept = my - slope * mx;
  const fitted = x.map((xi) => intercept + slope * xi);
  const resid = y.map((yi, i) => yi - fitted[i]);
  const sd = std(resid);
  return {
    annualGrowth: Math.exp(slope) - 1,
    line: fitted.map(Math.exp),
    zScore: resid[resid.length - 1] / sd,
    deviation: Math.exp(resid[resid.length - 1]) - 1,
  };
}

// ---------- Statistiche ----------

export function stats(series) {
  const closes = series.map((p) => p.close);
  const rets = [];
  for (let i = 1; i < closes.length; i++) rets.push(Math.log(closes[i] / closes[i - 1]));
  const years = (series[series.length - 1].date - series[0].date) / (365.25 * 86400000);
  const cagr = Math.pow(closes[closes.length - 1] / closes[0], 1 / years) - 1;
  const vol = std(rets) * Math.sqrt(WEEKS_PER_YEAR);
  const ret1y = closes.length > WEEKS_PER_YEAR ? closes[closes.length - 1] / closes[closes.length - 1 - WEEKS_PER_YEAR] - 1 : null;
  const ret3y = closes.length > 3 * WEEKS_PER_YEAR ? Math.pow(closes[closes.length - 1] / closes[closes.length - 1 - 3 * WEEKS_PER_YEAR], 1 / 3) - 1 : null;
  return { years, cagr, vol, ret1y, ret3y, logReturns: rets };
}

// ---------- Proiezione Monte Carlo ----------

// Generatore pseudo-casuale con seme: stessi dati => stessa proiezione (risultati riproducibili).
export function mulberry32(seed) {
  return function () {
    seed |= 0; seed = (seed + 0x6d2b79f5) | 0;
    let t = Math.imul(seed ^ (seed >>> 15), 1 | seed);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

// Block bootstrap: ricampiona blocchi di settimane storiche consecutive, così conserva
// le "code grasse" e i periodi di crisi reali invece di assumere una curva a campana.
export function monteCarlo(logReturns, { years = 5, sims = 2000, block = 12, seed = 42, driftAdjust = 0 } = {}) {
  const rand = mulberry32(seed);
  const steps = Math.round(years * WEEKS_PER_YEAR);
  const n = logReturns.length;
  const paths = new Array(sims);
  for (let s = 0; s < sims; s++) {
    const path = new Float64Array(steps + 1);
    let acc = 0, t = 1;
    while (t <= steps) {
      const start = Math.floor(rand() * Math.max(1, n - block));
      for (let k = 0; k < block && t <= steps; k++, t++) {
        acc += logReturns[(start + k) % n] + driftAdjust;
        path[t] = acc;
      }
    }
    paths[s] = path;
  }
  const bands = { p5: [], p25: [], p50: [], p75: [], p95: [] };
  const col = new Float64Array(sims);
  for (let t = 0; t <= steps; t++) {
    for (let s = 0; s < sims; s++) col[s] = paths[s][t];
    const sorted = Array.from(col).sort((a, b) => a - b);
    bands.p5.push(Math.exp(quantile(sorted, 0.05)));
    bands.p25.push(Math.exp(quantile(sorted, 0.25)));
    bands.p50.push(Math.exp(quantile(sorted, 0.5)));
    bands.p75.push(Math.exp(quantile(sorted, 0.75)));
    bands.p95.push(Math.exp(quantile(sorted, 0.95)));
  }
  const finals = paths.map((p) => Math.exp(p[steps])).sort((a, b) => a - b);
  let worstDd = [];
  for (const p of paths) {
    let peak = 0, dd = 0;
    for (let t = 0; t <= steps; t++) { if (p[t] > peak) peak = p[t]; dd = Math.min(dd, p[t] - peak); }
    worstDd.push(Math.exp(dd) - 1);
  }
  worstDd.sort((a, b) => a - b);
  return {
    steps,
    bands, // multipli del prezzo attuale
    probLoss: finals.filter((f) => f < 1).length / sims,
    probDouble: finals.filter((f) => f >= 2).length / sims,
    medianMultiple: quantile(finals, 0.5),
    p5Multiple: quantile(finals, 0.05),
    p95Multiple: quantile(finals, 0.95),
    medianWorstDrawdown: quantile(worstDd, 0.5),
  };
}

// ---------- Profili e punteggio ----------

export const PROFILES = {
  bilanciato: {
    label: 'Bilanciato',
    weights: { trend: 0.30, momentum: 0.15, valuation: 0.30, drawdown: 0.25 },
    rebalanceZ: 1.5, // oltre questa sopravvalutazione suggerisce di prendere profitto parziale
    dipThreshold: -0.15, // ribasso dai massimi oltre cui suggerisce acquisti extra
    maxTrim: 0.2,
  },
  dinamico: {
    label: 'Dinamico',
    weights: { trend: 0.25, momentum: 0.15, valuation: 0.20, drawdown: 0.40 },
    rebalanceZ: 2.0,
    dipThreshold: -0.10,
    maxTrim: 0.1,
  },
};

const clamp = (v, a = -1, b = 1) => Math.max(a, Math.min(b, v));

// Calcola i sotto-punteggi (da -1 a +1) all'indice i usando solo dati fino a i (niente "sbirciate" nel futuro).
export function subScores(ctx, i) {
  const { closes, sma10, sma40, rsiArr, macdObj, trendLine, trendSd } = ctx;
  const price = closes[i];
  // Trend: posizione rispetto alla media a 40 settimane (~200 giorni) e incrocio 10/40 settimane.
  let trend = 0;
  if (sma40[i] != null) {
    trend += price > sma40[i] ? 0.5 : -0.5;
    trend += sma10[i] > sma40[i] ? 0.3 : -0.3;
    const slope = sma40[i - 4] != null ? sma40[i] / sma40[i - 4] - 1 : 0;
    trend += clamp(slope * 20, -0.2, 0.2);
  }
  // Momentum: RSI e MACD. Per il lungo periodo un RSI basso è un'occasione, uno molto alto un campanello.
  let momentum = 0;
  if (rsiArr[i] != null) {
    const r = rsiArr[i];
    if (r < 30) momentum += 0.6; else if (r > 75) momentum -= 0.6; else momentum += (50 - Math.abs(r - 55)) / 100;
  }
  if (macdObj.hist[i] != null) momentum += macdObj.hist[i] > 0 ? 0.3 : -0.3;
  // Valutazione: quanto il prezzo è sopra/sotto la sua tendenza storica di lungo periodo.
  const z = trendLine ? (Math.log(price) - Math.log(trendLine[i])) / trendSd : 0;
  const valuation = clamp(-z / 2);
  // Drawdown: ribassi dai massimi = prezzi di saldo per chi accumula sul lungo periodo.
  let peak = 0;
  for (let k = 0; k <= i; k++) peak = Math.max(peak, closes[k]);
  const dd = price / peak - 1;
  const drawdown = clamp(-dd * 4 - 0.1);
  return { trend: clamp(trend), momentum: clamp(momentum), valuation, drawdown, z, dd };
}

export function combine(sub, profile) {
  const w = profile.weights;
  return Math.round(100 * (w.trend * sub.trend + w.momentum * sub.momentum + w.valuation * sub.valuation + w.drawdown * sub.drawdown));
}

function buildContext(series, trendUntil = series.length) {
  const closes = series.map((p) => p.close);
  // La retta di tendenza viene stimata solo sui dati disponibili fino a trendUntil.
  const lt = logTrend(series.slice(0, trendUntil));
  const growth = Math.log(1 + lt.annualGrowth) / WEEKS_PER_YEAR;
  const base = Math.log(lt.line[0]);
  const trendLine = closes.map((_, i) => Math.exp(base + growth * i));
  const resid = series.slice(0, trendUntil).map((p, i) => Math.log(p.close) - Math.log(trendLine[i]));
  return {
    closes,
    sma10: sma(closes, 10),
    sma40: sma(closes, 40),
    rsiArr: rsi(closes, 14),
    macdObj: macd(closes),
    trendLine,
    trendSd: std(resid) || 1,
  };
}

// Verifica storica onesta: per ogni settimana passata calcola il punteggio con i soli dati disponibili allora
// (retta di tendenza ristimata ogni anno) e misura il rendimento effettivo dei 12 mesi successivi.
export function backtestScore(series, profile, horizonWeeks = WEEKS_PER_YEAR) {
  const minHistory = 3 * WEEKS_PER_YEAR;
  if (series.length < minHistory + horizonWeeks + 10) return null;
  const buckets = { high: [], mid: [], low: [] };
  const all = [];
  let ctx = null;
  for (let i = minHistory; i < series.length - horizonWeeks; i++) {
    if (!ctx || (i - minHistory) % WEEKS_PER_YEAR === 0) ctx = buildContext(series, i + 1);
    const score = combine(subScores(ctx, i), profile);
    const fwd = series[i + horizonWeeks].close / series[i].close - 1;
    all.push(fwd);
    (score >= 20 ? buckets.high : score <= -20 ? buckets.low : buckets.mid).push(fwd);
  }
  const summarize = (a) => (a.length ? { n: a.length, avg: mean(a), winRate: a.filter((x) => x > 0).length / a.length } : null);
  return { horizonWeeks, all: summarize(all), high: summarize(buckets.high), mid: summarize(buckets.mid), low: summarize(buckets.low) };
}

// ---------- Analisi completa ----------

export function analyze(series, { profileKey = 'bilanciato', years = 5, fundamentals = null } = {}) {
  if (series.length < 60) throw new Error('Servono almeno ~60 settimane di dati per un\'analisi sensata.');
  const profile = PROFILES[profileKey];
  const ctx = buildContext(series);
  const i = series.length - 1;
  const price = ctx.closes[i];
  const sub = subScores(ctx, i);
  const score = combine(sub, profile);
  const st = stats(series);
  const lt = logTrend(series);
  const dd = drawdowns(ctx.closes);
  const sr = supportResistance(series);
  // Proiezione: rendimenti storici ricampionati. Se lo storico è molto breve o eccezionale,
  // la crescita viene "ridimensionata" a metà strada verso un 6% annuo di riferimento, per non promettere troppo.
  const histDrift = mean(st.logReturns);
  const refDrift = Math.log(1.06) / WEEKS_PER_YEAR;
  const shrink = Math.min(1, st.years / 20);
  const driftAdjust = (refDrift - histDrift) * (1 - shrink);
  const mc = monteCarlo(st.logReturns, { years, driftAdjust, seed: Math.round(price * 1000) % 100000 });
  const bt = backtestScore(series, profile);

  const signals = describeSignals(ctx, i, sub, lt, dd, st);
  const advice = buildAdvice({ score, sub, profile, dd, lt, sr, price, st, mc, fundamentals });

  return {
    price, score, sub, profile, stats: st, trend: lt, drawdown: dd, levels: sr,
    projection: mc, years, backtest: bt, signals, advice,
    indicators: { sma10: ctx.sma10, sma40: ctx.sma40, rsi: ctx.rsiArr, macd: ctx.macdObj, trendLine: lt.line },
  };
}

function describeSignals(ctx, i, sub, lt, dd, st) {
  const out = [];
  const price = ctx.closes[i];
  const s40 = ctx.sma40[i], s10 = ctx.sma10[i];
  const pct = (x) => (x * 100).toFixed(1) + '%';
  if (s40 != null) {
    out.push({
      tone: price > s40 ? 'pos' : 'neg',
      title: price > s40 ? 'Sopra la media di lungo periodo' : 'Sotto la media di lungo periodo',
      text: `Il prezzo è ${price > s40 ? 'sopra' : 'sotto'} la media mobile a 40 settimane (equivalente alla "200 giorni") del ${pct(Math.abs(price / s40 - 1))}. ${price > s40 ? 'Il trend principale è rialzista.' : 'Il trend principale è debole o ribassista.'}`,
    });
    // incrocio recente (ultime 8 settimane)
    for (let k = i; k > i - 8 && k > 0; k--) {
      if (ctx.sma10[k - 1] == null || ctx.sma40[k - 1] == null) break;
      const before = ctx.sma10[k - 1] - ctx.sma40[k - 1], after = ctx.sma10[k] - ctx.sma40[k];
      if (before <= 0 && after > 0) { out.push({ tone: 'pos', title: 'Golden cross recente', text: `${i - k} settimane fa la media breve (10 sett.) ha superato quella lunga (40 sett.): storicamente segnala l'inizio di una fase positiva.` }); break; }
      if (before >= 0 && after < 0) { out.push({ tone: 'neg', title: 'Death cross recente', text: `${i - k} settimane fa la media breve (10 sett.) è scesa sotto quella lunga (40 sett.): segnale di debolezza, spesso però tardivo.` }); break; }
    }
    if (!out.some((s) => s.title.includes('cross'))) {
      out.push({ tone: s10 > s40 ? 'pos' : 'neg', title: s10 > s40 ? 'Medie allineate al rialzo' : 'Medie allineate al ribasso', text: `La media a 10 settimane è ${s10 > s40 ? 'sopra' : 'sotto'} quella a 40 settimane.` });
    }
  }
  const r = ctx.rsiArr[i];
  if (r != null) {
    out.push({
      tone: r < 30 ? 'pos' : r > 70 ? 'neg' : 'neu',
      title: `RSI settimanale ${r.toFixed(0)}`,
      text: r < 30 ? 'Ipervenduto: il prezzo è sceso molto e in fretta. Per chi investe a lungo termine spesso è una zona di acquisto.' : r > 70 ? 'Ipercomprato: salita molto rapida, aumenta la probabilità di una pausa o di un ritracciamento.' : 'Zona neutra: né eccessi di euforia né di panico.',
    });
  }
  const h = ctx.macdObj.hist[i];
  if (h != null) out.push({ tone: h > 0 ? 'pos' : 'neg', title: `MACD ${h > 0 ? 'positivo' : 'negativo'}`, text: h > 0 ? 'La spinta di breve è in accelerazione rispetto alla media.' : 'La spinta di breve sta rallentando.' });
  out.push({
    tone: lt.zScore > 1.5 ? 'neg' : lt.zScore < -1 ? 'pos' : 'neu',
    title: lt.zScore > 0 ? `${pct(lt.deviation)} sopra la tendenza storica` : `${pct(-lt.deviation)} sotto la tendenza storica`,
    text: `La linea di tendenza che meglio descrive tutto lo storico sale del ${pct(lt.annualGrowth)} l'anno (è diversa dal rendimento medio annuo perché pesa tutto il percorso, non solo il punto di partenza e di arrivo). Oggi il prezzo è a ${Math.abs(lt.zScore).toFixed(1)} deviazioni standard ${lt.zScore > 0 ? 'sopra' : 'sotto'} quella linea: ${lt.zScore > 1.5 ? 'relativamente caro' : lt.zScore < -1 ? 'relativamente economico' : 'in linea con la sua storia'}.`,
  });
  out.push({
    tone: dd.current < -0.2 ? 'pos' : 'neu',
    title: dd.current > -0.01 ? 'Sui massimi storici' : `${pct(-dd.current)} sotto i massimi`,
    text: `Il ribasso massimo mai registrato nello storico è stato del ${pct(-dd.max)}. ${dd.current < -0.2 ? 'Ribassi così ampi in passato sono stati buone occasioni di accumulo per chi ha avuto pazienza.' : ''}`,
  });
  return out;
}

function buildAdvice({ score, sub, profile, dd, lt, sr, price, st, mc, fundamentals }) {
  let action, tone;
  if (score >= 35) { action = 'Accumula'; tone = 'pos'; }
  else if (score >= 10) { action = 'Compra / continua il PAC'; tone = 'pos'; }
  else if (score > -15) { action = 'Mantieni'; tone = 'neu'; }
  else if (score > -35) { action = 'Mantieni con prudenza'; tone = 'neg'; }
  else { action = 'Alleggerisci'; tone = 'neg'; }

  const tips = [];
  const fmt = (v) => v.toLocaleString('it-IT', { maximumFractionDigits: v < 10 ? 4 : 2 });

  if (lt.zScore > profile.rebalanceZ) {
    tips.push(`Prezzo molto sopra la sua tendenza storica: valuta di <b>prendere profitto su una parte</b> (fino al ${Math.round(profile.maxTrim * 100)}% della posizione) o di ribilanciare verso la tua allocazione obiettivo, senza uscire del tutto.`);
    if (action === 'Accumula' || action === 'Compra / continua il PAC') action = 'Mantieni (non aumentare)';
  }
  if (dd.current <= profile.dipThreshold) {
    tips.push(`Il prezzo è ${(-dd.current * 100).toFixed(0)}% sotto i massimi: per un profilo ${profile.label.toLowerCase()} è sensato un <b>acquisto extra</b> oltre al PAC, meglio se a rate (es. 3 tranche a distanza di un mese).`);
  }
  if (sr.supports.length) {
    tips.push(`Livelli di supporto (zone dove il prezzo in passato ha rimbalzato): ${sr.supports.map((s) => '<b>' + fmt(s.price) + '</b>').join(', ')}. Ordini di acquisto programmati in queste zone migliorano il prezzo medio di carico.`);
  }
  if (sr.resistances.length) {
    tips.push(`Resistenze (zone dove il prezzo si è fermato in passato): ${sr.resistances.map((s) => '<b>' + fmt(s.price) + '</b>').join(', ')}. Un superamento deciso e duraturo conferma il trend rialzista.`);
  } else {
    tips.push('Il prezzo è sopra tutte le resistenze recenti (zona di massimi): il trend è forte, ma evita di concentrare grossi acquisti in un\'unica soluzione.');
  }
  if (sub.trend < -0.4) {
    tips.push(`Trend di lungo periodo negativo: chi preferisce limitare i ribassi può usare come <b>soglia di controllo</b> una chiusura settimanale sotto ${fmt(price * (1 - Math.min(0.25, st.vol)))} (circa una volatilità annua sotto il prezzo attuale) per rivedere il peso in portafoglio.`);
  }
  tips.push(`Con la volatilità storica (${(st.vol * 100).toFixed(0)}% annuo) metti in conto, nei prossimi anni, cali temporanei intorno al ${(-mc.medianWorstDrawdown * 100).toFixed(0)}% anche nello scenario mediano. Investi solo denaro di cui non hai bisogno in quell'orizzonte.`);
  if (fundamentals?.expenseRatio != null && fundamentals.expenseRatio > 0.005) {
    tips.push(`Costo annuo (TER) del ${(fundamentals.expenseRatio * 100).toFixed(2)}%: alto per un investimento di lungo periodo. Verifica se esiste un ETF equivalente più economico.`);
  }
  if (fundamentals?.pe != null) {
    const pe = fundamentals.pe;
    tips.push(`Rapporto prezzo/utili (P/E) di ${pe.toFixed(1)}: ${pe > 30 ? 'elevato, il mercato sconta già molta crescita futura' : pe < 12 ? 'contenuto, valutazione a sconto' : 'nella norma storica dei mercati azionari'}.`);
  }
  return { action, tone, tips };
}
