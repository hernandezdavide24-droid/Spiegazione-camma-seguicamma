// Lettura di uno screenshot di grafico: con Claude (AI) oppure con digitalizzazione manuale.

import { resampleEvenly } from './analysis.js';

const SDK_URL = 'https://cdn.jsdelivr.net/npm/@anthropic-ai/sdk/+esm';

const CHART_SCHEMA = {
  type: 'object',
  additionalProperties: false,
  required: ['is_price_chart', 'asset_name', 'currency', 'y_axis_log', 'points', 'observations'],
  properties: {
    is_price_chart: { type: 'boolean', description: 'false se l\'immagine non è un grafico di prezzo leggibile' },
    asset_name: { type: 'string', description: 'Nome o ticker dello strumento, se visibile; altrimenti stringa vuota' },
    currency: { type: 'string' },
    y_axis_log: { type: 'boolean' },
    points: {
      type: 'array',
      description: 'Da 40 a 150 punti della curva del prezzo, in ordine cronologico, letti usando le etichette degli assi',
      items: {
        type: 'object',
        additionalProperties: false,
        required: ['date', 'price'],
        properties: {
          date: { type: 'string', description: 'Data ISO AAAA-MM-GG stimata dall\'asse orizzontale' },
          price: { type: 'number' },
        },
      },
    },
    observations: { type: 'string', description: 'Breve lettura tecnica in italiano: trend, supporti/resistenze visibili, figure grafiche' },
  },
};

const PROMPT = `Sei un analista tecnico. Questa immagine dovrebbe essere un grafico di prezzo (azione, ETF, indice, materia prima o cambio).
Ricostruisci la serie storica della curva principale del prezzo leggendo con cura le etichette degli assi:
- usa le date dell'asse orizzontale per stimare la data di ogni punto; se mancano anno o giorno, stimali in modo coerente;
- usa la scala dell'asse verticale (controlla se è logaritmica) per stimare il prezzo;
- se ci sono candele usa i prezzi di chiusura; ignora volumi, indicatori e linee di medie mobili;
- prendi punti a intervalli regolari e includi i massimi e minimi evidenti e l'ultimo punto a destra.
Se l'immagine non è un grafico di prezzo, imposta is_price_chart a false e lascia points vuoto.`;

export async function readChartWithClaude(file, apiKey) {
  const { default: Anthropic } = await import(SDK_URL);
  // La chiave viene usata direttamente dal browser: è la tua chiave personale, resta sul tuo dispositivo.
  const client = new Anthropic({ apiKey, dangerouslyAllowBrowser: true });
  const data = await fileToBase64(file);
  const response = await client.beta.messages.create({
    model: 'claude-opus-5-5',
    max_tokens: 16000,
    betas: ['server-side-fallback-2026-07-01'],
    fallbacks: 'default',
    output_config: { effort: 'high', format: { type: 'json_schema', schema: CHART_SCHEMA } },
    messages: [{
      role: 'user',
      content: [
        { type: 'image', source: { type: 'base64', media_type: file.type || 'image/png', data } },
        { type: 'text', text: PROMPT },
      ],
    }],
  });
  if (response.stop_reason === 'refusal') throw new Error('Claude non ha elaborato l\'immagine. Prova con la modalità manuale.');
  if (response.stop_reason === 'max_tokens') throw new Error('Risposta troncata: riprova con un\'immagine più semplice.');
  const text = response.content.filter((b) => b.type === 'text').map((b) => b.text).join('');
  const parsed = JSON.parse(text);
  if (!parsed.is_price_chart || parsed.points.length < 10) throw new Error('L\'immagine non sembra un grafico di prezzo leggibile. Prova con la modalità manuale.');
  const pts = parsed.points
    .map((p) => ({ date: new Date(p.date + 'T00:00:00Z'), close: p.price }))
    .filter((p) => !isNaN(p.date) && p.close > 0);
  return {
    series: resampleEvenly(pts, 7),
    name: parsed.asset_name || 'Grafico da screenshot',
    currency: parsed.currency,
    observations: parsed.observations,
  };
}

function fileToBase64(file) {
  return new Promise((resolve, reject) => {
    const r = new FileReader();
    r.onload = () => resolve(String(r.result).split(',')[1]);
    r.onerror = () => reject(new Error('Impossibile leggere il file.'));
    r.readAsDataURL(file);
  });
}

// ---------- Digitalizzazione manuale ----------
// L'utente clicca due punti di riferimento sull'asse X (con le date) e due sull'asse Y (con i prezzi),
// poi clicca lungo la curva. Da pixel a valori con una trasformazione lineare (o logaritmica per l'asse Y).

export function pixelsToSeries(calib, clicks, yLog) {
  const { x1, x2, y1, y2 } = calib; // { px, value }
  const t1 = x1.value.getTime(), t2 = x2.value.getTime();
  const fy = yLog ? Math.log : (v) => v;
  const iy = yLog ? Math.exp : (v) => v;
  const pts = clicks.map((c) => {
    const t = t1 + ((c.x - x1.px) / (x2.px - x1.px)) * (t2 - t1);
    const v = iy(fy(y1.value) + ((c.y - y1.px) / (y2.px - y1.px)) * (fy(y2.value) - fy(y1.value)));
    return { date: new Date(t), close: v };
  }).filter((p) => p.close > 0);
  return resampleEvenly(pts, 7);
}
