package it.lampada.bibbia

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.lampada.bibbia.data.bible.VerseRef
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Verifica che i dati negli assets si leggano correttamente sul dispositivo (Robolectric). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class RepositoryTest {
    private val container = (ApplicationProvider.getApplicationContext<LampadaApp>()).container

    @Test
    fun bibleIsComplete() = runBlocking {
        val bible = container.bible
        assertEquals(66, bible.books.size)
        assertEquals(1189, bible.chapterIndex.size)
        var verses = 0
        for (b in bible.books) for (c in 1..b.chapters) verses += bible.chapter(b.id, c).verses.size
        assertEquals(31102, verses)
        assertEquals(
            "Poiché Iddio ha tanto amato il mondo, che ha dato il suo unigenito Figliuolo, affinché chiunque " +
                "crede in lui non perisca, ma abbia vita eterna.",
            bible.verse(VerseRef(43, 3, 16))!!.text,
        )
        assertEquals("Salmo di Davide.", bible.chapter(19, 23).heading)
    }

    @Test
    fun searchIgnoresAccentsAndCase() = runBlocking {
        val hits = container.bible.search("NEL PRINCIPIO IDDIO CREO").toList()
        assertTrue(hits.any { it.verse.ref == VerseRef(1, 1, 1) })
    }

    @Test
    fun verseAnalysisHasCrossReferencesAndGlossary() = runBlocking {
        val a = container.analysis.verseAnalysis(VerseRef(43, 3, 16))
        assertNotNull(a)
        assertTrue(a!!.crossRefs.isNotEmpty())
        assertTrue(a.glossary.any { it.term == "Iddio" })
        val ps = container.analysis.verseAnalysis(VerseRef(19, 23, 1))!!
        assertTrue(ps.glossary.any { it.term == "Eterno" })
    }

    @Test
    fun dailyVerseLoads() = runBlocking {
        val v = container.dailyVerses.forDate()
        assertTrue(v.text.isNotBlank())
    }
}
