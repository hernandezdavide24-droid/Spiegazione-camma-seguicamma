# Advent TI: piano dell'app

App per iPhone e Android con gli eventi di auto, moto e aerei in Ticino, nell'Italia vicina e nella Svizzera interna vicina.
Il prototipo cliccabile è in `prototipo.html` (anteprima Claude) e `index.html` (stessa app come pagina normale, per GitHub Pages).

## Decisioni prese
- Utente: principiante, budget iniziale quasi zero
- Lingue: italiano e inglese
- Categorie: raduni auto, tour in auto, moto, eventi aerei, gare e track day
- Area: Ticino, Italia vicina (Lombardia, Piemonte) e Svizzera interna vicina (Uri, Grigioni)
- Fonti: siti di eventi, inserimento da organizzatori, social (vedi limiti sotto)
- Promozione: gratuita con approvazione; gli organizzatori verificati pubblicano subito. Gli eventi in evidenza a pagamento arriveranno più avanti.

## Fonti segnalate (verificate via ricerca web)
| Fonte | Tipo | Note |
|---|---|---|
| tio.ch/agenda | Agenda | Pagine per giorno `/agenda/day/AAAAMMGG`, filtri per zona; nessuna categoria motori, si filtra per parole chiave |
| inagenda.ch | Agenda | Piattaforma dietro l'agenda di tio.ch: chiedere accesso ai dati |
| tio.ch/rss | Notizie | Da verificare se esiste un feed della sola agenda |
| TCS Sezione Ticino | Agenda | Poche voci ma affidabili |
| automotoclubgeneroso.ch | Club | Dal 2026 si chiama "Moto Club Generoso": eventi soprattutto di moto |
| Patrouille Suisse (pagina + PDF) | Ufficiale | Il PDF cambia nome a ogni versione: leggere il link dalla pagina. Stagione 2026 finita il 6 settembre, senza tappe in Ticino |
| Forze aeree – attività di volo | Ufficiale | Esibizioni militari |
| localcities.ch, freizeit.ch | Agenda | Comuni / Svizzera tedesca |
| swissactivities.com | Secondaria | Attività prenotabili più che eventi |
| laRegione, RSI, aeroTELEGRAPH | Notizie | Solo fatti + link, niente testi o foto copiati |

Prima di attivare il robot: controllare le condizioni d'uso di ogni sito.

## Fonti su ogni evento
- In fondo al dettaglio: sezione "Fonti" con il link di ogni pagina usata.
- Se l'evento arriva da un social: icona e link del profilo o del canale.
- Modulo "Proponi": almeno una fonte obbligatoria (sito → link web; Instagram/Facebook/TikTok/YouTube → link del profilo o canale), altre facoltative. Il link viene controllato in base al tipo.

## Nome
Nome scelto: **Advent TI** ("TI" = Ticino), così si distingue da "Advent Events", che esiste già sull'App Store. Sottotitolo proposto per lo store (massimo 30 caratteri): "Raduni auto, moto e aerei". Prima di pubblicare va controllato che il nome sia libero su App Store e Google Play.

## Schermate (5 schede)
Home · Calendario · Mappa · Proponi · Profilo

- **Home**: carosello "In evidenza" con locandine grandi, "Questo weekend", elenco "In arrivo" a post stile Instagram (club, locandina, cuore, recensioni, mappa).
- **Mappa**: tutti gli eventi colorati per categoria; quelli promossi pulsano. 3 stili gratuiti senza chiavi: Stradale (OpenStreetMap, predefinito), Topografica (OpenTopoMap), swisstopo (carta nazionale). La Scura (CARTO) è stata tolta perché non caricava; se uno stile non carica l'app torna da sola a Stradale. Nell'anteprima di Claude le mappe esterne sono bloccate e si vede una mappa disegnata (swisstopo/UST + Natural Earth); da GitHub Pages si vedono le mappe vere.
- **Google Maps**: le immagini non si possono copiare; nell'app vera react-native-maps mostra Google Maps su Android (chiave gratuita per le app). Ogni evento ha il pulsante "Apri in Google Maps".
- Licenze tasselli: credito visibile sempre; OSM/OpenTopoMap uso moderato (con molti utenti passare a un servizio dedicato, anche gratuito come OpenFreeMap); swisstopo gratuito.

## Accesso e ruoli
- Ospite: guarda tutto senza account.
- Accesso con **Apple** e **Google** (App Store, regola 4.8: con l'accesso Google serve anche un'alternativa come "Accedi con Apple").
- Ruoli: appassionato · organizzatore (in verifica → verificato) · amministratore.
- Organizzatore: nome del club + link ufficiale; dopo la verifica pubblica senza passare dalla coda.

## Recensioni
- Stelle 1–5 + commento, su eventi e organizzatori; in qualsiasi momento (con l'edizione facoltativa: molti eventi si ripetono).
- Risposta pubblica dell'organizzatore (una per recensione).
- Pulsante **"Segnala"** su recensioni ed eventi (serve l'accesso). Motivi: offensivo, spam, falso, dati personali, dati sbagliati, annullato, evento falso, altro (nota obbligatoria).
- Coda **"Segnalazioni"** per l'amministratore: segnalazioni raggruppate per contenuto, con motivi e note; azioni Rimuovi / Mantieni / Apri.

## Promozione (gratis per ora)
- In evidenza nella Home · badge "In evidenza" (elenco, locandine, mappa) · notifica ai vicini (una volta per evento, chi segue la categoria entro 30 km) · statistiche (viste, salvataggi, clic sulle fonti).

## Locandine
- Caricate dall'organizzatore (ridotte a ~900 px).
- Generate dall'app se manca l'immagine.
- Prese dalla fonte solo con permesso: casella obbligatoria + credito visibile. Il robot non copia immagini in automatico.

## Tabelle Supabase
profiles · organizers · events · event_sources · posters · reviews · reports · promotions · event_stats

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
