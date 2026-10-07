package it.lampada.bibbia.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.lampada.bibbia.AppContainer
import it.lampada.bibbia.data.bible.VerseRef
import it.lampada.bibbia.data.calendar.ChristianCalendar
import it.lampada.bibbia.data.daily.DailyVerse
import it.lampada.bibbia.data.db.BookmarkEntity
import it.lampada.bibbia.data.prefs.Settings
import it.lampada.bibbia.ui.calendar.formatDate
import it.lampada.bibbia.ui.calendar.visibleFor
import it.lampada.bibbia.ui.components.ColorDot
import it.lampada.bibbia.ui.components.SectionTitle
import it.lampada.bibbia.ui.components.shareText
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    container: AppContainer,
    settings: Settings,
    onOpenVerse: (VerseRef) -> Unit,
    onContinueReading: () -> Unit,
    onOpenAnalysis: (Int, Int, Int) -> Unit,
    onOpenFeast: (String, LocalDate) -> Unit,
    onOpenSeason: (String) -> Unit,
    onOpenCalendar: () -> Unit,
) {
    val context = LocalContext.current
    val today = remember { LocalDate.now() }
    val verse by produceState<DailyVerse?>(null) { value = container.dailyVerses.forDate(today) }
    val last by container.settings.lastPosition.collectAsStateWithLifecycle(null)

    // Al primo avvio chiede il permesso per la notifica del versetto del giorno (Android 13+).
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(settings.notificationsAsked) {
        if (!settings.notificationsAsked && Build.VERSION.SDK_INT >= 33) {
            container.settings.setNotificationsAsked()
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val greeting = when (LocalTime.now().hour) {
        in 5..12 -> "Buongiorno"
        in 13..17 -> "Buon pomeriggio"
        else -> "Buonasera"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(greeting)
                        Text(
                            formatDate(today),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            // Versetto del giorno
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Versetto del giorno", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(10.dp))
                    val v = verse
                    if (v == null) {
                        Text("…")
                    } else {
                        Text(v.text, style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Serif)
                        Spacer(Modifier.height(8.dp))
                        Text(v.reference, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { onOpenVerse(v.passage.start) }) {
                                Icon(Icons.AutoMirrored.Outlined.MenuBook, null)
                                Spacer(Modifier.width(6.dp))
                                Text("Leggi nel contesto")
                            }
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = {
                                val s = v.passage.start
                                onOpenAnalysis(s.book, s.chapter, s.verse)
                            }) { Icon(Icons.Outlined.Lightbulb, "Analisi") }
                            IconButton(onClick = {
                                val s = v.passage.start
                                val now = System.currentTimeMillis()
                                val refs = (s.verse..v.passage.end.verse).map { BookmarkEntity(s.book, s.chapter, it, now) }
                                container.appScope.launch { container.study.insertBookmarks(refs) }
                                android.widget.Toast.makeText(context, "Salvato nei preferiti", android.widget.Toast.LENGTH_SHORT).show()
                            }) { Icon(Icons.Outlined.BookmarkBorder, "Salva") }
                            IconButton(onClick = {
                                shareText(context, "«${v.text}»\n— ${v.reference} (Riveduta 1927)")
                            }) { Icon(Icons.Outlined.Share, "Condividi") }
                        }
                    }
                }
            }

            // Riprendi la lettura
            last?.let { ref ->
                Spacer(Modifier.height(12.dp))
                Card(
                    onClick = onContinueReading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Outlined.MenuBook, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Riprendi la lettura", style = MaterialTheme.typography.labelLarge)
                            Text(container.bible.refs.format(ref), style = MaterialTheme.typography.titleMedium)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                    }
                }
            }

            // Tempo liturgico e feste
            val season = remember(today) { ChristianCalendar.seasonOn(today) }
            val todayFeasts = remember(today, settings.traditions) {
                ChristianCalendar.feastsOn(today).filter { it.feast.visibleFor(settings.traditions) }
            }
            val upcoming = remember(today, settings.traditions) {
                ChristianCalendar.upcoming(today.plusDays(1), 30)
                    .filter { it.feast.visibleFor(settings.traditions) }
                    .take(3)
            }

            SectionTitle("Oggi nel calendario cristiano")
            todayFeasts.forEach { occ ->
                CalendarRow(
                    color = Color(occ.feast.color.argb),
                    title = occ.feast.name,
                    subtitle = occ.feast.summary,
                    onClick = { onOpenFeast(occ.feast.id, occ.date) },
                )
            }
            CalendarRow(
                color = Color(season.season.color.argb),
                title = season.season.name,
                subtitle = "Giorno ${season.dayNumber(today)} di ${season.length} · tocca per sapere come viverlo",
                onClick = { onOpenSeason(season.season.id) },
            )

            if (upcoming.isNotEmpty()) {
                SectionTitle("Prossime feste")
                upcoming.forEach { occ ->
                    CalendarRow(
                        color = Color(occ.feast.color.argb),
                        title = occ.feast.name,
                        subtitle = formatDate(occ.date),
                        onClick = { onOpenFeast(occ.feast.id, occ.date) },
                    )
                }
                TextButton(onClick = onOpenCalendar) { Text("Apri il calendario") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CalendarRow(color: Color, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        ColorDot(color, 14)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
    }
}
