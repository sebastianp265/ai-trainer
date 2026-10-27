package com.github.sebastianp265.aitrainer.domain.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

data class WeightMeasurement(
    val id: Long,
    val measuredAt: Instant,
    val weightKg: Double,
    val source: WeightSource,
)

data class DailyEnergy(
    val date: LocalDate,
    val kcalBurned: Int?,
    val kcalBurnedSource: EnergySource?,
    val kcalEaten: Int?,
    val proteinG: Double?,
    val carbsG: Double?,
    val fatG: Double?,
)

data class HealthDayTotals(
    val date: LocalDate,
    val kcalEaten: Int?,
    val proteinG: Double?,
    val carbsG: Double?,
    val fatG: Double?,
    val kcalBurned: Int?,
)

data class LoggedSet(
    val date: LocalDate,
    val primaryMuscles: List<MuscleGroup>,
    val secondaryMuscles: List<MuscleGroup>,
    val isWarmup: Boolean,
)
