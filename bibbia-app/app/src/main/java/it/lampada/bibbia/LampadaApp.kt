package it.lampada.bibbia

import android.app.Application
import it.lampada.bibbia.notifications.DailyVerseScheduler
import it.lampada.bibbia.notifications.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LampadaApp : Application() {
    lateinit var container: AppContainer
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.createChannels(this)
        scope.launch {
            val s = container.settings.current()
            if (s.dailyVerseEnabled) {
                // KEEP: se una notifica è già programmata non la sposta.
                DailyVerseScheduler.schedule(this@LampadaApp, s.dailyVerseHour, s.dailyVerseMinute, replace = false)
            }
        }
    }
}
