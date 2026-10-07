package it.lampada.bibbia.blocker

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import java.time.LocalDate
import java.time.ZoneId

/** Calcola quanto tempo (in primo piano) sono state usate oggi le app indicate. */
class UsageTracker(private val context: Context) {

    fun hasPermission(): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= 29) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Millisecondi di utilizzo da mezzanotte a ora, per ciascun pacchetto richiesto. */
    fun usageToday(packages: Set<String>): Map<String, Long> {
        if (packages.isEmpty() || !hasPermission()) return emptyMap()
        val usm = context.getSystemService(UsageStatsManager::class.java) ?: return emptyMap()
        val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(start, now) ?: return emptyMap()
        val event = UsageEvents.Event()
        val openSince = HashMap<String, Long>()
        val total = HashMap<String, Long>()

        fun close(pkg: String, at: Long) {
            val since = openSince.remove(pkg) ?: return
            total[pkg] = (total[pkg] ?: 0L) + (at - since).coerceAtLeast(0)
        }

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            when (event.eventType) {
                RESUMED -> if (pkg in packages && pkg !in openSince) openSince[pkg] = event.timeStamp
                PAUSED, STOPPED -> if (pkg in packages) close(pkg, event.timeStamp)
                SCREEN_OFF, SHUTDOWN -> openSince.keys.toList().forEach { close(it, event.timeStamp) }
            }
        }
        openSince.keys.toList().forEach { close(it, now) }
        return total
    }

    private companion object {
        // Valori costanti di UsageEvents.Event (alcuni nomi esistono solo dalle API 28-29).
        const val RESUMED = 1 // MOVE_TO_FOREGROUND / ACTIVITY_RESUMED
        const val PAUSED = 2 // MOVE_TO_BACKGROUND / ACTIVITY_PAUSED
        const val SCREEN_OFF = 16 // SCREEN_NON_INTERACTIVE
        const val SHUTDOWN = 26 // DEVICE_SHUTDOWN
        const val STOPPED = 23 // ACTIVITY_STOPPED
    }
}
