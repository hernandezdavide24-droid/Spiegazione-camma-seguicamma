package it.lampada.bibbia

import it.lampada.bibbia.data.calendar.ChristianCalendar
import it.lampada.bibbia.data.calendar.FeastCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CalendarTest {

    @Test
    fun westernEaster() {
        assertEquals(LocalDate.of(2024, 3, 31), ChristianCalendar.westernEaster(2024))
        assertEquals(LocalDate.of(2025, 4, 20), ChristianCalendar.westernEaster(2025))
        assertEquals(LocalDate.of(2026, 4, 5), ChristianCalendar.westernEaster(2026))
        assertEquals(LocalDate.of(2027, 3, 28), ChristianCalendar.westernEaster(2027))
        assertEquals(LocalDate.of(2038, 4, 25), ChristianCalendar.westernEaster(2038))
    }

    @Test
    fun orthodoxEaster() {
        assertEquals(LocalDate.of(2024, 5, 5), ChristianCalendar.orthodoxEaster(2024))
        assertEquals(LocalDate.of(2025, 4, 20), ChristianCalendar.orthodoxEaster(2025))
        assertEquals(LocalDate.of(2026, 4, 12), ChristianCalendar.orthodoxEaster(2026))
        assertEquals(LocalDate.of(2027, 5, 2), ChristianCalendar.orthodoxEaster(2027))
    }

    @Test
    fun advent() {
        assertEquals(LocalDate.of(2022, 11, 27), ChristianCalendar.firstSundayOfAdvent(2022))
        assertEquals(LocalDate.of(2024, 12, 1), ChristianCalendar.firstSundayOfAdvent(2024))
        assertEquals(LocalDate.of(2025, 11, 30), ChristianCalendar.firstSundayOfAdvent(2025))
        assertEquals(LocalDate.of(2026, 11, 29), ChristianCalendar.firstSundayOfAdvent(2026))
    }

    @Test
    fun movableFeasts2026() {
        val occ = ChristianCalendar.occurrences(2026).associate { it.feast.id to it.date }
        assertEquals(LocalDate.of(2026, 2, 18), occ["ceneri"])
        assertEquals(LocalDate.of(2026, 3, 29), occ["palme"])
        assertEquals(LocalDate.of(2026, 5, 14), occ["ascensione"])
        assertEquals(LocalDate.of(2026, 5, 24), occ["pentecoste"])
        assertEquals(LocalDate.of(2026, 1, 11), occ["battesimo"])
        assertEquals(LocalDate.of(2026, 11, 22), occ["cristo_re"])
    }

    @Test
    fun orthodoxEasterHiddenWhenSameDay() {
        val ids2025 = ChristianCalendar.occurrences(2025).map { it.feast.id }
        assertTrue("pasqua_ortodossa" !in ids2025)
        val ids2026 = ChristianCalendar.occurrences(2026).map { it.feast.id }
        assertTrue("pasqua_ortodossa" in ids2026)
    }

    @Test
    fun everyDayHasExactlyOneSeason() {
        for (year in 2000..2060) {
            var d = LocalDate.of(year, 1, 1)
            while (d.year == year) {
                val spans = ChristianCalendar.seasons(year).filter { it.covers(d) }
                assertEquals("$d: ${spans.map { it.season.id }}", 1, spans.size)
                d = d.plusDays(1)
            }
        }
    }

    @Test
    fun multiDayFeastsSpanYears() {
        val onJan20 = ChristianCalendar.feastsOn(LocalDate.of(2027, 1, 20)).map { it.feast.id }
        assertTrue("unita_cristiani" in onJan20)
        assertTrue(ChristianCalendar.feastsOn(LocalDate.of(2026, 10, 4)).any { it.feast.id == "tempo_creato" })
        assertTrue(ChristianCalendar.feastsOn(LocalDate.of(2026, 10, 5)).none { it.feast.id == "tempo_creato" })
    }

    @Test
    fun allReadingsAreValidReferences() {
        val refs = FeastCatalog.feasts.flatMap { f -> f.readings.map { f.id to it } } +
            FeastCatalog.seasons.flatMap { s -> s.readings.map { s.id to it } }
        for ((id, ref) in refs) assertNotNull("$id: $ref", TestAssets.refs.parse(ref))
        val ids = FeastCatalog.feasts.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }
}
