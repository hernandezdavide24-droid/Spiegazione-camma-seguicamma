package it.lampada.bibbia.data.bible

import android.content.res.AssetManager
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class SearchHit(val verse: Verse, val ranges: List<IntRange>)

/**
 * Legge il testo della Riveduta 1927 dagli assets (un file per libro) e i riferimenti incrociati.
 * I libri letti restano in una piccola cache in memoria.
 */
class BibleRepository(private val assets: AssetManager) {

    val books: List<Book> by lazy {
        assets.open("bibbia/libri.tsv").bufferedReader().useLines { lines ->
            lines.filter { it.isNotBlank() }.map { line ->
                val p = line.split('\t')
                Book(
                    id = p[0].toInt(),
                    osis = p[1],
                    name = p[2],
                    abbr = p[3],
                    testament = Testament.valueOf(p[4]),
                    verseCounts = p[5].split(',').map { it.toInt() },
                )
            }.toList()
        }
    }

    val refs: RefFormatter by lazy { RefFormatter(books) }

    /** Elenco piatto di tutti i capitoli (1189), usato dal lettore per scorrere da un libro all'altro. */
    val chapterIndex: List<Pair<Int, Int>> by lazy {
        books.flatMap { b -> (1..b.chapters).map { b.id to it } }
    }

    fun globalChapterIndex(book: Int, chapter: Int): Int =
        books.take(book - 1).sumOf { it.chapters } + (chapter - 1)

    fun book(id: Int): Book = books[id - 1]

    private class LoadedBook(val headings: Map<Int, String>, val chapters: Map<Int, List<Verse>>)

    private val cache = LruCache<Int, LoadedBook>(8)
    private val xrefCache = LruCache<Int, Map<Pair<Int, Int>, List<Passage>>>(4)
    private val mutex = Mutex()

    private suspend fun load(bookId: Int): LoadedBook = withContext(Dispatchers.IO) {
        cache.get(bookId) ?: mutex.withLock {
            cache.get(bookId) ?: parseBook(bookId).also { cache.put(bookId, it) }
        }
    }

    private fun parseBook(bookId: Int): LoadedBook {
        val headings = HashMap<Int, String>()
        val chapters = HashMap<Int, MutableList<Verse>>()
        assets.open("bibbia/%02d.txt".format(bookId)).bufferedReader().useLines { lines ->
            for (line in lines) {
                if (line.isEmpty()) continue
                val t1 = line.indexOf('\t')
                val t2 = line.indexOf('\t', t1 + 1)
                val c = line.substring(0, t1).toInt()
                val v = line.substring(t1 + 1, t2).toInt()
                val text = line.substring(t2 + 1)
                if (v == 0) headings[c] = text
                else chapters.getOrPut(c) { ArrayList() } += Verse(bookId, c, v, text)
            }
        }
        return LoadedBook(headings, chapters)
    }

    suspend fun chapter(bookId: Int, chapter: Int): Chapter {
        val loaded = load(bookId)
        return Chapter(book(bookId), chapter, loaded.headings[chapter], loaded.chapters[chapter].orEmpty())
    }

    suspend fun verse(ref: VerseRef): Verse? {
        val list = load(ref.book).chapters[ref.chapter] ?: return null
        return list.getOrNull(ref.verse - 1)?.takeIf { it.number == ref.verse }
            ?: list.firstOrNull { it.number == ref.verse }
    }

    suspend fun passage(p: Passage): List<Verse> {
        val loaded = load(p.book)
        return (p.start.chapter..p.end.chapter).flatMap { c ->
            loaded.chapters[c].orEmpty().filter { v ->
                (c > p.start.chapter || v.number >= p.start.verse) && (c < p.end.chapter || v.number <= p.end.verse)
            }
        }
    }

    suspend fun passageText(p: Passage): String {
        val verses = passage(p)
        return if (verses.size == 1) verses.first().text
        else verses.joinToString(" ") { "${it.number} ${it.text}" }
    }

    /** Riferimenti incrociati per un versetto (dai più votati su openbible.info). */
    suspend fun crossRefs(ref: VerseRef): List<Passage> = withContext(Dispatchers.IO) {
        val map = xrefCache.get(ref.book) ?: loadXrefs(ref.book).also { xrefCache.put(ref.book, it) }
        map[ref.chapter to ref.verse].orEmpty()
    }

    private fun loadXrefs(bookId: Int): Map<Pair<Int, Int>, List<Passage>> {
        val out = HashMap<Pair<Int, Int>, List<Passage>>()
        val stream = try {
            assets.open("bibbia/xref/%02d.txt".format(bookId))
        } catch (_: java.io.IOException) {
            return out
        }
        stream.bufferedReader().useLines { lines ->
            for (line in lines) {
                val p = line.split('\t')
                if (p.size < 3) continue
                out[p[0].toInt() to p[1].toInt()] = p[2].split(';').mapNotNull(::parseXref)
            }
        }
        return out
    }

    /** "43.3.16", "43.3.16-18" oppure "43.3.16-4.2". */
    private fun parseXref(s: String): Passage? {
        val (from, to) = s.split('-').let { it[0] to it.getOrNull(1) }
        val f = from.split('.').map { it.toInt() }
        val start = VerseRef(f[0], f[1], f[2])
        val end = when {
            to == null -> start
            '.' in to -> to.split('.').let { VerseRef(f[0], it[0].toInt(), it[1].toInt()) }
            else -> VerseRef(f[0], f[1], to.toInt())
        }
        return if (end < start) null else Passage(start, end)
    }

    /**
     * Cerca una parola o frase in tutta la Bibbia (o in un testamento), ignorando maiuscole e accenti.
     * Emette i risultati man mano, fino a [limit].
     */
    fun search(query: String, testament: Testament? = null, limit: Int = 500): Flow<SearchHit> = flow {
        val q = TextSearch.normalizeQuery(query)
        if (q.length < 2) return@flow
        var count = 0
        for (b in books) {
            if (testament != null && b.testament != testament) continue
            val loaded = load(b.id)
            for (c in 1..b.chapters) {
                for (v in loaded.chapters[c].orEmpty()) {
                    val ranges = TextSearch.matches(v.text, q)
                    if (ranges.isNotEmpty()) {
                        emit(SearchHit(v, ranges))
                        if (++count >= limit) return@flow
                    }
                }
            }
        }
    }.flowOn(Dispatchers.Default)
}
