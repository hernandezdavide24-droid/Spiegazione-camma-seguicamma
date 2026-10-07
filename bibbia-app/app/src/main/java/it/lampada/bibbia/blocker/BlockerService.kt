package it.lampada.bibbia.blocker

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import it.lampada.bibbia.LampadaApp
import it.lampada.bibbia.notifications.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Si accorge di quale app è in primo piano (solo il nome del pacchetto, nessun contenuto dello
 * schermo) e, se è un'app limitata, applica il limite giornaliero e le fasce orarie.
 */
class BlockerService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var repo: BlockerRepository

    private var rules: Map<String, AppRule> = emptyMap()
    private var schedules: List<Schedule> = emptyList()
    private var current: String? = null
    private var checkJob: Job? = null
    private val warnedToday = HashMap<String, LocalDate>()
    private var ignored: Set<String> = emptySet()

    override fun onServiceConnected() {
        super.onServiceConnected()
        repo = (application as LampadaApp).container.blocker
        ignored = buildSet {
            add("com.android.systemui")
            getSystemService(InputMethodManager::class.java)?.enabledInputMethodList?.forEach { add(it.packageName) }
        }
        scope.launch {
            repo.rules.collect { list ->
                rules = list.associateBy { it.packageName }
                current?.let(::check)
            }
        }
        scope.launch {
            repo.scheduleRules.collect { list ->
                schedules = list
                current?.let(::check)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg in ignored || pkg == current) return
        current = pkg
        handler.removeCallbacksAndMessages(null)
        check(pkg)
    }

    private fun check(pkg: String) {
        val rule = rules[pkg]
        checkJob?.cancel()
        handler.removeCallbacksAndMessages(null)
        if (rule == null || !rule.enabled) return
        checkJob = scope.launch {
            val used = if (rule.dailyLimitMinutes > 0) {
                withContext(Dispatchers.Default) { repo.usageToday(setOf(pkg))[pkg] ?: 0L }
            } else 0L
            val decision = BlockRules.evaluate(rule, schedules, used, LocalDateTime.now())
            if (current != pkg) return@launch
            when (decision) {
                is BlockDecision.LimitReached -> block(
                    rule.label,
                    "Oggi hai usato ${rule.label} per ${minutes(decision.usedMillis)}: il limite che hai scelto è di " +
                        "${decision.limitMinutes} minuti. Potrai riaprirla domani.",
                )
                is BlockDecision.InSchedule -> block(
                    rule.label,
                    "${rule.label} è in pausa fino alle ${decision.until.format(TIME)}" +
                        (if (decision.until.toLocalDate() != LocalDate.now()) " di domani." else "."),
                )
                is BlockDecision.Allowed -> {
                    decision.remainingMillis?.let { remaining -> maybeWarn(rule, remaining) }
                    decision.nextCheckMillis?.let { delay ->
                        // Piccolo margine perché le statistiche d'uso arrivano con qualche secondo di ritardo.
                        handler.postDelayed({ if (current == pkg) check(pkg) }, delay + 3_000L)
                    }
                }
            }
        }
    }

    private fun maybeWarn(rule: AppRule, remainingMillis: Long) {
        val today = LocalDate.now()
        if (warnedToday[rule.packageName] == today) return
        val warnAt = remainingMillis - WARNING_BEFORE
        if (warnAt <= 0) {
            warnedToday[rule.packageName] = today
            Notifications.showBlockWarning(this, rule.label, ((remainingMillis + 59_999) / 60_000).toInt())
        } else {
            handler.postDelayed({
                if (current == rule.packageName && warnedToday[rule.packageName] != LocalDate.now()) {
                    warnedToday[rule.packageName] = LocalDate.now()
                    Notifications.showBlockWarning(this, rule.label, (WARNING_BEFORE / 60_000).toInt())
                }
            }, warnAt)
        }
    }

    private fun block(label: String, message: String) {
        performGlobalAction(GLOBAL_ACTION_HOME)
        startActivity(
            Intent(this, BlockActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .putExtra(BlockActivity.EXTRA_APP, label)
                .putExtra(BlockActivity.EXTRA_MESSAGE, message),
        )
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val WARNING_BEFORE = 5 * 60_000L
        val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        fun minutes(ms: Long): String {
            val m = ms / 60_000
            return if (m >= 60) "${m / 60} h ${m % 60} min" else "$m minuti"
        }
    }
}
