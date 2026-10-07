package it.lampada.bibbia.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import it.lampada.bibbia.AppContainer
import it.lampada.bibbia.data.prefs.Settings
import it.lampada.bibbia.data.prefs.ThemeMode
import it.lampada.bibbia.notifications.DailyVerseScheduler
import it.lampada.bibbia.notifications.Notifications
import it.lampada.bibbia.ui.components.BackTopBar
import it.lampada.bibbia.ui.components.Paragraph
import it.lampada.bibbia.ui.components.SectionTitle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(onOpenBlocker: () -> Unit, onOpenSettings: () -> Unit, onOpenInfo: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Altro") }) }) { padding ->
        Column(Modifier.padding(padding)) {
            MoreRow(Icons.Outlined.Block, "Limita app", "Tempo massimo e fasce orarie per Instagram e altre app", onOpenBlocker)
            HorizontalDivider()
            MoreRow(Icons.Outlined.Settings, "Impostazioni", "Versetto del giorno, testo, tema", onOpenSettings)
            HorizontalDivider()
            MoreRow(Icons.Outlined.Info, "Informazioni", "Traduzione, fonti e licenze", onOpenInfo)
        }
    }
}

@Composable
private fun MoreRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(container: AppContainer, settings: Settings, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showTime by remember { mutableStateOf(false) }
    var scale by remember(settings.fontScale) { mutableFloatStateOf(settings.fontScale) }
    var canNotify by remember { mutableStateOf(Notifications.canNotify(context)) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        canNotify = it
    }

    fun applyDaily(enabled: Boolean, hour: Int, minute: Int) {
        container.appScope.launch {
            container.settings.setDailyVerse(enabled, hour, minute)
            if (enabled) DailyVerseScheduler.schedule(context, hour, minute, replace = true)
            else DailyVerseScheduler.cancel(context)
        }
    }

    Scaffold(topBar = { BackTopBar("Impostazioni", onBack) }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            SectionTitle("Versetto del giorno")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Notifica giornaliera", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Un versetto ogni giorno, lo stesso che trovi nella schermata Oggi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = settings.dailyVerseEnabled,
                    onCheckedChange = { on ->
                        applyDaily(on, settings.dailyVerseHour, settings.dailyVerseMinute)
                        if (on && !canNotify && Build.VERSION.SDK_INT >= 33) {
                            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = settings.dailyVerseEnabled) { showTime = true }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Orario", modifier = Modifier.weight(1f))
                Text(
                    "%02d:%02d".format(settings.dailyVerseHour, settings.dailyVerseMinute),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (settings.dailyVerseEnabled && !canNotify) {
                Text(
                    "Le notifiche sono disattivate per Lampada.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                if (Build.VERSION.SDK_INT >= 33) {
                    TextButton(onClick = { permission.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                        Text("Consenti le notifiche")
                    }
                }
            }
            OutlinedButton(onClick = {
                scope.launch {
                    val v = container.dailyVerses.forDate()
                    Notifications.showDailyVerse(context, v.reference, v.text, v.passage.start)
                }
            }) { Text("Prova la notifica adesso") }

            SectionTitle("Lettura")
            Text("Dimensione del testo", style = MaterialTheme.typography.titleSmall)
            Slider(
                value = scale,
                onValueChange = { scale = it },
                onValueChangeFinished = { container.appScope.launch { container.settings.setFontScale(scale) } },
                valueRange = 0.8f..1.8f,
                steps = 9,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Carattere con grazie (stile libro)", modifier = Modifier.weight(1f))
                Switch(checked = settings.serifFont, onCheckedChange = { container.appScope.launch { container.settings.setSerif(it) } })
            }

            SectionTitle("Aspetto")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { i, mode ->
                    SegmentedButton(
                        selected = settings.theme == mode,
                        onClick = { container.appScope.launch { container.settings.setTheme(mode) } },
                        shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size),
                    ) { Text(mode.label) }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showTime) {
        val state = rememberTimePickerState(settings.dailyVerseHour, settings.dailyVerseMinute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTime = false },
            title = { Text("Orario del versetto") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    applyDaily(true, state.hour, state.minute)
                    showTime = false
                }) { Text("Salva") }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("Annulla") } },
        )
    }
}

@Composable
fun InfoScreen(onBack: () -> Unit) {
    Scaffold(topBar = { BackTopBar("Informazioni", onBack) }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            SectionTitle("Lampada")
            Paragraph(
                "«La tua parola è una lampada al mio piè ed una luce sul mio sentiero» (Salmo 119:105). " +
                    "Un'app per leggere la Bibbia ogni giorno, capirla meglio e dedicarle tempo. Funziona " +
                    "interamente senza internet e non raccoglie dati: tutto resta sul telefono.",
            )
            SectionTitle("La traduzione")
            Paragraph(
                "Il testo è la Bibbia «Riveduta» del 1927, curata da Giovanni Luzzi (1856-1948): una revisione " +
                    "della traduzione di Giovanni Diodati condotta sui testi originali ebraici e greci. Comprende " +
                    "i 66 libri dell'Antico e del Nuovo Testamento. È di pubblico dominio.",
            )
            Paragraph(
                "La numerazione dei versetti segue quella delle Bibbie protestanti e inglesi; in alcuni libri " +
                    "(per esempio Gioele, Malachia e alcuni capitoli di Numeri, Re e Cronache) le edizioni " +
                    "italiane stampate possono avere una numerazione leggermente diversa.",
                Modifier.padding(top = 8.dp),
            )
            SectionTitle("Fonti")
            Paragraph(
                "• Testo: eBible.org (ita1927) e CrossWire/open-bibles, confrontati versetto per versetto.\n" +
                    "• Collegamenti fra versetti: openbible.info, licenza Creative Commons Attribuzione (CC BY).\n" +
                    "• Introduzioni ai libri, sintesi dei capitoli, glossario e calendario: testi scritti per " +
                    "Lampada come guida introduttiva, adatta anche a chi si avvicina per la prima volta alla Bibbia.",
            )
            SectionTitle("Libri non inclusi")
            Paragraph(
                "La Riveduta non contiene i libri deuterocanonici presenti nelle Bibbie cattoliche e ortodosse " +
                    "(Tobia, Giuditta, 1-2 Maccabei, Sapienza, Siracide, Baruc e alcune parti di Ester e Daniele).",
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}
