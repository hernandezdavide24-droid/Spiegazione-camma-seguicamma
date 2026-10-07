package it.lampada.bibbia

import it.lampada.bibbia.data.bible.Book
import it.lampada.bibbia.data.bible.RefFormatter
import it.lampada.bibbia.data.bible.Testament
import java.io.File

/** Accesso agli assets dell'app dai test JVM (la cartella di lavoro è il modulo "app"). */
object TestAssets {
    val dir: File = File(System.getProperty("assetsDir") ?: "src/main/assets")

    fun text(path: String): String = File(dir, path).readText()

    val books: List<Book> by lazy {
        text("bibbia/libri.tsv").lines().filter { it.isNotBlank() }.map { line ->
            val p = line.split('\t')
            Book(p[0].toInt(), p[1], p[2], p[3], Testament.valueOf(p[4]), p[5].split(',').map { it.toInt() })
        }
    }

    val refs: RefFormatter by lazy { RefFormatter(books) }
}
