package com.github.sebastianp265.aitrainer.feature.workout.impl

import com.github.sebastianp265.aitrainer.integration.workout.ExerciseStrength
import com.github.sebastianp265.aitrainer.shared.workout.WorkingSetRow
import kotlinx.datetime.LocalDate

private const val MAX_REPS_FOR_ESTIMATE = 12

private const val EPLEY_REPS_DIVISOR = 30.0

internal fun estimatedOneRepMax(weightKg: Double, reps: Int): Double = weightKg * (1 + reps / EPLEY_REPS_DIVISOR)

internal fun isEstimable(weightKg: Double, reps: Int) = weightKg > 0 && reps in 1..MAX_REPS_FOR_ESTIMATE

internal fun strengthInRange(workingSets: List<WorkingSetRow>, from: LocalDate, toInclusive: LocalDate): List<ExerciseStrength> =
    workingSets
        .filter { isEstimable(it.weightKg, it.reps) && it.date <= toInclusive }
        .groupBy { it.exerciseKey }
        .mapNotNull { (exerciseKey, sets) ->
            val inRange = sets.filter { it.date >= from }
            if (inRange.isEmpty()) return@mapNotNull null
            val latestDay = inRange.maxOf { it.date }
            val latest = inRange.filter { it.date == latestDay }.maxOf { estimatedOneRepMax(it.weightKg, it.reps) }
            val best = sets.maxOf { estimatedOneRepMax(it.weightKg, it.reps) }
            ExerciseStrength(exerciseKey, latest, isRecord = latest >= best, workingSetCount = inRange.size)
        }
        .sortedByDescending { it.workingSetCount }
