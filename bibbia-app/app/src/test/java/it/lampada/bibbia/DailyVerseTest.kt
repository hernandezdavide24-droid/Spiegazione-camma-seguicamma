package it.lampada.bibbia

import it.lampada.bibbia.data.daily.DailyVersePicker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DailyVerseTest {
    private val refs = TestAssets.text("versetti_del_giorno.txt").lines()
        .map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("//") }

    @Test
    fun listIsValid() {
        assertTrue("servono almeno 366 versetti, trovati ${refs.size}", refs.size >= 366)
        assertEquals("riferimenti duplicati", refs.size, refs.toSet().size)
        for (r in refs) assertNotNull(r, TestAssets.refs.parse(r))
    }

    @Test
    fun sameDaySameVerseAndNoRepeatsInYear() {
        val d = LocalDate.of(2026, 10, 7)
        assertEquals(DailyVersePicker.pick(d, refs), DailyVersePicker.pick(d, refs))
        val year = (0 until 365).map { DailyVersePicker.pick(LocalDate.of(2026, 1, 1).plusDays(it.toLong()), refs) }
        assertEquals(365, year.toSet().size)
    }
}
