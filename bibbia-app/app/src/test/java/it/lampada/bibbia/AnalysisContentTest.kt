package it.lampada.bibbia

import it.lampada.bibbia.data.analysis.AnalysisParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** I testi delle analisi devono essere coerenti con la Bibbia: ogni capitolo, sezioni valide. */
class AnalysisContentTest {

    private val files = File(TestAssets.dir, "analisi").listFiles { f -> f.name.matches(Regex("\\d\\d\\.txt")) }
        .orEmpty().sortedBy { it.name }

    @Test
    fun everyBookFileMatchesTheBible() {
        assertTrue("nessun file di analisi trovato", files.isNotEmpty())
        for (f in files) {
            val bookId = f.name.take(2).toInt()
            val book = TestAssets.books[bookId - 1]
            val analysis = AnalysisParser.parseBook(bookId, f.readText())
            val intro = analysis.intro
            assertNotNull("${book.name}: introduzione mancante", intro)
            for (key in listOf("sintesi", "autore", "data", "contesto", "temi", "struttura", "chiave")) {
                assertNotNull("${book.name}: campo $key", intro!!.field(key))
            }
            intro!!.field("chiave")!!.split(';').map { it.trim() }.filter { it.isNotEmpty() }.forEach {
                assertNotNull("${book.name}: versetto chiave $it", TestAssets.refs.parse(it))
            }
            assertEquals("${book.name}: capitoli", (1..book.chapters).toSet(), analysis.chapters.keys)
            for ((c, outline) in analysis.chapters) {
                assertTrue("${book.name} $c: titolo", outline.title.isNotBlank())
                assertTrue("${book.name} $c: sintesi", outline.summary.isNotBlank())
                assertTrue("${book.name} $c: sezioni", outline.sections.isNotEmpty())
                assertEquals("${book.name} $c: prima sezione", 1, outline.sections.first().startVerse)
                outline.sections.zipWithNext().forEach { (a, b) ->
                    assertTrue("${book.name} $c: ordine sezioni", a.startVerse < b.startVerse)
                }
                assertTrue("${book.name} $c: sezione oltre la fine", outline.sections.last().startVerse <= book.verseCount(c))
            }
        }
    }

    @Test
    fun glossaryParses() {
        val entries = AnalysisParser.parseGlossary(TestAssets.text("analisi/glossario.txt"))
        assertTrue(entries.size > 100)
        assertEquals(entries.size, entries.map { it.term.lowercase() }.toSet().size)
    }
}
