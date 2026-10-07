package it.lampada.bibbia.data.analysis

import android.content.res.AssetManager
import it.lampada.bibbia.data.bible.BibleRepository
import it.lampada.bibbia.data.bible.Passage
import it.lampada.bibbia.data.bible.TextSearch
import it.lampada.bibbia.data.bible.Verse
import it.lampada.bibbia.data.bible.VerseRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class CrossRefItem(val passage: Passage, val label: String, val text: String)

/** Tutto ciò che serve per spiegare un versetto, calcolato offline. */
data class VerseAnalysis(
    val verse: Verse,
    val reference: String,
    val previous: Verse?,
    val next: Verse?,
    val chapterTitle: String?,
    val section: Section?,
    val sectionRange: String?,
    val chapterSummary: String?,
    val glossary: List<GlossaryEntry>,
    val crossRefs: List<CrossRefItem>,
    val bookIntro: BookIntro?,
)

class AnalysisRepository(
    private val assets: AssetManager,
    private val bible: BibleRepository,
) {
    private val books = HashMap<Int, BookAnalysis>()
    private var glossaryCache: List<GlossaryEntry>? = null
    private val mutex = Mutex()

    suspend fun book(bookId: Int): BookAnalysis = mutex.withLock {
        books[bookId] ?: withContext(Dispatchers.IO) {
            val text = try {
                assets.open("analisi/%02d.txt".format(bookId)).bufferedReader().use { it.readText() }
            } catch (_: java.io.IOException) {
                ""
            }
            AnalysisParser.parseBook(bookId, text)
        }.also { books[bookId] = it }
    }

    suspend fun glossary(): List<GlossaryEntry> = mutex.withLock {
        glossaryCache ?: withContext(Dispatchers.IO) {
            AnalysisParser.parseGlossary(
                assets.open("analisi/glossario.txt").bufferedReader().use { it.readText() },
            )
        }.also { glossaryCache = it }
    }

    suspend fun chapter(bookId: Int, chapter: Int): ChapterOutline? = book(bookId).chapters[chapter]

    /** Voci del glossario che compaiono nel testo (come parole intere). */
    suspend fun glossaryFor(text: String): List<GlossaryEntry> {
        val folded = TextSearch.fold(text)
        return glossary().filter { e -> e.variants.any { TextSearch.containsWord(folded, TextSearch.fold(it)) } }
    }

    suspend fun verseAnalysis(ref: VerseRef): VerseAnalysis? {
        val verse = bible.verse(ref) ?: return null
        val chapter = bible.chapter(ref.book, ref.chapter)
        val outline = chapter(ref.book, ref.chapter)
        val last = chapter.verses.lastOrNull()?.number ?: ref.verse
        val sectionInfo = outline?.sectionFor(ref.verse, last)
        val refs = bible.refs
        val crossRefs = bible.crossRefs(ref).map { p ->
            CrossRefItem(p, refs.format(p), bible.passageText(p))
        }
        return VerseAnalysis(
            verse = verse,
            reference = refs.format(ref),
            previous = chapter.verses.firstOrNull { it.number == ref.verse - 1 },
            next = chapter.verses.firstOrNull { it.number == ref.verse + 1 },
            chapterTitle = outline?.title?.takeIf { it.isNotBlank() },
            section = sectionInfo?.first,
            sectionRange = sectionInfo?.let { (s, end) ->
                if (s.startVerse == end) "v. ${s.startVerse}" else "vv. ${s.startVerse}-$end"
            },
            chapterSummary = outline?.summary?.takeIf { it.isNotBlank() },
            glossary = glossaryFor(verse.text),
            crossRefs = crossRefs,
            bookIntro = book(ref.book).intro,
        )
    }
}
