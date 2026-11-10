package com.github.sebastianp265.aitrainer.feature.health.impl

import com.github.sebastianp265.aitrainer.integration.health.SleepSession
import com.github.sebastianp265.aitrainer.integration.health.TimedValue
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class HealthDayRulesUTest {

    private val zone = TimeZone.of("Europe/Warsaw")
    private val monday = LocalDate(2026, 11, 2)
    private val tuesday = LocalDate(2026, 11, 3)

    @Test
    fun `weight of a day is the last measurement of that day`() {
        val weights = listOf(
            TimedValue(Instant.parse("2026-11-02T06:00:00+01:00"), 81.0),
            TimedValue(Instant.parse("2026-11-02T21:00:00+01:00"), 82.0),
            TimedValue(Instant.parse("2026-11-02T12:00:00+01:00"), 81.5),
        )

        assertEquals(mapOf(monday to 82.0), HealthDayRules.lastOfDay(weights, zone))
    }

    @Test
    fun `a measurement just after local midnight belongs to the next day`() {
        val weights = listOf(TimedValue(Instant.parse("2026-11-02T23:30:00Z"), 80.0))

        assertEquals(mapOf(tuesday to 80.0), HealthDayRules.lastOfDay(weights, zone))
    }

    @Test
    fun `sleep belongs to the day of waking up`() {
        val night = SleepSession(endedAt = Instant.parse("2026-11-03T06:30:00+01:00"), minutes = 450)

        assertEquals(mapOf(tuesday to 450), HealthDayRules.sleepOnWakingDay(listOf(night), zone))
    }

    @Test
    fun `sleep sessions ending on the same day add up`() {
        val sessions = listOf(
            SleepSession(Instant.parse("2026-11-03T06:30:00+01:00"), 400),
            SleepSession(Instant.parse("2026-11-03T15:00:00+01:00"), 30),
        )

        assertEquals(mapOf(tuesday to 430), HealthDayRules.sleepOnWakingDay(sessions, zone))
    }

    @Test
    fun `resting heart rate of a day is the average of its values`() {
        val values = listOf(
            TimedValue(Instant.parse("2026-11-02T08:00:00+01:00"), 50.0),
            TimedValue(Instant.parse("2026-11-02T20:00:00+01:00"), 56.0),
            TimedValue(Instant.parse("2026-11-03T08:00:00+01:00"), 60.0),
        )

        assertEquals(mapOf(monday to 53.0, tuesday to 60.0), HealthDayRules.averageOfDay(values, zone))
    }
}
