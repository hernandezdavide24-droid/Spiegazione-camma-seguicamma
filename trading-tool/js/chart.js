// Grafico su canvas: prezzo (scala logaritmica), medie mobili, tendenza storica, livelli e ventaglio di proiezione.

const css = (name) => getComputedStyle(document.documentElement).getPropertyValue(name).trim();

export function drawChart(canvas, { series, analysis, showYears = 10, tooltipEl }) {
  const ctx = canvas.getContext('2d');
  const dpr = window.devicePixelRatio || 1;
  const W = canvas.clientWidth, H = canvas.clientHeight;
  canvas.width = W * dpr; canvas.height = H * dpr;
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  ctx.clearRect(0, 0, W, H);

  const n = series.length;
  const start = showYears ? Math.max(0, n - Math.round(showYears * 52)) : 0;
  const hist = series.slice(start);
  const ind = analysis.indicators;
  const proj = analysis.projection;
  const price = analysis.price;
  const lastT = hist[hist.length - 1].date.getTime();
  const weekMs = 7 * 86400000;
  const projDates = proj.bands.p50.map((_, k) => lastT + k * weekMs);

  const t0 = hist[0].date.getTime(), t1 = projDates[projDates.length - 1];
  let lo = Infinity, hi = -Infinity;
  for (const p of hist) { lo = Math.min(lo, p.close); hi = Math.max(hi, p.close); }
  for (let k = 0; k < projDates.length; k++) { lo = Math.min(lo, price * proj.bands.p5[k]); hi = Math.max(hi, price * proj.bands.p95[k]); }
  const lLo = Math.log(lo) - 0.05, lHi = Math.log(hi) + 0.05;

  const pad = { l: 8, r: 62, t: 12, b: 26 };
  const X = (t) => pad.l + ((t - t0) / (t1 - t0)) * (W - pad.l - pad.r);
  const Y = (v) => pad.t + (1 - (Math.log(v) - lLo) / (lHi - lLo)) * (H - pad.t - pad.b);

  const c = {
    grid: css('--grid'), text: css('--muted'), price: css('--ink'), sma40: css('--accent'), sma10: css('--accent-2'),
    trend: css('--muted'), fan: css('--fan'), fan2: css('--fan-2'), median: css('--pos'), sup: css('--pos'), res: css('--neg'),
  };
  ctx.font = '11px system-ui, sans-serif';

  // griglia orizzontale con prezzi "tondi"
  ctx.strokeStyle = c.grid; ctx.fillStyle = c.text; ctx.lineWidth = 1;
  for (const v of niceLogTicks(Math.exp(lLo), Math.exp(lHi))) {
    const y = Y(v);
    ctx.beginPath(); ctx.moveTo(pad.l, y); ctx.lineTo(W - pad.r, y); ctx.stroke();
    ctx.fillText(fmtPrice(v), W - pad.r + 6, y + 4);
  }
  // anni sull'asse X
  const y0 = new Date(t0).getUTCFullYear(), y1 = new Date(t1).getUTCFullYear();
  const stepY = Math.max(1, Math.ceil((y1 - y0) / Math.max(2, Math.floor((W - 80) / 60))));
  for (let y = y0 + 1; y <= y1; y += stepY) {
    const x = X(Date.UTC(y, 0, 1));
    if (x < pad.l + 16) continue;
    ctx.beginPath(); ctx.moveTo(x, pad.t); ctx.lineTo(x, H - pad.b); ctx.stroke();
    ctx.fillText(String(y), x - 14, H - 8);
  }
  // separatore "oggi"
  ctx.setLineDash([4, 4]); ctx.strokeStyle = c.text;
  ctx.beginPath(); ctx.moveTo(X(lastT), pad.t); ctx.lineTo(X(lastT), H - pad.b); ctx.stroke();
  ctx.setLineDash([]);
  ctx.fillText('oggi', X(lastT) + 4, pad.t + 10);

  // ventaglio di proiezione
  const band = (a, b, color) => {
    ctx.fillStyle = color; ctx.beginPath();
    projDates.forEach((t, k) => (k ? ctx.lineTo(X(t), Y(price * a[k])) : ctx.moveTo(X(t), Y(price * a[k]))));
    for (let k = projDates.length - 1; k >= 0; k--) ctx.lineTo(X(projDates[k]), Y(price * b[k]));
    ctx.closePath(); ctx.fill();
  };
  band(proj.bands.p5, proj.bands.p95, c.fan);
  band(proj.bands.p25, proj.bands.p75, c.fan2);
  line(ctx, projDates.map((t, k) => [X(t), Y(price * proj.bands.p50[k])]), c.median, 2, [6, 4]);

  // supporti e resistenze
  ctx.font = '10px system-ui, sans-serif';
  for (const [lvls, color] of [[analysis.levels.supports, c.sup], [analysis.levels.resistances, c.res]]) {
    for (const l of lvls) {
      if (l.price < Math.exp(lLo) || l.price > Math.exp(lHi)) continue;
      ctx.globalAlpha = 0.55;
      line(ctx, [[X(t0), Y(l.price)], [X(lastT), Y(l.price)]], color, 1, [2, 3]);
      ctx.globalAlpha = 1;
    }
  }

  const seg = (arr) => arr.slice(start).map((v, k) => (v == null ? null : [X(hist[k].date.getTime()), Y(v)]));
  line(ctx, seg(ind.trendLine), c.trend, 1, [1, 3]);
  line(ctx, seg(ind.sma40), c.sma40, 1.5);
  line(ctx, seg(ind.sma10), c.sma10, 1.2);
  line(ctx, hist.map((p) => [X(p.date.getTime()), Y(p.close)]), c.price, 1.6);

  // tooltip
  if (tooltipEl) {
    canvas.onmousemove = (e) => {
      const r = canvas.getBoundingClientRect();
      const t = t0 + ((e.clientX - r.left - pad.l) / (W - pad.l - pad.r)) * (t1 - t0);
      let html;
      if (t <= lastT) {
        let best = 0;
        for (let k = 0; k < hist.length; k++) if (Math.abs(hist[k].date - t) < Math.abs(hist[best].date - t)) best = k;
        const p = hist[best];
        html = `<b>${p.date.toLocaleDateString('it-IT')}</b><br>Prezzo ${fmtPrice(p.close)}`;
      } else {
        const k = Math.min(projDates.length - 1, Math.round((t - lastT) / weekMs));
        html = `<b>${new Date(projDates[k]).toLocaleDateString('it-IT', { month: 'short', year: 'numeric' })}</b> (proiezione)<br>
          Mediana ${fmtPrice(price * proj.bands.p50[k])}<br>50% tra ${fmtPrice(price * proj.bands.p25[k])} e ${fmtPrice(price * proj.bands.p75[k])}<br>90% tra ${fmtPrice(price * proj.bands.p5[k])} e ${fmtPrice(price * proj.bands.p95[k])}`;
      }
      tooltipEl.innerHTML = html;
      tooltipEl.hidden = false;
      const x = e.clientX - r.left;
      tooltipEl.style.left = Math.min(x + 12, W - 190) + 'px';
      tooltipEl.style.top = Math.max(0, e.clientY - r.top - 60) + 'px';
    };
    canvas.onmouseleave = () => (tooltipEl.hidden = true);
  }
}

function line(ctx, pts, color, width, dash = []) {
  ctx.strokeStyle = color; ctx.lineWidth = width; ctx.setLineDash(dash);
  ctx.beginPath();
  let drawing = false;
  for (const p of pts) {
    if (!p) { drawing = false; continue; }
    if (drawing) ctx.lineTo(p[0], p[1]); else { ctx.moveTo(p[0], p[1]); drawing = true; }
  }
  ctx.stroke(); ctx.setLineDash([]);
}

function niceLogTicks(lo, hi) {
  const ticks = [];
  const mults = hi / lo > 8 ? [1, 2, 5] : hi / lo > 2.5 ? [1, 1.5, 2, 3, 5, 7] : [1, 1.2, 1.4, 1.6, 1.8, 2, 2.5, 3, 4, 5, 6, 8];
  for (let e = Math.floor(Math.log10(lo)); e <= Math.ceil(Math.log10(hi)); e++) {
    for (const m of mults) { const v = m * 10 ** e; if (v >= lo && v <= hi) ticks.push(v); }
  }
  return ticks.length > 9 ? ticks.filter((_, i) => i % Math.ceil(ticks.length / 8) === 0) : ticks;
}

export function fmtPrice(v) {
  return v.toLocaleString('it-IT', { maximumFractionDigits: v < 2 ? 4 : v < 100 ? 2 : 0 });
}
