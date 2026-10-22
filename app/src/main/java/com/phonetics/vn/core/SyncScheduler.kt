package com.phonetics.vn.core

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * When to run the daily DeepSeek sync so it lands in the cheaper **off-peak** window
 * (Beijing 09:00-12:00 & 14:00-18:00 weekdays are peak; every other hour, weekends
 * and Chinese public holidays are half price). Chinese public holidays are not
 * computed here (approx. by weekday/weekend only).
 */
object SyncScheduler {

    private val BEIJING: ZoneId = ZoneId.of("Asia/Shanghai")

    /** DeepSeek peak: Beijing weekday 09:00-11:59 and 14:00-17:59. */
    fun isPeak(instant: Instant): Boolean {
        val z = ZonedDateTime.ofInstant(instant, BEIJING)
        if (z.dayOfWeek.value >= 6) return false // Sat/Sun fully off-peak
        val h = z.hour
        return (h in 9..11) || (h in 14..17)
    }

    fun isOffPeak(instant: Instant): Boolean = !isPeak(instant)

    /** Next off-peak instant strictly after [from] (hour granularity stepping). */
    fun nextOffPeak(from: Instant): Instant {
        var c = from.plusSeconds(1)
        while (isPeak(c)) c = c.plusSeconds(3600)
        return c
    }
}
