package it.lampada.bibbia.data.calendar

import it.lampada.bibbia.data.calendar.DateRule.Fixed
import it.lampada.bibbia.data.calendar.DateRule.FromAdvent
import it.lampada.bibbia.data.calendar.DateRule.FromEaster
import it.lampada.bibbia.data.calendar.DateRule.FromOrthodoxEaster
import it.lampada.bibbia.data.calendar.Importance.FESTA
import it.lampada.bibbia.data.calendar.Importance.PRINCIPALE
import it.lampada.bibbia.data.calendar.Importance.RICORRENZA
import it.lampada.bibbia.data.calendar.LiturgicalColor.BIANCO
import it.lampada.bibbia.data.calendar.LiturgicalColor.ROSSO
import it.lampada.bibbia.data.calendar.LiturgicalColor.VERDE
import it.lampada.bibbia.data.calendar.LiturgicalColor.VIOLA
import it.lampada.bibbia.data.calendar.Tradition.CATTOLICA
import it.lampada.bibbia.data.calendar.Tradition.CATTOLICA_ORTODOSSA
import it.lampada.bibbia.data.calendar.Tradition.COMUNE
import it.lampada.bibbia.data.calendar.Tradition.OCCIDENTALE
import it.lampada.bibbia.data.calendar.Tradition.ORTODOSSA
import it.lampada.bibbia.data.calendar.Tradition.PROTESTANTE

/**
 * Feste e periodi dell'anno cristiano. I riferimenti biblici usano la numerazione dei versetti
 * della Bibbia inclusa nell'app (la stessa delle Bibbie protestanti e inglesi).
 */
object FeastCatalog {

    val seasons = listOf(
        Season(
            id = "avvento",
            name = "Avvento",
            color = VIOLA,
            description = "Le quattro settimane prima di Natale. «Avvento» significa «venuta»: si prepara il " +
                "cuore a celebrare la nascita di Gesù e si guarda con speranza al suo ritorno alla fine dei " +
                "tempi. È un tempo di attesa, di speranza e di conversione.",
            whatToDo = listOf(
                "Accendi ogni domenica una candela della corona d'Avvento: speranza, pace, gioia, amore.",
                "Leggi ogni giorno un brano dei profeti che annunciano il Messia, per esempio Isaia.",
                "Prepara il presepe o un calendario d'Avvento con un versetto al giorno.",
                "Scegli un gesto concreto di carità verso chi è solo o in difficoltà.",
                "Riduci il superfluo (acquisti, schermi) per fare spazio all'essenziale.",
            ),
            readings = listOf("Is 9:2-7", "Is 40:1-11", "Lc 1:26-38", "Rm 13:11-14"),
        ),
        Season(
            id = "natale",
            name = "Tempo di Natale",
            color = BIANCO,
            description = "Dal 25 dicembre al Battesimo del Signore si celebra il mistero di Dio che si fa uomo " +
                "in Gesù: la sua nascita, la visita dei Magi e il suo battesimo. Il bianco è il colore della " +
                "gioia e della luce.",
            whatToDo = listOf(
                "Leggi i racconti della nascita: Luca 2 e Matteo 1-2.",
                "Vivi i pasti di festa come momenti di gratitudine, con una preghiera prima di mangiare.",
                "Visita parenti anziani o malati.",
                "Scrivi un biglietto a chi è lontano o in difficoltà.",
            ),
            readings = listOf("Lc 2:1-20", "Gv 1:1-14", "Mt 2:1-12", "Tt 2:11-14"),
        ),
        Season(
            id = "ordinario",
            name = "Tempo ordinario",
            color = VERDE,
            description = "Le settimane fra il tempo di Natale e la Quaresima, e da Pentecoste all'Avvento. " +
                "«Ordinario» non vuol dire banale: è il tempo per crescere nella vita di ogni giorno seguendo " +
                "l'insegnamento di Gesù. Il verde è il colore della speranza e della crescita.",
            whatToDo = listOf(
                "Leggi con regolarità un Vangelo, dall'inizio alla fine.",
                "Fissa un momento quotidiano di preghiera, anche breve.",
                "Partecipa alla vita della tua comunità.",
                "Scegli una parola di Gesù da mettere in pratica ogni settimana.",
            ),
            readings = listOf("Mt 5:1-16", "Sal 1", "Gc 1:22-25", "Col 3:12-17"),
        ),
        Season(
            id = "quaresima",
            name = "Quaresima",
            color = VIOLA,
            description = "I quaranta giorni dal Mercoledì delle Ceneri alla Settimana Santa ricordano i 40 giorni " +
                "di Gesù nel deserto e preparano alla Pasqua con preghiera, digiuno e carità. È un tempo di " +
                "conversione: cambiare direzione e tornare a Dio.",
            whatToDo = listOf(
                "Scegli una forma di digiuno: un pasto, i dolci, i social, una cattiva abitudine.",
                "Dona ciò che risparmi a chi ha bisogno.",
                "Leggi ogni giorno un Salmo, per esempio i Salmi 51, 32 e 130.",
                "Fai un esame di coscienza e chiedi perdono a chi hai ferito.",
                "Usa il blocco app per liberare tempo per la preghiera e la lettura.",
            ),
            readings = listOf("Mt 4:1-11", "Gl 2:12-13", "Sal 51", "Is 58:6-9", "Mt 6:1-18"),
        ),
        Season(
            id = "settimana_santa",
            name = "Settimana Santa",
            color = ROSSO,
            description = "Dalla Domenica delle Palme al Sabato Santo: la settimana più importante dell'anno " +
                "cristiano. Si ripercorrono gli ultimi giorni di Gesù: l'ingresso a Gerusalemme, l'Ultima Cena, " +
                "la passione, la morte in croce e l'attesa della risurrezione.",
            whatToDo = listOf(
                "Leggi ogni giorno un pezzo del racconto della Passione (per esempio Marco 11-16).",
                "Partecipa alle celebrazioni del Giovedì, del Venerdì e del Sabato Santo.",
                "Il Venerdì Santo vivi un momento di silenzio verso le 15, l'ora della morte di Gesù.",
                "Riduci rumore e distrazioni per accompagnare Gesù con la preghiera.",
            ),
            readings = listOf("Mc 11:1-11", "Gv 13:1-17", "Is 52:13-53:12", "Gv 19:16-42"),
        ),
        Season(
            id = "pasqua",
            name = "Tempo di Pasqua",
            color = BIANCO,
            description = "Cinquanta giorni di festa, da Pasqua a Pentecoste: la risurrezione di Gesù è il " +
                "centro della fede cristiana. Comprende l'Ascensione e si conclude con il dono dello Spirito " +
                "Santo.",
            whatToDo = listOf(
                "Leggi gli Atti degli Apostoli, il racconto della nascita della Chiesa.",
                "Saluta con l'antico annuncio «Cristo è risorto!» – «È veramente risorto!».",
                "Vivi la gioia: canta, condividi un pasto, fai festa con altri.",
                "Racconta a qualcuno che cosa significa per te la risurrezione.",
            ),
            readings = listOf("Lc 24:13-35", "Gv 20:1-18", "1Cor 15:1-20", "At 2:42-47"),
        ),
    )

    val feasts = listOf(
        Feast(
            id = "maria_madre_di_dio",
            name = "Maria Madre di Dio",
            rule = Fixed(1, 1),
            tradition = CATTOLICA,
            importance = FESTA,
            color = BIANCO,
            summary = "Ottava di Natale e primo giorno dell'anno.",
            meaning = "Nel giorno che chiude l'ottava di Natale la Chiesa cattolica onora Maria come Madre di Dio, " +
                "titolo riconosciuto dal Concilio di Efeso nel 431 per affermare che il bambino nato da lei è " +
                "veramente Dio. Lo stesso giorno ricorda la circoncisione di Gesù e il nome che gli fu dato " +
                "(Luca 2:21), ed è la Giornata mondiale della pace.",
            whatToDo = listOf(
                "Affida a Dio l'anno nuovo con una preghiera.",
                "Scrivi un proposito spirituale per l'anno.",
                "Prega per la pace nel mondo e nella tua famiglia.",
            ),
            readings = listOf("Lc 2:16-21", "Gal 4:4-7", "Nm 6:22-27"),
        ),
        Feast(
            id = "epifania",
            name = "Epifania del Signore",
            rule = Fixed(1, 6),
            tradition = COMUNE,
            importance = PRINCIPALE,
            color = BIANCO,
            summary = "Gesù si manifesta a tutti i popoli.",
            meaning = "«Epifania» significa «manifestazione». In Occidente si ricorda soprattutto la visita dei " +
                "Magi, sapienti venuti da Oriente: Gesù si rivela salvatore non solo di Israele ma di tutti i " +
                "popoli. Le chiese ortodosse in questo giorno celebrano la Teofania, cioè il battesimo di Gesù " +
                "nel Giordano (chi segue il calendario giuliano la celebra il 19 gennaio).",
            whatToDo = listOf(
                "Leggi il racconto dei Magi (Matteo 2:1-12).",
                "Chiediti quale «dono» puoi offrire a Dio quest'anno: tempo, talenti, attenzione agli altri.",
                "Prega per chi non conosce ancora Gesù.",
                "In Italia è festa per i bambini: un'occasione per raccontare loro la storia dei Magi.",
            ),
            readings = listOf("Mt 2:1-12", "Is 60:1-6", "Ef 3:2-6"),
        ),
        Feast(
            id = "natale_ortodosso",
            name = "Natale ortodosso",
            rule = Fixed(1, 7),
            tradition = ORTODOSSA,
            importance = FESTA,
            color = BIANCO,
            summary = "Il Natale delle chiese che seguono il calendario giuliano.",
            meaning = "Le chiese ortodosse di Russia, Serbia, Georgia, Gerusalemme e altre seguono ancora il " +
                "calendario giuliano: il loro 25 dicembre cade il 7 gennaio del nostro calendario. Altre chiese " +
                "ortodosse, come quella greca e quella romena, celebrano il Natale il 25 dicembre.",
            whatToDo = listOf(
                "Se conosci cristiani ortodossi, fai loro gli auguri.",
                "Prega per l'unità dei cristiani.",
            ),
            readings = listOf("Lc 2:1-20", "Gal 4:4-7"),
        ),
        Feast(
            id = "battesimo",
            name = "Battesimo del Signore",
            rule = DateRule.SundayAfterEpiphany,
            tradition = OCCIDENTALE,
            importance = FESTA,
            color = BIANCO,
            summary = "Gesù viene battezzato da Giovanni nel Giordano.",
            meaning = "Gesù, pur senza peccato, si mette in fila con i peccatori e riceve il battesimo di " +
                "Giovanni. Lo Spirito scende su di lui e la voce del Padre dice: «Questo è il mio diletto " +
                "Figliuolo». È l'inizio del suo ministero pubblico e chiude il tempo di Natale. Gli ortodossi " +
                "ricordano il battesimo di Gesù nella festa del 6 gennaio.",
            whatToDo = listOf(
                "Ricorda il tuo battesimo: se conosci la data, segnala e rendi grazie.",
                "Leggi Matteo 3:13-17: le parole del Padre valgono anche per te, figlio amato.",
                "Se non sei battezzato e lo desideri, parlane con un pastore o un sacerdote.",
            ),
            readings = listOf("Mt 3:13-17", "Is 42:1-7", "At 10:34-38"),
        ),
        Feast(
            id = "unita_cristiani",
            name = "Settimana di preghiera per l'unità dei cristiani",
            rule = Fixed(1, 18),
            tradition = COMUNE,
            importance = RICORRENZA,
            color = VERDE,
            durationDays = 8,
            summary = "Dal 18 al 25 gennaio cristiani di tutte le confessioni pregano insieme.",
            meaning = "Ogni anno, dal 18 al 25 gennaio (festa della conversione dell'apostolo Paolo), cattolici, " +
                "ortodossi e protestanti pregano insieme perché si realizzi la preghiera di Gesù: «che siano " +
                "tutti uno» (Giovanni 17:21). L'iniziativa è nata all'inizio del Novecento.",
            whatToDo = listOf(
                "Partecipa a una preghiera ecumenica nella tua città.",
                "Conosci una comunità cristiana diversa dalla tua.",
                "Prega con Giovanni 17.",
                "Supera un pregiudizio verso cristiani di un'altra confessione.",
            ),
            readings = listOf("Gv 17:20-23", "Ef 4:1-6", "1Cor 1:10-13"),
        ),
        Feast(
            id = "presentazione",
            name = "Presentazione di Gesù al Tempio",
            rule = Fixed(2, 2),
            tradition = COMUNE,
            importance = FESTA,
            color = BIANCO,
            summary = "La Candelora: Gesù è la luce delle nazioni.",
            meaning = "Quaranta giorni dopo la nascita, Maria e Giuseppe portano Gesù al Tempio di Gerusalemme, " +
                "come prescriveva la Legge di Mosè. L'anziano Simeone lo prende in braccio e lo riconosce come " +
                "«luce da illuminar le genti». Per questo è detta «Candelora»: si benedicono le candele. La " +
                "celebrano cattolici, ortodossi, anglicani e luterani.",
            whatToDo = listOf(
                "Accendi una candela e prega con il cantico di Simeone (Luca 2:29-32).",
                "Ringrazia per gli anziani che ti hanno trasmesso la fede.",
                "Chiediti dove puoi portare un po' di luce questa settimana.",
            ),
            readings = listOf("Lc 2:22-40", "Ml 3:1-4", "Eb 2:14-18"),
        ),
        Feast(
            id = "ceneri",
            name = "Mercoledì delle Ceneri",
            rule = FromEaster(-46),
            tradition = OCCIDENTALE,
            importance = PRINCIPALE,
            color = VIOLA,
            summary = "Inizio della Quaresima.",
            meaning = "Apre i quaranta giorni di preparazione alla Pasqua. Nelle chiese cattolica e anglicana e " +
                "in molte chiese luterane e metodiste si riceve un segno di cenere sulla fronte, con le parole " +
                "«Convertitevi e credete al Vangelo» oppure «Ricordati che sei polvere e in polvere ritornerai» " +
                "(da Genesi 3:19). È un invito all'umiltà e al ritorno a Dio. Per i cattolici è giorno di " +
                "digiuno e di astinenza dalla carne.",
            whatToDo = listOf(
                "Decidi il tuo impegno di Quaresima: che cosa lasciare e che cosa fare in più.",
                "Digiuna o fai un pasto semplice.",
                "Leggi Gioele 2:12-13 e Matteo 6:1-18.",
                "Valuta di limitare per 40 giorni un'app che ti distrae, con il blocco app.",
            ),
            readings = listOf("Gl 2:12-18", "Mt 6:1-6", "Mt 6:16-18", "2Cor 5:20-6:2"),
        ),
        Feast(
            id = "annunciazione",
            name = "Annunciazione del Signore",
            rule = Fixed(3, 25),
            tradition = COMUNE,
            importance = FESTA,
            color = BIANCO,
            summary = "L'angelo Gabriele annuncia a Maria la nascita di Gesù.",
            meaning = "Nove mesi prima di Natale si ricorda l'annuncio dell'angelo a Maria e la sua risposta: " +
                "«Ecco, io son l'ancella del Signore». In quel momento il Figlio di Dio diventa uomo nel suo " +
                "grembo (l'Incarnazione). La festeggiano cattolici, ortodossi, anglicani e luterani.",
            note = "Se il 25 marzo cade nella Settimana Santa o nella settimana di Pasqua, la Chiesa cattolica " +
                "sposta la celebrazione al lunedì dopo la domenica successiva alla Pasqua.",
            whatToDo = listOf(
                "Leggi Luca 1:26-38.",
                "Medita sulla risposta di Maria e chiediti a che cosa Dio ti sta chiamando.",
                "Ringrazia per il dono della vita.",
            ),
            readings = listOf("Lc 1:26-38", "Is 7:10-14", "Eb 10:4-10"),
        ),
        Feast(
            id = "palme",
            name = "Domenica delle Palme",
            rule = FromEaster(-7),
            tradition = COMUNE,
            importance = PRINCIPALE,
            color = ROSSO,
            summary = "Gesù entra a Gerusalemme acclamato dalla folla.",
            meaning = "Gesù entra a Gerusalemme su un asinello e la folla lo accoglie agitando rami e gridando " +
                "«Osanna al Figliuolo di Davide!». Pochi giorni dopo la stessa città lo condannerà. Si " +
                "benedicono rami d'ulivo o di palma e si legge il racconto della Passione. Inizia la Settimana " +
                "Santa.",
            whatToDo = listOf(
                "Porta a casa un ramo d'ulivo benedetto e mettilo accanto a una croce o alla Bibbia.",
                "Leggi il racconto della Passione in uno dei Vangeli.",
                "Chiediti: accolgo Gesù solo quando è facile?",
            ),
            readings = listOf("Mt 21:1-11", "Is 50:4-7", "Fil 2:6-11", "Mt 26:14-27:66"),
        ),
        Feast(
            id = "giovedi_santo",
            name = "Giovedì Santo",
            rule = FromEaster(-3),
            tradition = COMUNE,
            importance = PRINCIPALE,
            color = BIANCO,
            summary = "L'Ultima Cena e la lavanda dei piedi.",
            meaning = "Gesù celebra con i discepoli la cena pasquale, istituisce la Cena del Signore (Eucaristia " +
                "o Santa Cena) e lava loro i piedi, lasciando il comandamento dell'amore: «Com'io v'ho amati, " +
                "anche voi amatevi gli uni gli altri». La sera prega nel Getsemani e viene arrestato.",
            whatToDo = listOf(
                "Partecipa alla celebrazione della sera (Messa nella Cena del Signore o culto con Santa Cena).",
                "Fai un servizio umile a qualcuno, senza farlo sapere.",
                "Veglia un momento in preghiera, come Gesù chiese ai discepoli nel Getsemani.",
            ),
            readings = listOf("Gv 13:1-15", "1Cor 11:23-26", "Es 12:1-14", "Mt 26:36-46"),
        ),
        Feast(
            id = "venerdi_santo",
            name = "Venerdì Santo",
            rule = FromEaster(-2),
            tradition = COMUNE,
            importance = PRINCIPALE,
            color = ROSSO,
            summary = "La passione e la morte di Gesù in croce.",
            meaning = "Si ricorda la crocifissione di Gesù. Per i cristiani è il giorno in cui Gesù ha dato la " +
                "vita per i peccati del mondo, prendendo su di sé il male per riconciliarci con Dio. È un " +
                "giorno di silenzio, digiuno e adorazione della croce; nelle chiese cattoliche non si celebra " +
                "la Messa.",
            whatToDo = listOf(
                "Leggi il racconto della Passione (Giovanni 18-19).",
                "Alle 15, ora della morte di Gesù, fermati per un minuto di silenzio e preghiera.",
                "Partecipa alla Via Crucis o al culto del Venerdì Santo.",
                "Mangia in modo semplice; per i cattolici è giorno di digiuno e di astinenza dalla carne.",
            ),
            readings = listOf("Is 52:13-53:12", "Gv 18:1-19:42", "Eb 4:14-16", "Sal 22"),
        ),
        Feast(
            id = "sabato_santo",
            name = "Sabato Santo",
            rule = FromEaster(-1),
            tradition = COMUNE,
            importance = FESTA,
            color = VIOLA,
            summary = "Il silenzio del sepolcro e la Veglia pasquale.",
            meaning = "È il giorno del silenzio: Gesù giace nel sepolcro e la Chiesa attende in preghiera. Nella " +
                "notte si celebra la Veglia pasquale: si accende il fuoco nuovo e il cero pasquale, si leggono " +
                "le grandi tappe della storia della salvezza e si annuncia la risurrezione.",
            whatToDo = listOf(
                "Vivi la giornata con sobrietà e silenzio.",
                "Partecipa alla Veglia pasquale nella notte.",
                "Leggi Romani 6:3-11 sul significato del battesimo.",
            ),
            readings = listOf("Mt 27:57-66", "Rm 6:3-11", "Es 14:15-31"),
        ),
        Feast(
            id = "pasqua",
            name = "Pasqua di Risurrezione",
            rule = FromEaster(0),
            tradition = COMUNE,
            importance = PRINCIPALE,
            color = BIANCO,
            summary = "Gesù è risorto: la festa più importante dell'anno.",
            meaning = "Il terzo giorno dopo la morte, le donne trovano il sepolcro vuoto: Gesù è risorto. È il " +
                "cuore della fede cristiana: «se Cristo non è risuscitato, vana è la vostra fede», scrive Paolo " +
                "(1 Corinzi 15:17). La risurrezione è la vittoria sulla morte e sul peccato e la promessa della " +
                "vita eterna. La data cambia ogni anno: è la domenica dopo la prima luna piena di primavera.",
            whatToDo = listOf(
                "Partecipa al culto o alla Messa di Pasqua.",
                "Leggi i racconti della risurrezione (Giovanni 20, Luca 24).",
                "Saluta gli altri con «Cristo è risorto!».",
                "Condividi un pasto di festa e invita qualcuno che sarebbe solo.",
            ),
            readings = listOf("Gv 20:1-18", "Mt 28:1-10", "1Cor 15:1-8", "Col 3:1-4"),
        ),
        Feast(
            id = "pasquetta",
            name = "Lunedì dell'Angelo",
            rule = FromEaster(1),
            tradition = OCCIDENTALE,
            importance = RICORRENZA,
            color = BIANCO,
            summary = "La Pasquetta, il giorno dopo Pasqua.",
            meaning = "Ricorda l'angelo che al sepolcro disse alle donne: «Egli non è qui, poiché è risuscitato» " +
                "(Matteo 28:6). In Italia è tradizione passarlo all'aperto con amici e famiglia, come i due " +
                "discepoli in cammino verso Emmaus che riconobbero Gesù «nello spezzare il pane».",
            whatToDo = listOf(
                "Fai una passeggiata con amici e leggete insieme Luca 24:13-35.",
                "Ringrazia Dio per la bellezza del creato.",
                "Racconta a qualcuno la gioia della Pasqua.",
            ),
            readings = listOf("Mt 28:8-15", "Lc 24:13-35"),
        ),
        Feast(
            id = "pasqua_ortodossa",
            name = "Pasqua ortodossa",
            rule = FromOrthodoxEaster(0),
            tradition = ORTODOSSA,
            importance = PRINCIPALE,
            color = BIANCO,
            summary = "La Pasqua delle chiese ortodosse.",
            meaning = "Le chiese ortodosse calcolano la data della Pasqua con il calendario giuliano: per questo " +
                "spesso la celebrano qualche settimana dopo cattolici e protestanti (in alcuni anni le date " +
                "coincidono). Nella notte si annuncia «Christós anésti!» («Cristo è risorto!») e si risponde " +
                "«Alithós anésti!» («È veramente risorto!»).",
            whatToDo = listOf(
                "Se conosci cristiani ortodossi, fai loro gli auguri.",
                "Prega per l'unità dei cristiani e per una data comune della Pasqua.",
                "Rileggi il racconto della risurrezione.",
            ),
            readings = listOf("Gv 20:19-31", "At 1:1-8"),
        ),
        Feast(
            id = "ascensione",
            name = "Ascensione del Signore",
            rule = FromEaster(39),
            tradition = COMUNE,
            importance = PRINCIPALE,
            color = BIANCO,
            summary = "Gesù risorto sale al cielo.",
            meaning = "Quaranta giorni dopo la Pasqua Gesù risorto promette lo Spirito Santo, affida ai " +
                "discepoli la missione di essere suoi testimoni «fino all'estremità della terra» e viene " +
                "innalzato al cielo. Non è un'assenza: Gesù siede alla destra del Padre e intercede per noi.",
            note = "In Italia la Chiesa cattolica la celebra la domenica successiva.",
            whatToDo = listOf(
                "Leggi Atti 1:1-11.",
                "Chiediti come essere testimone di Gesù nella tua vita di tutti i giorni.",
                "Prega il Padre Nostro pensando al Regno che Gesù ci affida.",
            ),
            readings = listOf("At 1:1-11", "Lc 24:46-53", "Ef 1:17-23"),
        ),
        Feast(
            id = "pentecoste",
            name = "Pentecoste",
            rule = FromEaster(49),
            tradition = COMUNE,
            importance = PRINCIPALE,
            color = ROSSO,
            summary = "Il dono dello Spirito Santo e la nascita della Chiesa.",
            meaning = "Cinquanta giorni dopo la Pasqua, durante la festa ebraica delle Settimane, lo Spirito " +
                "Santo scende sui discepoli riuniti a Gerusalemme come lingue di fuoco. Pietro annuncia Gesù e " +
                "circa tremila persone si fanno battezzare: è la nascita della Chiesa. Il rosso ricorda il " +
                "fuoco dello Spirito.",
            whatToDo = listOf(
                "Leggi Atti 2.",
                "Rifletti sul «frutto dello Spirito» (Galati 5:22-23) e chiedi a Dio di farlo crescere in te.",
                "Scopri quale dono hai ricevuto per servire gli altri e mettilo in pratica.",
                "Prega per i cristiani perseguitati nel mondo.",
            ),
            readings = listOf("At 2:1-21", "Gv 14:15-26", "Gal 5:16-25", "Rm 8:14-17"),
        ),
        Feast(
            id = "trinita",
            name = "Santissima Trinità",
            rule = FromEaster(56),
            tradition = OCCIDENTALE,
            importance = FESTA,
            color = BIANCO,
            summary = "Un solo Dio: Padre, Figlio e Spirito Santo.",
            meaning = "La domenica dopo Pentecoste cattolici e protestanti celebrano il mistero centrale della " +
                "fede: Dio è uno solo in tre persone, Padre, Figlio e Spirito Santo, una comunione d'amore. Per " +
                "gli ortodossi è la stessa Pentecoste la festa della Trinità.",
            whatToDo = listOf(
                "Prega con la benedizione con cui Paolo chiude la seconda lettera ai Corinzi.",
                "Leggi Matteo 28:16-20.",
                "Coltiva relazioni che riflettano l'amore della Trinità: ascolto, dono, unità.",
            ),
            readings = listOf("Mt 28:16-20", "2Cor 13:14", "Gv 16:12-15"),
        ),
        Feast(
            id = "corpus_domini",
            name = "Corpus Domini",
            rule = FromEaster(60),
            tradition = CATTOLICA,
            importance = FESTA,
            color = BIANCO,
            summary = "Festa del Corpo e del Sangue di Cristo.",
            meaning = "La Chiesa cattolica celebra la presenza di Gesù nell'Eucaristia. In molte città si fanno " +
                "processioni e infiorate. La festa fu estesa a tutta la Chiesa da papa Urbano IV nel 1264.",
            note = "In Italia si celebra la domenica successiva.",
            whatToDo = listOf(
                "Partecipa alla Messa e alla processione.",
                "Leggi Giovanni 6:51-58 e 1 Corinzi 11:23-26.",
                "Fai un momento di adorazione silenziosa.",
            ),
            readings = listOf("Gv 6:51-58", "1Cor 11:23-26", "Es 24:3-8"),
        ),
        Feast(
            id = "giovanni_battista",
            name = "Natività di Giovanni Battista",
            rule = Fixed(6, 24),
            tradition = COMUNE,
            importance = FESTA,
            color = BIANCO,
            summary = "La nascita del precursore di Gesù.",
            meaning = "Giovanni Battista è il profeta che preparò la strada al Messia; è uno dei pochissimi santi " +
                "di cui si celebra la nascita. La data, sei mesi prima di Natale, segue Luca 1:36. Nacque da " +
                "Zaccaria ed Elisabetta in età avanzata, e il suo nome significa «Dio fa grazia».",
            whatToDo = listOf(
                "Leggi Luca 1:57-80 e il cantico di Zaccaria.",
                "Pensa a chi ti ha preparato la strada verso Dio e ringrazialo.",
                "Come Giovanni, indica Gesù agli altri: «Bisogna che egli cresca, e che io diminuisca».",
            ),
            readings = listOf("Lc 1:57-80", "Is 49:1-6", "Gv 3:27-30"),
        ),
        Feast(
            id = "pietro_paolo",
            name = "Santi Pietro e Paolo",
            rule = Fixed(6, 29),
            tradition = CATTOLICA_ORTODOSSA,
            importance = FESTA,
            color = ROSSO,
            summary = "I due grandi apostoli, martiri a Roma.",
            meaning = "Si ricordano insieme Pietro, il pescatore che Gesù scelse come roccia della Chiesa, e " +
                "Paolo, il persecutore diventato apostolo dei pagani. Secondo la tradizione entrambi morirono " +
                "martiri a Roma sotto l'imperatore Nerone, intorno al 64-67 d.C. Sono i patroni di Roma.",
            whatToDo = listOf(
                "Leggi la professione di fede di Pietro (Matteo 16:13-19) e la conversione di Paolo (Atti 9:1-19).",
                "Prega per chi guida le comunità cristiane.",
                "Leggi per intero una lettera di Paolo, magari la più breve: Filemone.",
            ),
            readings = listOf("Mt 16:13-19", "At 12:1-11", "2Tm 4:6-8"),
        ),
        Feast(
            id = "trasfigurazione",
            name = "Trasfigurazione del Signore",
            rule = Fixed(8, 6),
            tradition = COMUNE,
            importance = FESTA,
            color = BIANCO,
            summary = "Sul monte Gesù appare nella sua gloria.",
            meaning = "Gesù sale su un alto monte con Pietro, Giacomo e Giovanni e il suo volto risplende come il " +
                "sole; accanto a lui appaiono Mosè ed Elia, cioè la Legge e i Profeti. La voce del Padre dice: " +
                "«ascoltatelo». È un anticipo della gloria della risurrezione, donato ai discepoli prima della " +
                "croce. La celebrano cattolici, ortodossi, anglicani e luterani (alcuni in date diverse).",
            whatToDo = listOf(
                "Leggi Matteo 17:1-9.",
                "Ritagliati un tempo di preghiera in un luogo bello e silenzioso, magari in mezzo alla natura.",
                "Medita sull'invito «ascoltatelo»: che cosa ti sta dicendo Gesù?",
            ),
            readings = listOf("Mt 17:1-9", "2Pt 1:16-19", "Dn 7:9-14"),
        ),
        Feast(
            id = "assunzione",
            name = "Assunzione di Maria (Dormizione)",
            rule = Fixed(8, 15),
            tradition = CATTOLICA_ORTODOSSA,
            importance = PRINCIPALE,
            color = BIANCO,
            summary = "Maria accolta in cielo; per gli ortodossi la Dormizione.",
            meaning = "I cattolici credono che Maria, al termine della vita terrena, sia stata assunta in cielo in " +
                "anima e corpo (dogma proclamato nel 1950). Gli ortodossi celebrano la Dormizione, il suo " +
                "«addormentarsi» e passare alla vita eterna. Le chiese protestanti non la celebrano, perché il " +
                "fatto non è narrato nella Bibbia. In Italia coincide con Ferragosto.",
            whatToDo = listOf(
                "Leggi il Magnificat, il cantico di Maria (Luca 1:46-55).",
                "Rifletti sulla speranza cristiana della risurrezione.",
                "Se sei in vacanza, ritagliati comunque un momento di preghiera.",
            ),
            readings = listOf("Lc 1:39-56", "1Cor 15:20-26"),
        ),
        Feast(
            id = "tempo_creato",
            name = "Tempo del Creato",
            rule = Fixed(9, 1),
            tradition = COMUNE,
            importance = RICORRENZA,
            color = VERDE,
            durationDays = 34,
            summary = "Dal 1° settembre al 4 ottobre: preghiera e cura per il creato.",
            meaning = "Dal 1989 il Patriarcato ecumenico di Costantinopoli dedica il 1° settembre alla preghiera " +
                "per il creato; oggi cattolici, ortodossi e protestanti vivono insieme questo tempo fino al 4 " +
                "ottobre, festa di san Francesco d'Assisi. È un invito a lodare Dio creatore e a custodire la " +
                "terra che ci ha affidato (Genesi 2:15).",
            whatToDo = listOf(
                "Leggi Genesi 1-2 e il Salmo 104.",
                "Prega all'aperto, lodando Dio per il creato.",
                "Scegli un gesto concreto: meno sprechi, meno plastica, più cura.",
                "Ringrazia per il cibo prima di ogni pasto.",
            ),
            readings = listOf("Gen 1:1-2:3", "Sal 104", "Rm 8:18-25", "Mt 6:25-34"),
        ),
        Feast(
            id = "santa_croce",
            name = "Esaltazione della Santa Croce",
            rule = Fixed(9, 14),
            tradition = CATTOLICA_ORTODOSSA,
            importance = FESTA,
            color = ROSSO,
            summary = "La croce, segno dell'amore di Dio.",
            meaning = "La festa nasce a Gerusalemme nel IV secolo, legata alla dedicazione della basilica del " +
                "Santo Sepolcro (anno 335). Si celebra la croce non come strumento di morte, ma come segno della " +
                "vittoria dell'amore di Cristo. È ricordata anche da anglicani e luterani.",
            whatToDo = listOf(
                "Leggi Giovanni 3:13-17.",
                "Prega davanti a una croce, ringraziando per il sacrificio di Gesù.",
                "Affida a Dio la fatica che stai portando in questo periodo.",
            ),
            readings = listOf("Gv 3:13-17", "Nm 21:4-9", "Fil 2:6-11"),
        ),
        Feast(
            id = "riforma",
            name = "Festa della Riforma",
            rule = Fixed(10, 31),
            tradition = PROTESTANTE,
            importance = FESTA,
            color = ROSSO,
            summary = "Il ricordo delle 95 tesi di Lutero (1517).",
            meaning = "Il 31 ottobre 1517 Martin Lutero rese pubbliche a Wittenberg le sue 95 tesi, dando inizio " +
                "alla Riforma protestante. Le chiese evangeliche ricordano i principi riscoperti allora: la " +
                "salvezza per sola grazia, mediante la fede, fondata sulla Scrittura e su Cristo solo. Nel 2017 " +
                "i 500 anni della Riforma sono stati ricordati anche insieme ai cattolici.",
            whatToDo = listOf(
                "Leggi Romani 1:16-17 ed Efesini 2:8-10.",
                "Ringrazia per il dono della Bibbia nella tua lingua.",
                "Prega per l'unità e la riconciliazione fra i cristiani.",
            ),
            readings = listOf("Rm 1:16-17", "Rm 3:21-28", "Ef 2:8-10"),
        ),
        Feast(
            id = "ognissanti",
            name = "Tutti i Santi",
            rule = Fixed(11, 1),
            tradition = OCCIDENTALE,
            importance = PRINCIPALE,
            color = BIANCO,
            summary = "Festa di tutti i santi, conosciuti e sconosciuti.",
            meaning = "Si ricordano tutti coloro che hanno vissuto la fede e ora sono con Dio, anche quelli di cui " +
                "nessuno conosce il nome. Nel Nuovo Testamento «santi» sono tutti i credenti (per esempio " +
                "Romani 1:7): la festa ricorda che la santità è la chiamata di ogni cristiano. Gli ortodossi " +
                "celebrano tutti i santi la domenica dopo Pentecoste.",
            whatToDo = listOf(
                "Leggi le Beatitudini (Matteo 5:1-12).",
                "Ricorda una persona di fede che ti ha ispirato e ringrazia Dio per lei.",
                "Scegli una beatitudine da vivere questa settimana.",
            ),
            readings = listOf("Mt 5:1-12", "Ap 7:9-17", "1Gv 3:1-3"),
        ),
        Feast(
            id = "defunti",
            name = "Commemorazione dei defunti",
            rule = Fixed(11, 2),
            tradition = CATTOLICA,
            importance = FESTA,
            color = VIOLA,
            summary = "Preghiera e memoria per chi è morto.",
            meaning = "Il giorno dopo Ognissanti si ricordano tutti i defunti; è tradizione visitare i cimiteri e " +
                "portare fiori. La fede cristiana guarda alla morte con la speranza della risurrezione: «Io son " +
                "la resurrezione e la vita» (Giovanni 11:25).",
            whatToDo = listOf(
                "Visita la tomba di una persona cara.",
                "Prega per i tuoi cari defunti e affidali a Dio.",
                "Leggi Giovanni 11:17-27 e 1 Tessalonicesi 4:13-18.",
                "Riconciliati con qualcuno finché sei in tempo.",
            ),
            readings = listOf("Gv 11:17-27", "1Ts 4:13-18", "Gb 19:23-27"),
        ),
        Feast(
            id = "cristo_re",
            name = "Cristo Re dell'universo",
            rule = FromAdvent(-7),
            tradition = OCCIDENTALE,
            importance = FESTA,
            color = BIANCO,
            summary = "Ultima domenica dell'anno liturgico.",
            meaning = "L'ultima domenica prima dell'Avvento chiude l'anno liturgico proclamando che Gesù è il Re " +
                "di tutto. Un re diverso dagli altri: regna dalla croce e servendo. Istituita dalla Chiesa " +
                "cattolica nel 1925, è celebrata anche da molte chiese protestanti.",
            whatToDo = listOf(
                "Leggi Matteo 25:31-46.",
                "Servi Gesù negli ultimi: visita un malato, aiuta un povero.",
                "Chiediti: chi o che cosa comanda davvero nella mia vita?",
            ),
            readings = listOf("Mt 25:31-46", "Col 1:12-20", "Lc 23:35-43"),
        ),
        Feast(
            id = "avvento",
            name = "Prima domenica di Avvento",
            rule = FromAdvent(0),
            tradition = OCCIDENTALE,
            importance = PRINCIPALE,
            color = VIOLA,
            summary = "Inizia l'anno liturgico e l'attesa del Natale.",
            meaning = "Comincia il nuovo anno liturgico. Le quattro domeniche di Avvento preparano al Natale " +
                "ricordando l'attesa dei profeti, la predicazione di Giovanni Battista e il «sì» di Maria. Le " +
                "chiese ortodosse vivono un periodo simile, il digiuno della Natività, dal 15 novembre.",
            whatToDo = listOf(
                "Prepara una corona d'Avvento con quattro candele e accendine una ogni domenica.",
                "Scegli un brano di Isaia da leggere ogni giorno.",
                "Decidi un gesto di carità per le settimane prima di Natale.",
            ),
            readings = listOf("Is 2:1-5", "Mt 24:37-44", "Rm 13:11-14"),
        ),
        Feast(
            id = "immacolata",
            name = "Immacolata Concezione",
            rule = Fixed(12, 8),
            tradition = CATTOLICA,
            importance = PRINCIPALE,
            color = BIANCO,
            summary = "Maria preservata dal peccato originale.",
            meaning = "La Chiesa cattolica crede che Maria, in vista della sua missione di madre di Gesù, sia stata " +
                "preservata dal peccato originale fin dal primo istante della sua esistenza (dogma proclamato " +
                "nel 1854). Non è la festa del concepimento di Gesù, che si ricorda all'Annunciazione (25 " +
                "marzo). In Italia è tradizione preparare l'albero e il presepe in questo giorno.",
            whatToDo = listOf(
                "Leggi Luca 1:26-38.",
                "Prepara il presepe in famiglia leggendo i racconti della nascita.",
                "Chiedi a Dio un cuore puro.",
            ),
            readings = listOf("Lc 1:26-38", "Gen 3:9-15", "Ef 1:3-12"),
        ),
        Feast(
            id = "vigilia",
            name = "Vigilia di Natale",
            rule = Fixed(12, 24),
            tradition = COMUNE,
            importance = FESTA,
            color = BIANCO,
            summary = "La notte dell'attesa.",
            meaning = "La sera del 24 dicembre si attende la nascita di Gesù. In molte chiese si celebra la Messa " +
                "o il culto della notte. Nella tradizione italiana è spesso una cena in famiglia «di magro», " +
                "cioè senza carne.",
            whatToDo = listOf(
                "Leggete insieme in famiglia Luca 2:1-20 prima della cena.",
                "Partecipa alla celebrazione della notte.",
                "Metti Gesù Bambino nel presepe.",
                "Invita qualcuno che altrimenti sarebbe solo.",
            ),
            readings = listOf("Is 9:2-7", "Lc 2:1-14", "Tt 2:11-14"),
        ),
        Feast(
            id = "natale",
            name = "Natale del Signore",
            rule = Fixed(12, 25),
            tradition = COMUNE,
            importance = PRINCIPALE,
            color = BIANCO,
            summary = "La nascita di Gesù.",
            meaning = "Il Figlio di Dio nasce a Betlemme da Maria: Dio si fa uomo («la Parola è stata fatta " +
                "carne», Giovanni 1:14) per salvare l'umanità. Dopo la Pasqua è la festa più importante. Alcune " +
                "chiese ortodosse, che seguono il calendario giuliano, lo celebrano il 7 gennaio.",
            whatToDo = listOf(
                "Partecipa alla celebrazione di Natale.",
                "Leggi l'inizio del Vangelo di Giovanni (1:1-18).",
                "Fai un dono a chi non può ricambiare.",
                "Ringrazia Dio prima del pranzo con una preghiera condivisa.",
            ),
            readings = listOf("Lc 2:1-20", "Gv 1:1-18", "Eb 1:1-6", "Is 52:7-10"),
        ),
        Feast(
            id = "santo_stefano",
            name = "Santo Stefano",
            rule = Fixed(12, 26),
            tradition = COMUNE,
            importance = FESTA,
            color = ROSSO,
            summary = "Il primo martire cristiano.",
            meaning = "Stefano, uno dei sette uomini scelti dagli apostoli per servire i poveri, fu lapidato a " +
                "Gerusalemme per la sua fede (Atti 6-7). Morendo perdonò chi lo uccideva, come Gesù. Fra i " +
                "presenti c'era Saulo, il futuro apostolo Paolo. Gli ortodossi lo ricordano il 27 dicembre.",
            whatToDo = listOf(
                "Leggi Atti 6:8-7:60.",
                "Perdona qualcuno che ti ha fatto del male.",
                "Prega per i cristiani perseguitati oggi.",
            ),
            readings = listOf("At 6:8-15", "At 7:54-60", "Mt 10:17-22"),
        ),
    )
}
