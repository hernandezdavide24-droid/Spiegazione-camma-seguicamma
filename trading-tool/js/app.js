import { analyze, WEEKS_PER_YEAR } from './analysis.js';
import { fetchSeries, searchSymbols, demoSeries, settings } from './data.js';
import { drawChart, fmtPrice } from './chart.js';
import { readChartWithClaude, pixelsToSeries } from './vision.js';

const $ = (id) => document.getElementById(id);
const pct = (x, d = 1) => (x == null ? '—' : (x >= 0 ? '+' : '') + (x * 100).toFixed(d) + '%');

let current = null; // { series, name, symbol, currency, fundamentals, observations }
let lastAnalysis = null;

// ---------- Impostazioni ----------

$('openSettings').onclick = () => {
  $('provider').value = settings.provider;
  $('avKey').value = settings.avKey;
  $('tdKey').value = settings.tdKey;
  $('claudeKey').value = settings.claudeKey;
  $('settings').showModal();
};
$('settings').addEventListener('close', () => {
  if ($('settings').returnValue !== 'save') return;
  settings.provider = $('provider').value;
  settings.avKey = $('avKey').value;
  settings.tdKey = $('tdKey').value;
  settings.claudeKey = $('claudeKey').value;
});

// ---------- Tab ----------

document.querySelectorAll('[data-tab]').forEach((b) => {
  b.onclick = () => {
    document.querySelectorAll('[data-tab]').forEach((x) => x.setAttribute('aria-selected', x === b));
    $('tab-ticker').hidden = b.dataset.tab !== 'ticker';
    $('tab-shot').hidden = b.dataset.tab !== 'shot';
  };
});

// ---------- Stato e messaggi ----------

function showError(msg) { $('error').textContent = msg; $('error').hidden = !msg; }
function setStatus(html) { $('status').innerHTML = html; }
function busy(on, text = 'Analisi in corso…') {
  $('analyzeBtn').disabled = on;
  setStatus(on ? `<span class="spinner"></span>${text}` : '');
}

// ---------- Da ticker ----------

$('tickerForm').onsubmit = async (e) => {
  e.preventDefault();
  const sym = $('symbol').value.trim();
  if (!sym) return;
  $('suggestions').hidden = true;
  showError('');
  busy(true, 'Scarico lo storico dei prezzi…');
  try {
    const data = await fetchSeries(sym);
    current = data;
    run();
    const first = data.series[0].date.toLocaleDateString('it-IT');
    setStatus(`Dati ${data.provider === 'twelvedata' ? 'Twelve Data' : 'Alpha Vantage'} dal ${first}, ${data.series.length} settimane${data.fromCache ? ' (in cache, aggiornati entro 12 ore)' : ''}.`);
  } catch (err) {
    busy(false);
    showError(err.message);
  } finally {
    $('analyzeBtn').disabled = false;
  }
};

document.querySelectorAll('[data-sym]').forEach((b) => {
  b.onclick = () => { $('symbol').value = b.dataset.sym; $('tickerForm').requestSubmit(); };
});
$('demoBtn').onclick = () => {
  showError('');
  current = demoSeries();
  run();
  setStatus('Dati <b>simulati</b> a scopo dimostrativo: non rappresentano alcuno strumento reale.');
};

// suggerimenti di ricerca (con pausa per non consumare richieste API a ogni tasto)
let searchTimer;
$('symbol').addEventListener('input', () => {
  clearTimeout(searchTimer);
  const q = $('symbol').value.trim();
  if (q.length < 3 || q.includes('/') || !(settings.provider === 'twelvedata' || settings.avKey)) { $('suggestions').hidden = true; return; }
  searchTimer = setTimeout(async () => {
    try {
      const res = (await searchSymbols(q)).slice(0, 8);
      const ul = $('suggestions');
      ul.innerHTML = '';
      for (const r of res) {
        const li = document.createElement('li');
        li.innerHTML = `<b>${esc(r.symbol)}</b> — ${esc(r.name || '')} <span class="muted">${esc([r.type, r.region, r.currency].filter(Boolean).join(' · '))}</span>`;
        li.onclick = () => { $('symbol').value = r.symbol; ul.hidden = true; };
        ul.appendChild(li);
      }
      ul.hidden = !res.length;
    } catch { $('suggestions').hidden = true; }
  }, 700);
});
document.addEventListener('click', (e) => { if (!e.target.closest('.suggest')) $('suggestions').hidden = true; });

// ---------- Da screenshot ----------

let shotFile = null;
const drop = $('drop');
drop.addEventListener('dragover', (e) => { e.preventDefault(); drop.classList.add('over'); });
drop.addEventListener('dragleave', () => drop.classList.remove('over'));
drop.addEventListener('drop', (e) => { e.preventDefault(); drop.classList.remove('over'); if (e.dataTransfer.files[0]) pickShot(e.dataTransfer.files[0]); });
$('shotFile').onchange = (e) => e.target.files[0] && pickShot(e.target.files[0]);
document.addEventListener('paste', (e) => {
  const item = [...(e.clipboardData?.items || [])].find((i) => i.type.startsWith('image/'));
  if (item && !$('tab-shot').hidden) pickShot(item.getAsFile());
});

function pickShot(file) {
  if (!file.type.startsWith('image/')) return showError('Il file deve essere un\'immagine (PNG o JPG).');
  showError('');
  shotFile = file;
  drop.querySelector('p').innerHTML = `Immagine caricata: <b>${esc(file.name || 'dagli appunti')}</b>. Scegli come leggerla.`;
  $('shotOptions').hidden = false;
  $('digitizer').hidden = true;
}

$('aiRead').onclick = async () => {
  if (!settings.claudeKey) return showError('Inserisci la chiave Anthropic nelle Impostazioni, oppure usa la digitalizzazione manuale.');
  showError('');
  $('aiRead').disabled = true;
  setStatus('<span class="spinner"></span>Claude sta leggendo il grafico (può richiedere fino a un minuto)…');
  try {
    const data = await readChartWithClaude(shotFile, settings.claudeKey);
    if (data.series.length < 60) throw new Error(`Il grafico copre solo ${data.series.length} settimane: per il lungo periodo servono almeno ~60 settimane (meglio 3+ anni).`);
    current = { ...data, symbol: 'screenshot', fundamentals: null };
    run();
    setStatus(`Serie ricostruita dall'immagine: ${data.series.length} settimane dal ${data.series[0].date.toLocaleDateString('it-IT')}. <b>I valori sono stimati</b> leggendo il grafico: verifica che la curva nel grafico qui sotto somigli all'originale.`);
  } catch (err) {
    setStatus('');
    showError(err.message?.includes('401') ? 'Chiave Anthropic non valida.' : err.message);
  } finally {
    $('aiRead').disabled = false;
  }
};

// digitalizzazione manuale
const dig = { img: null, scale: 1, calib: {}, clicks: [], stage: 0 };
const STAGES = [
  { key: 'x1', text: '1/5 — Clicca su un punto dell\'asse orizzontale di cui conosci la data (il più a sinistra possibile), poi scrivi la data.', kind: 'date' },
  { key: 'x2', text: '2/5 — Clicca su un secondo punto dell\'asse orizzontale (il più a destra possibile) e scrivi la data.', kind: 'date' },
  { key: 'y1', text: '3/5 — Clicca su un\'etichetta di prezzo in basso sull\'asse verticale e scrivi il valore.', kind: 'price' },
  { key: 'y2', text: '4/5 — Clicca su un\'etichetta di prezzo in alto sull\'asse verticale e scrivi il valore.', kind: 'price' },
  { key: 'curve', text: '5/5 — Clicca lungo la curva del prezzo da sinistra a destra (almeno 25–30 punti, includi massimi, minimi e l\'ultimo punto). Poi premi "Analizza la curva".' },
];

$('manualRead').onclick = () => {
  const url = URL.createObjectURL(shotFile);
  const img = new Image();
  img.onload = () => {
    dig.img = img;
    resetDig();
    $('digitizer').hidden = false;
  };
  img.src = url;
};
function resetDig() {
  dig.calib = {}; dig.clicks = []; dig.stage = 0;
  renderDig();
}
function renderDig() {
  const cv = $('digCanvas');
  const maxW = Math.min(dig.img.naturalWidth, $('digitizer').clientWidth || 900);
  dig.scale = maxW / dig.img.naturalWidth;
  cv.width = maxW; cv.height = dig.img.naturalHeight * dig.scale;
  const ctx = cv.getContext('2d');
  ctx.drawImage(dig.img, 0, 0, cv.width, cv.height);
  const mark = (x, y, color, label) => {
    ctx.fillStyle = color; ctx.beginPath(); ctx.arc(x, y, 5, 0, 7); ctx.fill();
    if (label) { ctx.font = 'bold 12px system-ui'; ctx.fillText(label, x + 7, y - 7); }
  };
  for (const [k, v] of Object.entries(dig.calib)) mark(v.x, v.y, '#2563eb', k.toUpperCase());
  ctx.strokeStyle = '#dc2626'; ctx.lineWidth = 2; ctx.beginPath();
  dig.clicks.forEach((c, i) => (i ? ctx.lineTo(c.x, c.y) : ctx.moveTo(c.x, c.y)));
  ctx.stroke();
  dig.clicks.forEach((c) => mark(c.x, c.y, '#dc2626'));
  const st = STAGES[dig.stage];
  $('digStep').textContent = st.text;
  $('digDone').disabled = dig.stage < 4 || dig.clicks.length < 8;
  const inputs = $('digInputs');
  inputs.innerHTML = '';
  const pending = st.kind && dig.calib[st.key] && dig.calib[st.key].value == null;
  if (pending) {
    const inp = document.createElement('input');
    inp.type = st.kind === 'date' ? 'date' : 'number';
    inp.step = 'any';
    const ok = document.createElement('button');
    ok.className = 'primary'; ok.textContent = 'Conferma';
    ok.onclick = () => {
      const v = st.kind === 'date' ? new Date(inp.value + 'T00:00:00Z') : parseFloat(inp.value);
      if (st.kind === 'date' ? isNaN(v) : !(v > 0)) return showError('Valore non valido.');
      showError('');
      dig.calib[st.key].value = v;
      dig.stage++;
      renderDig();
    };
    inputs.append(inp, ok);
    inp.focus();
  }
}
$('digCanvas').onclick = (e) => {
  const r = e.target.getBoundingClientRect();
  const x = (e.clientX - r.left) * (e.target.width / r.width), y = (e.clientY - r.top) * (e.target.height / r.height);
  const st = STAGES[dig.stage];
  if (st.kind) {
    if (dig.calib[st.key]?.value != null) return;
    dig.calib[st.key] = { x, y, value: null };
  } else dig.clicks.push({ x, y });
  renderDig();
};
$('digUndo').onclick = () => { dig.clicks.pop(); renderDig(); };
$('digReset').onclick = resetDig;
$('digDone').onclick = () => {
  const c = dig.calib;
  const calib = {
    x1: { px: c.x1.x, value: c.x1.value }, x2: { px: c.x2.x, value: c.x2.value },
    y1: { px: c.y1.y, value: c.y1.value }, y2: { px: c.y2.y, value: c.y2.value },
  };
  const clicks = [...dig.clicks].sort((a, b) => a.x - b.x);
  const series = pixelsToSeries(calib, clicks, $('digLog').checked);
  if (series.length < 60) return showError(`La curva copre solo ${series.length} settimane: servono almeno ~60 settimane (meglio 3+ anni).`);
  showError('');
  current = { series, name: 'Grafico digitalizzato a mano', symbol: 'screenshot', fundamentals: null };
  run();
  setStatus(`Serie ricostruita a mano: ${series.length} settimane. I valori sono approssimati.`);
};

// ---------- Analisi e rendering ----------

['profile', 'horizon'].forEach((id) => ($(id).onchange = () => current && run()));
$('window').onchange = () => lastAnalysis && renderChart();
window.addEventListener('resize', () => lastAnalysis && renderChart());

function run() {
  showError('');
  try {
    lastAnalysis = analyze(current.series, {
      profileKey: $('profile').value,
      years: parseInt($('horizon').value, 10),
      fundamentals: current.fundamentals,
    });
  } catch (err) {
    busy(false);
    return showError(err.message);
  }
  busy(false);
  render(lastAnalysis);
}

function renderChart() {
  drawChart($('chart'), { series: current.series, analysis: lastAnalysis, showYears: parseInt($('window').value, 10), tooltipEl: $('tooltip') });
}

function render(a) {
  $('results').hidden = false;
  const cur = current.currency ? ' ' + current.currency : '';
  $('assetName').innerHTML = `${esc(current.name)} · ultimo prezzo <b>${fmtPrice(a.price)}${esc(cur)}</b> (${current.series.at(-1).date.toLocaleDateString('it-IT')}) · profilo ${a.profile.label.toLowerCase()}`;
  $('action').textContent = a.advice.action;
  $('action').className = 'action ' + a.advice.tone;
  $('gaugeMark').style.left = (a.score + 100) / 2 + '%';
  $('scoreText').innerHTML = `Punteggio complessivo <b>${a.score > 0 ? '+' : ''}${a.score}</b> su una scala da −100 a +100. Pensato per chi investe a lungo termine: un punteggio negativo invita alla prudenza, non a vendere tutto.`;

  const subLabel = { trend: 'Trend', momentum: 'Momentum', valuation: 'Valutazione', drawdown: 'Sconto dai massimi' };
  $('subs').innerHTML = Object.entries(subLabel).map(([k, l]) => {
    const v = a.sub[k];
    const cls = v > 0.15 ? 'pos' : v < -0.15 ? 'neg' : 'neu';
    return `<div class="sub">${l}<b class="${cls}">${v > 0 ? '+' : ''}${Math.round(v * 100)}</b><span class="muted">peso ${Math.round(a.profile.weights[k] * 100)}%</span></div>`;
  }).join('');

  renderChart();

  const p = a.projection;
  const y = a.years;
  $('kpis').innerHTML = [
    ['Scenario mediano', `${fmtPrice(a.price * p.medianMultiple)}`, pct(p.medianMultiple - 1, 0)],
    ['Scenario sfavorevole (5%)', `${fmtPrice(a.price * p.p5Multiple)}`, pct(p.p5Multiple - 1, 0)],
    ['Scenario favorevole (95%)', `${fmtPrice(a.price * p.p95Multiple)}`, pct(p.p95Multiple - 1, 0)],
    [`Probabilità di perdita a ${y} ${y === 1 ? 'anno' : 'anni'}`, (p.probLoss * 100).toFixed(0) + '%', ''],
    ['Rendimento annuo storico', pct(a.stats.cagr), `su ${a.stats.years.toFixed(0)} anni`],
    ['Volatilità annua', (a.stats.vol * 100).toFixed(0) + '%', 'oscillazione tipica'],
    ['Ribasso massimo storico', pct(a.drawdown.max, 0), ''],
    ['Ultimi 12 mesi', pct(a.stats.ret1y), a.stats.ret3y != null ? `3 anni: ${pct(a.stats.ret3y)}/anno` : ''],
  ].map(([l, v, s]) => `<div class="kpi"><span>${l}</span><b>${v}</b><span>${s}</span></div>`).join('');
  $('projText').innerHTML = `Tra ${y} ${y === 1 ? 'anno' : 'anni'}, nel 90% delle 2.000 simulazioni il prezzo finisce tra <b>${fmtPrice(a.price * p.p5Multiple)}</b> e <b>${fmtPrice(a.price * p.p95Multiple)}</b>; nella metà dei casi tra ${fmtPrice(a.price * p.bands.p25.at(-1))} e ${fmtPrice(a.price * p.bands.p75.at(-1))}. Più l'orizzonte è lungo, più conta la crescita media e meno il punto d'ingresso.${a.stats.years < 10 ? ' <b>Attenzione:</b> lo storico disponibile è breve, la proiezione è meno affidabile.' : ''}`;

  $('tips').innerHTML = a.advice.tips.map((t) => `<li>${t}</li>`).join('');
  $('signals').innerHTML = a.signals.map((s) => `<div class="signal ${s.tone}"><b>${esc(s.title)}</b>${esc(s.text)}</div>`).join('');
  $('aiObs').hidden = !current.observations;
  if (current.observations) $('aiObs').innerHTML = `<div class="signal"><b>Lettura dell'immagine (Claude)</b>${esc(current.observations)}</div>`;

  renderFundamentals(current.fundamentals);
  renderBacktest(a.backtest);
}

function renderFundamentals(f) {
  $('fundCard').hidden = !f;
  if (!f) return;
  const items = [['Tipo', f.kind]];
  if (f.name) items.push(['Nome', esc(f.name)]);
  if (f.expenseRatio != null) items.push(['Costo annuo (TER)', (f.expenseRatio * 100).toFixed(2) + '%']);
  if (f.dividendYield != null) items.push(['Rendimento da dividendi', (f.dividendYield * 100).toFixed(2) + '%']);
  if (f.pe != null) items.push(['P/E (prezzo/utili)', f.pe.toFixed(1)]);
  if (f.netAssets) items.push(['Patrimonio', (f.netAssets / 1e9).toFixed(1) + ' mld']);
  if (f.marketCap) items.push(['Capitalizzazione', (f.marketCap / 1e9).toFixed(1) + ' mld']);
  if (f.inception) items.push(['Nato il', esc(f.inception)]);
  if (f.sector) items.push(['Settore', esc(f.sector)]);
  $('fund').innerHTML = items.map(([l, v]) => `<div class="kpi"><span>${l}</span><b>${v}</b></div>`).join('');
  const parts = [];
  if (f.sectors?.length) parts.push('Settori principali: ' + f.sectors.map((s) => `${esc(s.name)} ${(s.weight * 100).toFixed(0)}%`).join(', '));
  if (f.holdings?.length) parts.push('Prime posizioni: ' + f.holdings.map((h) => `${esc(h.name)} ${(h.weight * 100).toFixed(1)}%`).join(', '));
  $('fundExtra').innerHTML = parts.join('<br>');
}

function renderBacktest(bt) {
  if (!bt) {
    $('backtest').innerHTML = '<p class="muted">Servono almeno 4–5 anni di dati per verificare il punteggio sul passato.</p>';
    return;
  }
  const row = (label, s) => s ? `<tr><td>${label}</td><td class="num">${s.n}</td><td class="num">${pct(s.avg)}</td><td class="num">${(s.winRate * 100).toFixed(0)}%</td></tr>` : `<tr><td>${label}</td><td class="num">0</td><td class="num">—</td><td class="num">—</td></tr>`;
  const edge = bt.high && bt.low ? bt.high.avg - bt.low.avg : null;
  let verdict;
  if (edge == null || !bt.high || !bt.low || bt.high.n < 30 || bt.low.n < 30) verdict = 'Pochi casi in una delle categorie: il confronto non è statisticamente solido.';
  else if (edge > 0.04) verdict = `Su questo strumento il punteggio ha distinto bene i momenti buoni da quelli cattivi (+${(edge * 100).toFixed(0)} punti percentuali di differenza). Resta un dato passato, non una garanzia.`;
  else verdict = 'Su questo strumento il punteggio <b>non</b> ha distinto in modo utile i momenti di ingresso: conviene affidarsi soprattutto al PAC costante e al ribilanciamento.';
  $('backtest').innerHTML = `<div class="table-scroll"><table>
    <tr><th>Quando il punteggio era…</th><th class="num">Settimane</th><th class="num">Rendimento medio a ${Math.round(bt.horizonWeeks / WEEKS_PER_YEAR * 12)} mesi</th><th class="num">% volte in guadagno</th></tr>
    ${row('Alto (≥ +20)', bt.high)}${row('Neutro', bt.mid)}${row('Basso (≤ −20)', bt.low)}${row('<b>Sempre (riferimento)</b>', bt.all)}
  </table></div><p class="muted">${verdict}</p>`;
}

function esc(s) {
  return String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}
