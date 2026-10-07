package it.lampada.bibbia.ui.saved

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.lampada.bibbia.AppContainer
import it.lampada.bibbia.data.bible.VerseRef
import it.lampada.bibbia.data.db.HighlightColor
import it.lampada.bibbia.data.db.ref
import it.lampada.bibbia.ui.components.ColorDot
import it.lampada.bibbia.ui.theme.highlightColor
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val NOTE_DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ITALIAN)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScreen(container: AppContainer, darkTheme: Boolean, onOpenVerse: (VerseRef) -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var colorFilter by rememberSaveable { mutableStateOf<HighlightColor?>(null) }
    val bookmarks by remember { container.study.allBookmarks() }.collectAsStateWithLifecycle(emptyList())
    val highlights by remember { container.study.allHighlights() }.collectAsStateWithLifecycle(emptyList())
    val notes by remember { container.study.allNotes() }.collectAsStateWithLifecycle(emptyList())

    Scaffold(topBar = { TopAppBar(title = { Text("I miei versetti") }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Preferiti (${bookmarks.size})") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Evidenziati (${highlights.size})") })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Note (${notes.size})") })
            }
            when (tab) {
                0 -> if (bookmarks.isEmpty()) {
                    Empty("Tocca un versetto mentre leggi e scegli «Salva» per ritrovarlo qui.")
                } else {
                    LazyColumn {
                        items(bookmarks, key = { "${it.book}.${it.chapter}.${it.verse}" }) { b ->
                            VerseRow(container, b.ref, color = null, onOpen = { onOpenVerse(b.ref) }) {
                                container.appScope.launch { container.study.deleteBookmarks(b.book, b.chapter, listOf(b.verse)) }
                            }
                        }
                    }
                }
                1 -> Column {
                    Row(
                        Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(selected = colorFilter == null, onClick = { colorFilter = null }, label = { Text("Tutti") })
                        HighlightColor.entries.forEach { c ->
                            FilterChip(
                                selected = colorFilter == c,
                                onClick = { colorFilter = if (colorFilter == c) null else c },
                                label = { Text(c.label) },
                                leadingIcon = { ColorDot(Color(c.argb), 12) },
                            )
                        }
                    }
                    val shown = highlights.filter { colorFilter == null || it.color == colorFilter }
                    if (shown.isEmpty()) {
                        Empty("Tocca un versetto mentre leggi e scegli un colore per evidenziarlo.")
                    } else {
                        LazyColumn {
                            items(shown, key = { "${it.book}.${it.chapter}.${it.verse}" }) { h ->
                                VerseRow(
                                    container, h.ref,
                                    color = highlightColor(h.color.argb, darkTheme),
                                    onOpen = { onOpenVerse(h.ref) },
                                ) {
                                    container.appScope.launch { container.study.deleteHighlights(h.book, h.chapter, listOf(h.verse)) }
                                }
                            }
                        }
                    }
                }
                else -> if (notes.isEmpty()) {
                    Empty("Tocca un versetto mentre leggi e scegli «Nota» per scrivere un pensiero o una preghiera.")
                } else {
                    LazyColumn {
                        items(notes, key = { "${it.book}.${it.chapter}.${it.verse}" }) { n ->
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenVerse(n.ref) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        container.bible.refs.format(n.ref),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        NOTE_DATE.format(Instant.ofEpochMilli(n.updatedAt).atZone(ZoneId.systemDefault())),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    IconButton(onClick = { container.appScope.launch { container.study.deleteNote(n) } }) {
                                        Icon(Icons.Outlined.Delete, "Elimina nota")
                                    }
                                }
                                Text(n.text, style = MaterialTheme.typography.bodyLarge)
                                Spacer(Modifier.height(4.dp))
                                VerseText(container, n.ref)
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VerseRow(
    container: AppContainer,
    ref: VerseRef,
    color: Color?,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (color != null) {
            Box(
                Modifier
                    .width(6.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color),
            )
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                container.bible.refs.format(ref),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            VerseText(container, ref)
        }
        IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, "Rimuovi") }
    }
    HorizontalDivider()
}

@Composable
private fun VerseText(container: AppContainer, ref: VerseRef) {
    val text by produceState("", ref) { value = container.bible.verse(ref)?.text.orEmpty() }
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        fontFamily = FontFamily.Serif,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 4,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun Empty(message: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            message,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
