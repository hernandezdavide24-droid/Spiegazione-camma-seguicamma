package it.lampada.bibbia.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.lampada.bibbia.AppContainer
import it.lampada.bibbia.data.calendar.ChristianCalendar
import it.lampada.bibbia.data.calendar.Feast
import it.lampada.bibbia.data.calendar.FeastOccurrence
import it.lampada.bibbia.data.calendar.Importance
import it.lampada.bibbia.data.calendar.Tradition
import it.lampada.bibbia.data.prefs.Settings
import it.lampada.bibbia.ui.components.ColorDot
import it.lampada.bibbia.ui.components.SectionTitle
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val IT = Locale.ITALIAN
private val DATE = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", IT)

fun formatDate(d: LocalDate): String = DATE.format(d)

fun Feast.visibleFor(traditions: Set<Tradition>): Boolean =
    tradition == Tradition.COMUNE || tradition in traditions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    container: AppContainer,
    settings: Settings,
    onOpenFeast: (String, LocalDate) -> Unit,
    onOpenSeason: (String) -> Unit,
) {
    val today = remember { LocalDate.now() }
    var monthText by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    var selectedText by rememberSaveable { mutableStateOf(today.toString()) }
    val month = YearMonth.parse(monthText)
    val selected = LocalDate.parse(selectedText)

    // Tutte le feste che toccano i giorni del mese mostrato (anche periodi iniziati prima).
    val monthFeasts = remember(month, settings.traditions) {
        val first = month.atDay(1)
        val last = month.atEndOfMonth()
        (ChristianCalendar.occurrences(month.year - 1) + ChristianCalendar.occurrences(month.year))
            .filter { it.feast.visibleFor(settings.traditions) }
            .filter { !it.endDate.isBefore(first) && !it.date.isAfter(last) }
            .sortedBy { it.date }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Calendario cristiano") }) }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
        ) {
            // Filtro per tradizione
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Tradition.entries.filter { it != Tradition.COMUNE }.forEach { t ->
                    FilterChip(
                        selected = t in settings.traditions,
                        onClick = {
                            val next = if (t in settings.traditions) settings.traditions - t else settings.traditions + t
                            container.appScope.launch { container.settings.setTraditions(next) }
                        },
                        label = { Text(t.label) },
                    )
                }
            }

            // Intestazione del mese
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                IconButton(onClick = { monthText = month.minusMonths(1).toString() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Mese precedente")
                }
                Text(
                    month.month.getDisplayName(TextStyle.FULL_STANDALONE, IT).replaceFirstChar { it.uppercase() } +
                        " ${month.year}",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { monthText = month.plusMonths(1).toString() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Mese successivo")
                }
            }
            if (month != YearMonth.from(today)) {
                TextButton(
                    onClick = {
                        monthText = YearMonth.from(today).toString()
                        selectedText = today.toString()
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) { Text("Torna a oggi") }
            }

            MonthGrid(
                month = month,
                today = today,
                selected = selected,
                feasts = monthFeasts,
                onSelect = { selectedText = it.toString() },
            )

            // Giorno selezionato
            val dayFeasts = monthFeasts.filter { it.covers(selected) }
            val season = ChristianCalendar.seasonOn(selected)
            SectionTitle(formatDate(selected).replaceFirstChar { it.uppercase() })
            dayFeasts.forEach { occ ->
                FeastRow(occ, showDate = false) { onOpenFeast(occ.feast.id, occ.date) }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpenSeason(season.season.id) }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ColorDot(Color(season.season.color.argb), 14)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(season.season.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Periodo dell'anno · colore liturgico ${season.season.color.label.lowercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
            }

            SectionTitle("Feste di questo mese")
            if (monthFeasts.isEmpty()) {
                Text(
                    "Nessuna festa per le tradizioni scelte.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            monthFeasts.forEach { occ -> FeastRow(occ, showDate = true) { onOpenFeast(occ.feast.id, occ.date) } }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    today: LocalDate,
    selected: LocalDate,
    feasts: List<FeastOccurrence>,
    onSelect: (LocalDate) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val leading = month.atDay(1).dayOfWeek.value - 1 // lunedì = 0
    val cells = leading + month.lengthOfMonth()
    val rows = (cells + 6) / 7
    Column {
        Row {
            DayOfWeek.entries.forEach { d ->
                Text(
                    d.getDisplayName(TextStyle.SHORT, IT).take(3),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        for (r in 0 until rows) {
            Row {
                for (c in 0 until 7) {
                    val dayNum = r * 7 + c - leading + 1
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                        if (dayNum in 1..month.lengthOfMonth()) {
                            val date = month.atDay(dayNum)
                            // Le feste di un solo giorno hanno il pallino; i periodi lunghi no, per non riempire la griglia.
                            val dayFeasts = feasts.filter { it.feast.durationDays == 1 && it.date == date }
                            val main = dayFeasts.firstOrNull { it.feast.importance == Importance.PRINCIPALE }
                            val isSelected = date == selected
                            Column(
                                Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(if (isSelected) colors.primaryContainer else Color.Transparent)
                                    .then(if (date == today) Modifier.border(1.5.dp, colors.primary, CircleShape) else Modifier)
                                    .clickable { onSelect(date) },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    "$dayNum",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (main != null || date.dayOfWeek == DayOfWeek.SUNDAY) FontWeight.Bold else FontWeight.Normal,
                                    color = if (date.dayOfWeek == DayOfWeek.SUNDAY) colors.primary else colors.onSurface,
                                )
                                if (dayFeasts.isNotEmpty()) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        dayFeasts.take(3).forEach { occ ->
                                            Box(
                                                Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(occ.feast.color.argb))
                                                    .border(0.5.dp, colors.outline, CircleShape),
                                            )
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
}

@Composable
private fun FeastRow(occ: FeastOccurrence, showDate: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ColorDot(Color(occ.feast.color.argb), 14)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                occ.feast.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (occ.feast.importance == Importance.PRINCIPALE) FontWeight.Bold else FontWeight.Medium,
            )
            val dateText = when {
                !showDate -> null
                occ.feast.durationDays > 1 -> "dal ${DateTimeFormatter.ofPattern("d MMMM", IT).format(occ.date)} " +
                    "al ${DateTimeFormatter.ofPattern("d MMMM", IT).format(occ.endDate)}"
                else -> DateTimeFormatter.ofPattern("EEEE d", IT).format(occ.date)
            }
            Text(
                listOfNotNull(dateText, occ.feast.tradition.label).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
    }
}
