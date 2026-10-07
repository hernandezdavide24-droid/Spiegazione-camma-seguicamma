package it.lampada.bibbia.blocker

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

/** Regola per una singola app. [dailyLimitMinutes] = 0 significa nessun limite di tempo. */
data class AppRule(
    val packageName: String,
    val label: String,
    val dailyLimitMinutes: Int,
    val useSchedules: Boolean,
    val enabled: Boolean,
)

/**
 * Fascia oraria di blocco, in minuti dalla mezzanotte. Se [startMinute] > [endMinute] la fascia
 * attraversa la mezzanotte (es. 22:00-07:00). [days] sono i giorni in cui la fascia *inizia*.
 */
data class Schedule(
    val id: Long,
    val startMinute: Int,
    val endMinute: Int,
    val days: Set<DayOfWeek>,
    val enabled: Boolean,
) {
    private val overnight get() = startMinute > endMinute

    /** Se la fascia è attiva in [now], restituisce quando finisce; altrimenti null. */
    fun activeUntil(now: LocalDateTime): LocalDateTime? {
        if (!enabled || startMinute == endMinute) return null
        val minute = now.hour * 60 + now.minute
        val today = now.toLocalDate()
        if (!overnight) {
            return if (now.dayOfWeek in days && minute in startMinute until endMinute) {
                today.atTime(LocalTime.of(endMinute / 60, endMinute % 60))
            } else null
        }
        // Fascia notturna: parte di sera "oggi" oppure è cominciata ieri sera.
        if (minute >= startMinute && now.dayOfWeek in days) {
            return today.plusDays(1).atTime(LocalTime.of(endMinute / 60, endMinute % 60))
        }
        if (minute < endMinute && now.dayOfWeek.minus(1) in days) {
            return today.atTime(LocalTime.of(endMinute / 60, endMinute % 60))
        }
        return null
    }

    /** Prossimo momento (dopo [now]) in cui la fascia inizia, entro una settimana. */
    fun nextStart(now: LocalDateTime): LocalDateTime? {
        if (!enabled || days.isEmpty() || startMinute == endMinute) return null
        for (offset in 0..7L) {
            val day = now.toLocalDate().plusDays(offset)
            if (day.dayOfWeek !in days) continue
            val start = day.atTime(LocalTime.of(startMinute / 60, startMinute % 60))
            if (start.isAfter(now)) return start
        }
        return null
    }

    companion object {
        fun formatMinute(m: Int): String = "%02d:%02d".format(m / 60, m % 60)
    }
}

sealed interface BlockDecision {
    /**
     * L'app si può usare. [remainingMillis] è il tempo che resta prima del limite (null se non c'è
     * un limite); [nextCheckMillis] dice fra quanto ricontrollare (limite o inizio di una fascia).
     */
    data class Allowed(val remainingMillis: Long?, val nextCheckMillis: Long?) : BlockDecision

    data class LimitReached(val usedMillis: Long, val limitMinutes: Int) : BlockDecision

    data class InSchedule(val until: LocalDateTime) : BlockDecision
}

object BlockRules {

    fun evaluate(
        rule: AppRule,
        schedules: List<Schedule>,
        usedTodayMillis: Long,
        now: LocalDateTime,
    ): BlockDecision {
        if (!rule.enabled) return BlockDecision.Allowed(null, null)

        if (rule.useSchedules) {
            val until = schedules.mapNotNull { it.activeUntil(now) }.maxOrNull()
            if (until != null) return BlockDecision.InSchedule(until)
        }

        var remaining: Long? = null
        if (rule.dailyLimitMinutes > 0) {
            val limit = rule.dailyLimitMinutes * 60_000L
            if (usedTodayMillis >= limit) {
                return BlockDecision.LimitReached(usedTodayMillis, rule.dailyLimitMinutes)
            }
            remaining = limit - usedTodayMillis
        }

        val nextScheduleMillis = if (rule.useSchedules) {
            schedules.mapNotNull { it.nextStart(now) }.minOrNull()
                ?.let { java.time.Duration.between(now, it).toMillis() }
        } else null

        // A mezzanotte il conteggio riparte: non serve ricontrollare oltre.
        val nextCheck = listOfNotNull(remaining, nextScheduleMillis).minOrNull()
        return BlockDecision.Allowed(remaining, nextCheck)
    }
}
