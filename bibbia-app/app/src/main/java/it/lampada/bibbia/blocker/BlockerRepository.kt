package it.lampada.bibbia.blocker

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.provider.Settings
import android.text.TextUtils
import it.lampada.bibbia.data.db.BlockScheduleEntity
import it.lampada.bibbia.data.db.BlockedAppEntity
import it.lampada.bibbia.data.db.BlockerDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.DayOfWeek

data class InstalledApp(val packageName: String, val label: String, val icon: Drawable?)

class BlockerRepository(private val context: Context, private val dao: BlockerDao) {

    val usage = UsageTracker(context)

    val apps: Flow<List<BlockedAppEntity>> = dao.apps()
    val schedules: Flow<List<BlockScheduleEntity>> = dao.schedules()

    val rules: Flow<List<AppRule>> = apps.map { list -> list.map { it.toRule() } }
    val scheduleRules: Flow<List<Schedule>> = schedules.map { list -> list.map { it.toSchedule() } }

    suspend fun saveApp(app: BlockedAppEntity) = dao.upsertApp(app)
    suspend fun removeApp(packageName: String) = dao.deleteApp(packageName)
    suspend fun saveSchedule(s: BlockScheduleEntity) = dao.upsertSchedule(s)
    suspend fun removeSchedule(s: BlockScheduleEntity) = dao.deleteSchedule(s)

    /** App installate che hanno un'icona nel launcher (esclusa Lampada stessa). */
    suspend fun installedApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .map { it.activityInfo.packageName to it }
            .distinctBy { it.first }
            .filter { it.first != context.packageName }
            .map { (pkg, info) -> InstalledApp(pkg, info.loadLabel(pm).toString(), info.loadIcon(pm)) }
            .sortedBy { it.label.lowercase() }
    }

    fun isAccessibilityEnabled(): Boolean {
        val expected = ComponentName(context, BlockerService::class.java)
        val enabled = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':').apply { setString(enabled) }
        return splitter.any { ComponentName.unflattenFromString(it) == expected }
    }

    fun usageToday(packages: Set<String>): Map<String, Long> = usage.usageToday(packages)
}

fun BlockedAppEntity.toRule() = AppRule(packageName, label, dailyLimitMinutes, useSchedules, enabled)

fun BlockScheduleEntity.toSchedule() = Schedule(id, startMinute, endMinute, daysFromMask(daysMask), enabled)

fun daysFromMask(mask: Int): Set<DayOfWeek> =
    DayOfWeek.entries.filter { mask and (1 shl (it.value - 1)) != 0 }.toSet()

fun maskFromDays(days: Set<DayOfWeek>): Int = days.fold(0) { acc, d -> acc or (1 shl (d.value - 1)) }
