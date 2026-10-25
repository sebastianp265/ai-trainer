package com.github.sebastianp265.aitrainer.domain.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

data class Exercise(
    val id: Long,
    val name: String,
    val primaryMuscles: List<MuscleGroup>,
    val secondaryMuscles: List<MuscleGroup>,
    val equipment: Equipment,
    val preference: ExercisePreference,
)

data class WorkoutSession(
    val id: Long,
    val date: LocalDate,
    val startedAt: Instant,
    val finishedAt: Instant?,
    val planSessionRef: String?,
    val notes: String?,
) {
    val isActive: Boolean get() = finishedAt == null
}

data class SetEntry(
    val id: Long,
    val sessionId: Long,
    val exerciseId: Long,
    val positionInSession: Int,
    val reps: Int,
    val weightKg: Double,
    val rpe: Double?,
    val isWarmup: Boolean,
)

data class SessionSummary(
    val id: Long,
    val date: LocalDate,
    val startedAt: Instant,
    val finishedAt: Instant?,
    val exerciseCount: Int,
    val setCount: Int,
)
