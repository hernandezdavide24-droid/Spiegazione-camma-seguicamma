package it.lampada.bibbia.data.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

enum class Tradition(val label: String) {
    COMUNE("Tutti i cristiani"),
    OCCIDENTALE("Cattolici e protestanti"),
    CATTOLICA_ORTODOSSA("Cattolici e ortodossi"),
    CATTOLICA("Cattolici"),
    ORTODOSSA("Ortodossi"),
    PROTESTANTE("Protestanti"),
}

/** Colore liturgico usato dalle chiese che seguono l'anno liturgico. */
enum class LiturgicalColor(val label: String, val argb: Long) {
    BIANCO("Bianco", 0xFFF3E9C6),
    ROSSO("Rosso", 0xFFC62828),
    VIOLA("Viola", 0xFF6A1B9A),
    VERDE("Verde", 0xFF2E7D32),
    ROSA("Rosa", 0xFFE891B0),
    NERO("Nero / viola", 0xFF37253F),
}

enum class Importance(val label: String) {
    PRINCIPALE("Festa principale"),
    FESTA("Festa"),
    RICORRENZA("Ricorrenza"),
}

sealed interface DateRule {
    /** Giorno fisso dell'anno. */
    data class Fixed(val month: Int, val day: Int) : DateRule

    /** Giorni prima (negativo) o dopo la Pasqua di cattolici e protestanti. */
    data class FromEaster(val days: Int) : DateRule

    /** Giorni dalla Pasqua ortodossa. */
    data class FromOrthodoxEaster(val days: Int) : DateRule

    /** Giorni dalla prima domenica di Avvento. */
    data class FromAdvent(val days: Int) : DateRule

    /** Domenica dopo l'Epifania (Battesimo del Signore). */
    data object SundayAfterEpiphany : DateRule
}

data class Feast(
    val id: String,
    val name: String,
    val rule: DateRule,
    val tradition: Tradition,
    val importance: Importance,
    val color: LiturgicalColor,
    /** Una frase per la lista. */
    val summary: String,
    /** Cosa si ricorda e perché è importante. */
    val meaning: String,
    /** Idee concrete per viverla. */
    val whatToDo: List<String>,
    /** Riferimenti biblici da leggere, es. "Lc 2:1-20". */
    val readings: List<String>,
    /** Durata in giorni (1 = un solo giorno; >1 per periodi come la Settimana per l'unità). */
    val durationDays: Int = 1,
    val note: String? = null,
)

data class FeastOccurrence(val feast: Feast, val date: LocalDate) {
    val endDate: LocalDate get() = date.plusDays(feast.durationDays - 1L)
    fun covers(d: LocalDate) = !d.isBefore(date) && !d.isAfter(endDate)
}

data class Season(
    val id: String,
    val name: String,
    val color: LiturgicalColor,
    val description: String,
    val whatToDo: List<String>,
    val readings: List<String>,
)

data class SeasonSpan(val season: Season, val start: LocalDate, val end: LocalDate) {
    fun covers(d: LocalDate) = !d.isBefore(start) && !d.isAfter(end)
    /** Giorno del periodo (1 = primo giorno). */
    fun dayNumber(d: LocalDate) = ChronoUnit.DAYS.between(start, d).toInt() + 1
    val length: Int get() = ChronoUnit.DAYS.between(start, end).toInt() + 1
}

object ChristianCalendar {

    /** Pasqua gregoriana (cattolici e protestanti): algoritmo di Meeus/Jones/Butcher. */
    fun westernEaster(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = (h + l - 7 * m + 114) % 31 + 1
        return LocalDate.of(year, month, day)
    }

    /**
     * Pasqua ortodossa: calcolo giuliano (algoritmo di Meeus) convertito nel calendario
     * gregoriano. Lo scarto fra i due calendari è di 13 giorni dal 1900 al 2099.
     */
    fun orthodoxEaster(year: Int): LocalDate {
        val a = year % 4
        val b = year % 7
        val c = year % 19
        val d = (19 * c + 15) % 30
        val e = (2 * a + 4 * b - d + 34) % 7
        val month = (d + e + 114) / 31
        val day = (d + e + 114) % 31 + 1
        val julianOffset = year / 100 - year / 400 - 2
        return LocalDate.of(year, month, day).plusDays(julianOffset.toLong())
    }

    /** Prima domenica di Avvento: quarta domenica prima di Natale. */
    fun firstSundayOfAdvent(year: Int): LocalDate {
        val christmas = LocalDate.of(year, 12, 25)
        val fourth = christmas.with(TemporalAdjusters.previous(DayOfWeek.SUNDAY))
        return fourth.minusWeeks(3)
    }

    fun baptismOfTheLord(year: Int): LocalDate =
        LocalDate.of(year, 1, 6).with(TemporalAdjusters.next(DayOfWeek.SUNDAY))

    fun dateOf(rule: DateRule, year: Int): LocalDate = when (rule) {
        is DateRule.Fixed -> LocalDate.of(year, rule.month, rule.day)
        is DateRule.FromEaster -> westernEaster(year).plusDays(rule.days.toLong())
        is DateRule.FromOrthodoxEaster -> orthodoxEaster(year).plusDays(rule.days.toLong())
        is DateRule.FromAdvent -> firstSundayOfAdvent(year).plusDays(rule.days.toLong())
        DateRule.SundayAfterEpiphany -> baptismOfTheLord(year)
    }

    fun occurrences(year: Int, feasts: List<Feast> = FeastCatalog.feasts): List<FeastOccurrence> =
        feasts.mapNotNull { f ->
            val d = dateOf(f.rule, year)
            // La Pasqua ortodossa si mostra solo quando cade in un giorno diverso.
            if (f.id == "pasqua_ortodossa" && d == westernEaster(year)) null else FeastOccurrence(f, d)
        }.sortedBy { it.date }

    fun feastsOn(date: LocalDate, feasts: List<Feast> = FeastCatalog.feasts): List<FeastOccurrence> =
        (occurrences(date.year, feasts) + occurrences(date.year - 1, feasts).filter { it.feast.durationDays > 1 })
            .filter { it.covers(date) }

    fun upcoming(from: LocalDate, count: Int, feasts: List<Feast> = FeastCatalog.feasts): List<FeastOccurrence> =
        (occurrences(from.year, feasts) + occurrences(from.year + 1, feasts))
            .filter { !it.date.isBefore(from) }
            .take(count)

    /** Periodi dell'anno liturgico che toccano l'anno indicato. */
    fun seasons(year: Int): List<SeasonSpan> {
        val s = FeastCatalog.seasons.associateBy { it.id }
        val easter = westernEaster(year)
        val advent = firstSundayOfAdvent(year)
        return listOf(
            SeasonSpan(s.getValue("natale"), LocalDate.of(year - 1, 12, 25), baptismOfTheLord(year)),
            SeasonSpan(s.getValue("ordinario"), baptismOfTheLord(year).plusDays(1), easter.minusDays(47)),
            SeasonSpan(s.getValue("quaresima"), easter.minusDays(46), easter.minusDays(8)),
            SeasonSpan(s.getValue("settimana_santa"), easter.minusDays(7), easter.minusDays(1)),
            SeasonSpan(s.getValue("pasqua"), easter, easter.plusDays(49)),
            SeasonSpan(s.getValue("ordinario"), easter.plusDays(50), advent.minusDays(1)),
            SeasonSpan(s.getValue("avvento"), advent, LocalDate.of(year, 12, 24)),
            SeasonSpan(s.getValue("natale"), LocalDate.of(year, 12, 25), baptismOfTheLord(year + 1)),
        )
    }

    fun seasonOn(date: LocalDate): SeasonSpan =
        seasons(date.year).first { it.covers(date) }
}
