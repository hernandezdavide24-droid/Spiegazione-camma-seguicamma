package it.lampada.bibbia.data.bible

import java.text.Normalizer
import java.util.Locale

/** Formattazione e lettura dei riferimenti biblici ("Giovanni 3:16", "Gv 3:16-18", "Sal 23"). */
class RefFormatter(private val books: List<Book>) {

    private val byKey: Map<String, Book> = buildMap {
        for (b in books) {
            put(key(b.abbr), b)
            put(key(b.name), b)
            put(key(b.osis), b)
        }
        // Forme alternative comuni.
        books.firstOrNull { it.osis == "Acts" }?.let { put(key("Atti"), it) }
        books.firstOrNull { it.osis == "Ps" }?.let { put(key("Salmo"), it); put(key("Sl"), it) }
        books.firstOrNull { it.osis == "Song" }?.let { put(key("Cantico"), it) }
        books.firstOrNull { it.osis == "Eccl" }?.let { put(key("Qoelet"), it); put(key("Qo"), it) }
    }

    fun book(id: Int): Book = books[id - 1]

    fun format(ref: VerseRef, short: Boolean = false): String =
        "${name(ref.book, short)} ${ref.chapter}:${ref.verse}"

    fun formatChapter(book: Int, chapter: Int, short: Boolean = false): String =
        "${name(book, short)} $chapter"

    fun format(p: Passage, short: Boolean = false): String {
        val s = p.start
        val e = p.end
        val base = "${name(s.book, short)} ${s.chapter}"
        val wholeChapters = s.verse == 1 && e.verse == book(e.book).verseCount(e.chapter)
        return when {
            wholeChapters && s.chapter == e.chapter -> base
            wholeChapters -> "$base-${e.chapter}"
            p.isSingleVerse -> "$base:${s.verse}"
            s.chapter == e.chapter -> "$base:${s.verse}-${e.verse}"
            else -> "$base:${s.verse}-${e.chapter}:${e.verse}"
        }
    }

    /** Formatta un insieme di versetti dello stesso capitolo, es. "Giovanni 3:16-18,21". */
    fun formatSelection(book: Int, chapter: Int, verses: Collection<Int>): String {
        val sorted = verses.toSortedSet().toList()
        if (sorted.isEmpty()) return formatChapter(book, chapter)
        val parts = mutableListOf<String>()
        var runStart = sorted.first()
        var prev = runStart
        for (v in sorted.drop(1) + Int.MIN_VALUE) {
            if (v == prev + 1) {
                prev = v
                continue
            }
            parts += if (runStart == prev) "$runStart" else "$runStart-$prev"
            runStart = v
            prev = v
        }
        return "${name(book, false)} $chapter:${parts.joinToString(",")}"
    }

    /**
     * Legge un riferimento come "Gv 3:16", "1 Cor 13", "Sal 23:1-4", "Mt 5:1-7:29", "Is 52:13-53:12",
     * "Lc 1-2". Accetta anche la virgola italiana ("Gv 3,16"). Restituisce null se non valido.
     */
    fun parse(text: String): Passage? {
        val m = REF.matchEntire(text.trim()) ?: return null
        val (bookPart, c1s, v1s, c2s, v2s) = m.destructured
        val book = byKey[key(bookPart)] ?: return null
        val c1 = c1s.toInt()
        if (c1 !in 1..book.chapters) return null
        val start: VerseRef
        val end: VerseRef
        if (v1s.isEmpty()) {
            // "Lc 1" oppure "Lc 1-2": capitoli interi.
            val cEnd = if (c2s.isEmpty()) c1 else c2s.toInt()
            if (cEnd !in c1..book.chapters || v2s.isNotEmpty()) return null
            start = VerseRef(book.id, c1, 1)
            end = VerseRef(book.id, cEnd, book.verseCount(cEnd))
        } else {
            val v1 = v1s.toInt()
            start = VerseRef(book.id, c1, v1)
            end = when {
                c2s.isEmpty() -> VerseRef(book.id, c1, v1)
                v2s.isEmpty() -> VerseRef(book.id, c1, c2s.toInt()) // "3:16-18": il secondo numero è un versetto
                else -> VerseRef(book.id, c2s.toInt(), v2s.toInt())
            }
        }
        if (!valid(book, start) || !valid(book, end) || end < start) return null
        return Passage(start, end)
    }

    private fun valid(book: Book, r: VerseRef) =
        r.chapter in 1..book.chapters && r.verse in 1..book.verseCount(r.chapter)

    private fun name(book: Int, short: Boolean) = if (short) book(book).abbr else book(book).name

    companion object {
        // libro  capitolo [:versetto] [- (capitolo:versetto | numero)]
        private val REF = Regex("""^((?:[1-3]\s*)?[A-Za-zÀ-ÿ’' ]+?)\.?\s*(\d+)(?:[:,.](\d+))?(?:\s*-\s*(\d+)(?:[:,.](\d+))?)?$""")

        fun key(s: String): String {
            val n = Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
            return n.lowercase(Locale.ITALIAN).replace(Regex("[^a-z0-9]"), "")
        }
    }
}
