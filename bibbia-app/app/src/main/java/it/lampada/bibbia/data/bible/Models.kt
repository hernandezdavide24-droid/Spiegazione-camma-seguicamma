package it.lampada.bibbia.data.bible

enum class Testament(val label: String) {
    AT("Antico Testamento"),
    NT("Nuovo Testamento"),
}

data class Book(
    val id: Int,
    val osis: String,
    val name: String,
    val abbr: String,
    val testament: Testament,
    /** Numero di versetti per ogni capitolo (indice 0 = capitolo 1). */
    val verseCounts: List<Int>,
) {
    val chapters: Int get() = verseCounts.size
    fun verseCount(chapter: Int): Int = verseCounts.getOrElse(chapter - 1) { 0 }
}

data class VerseRef(val book: Int, val chapter: Int, val verse: Int) : Comparable<VerseRef> {
    override fun compareTo(other: VerseRef): Int =
        compareValuesBy(this, other, { it.book }, { it.chapter }, { it.verse })
}

data class Verse(val book: Int, val chapter: Int, val number: Int, val text: String) {
    val ref: VerseRef get() = VerseRef(book, chapter, number)
}

data class Chapter(
    val book: Book,
    val number: Int,
    /** Titolo originale del Salmo (es. "Salmo di Davide."), se presente. */
    val heading: String?,
    val verses: List<Verse>,
)

/** Un brano: da [start] a [end] inclusi, sempre nello stesso libro. */
data class Passage(val start: VerseRef, val end: VerseRef) {
    val book: Int get() = start.book
    val isSingleVerse: Boolean get() = start == end
}
