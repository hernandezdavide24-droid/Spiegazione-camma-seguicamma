package it.lampada.bibbia.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import it.lampada.bibbia.AppContainer
import it.lampada.bibbia.data.bible.VerseRef
import it.lampada.bibbia.data.calendar.ChristianCalendar
import it.lampada.bibbia.data.calendar.FeastCatalog
import it.lampada.bibbia.data.calendar.LiturgicalColor
import it.lampada.bibbia.ui.components.BackTopBar
import it.lampada.bibbia.ui.components.BulletList
import it.lampada.bibbia.ui.components.ColorDot
import it.lampada.bibbia.ui.components.Paragraph
import it.lampada.bibbia.ui.components.PassageRow
import it.lampada.bibbia.ui.components.SectionTitle
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeastScreen(
    container: AppContainer,
    feastId: String,
    date: LocalDate,
    onOpenVerse: (VerseRef) -> Unit,
    onBack: () -> Unit,
) {
    val feast = FeastCatalog.feasts.firstOrNull { it.id == feastId }
    Scaffold(topBar = { BackTopBar(feast?.name ?: "Festa", onBack) }) { padding ->
        if (feast == null) {
            Text("Festa non trovata.", Modifier.padding(padding).padding(20.dp))
            return@Scaffold
        }
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            val dateText = if (feast.durationDays > 1) {
                "Dal ${formatDate(date)} al ${formatDate(date.plusDays(feast.durationDays - 1L))}"
            } else {
                formatDate(date).replaceFirstChar { it.uppercase() }
            }
            Text(dateText, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(feast.tradition.label) })
                AssistChip(onClick = {}, label = { Text(feast.importance.label) })
                ColorChip(feast.color)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                feast.summary,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
            )

            SectionTitle("Che cosa si ricorda")
            Paragraph(feast.meaning)
            feast.note?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            SectionTitle("Come viverla")
            BulletList(feast.whatToDo)

            SectionTitle("Da leggere nella Bibbia")
            feast.readings.forEach { r -> PassageRow(container, r) { onOpenVerse(it.start) } }

            SectionTitle("Nei prossimi anni")
            val nextYears = (date.year + 1..date.year + 3).map { y -> ChristianCalendar.dateOf(feast.rule, y) }
            nextYears.forEach { d -> Text("· ${formatDate(d)}", style = MaterialTheme.typography.bodyMedium) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun SeasonScreen(
    container: AppContainer,
    seasonId: String,
    onOpenVerse: (VerseRef) -> Unit,
    onBack: () -> Unit,
) {
    val season = FeastCatalog.seasons.firstOrNull { it.id == seasonId }
    Scaffold(topBar = { BackTopBar(season?.name ?: "Periodo", onBack) }) { padding ->
        if (season == null) {
            Text("Periodo non trovato.", Modifier.padding(padding).padding(20.dp))
            return@Scaffold
        }
        val today = LocalDate.now()
        val span = (ChristianCalendar.seasons(today.year) + ChristianCalendar.seasons(today.year + 1))
            .firstOrNull { it.season.id == seasonId && !it.end.isBefore(today) }
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            span?.let {
                Text(
                    if (it.covers(today)) "In corso: dal ${formatDate(it.start)} al ${formatDate(it.end)}"
                    else "Prossimamente: dal ${formatDate(it.start)} al ${formatDate(it.end)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
            }
            ColorChip(season.color)
            SectionTitle("Che cos'è")
            Paragraph(season.description)
            SectionTitle("Come viverlo")
            BulletList(season.whatToDo)
            SectionTitle("Da leggere nella Bibbia")
            season.readings.forEach { r -> PassageRow(container, r) { onOpenVerse(it.start) } }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ColorChip(color: LiturgicalColor) {
    AssistChip(
        onClick = {},
        label = { Text("Colore: ${color.label.lowercase()}") },
        leadingIcon = { ColorDot(Color(color.argb), 12) },
    )
}
