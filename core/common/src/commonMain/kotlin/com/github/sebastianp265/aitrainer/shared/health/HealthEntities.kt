package com.github.sebastianp265.aitrainer.shared.health

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

@Entity(tableName = "health_day")
internal data class HealthDayEntity(
    @PrimaryKey val date: LocalDate,
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

@Entity(tableName = "health_workout", indices = [Index("startedAt")])
internal data class HealthWorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val startedAt: Instant,
    val durationMinutes: Int,
    val distanceM: Double?,
    val kcal: Int?,
    val averageHeartRate: Int?,
)
