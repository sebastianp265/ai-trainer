package com.github.sebastianp265.aitrainer.feature.health.impl

import com.github.sebastianp265.aitrainer.integration.health.SleepSession
import com.github.sebastianp265.aitrainer.integration.health.TimedValue
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

internal object HealthDayRules {

    fun lastOfDay(values: List<TimedValue>, zone: TimeZone): Map<LocalDate, Double> =
        values.groupBy { it.at.toLocalDateTime(zone).date }
            .mapValues { (_, ofDay) -> ofDay.maxBy { it.at }.value }

    fun averageOfDay(values: List<TimedValue>, zone: TimeZone): Map<LocalDate, Double> =
        values.groupBy { it.at.toLocalDateTime(zone).date }
            .mapValues { (_, ofDay) -> ofDay.map { it.value }.average() }

    fun sleepOnWakingDay(sessions: List<SleepSession>, zone: TimeZone): Map<LocalDate, Int> =
        sessions.groupBy { it.endedAt.toLocalDateTime(zone).date }
            .mapValues { (_, ofDay) -> ofDay.sumOf { it.minutes } }
}
