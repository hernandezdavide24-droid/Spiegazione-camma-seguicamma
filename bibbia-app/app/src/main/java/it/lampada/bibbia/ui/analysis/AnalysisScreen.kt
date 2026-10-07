package it.lampada.bibbia.ui.analysis

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.lampada.bibbia.AppContainer
import it.lampada.bibbia.data.analysis.AnalysisParser
import it.lampada.bibbia.data.analysis.BookAnalysis
import it.lampada.bibbia.data.analysis.VerseAnalysis
import it.lampada.bibbia.data.bible.Chapter
import it.lampada.bibbia.data.bible.Verse
import it.lampada.bibbia.data.bible.VerseRef
import it.lampada.bibbia.ui.components.BackTopBar
import it.lampada.bibbia.ui.components.Loading
import it.lampada.bibbia.ui.components.Paragraph
import it.lampada.bibbia.ui.components.SectionTitle

/**
 * Analisi offline su tre livelli: il versetto (se indicato), il capitolo e il libro.
 * [verse] = 0 apre direttamente il capitolo.
 */
@Composable
fun AnalysisScreen(
    container: AppContainer,
    book: Int,
    chapter: Int,
    verse: Int,
    onOpenVerse: (VerseRef) -> Unit,
    onBack: () -> Unit,
) {
    val tabs = buildList {
        if (verse > 0) add("Versetto")
        add("Capitolo")
        add("Libro")
    }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val bookInfo = container.bible.book(book)
    val title = if (verse > 0) container.bible.refs.format(VerseRef(book, chapter, verse))
    else container.bible.refs.formatChapter(book, chapter)

    Scaffold(topBar = { BackTopBar("Analisi", onBack, subtitle = title) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = tab) {
                tabs.forEachIndexed { i, label ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label) })
                }
            }
            when (tabs[tab]) {
                "Versetto" -> VerseTab(container, VerseRef(book, chapter, verse), onOpenVerse)
                "Capitolo" -> ChapterTab(container, book, chapter, onOpenVerse)
                else -> BookTab(container, book, bookInfo.name, onOpenVerse)
            }
        }
    }
}

@Composable
private fun VerseTab(container: AppContainer, ref: VerseRef, onOpenVerse: (VerseRef) -> Unit) {
    val analysis by produceState<VerseAnalysis?>(null, ref) { value = container.analysis.verseAnalysis(ref) }
    val a = analysis ?: return Loading()
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(18.dp)) {
                Text(a.reference, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Text(a.verse.text, style = MaterialTheme.typography.bodyLarge, fontFamily = FontFamily.Serif)
            }
        }

        SectionTitle("Dove si trova")
        val path = buildList {
            add(container.bible.book(ref.book).name)
            add("capitolo ${ref.chapter}" + (a.chapterTitle?.let { ": $it" } ?: ""))
            a.section?.let { add("${it.title} (${a.sectionRange})") }
        }
        Paragraph(path.joinToString(" › "))

        if (a.previous != null || a.next != null) {
            SectionTitle("Il contesto immediato")
            Text(
                "Un versetto si capisce meglio leggendo ciò che lo precede e lo segue:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            a.previous?.let { ContextVerse(it, current = false) }
            ContextVerse(a.verse, current = true)
            a.next?.let { ContextVerse(it, current = false) }
        }

        a.chapterSummary?.let {
            SectionTitle("Cosa succede in questo capitolo")
            Paragraph(it)
        }

        if (a.glossary.isNotEmpty()) {
            SectionTitle("Parole da capire")
            a.glossary.forEach { g ->
                Text(g.term, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(g.explanation, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
            }
        }

        if (a.crossRefs.isNotEmpty()) {
            SectionTitle("Altri passi collegati")
            Text(
                "La Bibbia spiega la Bibbia: questi brani parlano dello stesso tema.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            a.crossRefs.forEach { x ->
                LinkRow(x.label, x.text) { onOpenVerse(x.passage.start) }
            }
        }

        a.bookIntro?.let { intro ->
            SectionTitle("Il libro in breve")
            intro.field("sintesi")?.let { Paragraph(it) }
            intro.field("autore")?.let { LabeledText("Autore", it) }
            intro.field("data")?.let { LabeledText("Quando", it) }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Le spiegazioni sono una guida introduttiva preparata per l'app; i collegamenti fra versetti " +
                "provengono da openbible.info.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ContextVerse(v: Verse, current: Boolean) {
    Row(Modifier.padding(vertical = 3.dp)) {
        Text(
            "${v.number}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(28.dp),
        )
        Text(
            v.text,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Serif,
            fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
            color = if (current) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ChapterTab(container: AppContainer, book: Int, chapter: Int, onOpenVerse: (VerseRef) -> Unit) {
    val data by produceState<Pair<Chapter, BookAnalysis>?>(null, book, chapter) {
        value = container.bible.chapter(book, chapter) to container.analysis.book(book)
    }
    val (ch, analysis) = data ?: return Loading()
    val outline = analysis.chapters[chapter]
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(
            container.bible.refs.formatChapter(book, chapter),
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = FontFamily.Serif,
        )
        outline?.title?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
        if (outline == null) {
            Spacer(Modifier.height(12.dp))
            Paragraph("La spiegazione di questo capitolo non è ancora disponibile.")
        } else {
            if (outline.summary.isNotBlank()) {
                SectionTitle("In sintesi")
                Paragraph(outline.summary)
            }
            if (outline.sections.isNotEmpty()) {
                SectionTitle("Le parti del capitolo")
                val last = ch.verses.lastOrNull()?.number ?: 1
                outline.sections.forEachIndexed { i, s ->
                    val end = outline.sections.getOrNull(i + 1)?.startVerse?.minus(1) ?: last
                    val range = if (s.startVerse == end) "v. ${s.startVerse}" else "vv. ${s.startVerse}-$end"
                    LinkRow(s.title, range) { onOpenVerse(VerseRef(book, chapter, s.startVerse)) }
                }
            }
        }
        analysis.intro?.field("sintesi")?.let {
            SectionTitle("Il capitolo nel libro")
            Paragraph(it)
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = { onOpenVerse(VerseRef(book, chapter, 1)) }) { Text("Leggi il capitolo") }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun BookTab(container: AppContainer, book: Int, name: String, onOpenVerse: (VerseRef) -> Unit) {
    val analysis by produceState<BookAnalysis?>(null, book) { value = container.analysis.book(book) }
    val a = analysis ?: return Loading()
    val intro = a.intro
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(intro?.title?.takeIf { it.isNotBlank() } ?: name, style = MaterialTheme.typography.headlineSmall, fontFamily = FontFamily.Serif)
        if (intro == null) {
            Spacer(Modifier.height(12.dp))
            Paragraph("L'introduzione a questo libro non è ancora disponibile.")
        } else {
            for ((key, label) in AnalysisParser.INTRO_LABELS) {
                val fieldValue = intro.field(key) ?: continue
                SectionTitle(label)
                if (key == "chiave") {
                    val passages = fieldValue.split(';').mapNotNull { container.bible.refs.parse(it.trim()) }
                    for (p in passages) {
                        val text by produceState("", p) { value = container.bible.passageText(p) }
                        LinkRow(container.bible.refs.format(p), text) { onOpenVerse(p.start) }
                    }
                } else {
                    for (paragraph in fieldValue.split('\n')) Paragraph(paragraph, Modifier.padding(bottom = 6.dp))
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun LabeledText(label: String, text: String) {
    Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun LinkRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, maxLines = 3)
            }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
    }
}
