package com.github.sebastianp265.aitrainer.integration.health

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

data class HealthDay(
    val date: LocalDate,
    val kcalEaten: Int?,
    val proteinG: Double?,
    val carbsG: Double?,
    val sugarG: Double?,
    val fatG: Double?,
    val saturatedFatG: Double?,
    val fiberG: Double?,
    val sodiumMg: Double?,
    val kcalBurned: Int?,
    val weightKg: Double?,
    val steps: Int?,
    val sleepMinutes: Int?,
    val restingHeartRate: Double?,
    val hrvMs: Double?,
)

data class HealthWorkout(
    val type: String,
    val startedAt: Instant,
    val durationMinutes: Int,
    val distanceM: Double?,
    val kcal: Int?,
    val averageHeartRate: Int?,
)
