package it.lampada.bibbia.data.daily

import android.content.res.AssetManager
import it.lampada.bibbia.data.bible.BibleRepository
import it.lampada.bibbia.data.bible.Passage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

data class DailyVerse(val date: LocalDate, val passage: Passage, val reference: String, val text: String)

class DailyVerseRepository(private val assets: AssetManager, private val bible: BibleRepository) {

    private val refs: List<String> by lazy {
        assets.open("versetti_del_giorno.txt").bufferedReader().useLines { lines ->
            lines.map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("//") }.toList()
        }
    }

    suspend fun forDate(date: LocalDate = LocalDate.now()): DailyVerse = withContext(Dispatchers.IO) {
        val raw = DailyVersePicker.pick(date, refs)
        val passage = bible.refs.parse(raw) ?: bible.refs.parse("Sal 119:105")!!
        DailyVerse(date, passage, bible.refs.format(passage), bible.passageText(passage))
    }
}
