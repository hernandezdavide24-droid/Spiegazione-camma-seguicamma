package it.lampada.bibbia.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import it.lampada.bibbia.data.bible.VerseRef
import it.lampada.bibbia.data.calendar.Tradition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "impostazioni")

enum class ThemeMode(val label: String) { SISTEMA("Come il telefono"), CHIARO("Chiaro"), SCURO("Scuro") }

data class Settings(
    val dailyVerseEnabled: Boolean = true,
    val dailyVerseHour: Int = 8,
    val dailyVerseMinute: Int = 0,
    val fontScale: Float = 1f,
    val serifFont: Boolean = true,
    val theme: ThemeMode = ThemeMode.SISTEMA,
    val traditions: Set<Tradition> = Tradition.entries.toSet(),
    val notificationsAsked: Boolean = false,
)

/** Impostazioni e ultimo punto di lettura, salvati con DataStore. */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val lastBook = intPreferencesKey("ultimo_libro")
        val lastChapter = intPreferencesKey("ultimo_capitolo")
        val lastVerse = intPreferencesKey("ultimo_versetto")
        val dailyEnabled = booleanPreferencesKey("versetto_giorno_attivo")
        val dailyHour = intPreferencesKey("versetto_giorno_ora")
        val dailyMinute = intPreferencesKey("versetto_giorno_minuto")
        val fontScale = floatPreferencesKey("dimensione_testo")
        val serif = booleanPreferencesKey("carattere_serif")
        val theme = stringPreferencesKey("tema")
        val traditions = stringSetPreferencesKey("tradizioni_calendario")
        val notificationsAsked = booleanPreferencesKey("notifiche_chieste")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { it.toSettings() }

    private fun Preferences.toSettings() = Settings(
        dailyVerseEnabled = this[Keys.dailyEnabled] ?: true,
        dailyVerseHour = this[Keys.dailyHour] ?: 8,
        dailyVerseMinute = this[Keys.dailyMinute] ?: 0,
        fontScale = this[Keys.fontScale] ?: 1f,
        serifFont = this[Keys.serif] ?: true,
        theme = this[Keys.theme]?.let { t -> ThemeMode.entries.firstOrNull { it.name == t } } ?: ThemeMode.SISTEMA,
        traditions = this[Keys.traditions]
            ?.mapNotNull { t -> Tradition.entries.firstOrNull { it.name == t } }?.toSet()
            ?: Tradition.entries.toSet(),
        notificationsAsked = this[Keys.notificationsAsked] ?: false,
    )

    suspend fun current(): Settings = settings.first()

    /** Ultimo punto di lettura; all'inizio Genesi 1:1. */
    val lastPosition: Flow<VerseRef> = context.dataStore.data.map {
        VerseRef(it[Keys.lastBook] ?: 1, it[Keys.lastChapter] ?: 1, it[Keys.lastVerse] ?: 1)
    }

    suspend fun saveLastPosition(ref: VerseRef) {
        context.dataStore.edit {
            it[Keys.lastBook] = ref.book
            it[Keys.lastChapter] = ref.chapter
            it[Keys.lastVerse] = ref.verse
        }
    }

    suspend fun setDailyVerse(enabled: Boolean, hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.dailyEnabled] = enabled
            it[Keys.dailyHour] = hour
            it[Keys.dailyMinute] = minute
        }
    }

    suspend fun setFontScale(scale: Float) {
        context.dataStore.edit { it[Keys.fontScale] = scale.coerceIn(0.8f, 1.8f) }
    }

    suspend fun setSerif(serif: Boolean) {
        context.dataStore.edit { it[Keys.serif] = serif }
    }

    suspend fun setTheme(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.theme] = mode.name }
    }

    suspend fun setTraditions(traditions: Set<Tradition>) {
        context.dataStore.edit { p -> p[Keys.traditions] = traditions.map { it.name }.toSet() }
    }

    suspend fun setNotificationsAsked() {
        context.dataStore.edit { it[Keys.notificationsAsked] = true }
    }
}
