package it.lampada.bibbia.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import it.lampada.bibbia.MainActivity
import it.lampada.bibbia.R
import it.lampada.bibbia.data.bible.VerseRef

object Notifications {
    const val CHANNEL_DAILY = "versetto_del_giorno"
    const val CHANNEL_BLOCKER = "limite_app"
    private const val ID_DAILY = 1
    private const val ID_BLOCK_WARNING = 2

    const val EXTRA_BOOK = "libro"
    const val EXTRA_CHAPTER = "capitolo"
    const val EXTRA_VERSE = "versetto"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_DAILY, "Versetto del giorno", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Un versetto della Bibbia ogni giorno all'ora che scegli."
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_BLOCKER, "Limite app", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Avvisi quando sta per finire il tempo concesso a un'app."
            },
        )
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun openVerseIntent(context: Context, ref: VerseRef): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_BOOK, ref.book)
            putExtra(EXTRA_CHAPTER, ref.chapter)
            putExtra(EXTRA_VERSE, ref.verse)
        }
        return PendingIntent.getActivity(
            context, ref.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun showDailyVerse(context: Context, reference: String, text: String, ref: VerseRef) {
        if (!canNotify(context)) return
        val n = NotificationCompat.Builder(context, CHANNEL_DAILY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Versetto del giorno · $reference")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openVerseIntent(context, ref))
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(ID_DAILY, n)
        } catch (_: SecurityException) {
            // Permesso revocato nel frattempo: niente notifica.
        }
    }

    fun showBlockWarning(context: Context, appLabel: String, minutesLeft: Int) {
        if (!canNotify(context)) return
        val n = NotificationCompat.Builder(context, CHANNEL_BLOCKER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Ancora $minutesLeft minuti di $appLabel")
            .setContentText("Poi $appLabel verrà messa in pausa fino a domani.")
            .setTimeoutAfter(minutesLeft * 60_000L)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(ID_BLOCK_WARNING, n)
        } catch (_: SecurityException) {
        }
    }
}
