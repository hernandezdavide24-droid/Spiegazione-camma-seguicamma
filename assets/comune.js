/* Funzioni comuni delle pagine dei meccanismi: trascinamento, pulsanti, grafico, parti, quiz, comparsa delle sezioni */
(function(){
  "use strict";
  const $ = id => document.getElementById(id);
  const NS = "http://www.w3.org/2000/svg";
  const rad = d => d * Math.PI / 180, deg = r => r * 180 / Math.PI;
  const wrap180 = d => ((d % 360) + 540) % 360 - 180;
  const fmt = (v, n=0) => v.toLocaleString('it-IT', { minimumFractionDigits:n, maximumFractionDigits:n });

  function el(tag, attrs, parent){
    const e = document.createElementNS(NS, tag);
    for (const k in attrs) e.setAttribute(k, attrs[k]);
    if (parent) parent.appendChild(e);
    return e;
  }
  // punto del puntatore nelle coordinate del viewBox
  function svgPt(svg, e){
    const r = svg.getBoundingClientRect(), vb = svg.viewBox.baseVal;
    return { x: vb.x + (e.clientX - r.left) * vb.width / r.width, y: vb.y + (e.clientY - r.top) * vb.height / r.height };
  }
  // trascinamento generico: cb.start(p,e) -> false per ignorare, cb.move(p), cb.end()
  function drag(svg, cb){
    let on = false;
    svg.addEventListener('pointerdown', e => {
      const p = svgPt(svg, e);
      if (cb.start && cb.start(p, e) === false) return;
      on = true; svg.classList.add('dragging'); svg.setPointerCapture(e.pointerId);
      hideHint();
    });
    svg.addEventListener('pointermove', e => { if (on && cb.move) cb.move(svgPt(svg, e)); });
    const end = () => { if (!on) return; on = false; svg.classList.remove('dragging'); if (cb.end) cb.end(); };
    svg.addEventListener('pointerup', end); svg.addEventListener('pointercancel', end);
    return { get active(){ return on; } };
  }
  // trascinamento rotatorio attorno a un centro: onDelta(gradi, verso antiorario = positivo sullo schermo)
  function dragRotate(svg, center, onDelta, extra){
    let last = 0;
    const ang = p => { const c = typeof center === 'function' ? center() : center; return deg(Math.atan2(-(p.y - c.y), p.x - c.x)); };
    return drag(svg, {
      start: (p, e) => { if (extra && extra.start && extra.start(p, e) === false) return false; last = ang(p); },
      move: p => { const a = ang(p); const d = wrap180(a - last); last = a; onDelta(d); },
      end: () => extra && extra.end && extra.end()
    });
  }
  let hintEl = null;
  function hideHint(){ if (hintEl){ hintEl.classList.add('gone'); hintEl = null; } }
  function hint(id){ hintEl = $(id); }

  function seg(id, cb, initial){
    const bs = [...document.querySelectorAll('#' + id + ' button')];
    const set = v => { bs.forEach(b => b.classList.toggle('on', b.dataset.v === String(v))); cb(v); };
    bs.forEach(b => b.addEventListener('click', () => set(b.dataset.v)));
    if (initial !== undefined) set(initial);
    return set;
  }
  function toggle(id, initial, cb){
    const b = $(id); let state = initial;
    const set = v => { state = v; b.classList.toggle('on', v); b.setAttribute('aria-pressed', v); cb(v); };
    b.addEventListener('click', () => set(!state));
    set(initial);
    return set;
  }
  function phases(id, active){
    document.querySelectorAll('#' + id + ' .phase').forEach(p => p.classList.toggle('on', p.dataset.ph === String(active)));
  }
  let flashT = 0;
  function flash(id, text, good){
    const f = $(id); f.textContent = text; f.classList.toggle('good', !!good); f.classList.add('show');
    clearTimeout(flashT); flashT = setTimeout(() => f.classList.remove('show'), 1800);
  }

  // grafico: area utile x 24..330, y 72..8 (come la pagina della camma)
  function graph(id, xmax, ymin, ymax){
    const g = $(id);
    const X = x => 24 + (x / xmax) * 306, Y = y => 72 - ((y - ymin) / (ymax - ymin)) * 64;
    const ghost = g.querySelector('.g-ghost'), trace = g.querySelector('.g-trace'), dot = g.querySelector('.g-dot');
    let fn = null;
    const path = (f, x1) => { let d = ''; const n = 180; for (let i = 0; i <= n; i++){ const x = x1 * i / n; d += (i ? 'L' : 'M') + X(x).toFixed(1) + ',' + Y(f(x)).toFixed(1); } return d; };
    return {
      curve(f){ fn = f; ghost.setAttribute('d', path(f, xmax)); },
      range(a, b){ ymin = a; ymax = b; if (fn) ghost.setAttribute('d', path(fn, xmax)); },
      at(x, y){
        if (fn) trace.setAttribute('d', path(fn, Math.max(0.0001, x)));
        dot.setAttribute('cx', X(x).toFixed(1)); dot.setAttribute('cy', Y(y === undefined ? fn(x) : y).toFixed(1));
      }
    };
  }

  // "Tocca una parte": evidenzia gli elementi .p-<nome>
  function parts(stageId, chipsId, infoId, INFO, idle){
    const stage = $(stageId), info = $(infoId);
    const chips = document.querySelectorAll('#' + chipsId + ' .chip');
    const clear = () => document.querySelectorAll('#' + stageId + ' .part.lit').forEach(x => x.classList.remove('lit'));
    chips.forEach(c => c.addEventListener('click', () => {
      const same = stage.dataset.focus === c.dataset.p;
      chips.forEach(x => x.classList.remove('on')); clear();
      if (same){ delete stage.dataset.focus; info.textContent = idle; return; }
      c.classList.add('on'); stage.dataset.focus = c.dataset.p;
      document.querySelectorAll('#' + stageId + ' .p-' + c.dataset.p).forEach(x => x.classList.add('lit'));
      info.innerHTML = typeof INFO[c.dataset.p] === 'function' ? INFO[c.dataset.p]() : INFO[c.dataset.p];
    }));
  }
  function quiz(ok, ko){
    document.querySelectorAll('.opt').forEach(o => o.addEventListener('click', () => {
      document.querySelectorAll('.opt').forEach(x => x.classList.remove('right','wrong'));
      const good = o.dataset.ok === '1';
      o.classList.add(good ? 'right' : 'wrong');
      $('fb').textContent = good ? '✅ ' + ok : '❌ ' + ko;
    }));
  }
  function reveal(){
    const all = document.querySelectorAll('.reveal');
    if (!('IntersectionObserver' in window)){ all.forEach(x => x.classList.add('in')); return; }
    const io = new IntersectionObserver(es => es.forEach(e => { if (e.isIntersecting){ e.target.classList.add('in'); io.unobserve(e.target); } }), { threshold:.15 });
    all.forEach(x => io.observe(x));
  }
  function loop(step){
    let last = null;
    function f(t){
      if (last === null) last = t;
      const dt = Math.min((t - last) / 1000, 0.033); last = t;
      step(dt); requestAnimationFrame(f);
    }
    requestAnimationFrame(f);
  }
  function scrollToStage(id){ $(id).scrollIntoView({ behavior:'smooth', block:'center' }); }

  window.Mecc = { $, el, svgPt, drag, dragRotate, hint, seg, toggle, phases, flash, graph, parts, quiz, reveal, loop, scrollToStage, rad, deg, wrap180, fmt };
})();
