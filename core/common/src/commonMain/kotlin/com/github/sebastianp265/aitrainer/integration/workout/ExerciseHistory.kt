package com.github.sebastianp265.aitrainer.integration.workout

import kotlinx.datetime.LocalDate

data class ExerciseHistory(
    val exerciseKey: String,
    val sessionCount: Int,
    val estimatedOneRepMaxKg: Double?,
    val bestSet: BestSet?,
    val oneRepMaxChangeKg: Double?,
    val sessions: List<ExerciseSessionEntry>,
)

data class BestSet(val weightKg: Double, val reps: Int)

data class ExerciseSessionEntry(
    val sessionId: Long,
    val date: LocalDate,
    val groups: List<LastTimeGroup>,
    val estimatedOneRepMaxKg: Double?,
    val averageRpe: Double?,
    val isRecord: Boolean,
)
