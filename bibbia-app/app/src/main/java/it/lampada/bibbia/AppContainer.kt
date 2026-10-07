package it.lampada.bibbia

import android.content.Context
import it.lampada.bibbia.blocker.BlockerRepository
import it.lampada.bibbia.data.analysis.AnalysisRepository
import it.lampada.bibbia.data.bible.BibleRepository
import it.lampada.bibbia.data.bible.VerseRef
import it.lampada.bibbia.data.daily.DailyVerseRepository
import it.lampada.bibbia.data.db.AppDatabase
import it.lampada.bibbia.data.prefs.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Richieste di apertura di un versetto nel lettore (da notifiche, ricerca, preferiti...). */
class ReaderNavigator {
    private val _pending = MutableStateFlow<VerseRef?>(null)
    val pending: StateFlow<VerseRef?> = _pending.asStateFlow()

    fun open(ref: VerseRef) {
        _pending.value = ref
    }

    fun consumed() {
        _pending.value = null
    }
}

/** Dipendenze condivise dall'app (semplice "service locator"). */
class AppContainer(context: Context) {
    private val app = context.applicationContext
    val bible = BibleRepository(app.assets)
    val analysis = AnalysisRepository(app.assets, bible)
    val database = AppDatabase.create(app)
    val study = database.studyDao()
    val settings = SettingsRepository(app)
    val dailyVerses = DailyVerseRepository(app.assets, bible)
    val blocker = BlockerRepository(app, database.blockerDao())
    val navigator = ReaderNavigator()
}
