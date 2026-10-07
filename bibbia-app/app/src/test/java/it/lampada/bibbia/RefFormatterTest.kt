package it.lampada.bibbia

import it.lampada.bibbia.data.bible.VerseRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RefFormatterTest {
    private val r = TestAssets.refs

    @Test
    fun parsesCommonForms() {
        val gv = r.parse("Gv 3:16")!!
        assertEquals(VerseRef(43, 3, 16), gv.start)
        assertEquals(gv.start, gv.end)

        val range = r.parse("Gv 3:16-18")!!
        assertEquals(VerseRef(43, 3, 18), range.end)

        val cross = r.parse("Is 52:13-53:12")!!
        assertEquals(VerseRef(23, 52, 13), cross.start)
        assertEquals(VerseRef(23, 53, 12), cross.end)

        val psalm = r.parse("Sal 23")!!
        assertEquals(VerseRef(19, 23, 1), psalm.start)
        assertEquals(VerseRef(19, 23, 6), psalm.end)

        assertEquals(VerseRef(46, 13, 4), r.parse("1Cor 13:4")!!.start)
        assertEquals(VerseRef(46, 13, 4), r.parse("1 Cor 13,4")!!.start)
        assertEquals(VerseRef(43, 1, 1), r.parse("Giovanni 1:1")!!.start)
        assertEquals(VerseRef(62, 4, 8), r.parse("1Gv 4:8")!!.start)
        assertEquals(VerseRef(42, 2, 20), r.parse("Lc 1-2")!!.end.copy(verse = 20))
    }

    @Test
    fun rejectsInvalid() {
        assertNull(r.parse("Gv 22:1"))
        assertNull(r.parse("Gv 3:99"))
        assertNull(r.parse("Xyz 1:1"))
        assertNull(r.parse("Gv 3:18-16"))
    }

    @Test
    fun formats() {
        assertEquals("Giovanni 3:16", r.format(r.parse("Gv 3:16")!!))
        assertEquals("Salmi 23", r.format(r.parse("Sal 23")!!))
        assertEquals("Isaia 52:13-53:12", r.format(r.parse("Is 52:13-53:12")!!))
        assertEquals("Giovanni 3:16-18,21", r.formatSelection(43, 3, listOf(18, 16, 17, 21)))
    }
}
