package it.lampada.bibbia.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import it.lampada.bibbia.AppContainer
import it.lampada.bibbia.data.bible.SearchHit
import it.lampada.bibbia.data.bible.Testament
import it.lampada.bibbia.data.bible.VerseRef
import it.lampada.bibbia.ui.components.BackTopBar

private const val LIMIT = 500

@Composable
fun SearchScreen(container: AppContainer, onOpenVerse: (VerseRef) -> Unit, onBack: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf("") }
    var testament by rememberSaveable { mutableStateOf<Testament?>(null) }
    val results = remember { mutableStateListOf<SearchHit>() }
    var searching by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) { if (submitted.isEmpty()) focus.requestFocus() }

    LaunchedEffect(submitted, testament) {
        results.clear()
        if (submitted.length < 2) return@LaunchedEffect
        searching = true
        // Si salta direttamente al versetto se si scrive un riferimento come "Gv 3:16".
        container.bible.refs.parse(submitted)?.let { p ->
            container.bible.verse(p.start)?.let { results += SearchHit(it, emptyList()) }
        }
        container.bible.search(submitted, testament, LIMIT).collect { results += it }
        searching = false
    }

    Scaffold(topBar = { BackTopBar("Cerca nella Bibbia", onBack) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Parola, frase o riferimento (es. Gv 3:16)") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    submitted = query.trim()
                    focusManager.clearFocus()
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .focusRequester(focus),
            )
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = testament == null, onClick = { testament = null }, label = { Text("Tutta") })
                FilterChip(selected = testament == Testament.AT, onClick = { testament = Testament.AT }, label = { Text("Antico T.") })
                FilterChip(selected = testament == Testament.NT, onClick = { testament = Testament.NT }, label = { Text("Nuovo T.") })
            }
            if (searching) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
            if (submitted.length >= 2 && !searching) {
                Text(
                    when {
                        results.isEmpty() -> "Nessun risultato per «$submitted»."
                        results.size >= LIMIT -> "Più di $LIMIT risultati: prova una frase più precisa."
                        else -> "${results.size} risultati"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            LazyColumn(Modifier.fillMaxSize()) {
                items(results, key = { "${it.verse.book}.${it.verse.chapter}.${it.verse.number}.${it.ranges.size}" }) { hit ->
                    SearchRow(container, hit) { onOpenVerse(hit.verse.ref) }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun SearchRow(container: AppContainer, hit: SearchHit, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val text = remember(hit) {
        buildAnnotatedString {
            var last = 0
            for (r in hit.ranges) {
                append(hit.verse.text.substring(last, r.first))
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = primary)) {
                    append(hit.verse.text.substring(r.first, r.last + 1))
                }
                last = r.last + 1
            }
            append(hit.verse.text.substring(last))
        }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            container.bible.refs.format(hit.verse.ref),
            style = MaterialTheme.typography.labelLarge,
            color = primary,
        )
        Text(text, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Serif)
    }
}
