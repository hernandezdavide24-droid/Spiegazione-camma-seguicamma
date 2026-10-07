package it.lampada.bibbia.ui.blocker

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.lampada.bibbia.AppContainer
import it.lampada.bibbia.blocker.InstalledApp
import it.lampada.bibbia.blocker.Schedule
import it.lampada.bibbia.blocker.daysFromMask
import it.lampada.bibbia.blocker.maskFromDays
import it.lampada.bibbia.data.db.BlockScheduleEntity
import it.lampada.bibbia.data.db.BlockedAppEntity
import it.lampada.bibbia.ui.components.BackTopBar
import it.lampada.bibbia.ui.components.SectionTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun BlockerScreen(container: AppContainer, onBack: () -> Unit) {
    val repo = container.blocker
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val apps by repo.apps.collectAsStateWithLifecycle(emptyList())
    val schedules by repo.schedules.collectAsStateWithLifecycle(emptyList())

    // Lo stato dei permessi si aggiorna quando si torna dalle impostazioni di sistema.
    var accessibilityOn by remember { mutableStateOf(repo.isAccessibilityEnabled()) }
    var usageOn by remember { mutableStateOf(repo.usage.hasPermission()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        accessibilityOn = repo.isAccessibilityEnabled()
        usageOn = repo.usage.hasPermission()
    }

    // Tempo di utilizzo di oggi, aggiornato ogni minuto.
    var usage by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    LaunchedEffect(apps, usageOn) {
        while (true) {
            val packages = apps.map { it.packageName }.toSet()
            usage = withContext(Dispatchers.Default) { repo.usageToday(packages) }
            delay(60_000)
        }
    }

    var showAddApp by remember { mutableStateOf(false) }
    var editApp by remember { mutableStateOf<BlockedAppEntity?>(null) }
    var editSchedule by remember { mutableStateOf<BlockScheduleEntity?>(null) }

    Scaffold(topBar = { BackTopBar("Limita app", onBack) }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Text(
                "Scegli le app che ti distraggono (per esempio Instagram) e decidi quanto usarle ogni giorno " +
                    "o in quali orari metterle in pausa. Quando il tempo finisce, al posto dell'app vedrai un " +
                    "versetto e un invito a fermarti.",
                style = MaterialTheme.typography.bodyMedium,
            )

            SectionTitle("Permessi necessari")
            PermissionRow(
                ok = accessibilityOn,
                title = "Servizio di accessibilità",
                description = "Serve per accorgersi quando apri un'app limitata. Lampada vede solo quale app è " +
                    "aperta, non il contenuto dello schermo. Nelle impostazioni cerca «Lampada – limite app» " +
                    "(a volte sotto «App installate» o «App scaricate»).",
            ) {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            PermissionRow(
                ok = usageOn,
                title = "Accesso ai dati di utilizzo",
                description = "Serve solo per il limite di tempo giornaliero: permette di contare i minuti " +
                    "passati in ogni app. Per le sole fasce orarie non è necessario.",
            ) {
                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }

            SectionTitle("App limitate")
            if (apps.isEmpty()) {
                Text(
                    "Nessuna app ancora. Aggiungine una per iniziare.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            apps.forEach { app ->
                AppRow(
                    app = app,
                    usedMillis = usage[app.packageName],
                    onClick = { editApp = app },
                    onToggle = { enabled -> scope.launch { repo.saveApp(app.copy(enabled = enabled)) } },
                )
            }
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(onClick = { showAddApp = true }) {
                Icon(Icons.Outlined.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("Aggiungi app")
            }

            SectionTitle("Fasce orarie di pausa")
            Text(
                "Negli orari scelti le app con l'opzione «Rispetta le fasce orarie» restano in pausa. " +
                    "Per esempio di sera prima di dormire o al mattino nel momento della preghiera.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            schedules.forEach { s ->
                ScheduleRow(
                    schedule = s,
                    onClick = { editSchedule = s },
                    onToggle = { enabled -> scope.launch { repo.saveSchedule(s.copy(enabled = enabled)) } },
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = {
                editSchedule = BlockScheduleEntity(
                    startMinute = 22 * 60,
                    endMinute = 7 * 60,
                    daysMask = maskFromDays(DayOfWeek.entries.toSet()),
                    enabled = true,
                )
            }) {
                Icon(Icons.Outlined.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("Aggiungi fascia oraria")
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showAddApp) {
        AddAppDialog(
            container = container,
            exclude = apps.map { it.packageName }.toSet(),
            onDismiss = { showAddApp = false },
            onPick = { app ->
                showAddApp = false
                editApp = BlockedAppEntity(app.packageName, app.label, 30, useSchedules = true, enabled = true)
            },
        )
    }
    editApp?.let { app ->
        EditAppDialog(
            app = app,
            isNew = apps.none { it.packageName == app.packageName },
            onDismiss = { editApp = null },
            onSave = {
                scope.launch { repo.saveApp(it) }
                editApp = null
            },
            onDelete = {
                scope.launch { repo.removeApp(app.packageName) }
                editApp = null
            },
        )
    }
    editSchedule?.let { s ->
        EditScheduleDialog(
            schedule = s,
            onDismiss = { editSchedule = null },
            onSave = {
                scope.launch { repo.saveSchedule(it) }
                editSchedule = null
            },
            onDelete = if (s.id != 0L) {
                {
                    scope.launch { repo.removeSchedule(s) }
                    editSchedule = null
                }
            } else null,
        )
    }
}

@Composable
private fun PermissionRow(ok: Boolean, title: String, description: String, onFix: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (ok) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(
                if (ok) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = if (ok) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title + if (ok) " · attivo" else " · da attivare", style = MaterialTheme.typography.titleSmall)
                Text(description, style = MaterialTheme.typography.bodySmall)
                if (!ok) {
                    TextButton(onClick = onFix, modifier = Modifier.padding(top = 4.dp)) { Text("Apri le impostazioni") }
                }
            }
        }
    }
}

private fun formatMinutes(m: Long): String = if (m >= 60) "${m / 60} h ${m % 60} min" else "$m min"

@Composable
private fun AppRow(app: BlockedAppEntity, usedMillis: Long?, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app.packageName)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(app.label, style = MaterialTheme.typography.titleSmall)
            val parts = buildList {
                if (app.dailyLimitMinutes > 0) {
                    val used = (usedMillis ?: 0L) / 60_000
                    add("Oggi ${formatMinutes(used)} su ${formatMinutes(app.dailyLimitMinutes.toLong())}")
                } else {
                    usedMillis?.let { add("Oggi ${formatMinutes(it / 60_000)} · nessun limite di tempo") }
                        ?: add("Nessun limite di tempo")
                }
                if (app.useSchedules) add("rispetta le fasce orarie")
            }
            Text(
                parts.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = app.enabled, onCheckedChange = onToggle)
    }
    HorizontalDivider()
}

@Composable
private fun AppIcon(packageName: String) {
    val context = LocalContext.current
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, packageName) {
        value = try {
            context.packageManager.getApplicationIcon(packageName).toBitmap(96, 96).asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
    bitmap?.let { Image(it, contentDescription = null, modifier = Modifier.size(40.dp)) }
        ?: Spacer(Modifier.size(40.dp))
}

@Composable
private fun ScheduleRow(schedule: BlockScheduleEntity, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    val days = daysFromMask(schedule.daysMask)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "${Schedule.formatMinute(schedule.startMinute)} – ${Schedule.formatMinute(schedule.endMinute)}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                describeDays(days),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = schedule.enabled, onCheckedChange = onToggle)
    }
    HorizontalDivider()
}

private fun dayShort(d: DayOfWeek) = d.getDisplayName(TextStyle.SHORT, Locale.ITALIAN).replaceFirstChar { it.uppercase() }

private fun describeDays(days: Set<DayOfWeek>): String = when {
    days.size == 7 -> "Tutti i giorni"
    days == DayOfWeek.entries.take(5).toSet() -> "Dal lunedì al venerdì"
    days == setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) -> "Sabato e domenica"
    days.isEmpty() -> "Nessun giorno"
    else -> days.sorted().joinToString(", ") { dayShort(it) }
}

@Composable
private fun AddAppDialog(
    container: AppContainer,
    exclude: Set<String>,
    onDismiss: () -> Unit,
    onPick: (InstalledApp) -> Unit,
) {
    val installed by produceState<List<InstalledApp>?>(null) { value = container.blocker.installedApps() }
    var filter by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Scegli un'app") },
        text = {
            Column {
                OutlinedTextField(
                    value = filter,
                    onValueChange = { filter = it },
                    placeholder = { Text("Cerca…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                val list = installed
                if (list == null) {
                    Text("Caricamento…")
                } else {
                    val shown = list.filter { it.packageName !in exclude && it.label.contains(filter, ignoreCase = true) }
                    LazyColumn(Modifier.heightIn(max = 400.dp)) {
                        items(shown, key = { it.packageName }) { app ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onPick(app) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val bmp = remember(app.packageName) { app.icon?.toBitmap(96, 96)?.asImageBitmap() }
                                if (bmp != null) Image(bmp, null, Modifier.size(36.dp)) else Spacer(Modifier.size(36.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(app.label)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}

private val LIMIT_PRESETS = listOf(0, 15, 30, 45, 60, 90, 120)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditAppDialog(
    app: BlockedAppEntity,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (BlockedAppEntity) -> Unit,
    onDelete: () -> Unit,
) {
    var limit by remember { mutableIntStateOf(app.dailyLimitMinutes) }
    var useSchedules by remember { mutableStateOf(app.useSchedules) }
    var custom by remember { mutableFloatStateOf(app.dailyLimitMinutes.coerceIn(5, 240).toFloat()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(app.label) },
        text = {
            Column {
                Text("Tempo massimo al giorno", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LIMIT_PRESETS.forEach { m ->
                        FilterChip(
                            selected = limit == m,
                            onClick = {
                                limit = m
                                if (m > 0) custom = m.toFloat()
                            },
                            label = { Text(if (m == 0) "Nessun limite" else formatMinutes(m.toLong())) },
                        )
                    }
                }
                if (limit > 0) {
                    Text("Su misura: ${formatMinutes(custom.toLong())}", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = custom,
                        onValueChange = {
                            custom = (it / 5).toInt() * 5f
                            limit = custom.toInt()
                        },
                        valueRange = 5f..240f,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Rispetta le fasce orarie", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "In pausa anche negli orari di blocco",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = useSchedules, onCheckedChange = { useSchedules = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(app.copy(dailyLimitMinutes = limit, useSchedules = useSchedules))
            }) { Text("Salva") }
        },
        dismissButton = {
            if (isNew) {
                TextButton(onClick = onDismiss) { Text("Annulla") }
            } else {
                TextButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, null)
                    Text("Rimuovi")
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun EditScheduleDialog(
    schedule: BlockScheduleEntity,
    onDismiss: () -> Unit,
    onSave: (BlockScheduleEntity) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var step by remember { mutableIntStateOf(0) } // 0 = inizio, 1 = fine
    val startState = rememberTimePickerState(schedule.startMinute / 60, schedule.startMinute % 60, is24Hour = true)
    val endState = rememberTimePickerState(schedule.endMinute / 60, schedule.endMinute % 60, is24Hour = true)
    var days by remember { mutableStateOf(daysFromMask(schedule.daysMask)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (step == 0) "Inizio della pausa" else "Fine della pausa") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = step == 0, onClick = { step = 0 },
                        label = { Text("Dalle %02d:%02d".format(startState.hour, startState.minute)) },
                    )
                    FilterChip(
                        selected = step == 1, onClick = { step = 1 },
                        label = { Text("Alle %02d:%02d".format(endState.hour, endState.minute)) },
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (step == 0) TimePicker(state = startState) else TimePicker(state = endState)
                Text("Giorni", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DayOfWeek.entries.forEach { d ->
                        FilterChip(
                            selected = d in days,
                            onClick = { days = if (d in days) days - d else days + d },
                            label = { Text(dayShort(d)) },
                        )
                    }
                }
                val start = startState.hour * 60 + startState.minute
                val end = endState.hour * 60 + endState.minute
                if (start > end) {
                    Text(
                        "La pausa continua fino al mattino seguente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (start == end) {
                    Text(
                        "Inizio e fine coincidono: scegli orari diversi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            val start = startState.hour * 60 + startState.minute
            val end = endState.hour * 60 + endState.minute
            TextButton(
                enabled = start != end && days.isNotEmpty(),
                onClick = {
                    onSave(schedule.copy(startMinute = start, endMinute = end, daysMask = maskFromDays(days)))
                },
            ) { Text("Salva") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = onDelete) { Text("Elimina", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("Annulla") }
            }
        },
    )
}
