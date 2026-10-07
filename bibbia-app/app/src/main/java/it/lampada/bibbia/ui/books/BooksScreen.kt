package it.lampada.bibbia.ui.books

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import it.lampada.bibbia.AppContainer
import it.lampada.bibbia.data.bible.Testament
import it.lampada.bibbia.ui.components.BackTopBar

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun BooksScreen(container: AppContainer, onPick: (book: Int, chapter: Int) -> Unit, onBack: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var expanded by rememberSaveable { mutableStateOf<Int?>(null) }
    val testament = if (tab == 0) Testament.AT else Testament.NT
    val books = container.bible.books.filter { it.testament == testament }

    Scaffold(topBar = { BackTopBar("Libri della Bibbia", onBack) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Antico Testamento") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Nuovo Testamento") })
            }
            LazyColumn(Modifier.fillMaxSize()) {
                items(books, key = { it.id }) { book ->
                    Column(Modifier.animateItem()) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (book.chapters == 1) onPick(book.id, 1)
                                    else expanded = if (expanded == book.id) null else book.id
                                }
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(book.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text(
                                if (book.chapters == 1) "1 capitolo" else "${book.chapters} capitoli",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (book.chapters > 1) {
                                Icon(
                                    if (expanded == book.id) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }
                        if (expanded == book.id) {
                            FlowRow(
                                Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                for (c in 1..book.chapters) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { onPick(book.id, c) },
                                    ) {
                                        Box(contentAlignment = Alignment.Center) { Text("$c") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
