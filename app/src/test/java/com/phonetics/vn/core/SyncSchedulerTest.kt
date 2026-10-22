package com.phonetics.vn.core

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncSchedulerTest {

    private val bj = ZoneId.of("Asia/Shanghai")

    @Test
    fun fridayMorningBeijingIsPeak() {
        // 2026-10-16 is a Friday; 10:00 is inside the 09:00-12:00 peak window.
        val friday10 = ZonedDateTime.of(2026, 10, 16, 10, 0, 0, 0, bj).toInstant()
        assertTrue(SyncScheduler.isPeak(friday10))
    }

    @Test
    fun fridayEarlyMorningIsOffPeak() {
        val friday3am = ZonedDateTime.of(2026, 10, 16, 3, 0, 0, 0, bj).toInstant()
        assertFalse(SyncScheduler.isPeak(friday3am))
    }

    @Test
    fun weekendIsOffPeak() {
        // 2026-10-17 is a Saturday.
        val saturdayNoon = ZonedDateTime.of(2026, 10, 17, 12, 0, 0, 0, bj).toInstant()
        assertFalse(SyncScheduler.isPeak(saturdayNoon))
    }

    @Test
    fun nextOffPeakMovesForwardOutOfPeakWindow() {
        val friday10 = ZonedDateTime.of(2026, 10, 16, 10, 0, 0, 0, bj).toInstant()
        val next = SyncScheduler.nextOffPeak(friday10)
        assertTrue(next.isAfter(friday10))
        assertFalse(SyncScheduler.isPeak(next))
    }
}
