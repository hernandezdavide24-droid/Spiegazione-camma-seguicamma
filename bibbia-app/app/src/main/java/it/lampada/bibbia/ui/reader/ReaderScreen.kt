package it.lampada.bibbia.ui.reader

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FormatColorReset
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.lampada.bibbia.AppContainer
import it.lampada.bibbia.data.analysis.ChapterOutline
import it.lampada.bibbia.data.bible.Chapter
import it.lampada.bibbia.data.bible.Verse
import it.lampada.bibbia.data.bible.VerseRef
import it.lampada.bibbia.data.db.BookmarkEntity
import it.lampada.bibbia.data.db.HighlightColor
import it.lampada.bibbia.data.db.HighlightEntity
import it.lampada.bibbia.data.db.NoteEntity
import it.lampada.bibbia.data.prefs.Settings
import it.lampada.bibbia.ui.components.Loading
import it.lampada.bibbia.ui.components.copyText
import it.lampada.bibbia.ui.components.shareText
import it.lampada.bibbia.ui.theme.highlightColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Richiesta di portare in vista un versetto (all'apertura o da un'altra schermata). */
private data class ScrollRequest(val ref: VerseRef, val flash: Boolean, val id: Long = System.nanoTime())

@Composable
fun ReaderScreen(
    container: AppContainer,
    settings: Settings,
    darkTheme: Boolean,
    onOpenBooks: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenAnalysis: (book: Int, chapter: Int, verse: Int) -> Unit,
) {
    // Si riparte dalla richiesta in sospeso (es. notifica) oppure dall'ultimo punto di lettura salvato.
    val start by produceState<ScrollRequest?>(null) {
        val pending = container.navigator.pending.value
        value = if (pending != null) {
            ScrollRequest(pending, flash = true)
        } else {
            ScrollRequest(container.settings.lastPosition.first(), flash = false)
        }
    }
    val s = start
    if (s == null) {
        Loading()
        return
    }
    ReaderContent(container, settings, darkTheme, s, onOpenBooks, onOpenSearch, onOpenAnalysis)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderContent(
    container: AppContainer,
    settings: Settings,
    darkTheme: Boolean,
    start: ScrollRequest,
    onOpenBooks: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenAnalysis: (book: Int, chapter: Int, verse: Int) -> Unit,
) {
    val bible = container.bible
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val pagerState = rememberPagerState(
        initialPage = bible.globalChapterIndex(start.ref.book, start.ref.chapter),
    ) { bible.chapterIndex.size }
    var scrollRequest by remember { mutableStateOf<ScrollRequest?>(start) }
    var selection by remember { mutableStateOf(emptySet<Int>()) }
    var showTextSettings by remember { mutableStateOf(false) }
    var noteTarget by remember { mutableStateOf<VerseRef?>(null) }

    val (curBook, curChapter) = bible.chapterIndex[pagerState.currentPage]
    val book = bible.book(curBook)

    LaunchedEffect(pagerState.currentPage) { selection = emptySet() }

    val pending by container.navigator.pending.collectAsStateWithLifecycle()
    LaunchedEffect(pending) {
        val ref = pending ?: return@LaunchedEffect
        container.navigator.consumed()
        scrollRequest = ScrollRequest(ref, flash = true)
        pagerState.scrollToPage(bible.globalChapterIndex(ref.book, ref.chapter))
    }

    val bookmarks by remember(curBook, curChapter) {
        container.study.bookmarksIn(curBook, curChapter)
    }.collectAsStateWithLifecycle(emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onOpenBooks)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${book.name} $curChapter")
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Scegli libro e capitolo")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSearch) { Icon(Icons.Outlined.Search, "Cerca") }
                    IconButton(onClick = { onOpenAnalysis(curBook, curChapter, 0) }) {
                        Icon(Icons.Outlined.Lightbulb, "Analisi del capitolo")
                    }
                    IconButton(onClick = { showTextSettings = true }) {
                        Icon(Icons.Outlined.FormatSize, "Dimensione del testo")
                    }
                },
            )
        },
        bottomBar = {
            if (selection.isNotEmpty()) {
                SelectionBar(
                    reference = bible.refs.formatSelection(curBook, curChapter, selection),
                    allBookmarked = selection.all { v -> bookmarks.any { it.verse == v } },
                    onClose = { selection = emptySet() },
                    onColor = { color ->
                        val now = System.currentTimeMillis()
                        val items = selection.map { HighlightEntity(curBook, curChapter, it, color, now) }
                        scope.launch { container.study.upsertHighlights(items) }
                        selection = emptySet()
                    },
                    onClearColor = {
                        val verses = selection.toList()
                        scope.launch { container.study.deleteHighlights(curBook, curChapter, verses) }
                        selection = emptySet()
                    },
                    onBookmark = { remove ->
                        val verses = selection.toList()
                        scope.launch {
                            if (remove) {
                                container.study.deleteBookmarks(curBook, curChapter, verses)
                            } else {
                                val now = System.currentTimeMillis()
                                container.study.insertBookmarks(verses.map { BookmarkEntity(curBook, curChapter, it, now) })
                            }
                        }
                        selection = emptySet()
                    },
                    onNote = { noteTarget = VerseRef(curBook, curChapter, selection.min()) },
                    onCopy = { share ->
                        val verses = selection
                        scope.launch {
                            val ch = bible.chapter(curBook, curChapter)
                            val text = quote(ch, verses, bible.refs.formatSelection(curBook, curChapter, verses))
                            if (share) shareText(context, text) else copyText(context, text)
                        }
                        selection = emptySet()
                    },
                    onAnalysis = {
                        onOpenAnalysis(curBook, curChapter, selection.min())
                        selection = emptySet()
                    },
                )
            }
        },
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            key = { it },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) { page ->
            val (b, c) = bible.chapterIndex[page]
            ChapterPage(
                container = container,
                settings = settings,
                darkTheme = darkTheme,
                book = b,
                chapter = c,
                isCurrent = page == pagerState.currentPage,
                scrollRequest = scrollRequest?.takeIf { it.ref.book == b && it.ref.chapter == c },
                onScrollHandled = { id -> if (scrollRequest?.id == id) scrollRequest = null },
                selection = if (page == pagerState.currentPage) selection else emptySet(),
                onToggleVerse = { v -> selection = if (v in selection) selection - v else selection + v },
                onOpenChapterAnalysis = { onOpenAnalysis(b, c, 0) },
                hasPrevious = page > 0,
                hasNext = page < bible.chapterIndex.lastIndex,
                onPrevious = { scope.launch { pagerState.animateScrollToPage(page - 1) } },
                onNext = { scope.launch { pagerState.animateScrollToPage(page + 1) } },
            )
        }
    }

    if (showTextSettings) {
        TextSettingsSheet(container, settings, onDismiss = { showTextSettings = false })
    }

    noteTarget?.let { ref ->
        NoteDialog(
            container = container,
            ref = ref,
            onDismiss = {
                noteTarget = null
                selection = emptySet()
            },
        )
    }
}

/** Testo da copiare o condividere: «...» — Giovanni 3:16 (Riveduta 1927). */
private fun quote(chapter: Chapter, verses: Set<Int>, reference: String): String {
    val selected = chapter.verses.filter { it.number in verses }
    val body = if (selected.size == 1) selected.first().text
    else selected.joinToString(" ") { "${it.number} ${it.text}" }
    return "«$body»\n— $reference (Riveduta 1927)"
}

@Composable
private fun ChapterPage(
    container: AppContainer,
    settings: Settings,
    darkTheme: Boolean,
    book: Int,
    chapter: Int,
    isCurrent: Boolean,
    scrollRequest: ScrollRequest?,
    onScrollHandled: (Long) -> Unit,
    selection: Set<Int>,
    onToggleVerse: (Int) -> Unit,
    onOpenChapterAnalysis: () -> Unit,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val bible = container.bible
    val content by produceState<Chapter?>(null, book, chapter) { value = bible.chapter(book, chapter) }
    val outline by produceState<ChapterOutline?>(null, book, chapter) {
        value = container.analysis.chapter(book, chapter)
    }
    val highlights by remember(book, chapter) {
        container.study.highlightsIn(book, chapter)
    }.collectAsStateWithLifecycle(emptyList<HighlightEntity>())
    val bookmarks by remember(book, chapter) {
        container.study.bookmarksIn(book, chapter)
    }.collectAsStateWithLifecycle(emptyList<BookmarkEntity>())
    val notes by remember(book, chapter) {
        container.study.notesIn(book, chapter)
    }.collectAsStateWithLifecycle(emptyList<NoteEntity>())

    val ch = content
    if (ch == null) {
        Loading()
        return
    }

    val listState = rememberLazyListState()
    var flashVerse by remember { mutableStateOf<Int?>(null) }
    val headerCount = if (ch.heading != null) 2 else 1

    // Porta in vista il versetto richiesto.
    LaunchedEffect(scrollRequest?.id) {
        val req = scrollRequest ?: return@LaunchedEffect
        // Dal primo versetto si parte dall'intestazione del capitolo; con l'evidenziazione si lascia
        // visibile anche il versetto precedente, per il contesto.
        val index = when {
            req.ref.verse <= 1 -> 0
            req.flash -> headerCount + req.ref.verse - 2
            else -> headerCount + req.ref.verse - 1
        }
        listState.scrollToItem(index)
        onScrollHandled(req.id)
        if (req.flash) {
            flashVerse = req.ref.verse
            delay(1800)
            flashVerse = null
        }
    }

    // Salva l'ultimo punto di lettura mentre si scorre (e quando l'app va in secondo piano).
    val currentRef by rememberUpdatedState(
        VerseRef(book, chapter, (listState.firstVisibleItemIndex - headerCount + 1).coerceIn(1, ch.verses.size)),
    )
    if (isCurrent) {
        LaunchedEffect(listState) {
            snapshotFlow { listState.firstVisibleItemIndex }.collectLatest {
                delay(400)
                container.settings.saveLastPosition(currentRef)
            }
        }
        val scope = rememberCoroutineScope()
        LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
            scope.launch { container.settings.saveLastPosition(currentRef) }
        }
    }

    val sections = remember(outline) { outline?.sections?.associateBy { it.startVerse }.orEmpty() }
    val highlightByVerse = remember(highlights) { highlights.associate { it.verse to it.color } }
    val bookmarked = remember(bookmarks) { bookmarks.map { it.verse }.toSet() }
    val withNotes = remember(notes) { notes.map { it.verse }.toSet() }
    val fontSize = 18f * settings.fontScale

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 32.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "header") {
            ChapterHeader(
                bookName = ch.book.name,
                chapter = chapter,
                title = outline?.title,
                onAnalysis = onOpenChapterAnalysis,
            )
        }
        ch.heading?.let { heading ->
            item(key = "heading") {
                Text(
                    heading,
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp, bottom = 8.dp),
                )
            }
        }
        itemsIndexed(ch.verses, key = { _, v -> v.number }) { _, verse ->
            Column {
                sections[verse.number]?.let { section ->
                    Text(
                        section.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 6.dp, top = if (verse.number == 1) 4.dp else 18.dp, bottom = 6.dp),
                    )
                }
                VerseItem(
                    verse = verse,
                    highlight = highlightByVerse[verse.number],
                    bookmarked = verse.number in bookmarked,
                    hasNote = verse.number in withNotes,
                    selected = verse.number in selection,
                    flashing = flashVerse == verse.number,
                    fontSize = fontSize,
                    serif = settings.serifFont,
                    darkTheme = darkTheme,
                    onClick = { onToggleVerse(verse.number) },
                )
            }
        }
        item(key = "footer") {
            ChapterFooter(hasPrevious, hasNext, onPrevious, onNext)
        }
    }
}

@Composable
private fun ChapterHeader(bookName: String, chapter: Int, title: String?, onAnalysis: () -> Unit) {
    Column(Modifier.padding(start = 6.dp, top = 8.dp, bottom = 12.dp)) {
        Text(
            bookName.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.5.sp,
        )
        Text(
            "Capitolo $chapter",
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = FontFamily.Serif,
        )
        if (!title.isNullOrBlank()) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        AssistChip(
            onClick = onAnalysis,
            label = { Text("Analisi del capitolo") },
            leadingIcon = { Icon(Icons.Outlined.Lightbulb, null, Modifier.size(AssistChipDefaults.IconSize)) },
        )
    }
}

@Composable
private fun VerseItem(
    verse: Verse,
    highlight: HighlightColor?,
    bookmarked: Boolean,
    hasNote: Boolean,
    selected: Boolean,
    flashing: Boolean,
    fontSize: Float,
    serif: Boolean,
    darkTheme: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val base = highlight?.let { highlightColor(it.argb, darkTheme) } ?: Color.Transparent
    val bg by animateColorAsState(if (flashing) colors.tertiaryContainer else base, label = "evidenzia")
    val text = remember(verse, bookmarked, hasNote, colors.primary, fontSize) {
        buildAnnotatedString {
            withStyle(
                SpanStyle(
                    fontSize = (fontSize * 0.62f).sp,
                    color = colors.primary,
                    fontWeight = FontWeight.Bold,
                    baselineShift = BaselineShift.Superscript,
                ),
            ) { append(verse.number.toString()) }
            append(" ")
            append(verse.text)
            if (bookmarked) {
                append(' ')
                appendInlineContent("segnalibro", "[salvato]")
            }
            if (hasNote) {
                append(' ')
                appendInlineContent("nota", "[nota]")
            }
        }
    }
    val iconSize = (fontSize * 0.9f).sp
    val inline = mapOf(
        "segnalibro" to InlineTextContent(Placeholder(iconSize, iconSize, PlaceholderVerticalAlign.TextCenter)) {
            Icon(Icons.Filled.Bookmark, contentDescription = "Salvato", tint = colors.primary)
        },
        "nota" to InlineTextContent(Placeholder(iconSize, iconSize, PlaceholderVerticalAlign.TextCenter)) {
            Icon(Icons.Filled.EditNote, contentDescription = "Nota", tint = colors.primary)
        },
    )
    Text(
        text = text,
        inlineContent = inline,
        style = TextStyle(
            fontSize = fontSize.sp,
            lineHeight = (fontSize * 1.6f).sp,
            fontFamily = if (serif) FontFamily.Serif else FontFamily.Default,
            color = colors.onSurface,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .then(
                if (selected) Modifier.border(1.5.dp, colors.primary, RoundedCornerShape(6.dp)) else Modifier,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 3.dp),
    )
}

@Composable
private fun ChapterFooter(hasPrevious: Boolean, hasNext: Boolean, onPrevious: () -> Unit, onNext: () -> Unit) {
    Column(Modifier.padding(top = 24.dp)) {
        HorizontalDivider()
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = onPrevious, enabled = hasPrevious) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null)
                Text("Precedente")
            }
            TextButton(onClick = onNext, enabled = hasNext) {
                Text("Successivo")
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
            }
        }
        Text(
            "Testo: Riveduta 1927 (G. Luzzi), di pubblico dominio. Scorri a destra o a sinistra per cambiare capitolo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
    }
}

@Composable
private fun SelectionBar(
    reference: String,
    allBookmarked: Boolean,
    onClose: () -> Unit,
    onColor: (HighlightColor) -> Unit,
    onClearColor: () -> Unit,
    onBookmark: (remove: Boolean) -> Unit,
    onNote: () -> Unit,
    onCopy: (share: Boolean) -> Unit,
    onAnalysis: () -> Unit,
) {
    Surface(tonalElevation = 3.dp, shadowElevation = 12.dp, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(reference, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f).padding(start = 4.dp))
                IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Annulla selezione") }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HighlightColor.entries.forEach { c ->
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(c.argb))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            .clickable { onColor(c) },
                    )
                }
                IconButton(onClick = onClearColor) {
                    Icon(Icons.Outlined.FormatColorReset, "Togli evidenziazione")
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                BarAction(
                    if (allBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    if (allBookmarked) "Rimuovi" else "Salva",
                ) { onBookmark(allBookmarked) }
                BarAction(Icons.Filled.EditNote, "Nota", onNote)
                BarAction(Icons.Outlined.ContentCopy, "Copia") { onCopy(false) }
                BarAction(Icons.Outlined.Share, "Condividi") { onCopy(true) }
                BarAction(Icons.Outlined.Lightbulb, "Analisi", onAnalysis)
            }
        }
    }
}

@Composable
private fun BarAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextSettingsSheet(container: AppContainer, settings: Settings, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var scale by remember { mutableFloatStateOf(settings.fontScale) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text("Testo", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Text(
                "Nel principio Iddio creò i cieli e la terra.",
                fontSize = (18f * scale).sp,
                fontFamily = if (settings.serifFont) FontFamily.Serif else FontFamily.Default,
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("A", fontSize = 14.sp)
                Slider(
                    value = scale,
                    onValueChange = { scale = it },
                    onValueChangeFinished = { scope.launch { container.settings.setFontScale(scale) } },
                    valueRange = 0.8f..1.8f,
                    steps = 9,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                )
                Text("A", fontSize = 24.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Carattere con grazie (stile libro)", modifier = Modifier.weight(1f))
                Switch(
                    checked = settings.serifFont,
                    onCheckedChange = { scope.launch { container.settings.setSerif(it) } },
                )
            }
            Spacer(Modifier.width(1.dp))
        }
    }
}

@Composable
private fun NoteDialog(container: AppContainer, ref: VerseRef, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val existing by produceState<NoteEntity?>(null, ref) {
        value = container.study.notesIn(ref.book, ref.chapter).first().firstOrNull { it.verse == ref.verse }
    }
    var text by remember(existing) { mutableStateOf(existing?.text.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nota su ${container.bible.refs.format(ref)}") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Scrivi un pensiero, una domanda, una preghiera…") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    if (text.isBlank()) {
                        existing?.let { container.study.deleteNote(it) }
                    } else {
                        container.study.upsertNote(
                            NoteEntity(ref.book, ref.chapter, ref.verse, text.trim(), System.currentTimeMillis()),
                        )
                    }
                }
                onDismiss()
            }) { Text("Salva") }
        },
        dismissButton = {
            if (existing != null) {
                TextButton(onClick = {
                    scope.launch { existing?.let { container.study.deleteNote(it) } }
                    onDismiss()
                }) { Text("Elimina") }
            } else {
                TextButton(onClick = onDismiss) { Text("Annulla") }
            }
        },
    )
}
