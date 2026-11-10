package com.github.sebastianp265.aitrainer.integration.health

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

data class NutritionTotals(
    val date: LocalDate,
    val kcal: Int?,
    val proteinG: Double?,
    val carbsG: Double?,
    val sugarG: Double?,
    val fatG: Double?,
    val saturatedFatG: Double?,
    val fiberG: Double?,
    val sodiumMg: Double?,
)

data class DailyAmount(val date: LocalDate, val amount: Int)

data class TimedValue(val at: Instant, val value: Double)

data class SleepSession(val endedAt: Instant, val minutes: Int)

data class ExternalWorkoutReading(
    val type: String,
    val startedAt: Instant,
    val durationMinutes: Int,
    val distanceM: Double?,
    val kcal: Int?,
    val averageHeartRate: Int?,
    val writtenByThisApp: Boolean,
)
