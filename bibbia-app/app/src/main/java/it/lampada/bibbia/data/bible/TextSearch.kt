package it.lampada.bibbia.data.bible

import java.text.Normalizer
import java.util.Locale

/** Normalizzazione del testo per ricerche che ignorano maiuscole, accenti e apostrofi tipografici. */
object TextSearch {
    private val marks = Regex("\\p{M}+")

    /**
     * Restituisce una versione "piatta" del testo con la stessa lunghezza dell'originale,
     * così le posizioni trovate valgono anche per evidenziare il testo originale.
     */
    fun fold(s: String): String {
        val sb = StringBuilder(s.length)
        for (ch in s) {
            val c = when (ch) {
                '’', '‘', '`', '´' -> '\''
                '“', '”', '«', '»' -> '"'
                else -> {
                    val base = Normalizer.normalize(ch.toString(), Normalizer.Form.NFD).replace(marks, "")
                    if (base.length == 1) base[0] else ch
                }
            }
            sb.append(c.lowercaseChar())
        }
        return sb.toString()
    }

    /** Trova tutte le occorrenze di [query] (già normalizzata con [fold]) in [text]. */
    fun matches(text: String, foldedQuery: String): List<IntRange> {
        if (foldedQuery.isBlank()) return emptyList()
        val folded = fold(text)
        val out = mutableListOf<IntRange>()
        var i = folded.indexOf(foldedQuery)
        while (i >= 0) {
            out += i until i + foldedQuery.length
            i = folded.indexOf(foldedQuery, i + foldedQuery.length)
        }
        return out
    }

    /** True se [word] compare come parola intera in [foldedText]. */
    fun containsWord(foldedText: String, foldedWord: String): Boolean {
        var i = foldedText.indexOf(foldedWord)
        while (i >= 0) {
            val before = if (i == 0) ' ' else foldedText[i - 1]
            val afterIdx = i + foldedWord.length
            val after = if (afterIdx >= foldedText.length) ' ' else foldedText[afterIdx]
            if (!before.isLetter() && !after.isLetter()) return true
            i = foldedText.indexOf(foldedWord, i + 1)
        }
        return false
    }

    fun normalizeQuery(q: String): String = fold(q.trim()).lowercase(Locale.ITALIAN)
}
