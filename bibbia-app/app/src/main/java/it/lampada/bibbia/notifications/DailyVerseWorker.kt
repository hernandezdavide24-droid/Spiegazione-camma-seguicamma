package it.lampada.bibbia.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import it.lampada.bibbia.LampadaApp
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** Mostra il versetto del giorno e programma la notifica del giorno dopo. */
class DailyVerseWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as LampadaApp).container
        val settings = container.settings.current()
        if (settings.dailyVerseEnabled) {
            val verse = container.dailyVerses.forDate()
            Notifications.showDailyVerse(applicationContext, verse.reference, verse.text, verse.passage.start)
            DailyVerseScheduler.schedule(applicationContext, settings.dailyVerseHour, settings.dailyVerseMinute, replace = true)
        }
        return Result.success()
    }
}

object DailyVerseScheduler {
    private const val WORK_NAME = "versetto_del_giorno"

    /**
     * Programma la prossima notifica all'ora indicata. Con [replace] = false lascia stare una
     * notifica già programmata (usato all'avvio dell'app).
     */
    fun schedule(context: Context, hour: Int, minute: Int, replace: Boolean) {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(LocalTime.of(hour, minute))
        // Margine di un minuto: se il worker gira esattamente all'ora prevista, passa a domani.
        if (!next.isAfter(now.plusMinutes(1))) next = next.plusDays(1)
        val delay = Duration.between(now, next).toMillis()
        val request = OneTimeWorkRequestBuilder<DailyVerseWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
