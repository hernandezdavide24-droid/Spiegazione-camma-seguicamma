package it.lampada.bibbia.data.analysis

/** Introduzione a un libro biblico (autore, epoca, contesto, temi...). */
data class BookIntro(
    val title: String,
    val fields: List<Pair<String, String>>,
) {
    fun field(key: String): String? = fields.firstOrNull { it.first == key }?.second
}

data class Section(val startVerse: Int, val title: String)

data class ChapterOutline(
    val chapter: Int,
    val title: String,
    val sections: List<Section>,
    val summary: String,
) {
    /** Sezione che contiene il versetto, con il suo ultimo versetto (se noto). */
    fun sectionFor(verse: Int, lastVerseOfChapter: Int): Pair<Section, Int>? {
        val idx = sections.indexOfLast { it.startVerse <= verse }
        if (idx < 0) return null
        val end = sections.getOrNull(idx + 1)?.startVerse?.minus(1) ?: lastVerseOfChapter
        return sections[idx] to end
    }
}

data class BookAnalysis(val bookId: Int, val intro: BookIntro?, val chapters: Map<Int, ChapterOutline>)

data class GlossaryEntry(val term: String, val variants: List<String>, val explanation: String)

/**
 * Formato dei file assets/analisi/NN.txt:
 * ```
 * #intro
 * titolo: Il Vangelo secondo Giovanni
 * autore: ...
 * #cap 3 Gesù e Nicodemo
 * @1 Il dialogo con Nicodemo
 * @22 L'ultima testimonianza di Giovanni Battista
 * Testo della sintesi (anche su più righe).
 * ```
 * Le righe che iniziano con "//" sono commenti.
 */
object AnalysisParser {

    /** Etichette leggibili dei campi dell'introduzione, nell'ordine di visualizzazione. */
    val INTRO_LABELS = linkedMapOf(
        "sintesi" to "In breve",
        "autore" to "Autore",
        "data" to "Quando fu scritto",
        "destinatari" to "Per chi fu scritto",
        "contesto" to "Contesto storico",
        "scopo" to "Scopo del libro",
        "temi" to "Temi principali",
        "struttura" to "Struttura",
        "cristo" to "Collegamenti con Gesù Cristo",
        "chiave" to "Versetti chiave",
    )

    fun parseBook(bookId: Int, text: String): BookAnalysis {
        var title = ""
        val fields = mutableListOf<Pair<String, String>>()
        val chapters = mutableMapOf<Int, ChapterOutline>()
        var mode = ""
        var capNum = 0
        var capTitle = ""
        var sections = mutableListOf<Section>()
        val summary = StringBuilder()
        var hasIntro = false

        fun flushChapter() {
            if (mode == "cap") {
                chapters[capNum] = ChapterOutline(capNum, capTitle, sections.toList(), summary.toString().trim())
            }
            sections = mutableListOf()
            summary.clear()
        }

        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("//")) continue
            when {
                line == "#intro" -> {
                    flushChapter(); mode = "intro"; hasIntro = true
                }
                line.startsWith("#cap ") -> {
                    flushChapter()
                    mode = "cap"
                    val rest = line.removePrefix("#cap ").trim()
                    val sp = rest.indexOf(' ')
                    capNum = (if (sp < 0) rest else rest.substring(0, sp)).toInt()
                    capTitle = if (sp < 0) "" else rest.substring(sp + 1).trim()
                }
                mode == "intro" -> {
                    val colon = line.indexOf(':')
                    if (colon > 0 && line.substring(0, colon).all { it.isLetter() }) {
                        val key = line.substring(0, colon).lowercase()
                        val value = line.substring(colon + 1).trim()
                        if (key == "titolo") title = value else fields += key to value
                    } else if (fields.isNotEmpty()) {
                        val (k, v) = fields.removeAt(fields.lastIndex)
                        fields += k to "$v\n$line"
                    }
                }
                mode == "cap" && line.startsWith("@") -> {
                    val rest = line.substring(1)
                    val sp = rest.indexOf(' ')
                    sections += Section(rest.substring(0, sp).toInt(), rest.substring(sp + 1).trim())
                }
                mode == "cap" -> {
                    if (summary.isNotEmpty()) summary.append(' ')
                    summary.append(line)
                }
            }
        }
        flushChapter()
        val intro = if (hasIntro) BookIntro(title, fields) else null
        return BookAnalysis(bookId, intro, chapters)
    }

    /** Formato: termine|variante1,variante2|spiegazione */
    fun parseGlossary(text: String): List<GlossaryEntry> =
        text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("//") }
            .mapNotNull { line ->
                val parts = line.split('|')
                if (parts.size != 3) return@mapNotNull null
                val variants = (listOf(parts[0]) + parts[1].split(','))
                    .map { it.trim() }.filter { it.isNotEmpty() }.distinct()
                GlossaryEntry(parts[0].trim(), variants, parts[2].trim())
            }
            .toList()
}
