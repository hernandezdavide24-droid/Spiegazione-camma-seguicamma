# Ruote e Ali: piano dell'app (nome provvisorio)

App per iPhone e Android con gli eventi di auto e aerei in Ticino, nell'Italia vicina e nella Svizzera interna vicina.
Il prototipo cliccabile è in `prototipo.html`.

## Decisioni prese
- Utente: principiante, budget iniziale quasi zero
- Lingue: italiano e inglese
- Categorie: raduni auto, tour in auto, eventi aerei, gare e track day
- Area: Ticino, Italia vicina (Lombardia, Piemonte) e Svizzera interna vicina (Uri, Grigioni)
- Fonti: siti di eventi, inserimento da organizzatori, social (vedi limiti sotto)
- Promozione: gratuita con approvazione; gli organizzatori verificati pubblicano subito. Gli eventi in evidenza a pagamento arriveranno più avanti.

## Architettura
| Parte | Strumento | Costo |
|---|---|---|
| App iOS + Android (+ web) | Expo / React Native | 0 |
| Database, login, ruoli | Supabase (piano gratuito) | 0 |
| Robot di raccolta giornaliero | GitHub Actions (cron) | 0 |
| Estrazione dati dagli annunci | API di un modello AI | ~1–5 CHF/mese (stima) |
| Notifiche push | Expo Push | 0 |
| Itinerari | Link a Google/Apple Maps | 0 |

Nota: un progetto Supabase gratuito va in pausa dopo 7 giorni senza attività. Il robot giornaliero lo tiene attivo.

## Flusso automatico
1. Ogni mattina il robot legge le fonti (ticino.ch eventi, aeroclub, autodromi, club). Rilegge solo le pagine cambiate.
2. L'AI estrae titolo, data, ora, luogo, categoria, programma e itinerario, e segnala i campi mancanti.
3. L'evento va in "Da verificare" con un punteggio di affidabilità e un controllo dei doppioni.
4. L'amministratore approva. Gli organizzatori verificati saltano questo passaggio.
5. Notifica push a chi segue quella categoria o quella zona.

## Social: limiti reali
- L'Instagram Basic Display API è stata chiusa il 4 dicembre 2024. L'API Graph attuale dà accesso solo al **proprio** account Business/Creator, non ai post di altri.
- Copiare i post con programmi non autorizzati viola i termini di Meta.
- Soluzioni: il pulsante "Incolla annuncio", con cui l'AI compila il modulo (già nel prototipo); gli organizzatori verificati; i siti dei club che ripubblicano gli eventi.

## Costi una tantum e annuali
- Apple Developer Program: 99 USD/anno, solo per pubblicare su App Store
- Google Play Console: 25 USD una volta
- Dominio (facoltativo): ~15 CHF/anno

## Fasi
1. **Base (2–4 settimane):** calendario, filtri, dettaglio, itinerario, "Proponi", approvazione. Eventi inseriti a mano. Test con Expo Go.
2. **Automazione (2–3 settimane):** robot su 10–15 fonti, estrazione AI, controllo dei doppioni.
3. **Pubblicazione:** privacy policy, test chiuso (TestFlight / Google Play), pubblicazione.
4. **Crescita:** notifiche per zona e categoria, preferiti, account organizzatori, eventi in evidenza a pagamento.

## Aspetti legali
- Privacy policy obbligatoria; si applicano la nLPD svizzera e il GDPR per gli utenti in Italia.
- Rispettare i termini d'uso delle fonti, citare sempre la fonte con il link e non usare foto senza permesso.
- Avviso agli utenti: "Verifica sempre i dettagli con l'organizzatore".
