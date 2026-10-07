package it.lampada.bibbia

import it.lampada.bibbia.blocker.AppRule
import it.lampada.bibbia.blocker.BlockDecision
import it.lampada.bibbia.blocker.BlockRules
import it.lampada.bibbia.blocker.Schedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime

class BlockRulesTest {
    private val ig = AppRule("com.instagram.android", "Instagram", 30, useSchedules = true, enabled = true)
    private val allDays = DayOfWeek.entries.toSet()
    private val night = Schedule(1, 22 * 60, 7 * 60, allDays, true)

    // 7 ottobre 2026 è un mercoledì.
    private fun at(h: Int, m: Int, day: Int = 7) = LocalDateTime.of(2026, 10, day, h, m)

    @Test
    fun underLimitIsAllowed() {
        val d = BlockRules.evaluate(ig, emptyList(), 10 * 60_000L, at(12, 0))
        assertTrue(d is BlockDecision.Allowed)
        assertEquals(20 * 60_000L, (d as BlockDecision.Allowed).remainingMillis)
    }

    @Test
    fun overLimitIsBlocked() {
        val d = BlockRules.evaluate(ig, emptyList(), 30 * 60_000L, at(12, 0))
        assertTrue(d is BlockDecision.LimitReached)
    }

    @Test
    fun overnightSchedule() {
        assertEquals(at(7, 0, 8), night.activeUntil(at(23, 30)))
        assertEquals(at(7, 0), night.activeUntil(at(6, 59)))
        assertNull(night.activeUntil(at(7, 0)))
        assertNull(night.activeUntil(at(21, 59)))
        val d = BlockRules.evaluate(ig, listOf(night), 0, at(23, 0))
        assertTrue(d is BlockDecision.InSchedule)
    }

    @Test
    fun overnightScheduleUsesStartDay() {
        // Solo il venerdì sera: sabato alle 6 è ancora bloccato, venerdì alle 6 no.
        val fri = Schedule(2, 22 * 60, 7 * 60, setOf(DayOfWeek.FRIDAY), true)
        assertEquals(at(7, 0, 10), fri.activeUntil(at(6, 0, 10)))
        assertNull(fri.activeUntil(at(6, 0, 9)))
    }

    @Test
    fun dayScheduleAndNextCheck() {
        val morning = Schedule(3, 8 * 60, 9 * 60, allDays, true)
        val d = BlockRules.evaluate(ig.copy(dailyLimitMinutes = 0), listOf(morning), 0, at(7, 30))
        assertEquals(30 * 60_000L, (d as BlockDecision.Allowed).nextCheckMillis)
        assertTrue(BlockRules.evaluate(ig, listOf(morning), 0, at(8, 15)) is BlockDecision.InSchedule)
    }

    @Test
    fun schedulesIgnoredWhenDisabledForApp() {
        val d = BlockRules.evaluate(ig.copy(useSchedules = false), listOf(night), 0, at(23, 0))
        assertTrue(d is BlockDecision.Allowed)
    }
}
