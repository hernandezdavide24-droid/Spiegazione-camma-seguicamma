package it.lampada.bibbia.data.daily

import java.time.LocalDate
import kotlin.random.Random

/**
 * Sceglie il versetto del giorno da una lista curata.
 * Ogni anno la lista viene mescolata con un seme fisso: lo stesso giorno dà sempre lo stesso
 * versetto (nell'app e nella notifica) e nell'arco dell'anno non ci sono ripetizioni
 * finché la lista non è esaurita.
 */
object DailyVersePicker {
    fun pick(date: LocalDate, refs: List<String>): String {
        require(refs.isNotEmpty())
        val order = refs.indices.shuffled(Random(date.year * 7919L))
        return refs[order[(date.dayOfYear - 1) % refs.size]]
    }
}
