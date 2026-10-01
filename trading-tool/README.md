# Analisi Lungo Termine

Pagina web (nessuna installazione) che analizza l'andamento di ETF, indici, materie prime e cambi e dà indicazioni operative pensate per chi investe a lungo termine.

## Come si usa

1. Apri `trading-tool/index.html` tramite un server web, per esempio GitHub Pages (`…/trading-tool/`) o `python3 -m http.server` in locale. Aperta con doppio clic (`file://`) non funziona, perché i moduli JavaScript lo impediscono.
2. In **⚙ Impostazioni** inserisci almeno una chiave gratuita:
   - **Alpha Vantage**: borse europee (es. `VWCE.DEX`, `SWDA.LON`), USA, forex (`EUR/USD`) e dati fondamentali. Limite: 25 richieste al giorno. Ogni analisi ne usa 1–3; i risultati restano in cache per 12 ore.
   - **Twelve Data**: USA, forex e oro (`XAU/USD`). Limite: 800 richieste al giorno. Il piano gratuito in genere non copre le borse europee.
   - **Anthropic** (facoltativa): serve per far leggere a Claude gli screenshot dei grafici.
3. Scrivi il simbolo, oppure premi "Prova con dati demo".

Le chiavi restano nel `localStorage` del browser e vengono inviate solo al servizio a cui appartengono.

## Cosa calcola

| Blocco | Metodo |
|---|---|
| Trend | Medie mobili a 10 e 40 settimane (≈ 50/200 giorni), golden cross e death cross, pendenza |
| Momentum | RSI a 14 settimane, MACD (12/26/9) |
| Valutazione | Distanza dalla linea di tendenza log-lineare di tutto lo storico (z-score) e, se disponibili, TER, dividendi e P/E |
| Ribasso dai massimi | Drawdown attuale e massimo storico |
| Livelli | Supporti e resistenze ricavati da massimi e minimi locali degli ultimi 5 anni |
| Proiezione | Monte Carlo su 2.000 scenari con *block bootstrap* dei rendimenti settimanali reali (orizzonte da 1 a 10 anni) |
| Verifica storica | Il punteggio viene ricalcolato nel passato usando solo i dati disponibili in quel momento e confrontato con il rendimento dei 12 mesi successivi |

Sono disponibili due profili, **bilanciato** e **dinamico**, che pesano i quattro blocchi in modo diverso.

## File

- `js/analysis.js`: indicatori, proiezione, punteggio e consigli (funzioni pure, testabili con Node)
- `js/data.js`: download dei dati da Alpha Vantage e Twelve Data, cache, serie demo
- `js/vision.js`: lettura degli screenshot con Claude oppure digitalizzazione manuale
- `js/chart.js`: grafico su canvas
- `js/app.js`: interfaccia

> Strumento educativo, non è consulenza finanziaria. I rendimenti passati non garantiscono quelli futuri.
