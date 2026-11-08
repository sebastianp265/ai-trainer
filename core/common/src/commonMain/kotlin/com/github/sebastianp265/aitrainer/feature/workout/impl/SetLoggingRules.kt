package com.github.sebastianp265.aitrainer.feature.workout.impl

import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
import com.github.sebastianp265.aitrainer.integration.workout.ExerciseBlock
import com.github.sebastianp265.aitrainer.integration.workout.SetEntry
import kotlin.math.roundToInt

internal fun groupSetsByExerciseInLoggingOrder(sets: List<SetEntry>, exercises: Map<String, Exercise>): List<ExerciseBlock> =
    sets.sortedBy { it.positionInSession }
        .groupBy { it.exerciseKey }
        .map { (exerciseKey, exerciseSets) -> ExerciseBlock(exercises.getValue(exerciseKey), exerciseSets) }

internal const val DEFAULT_REPS = 10
internal const val DEFAULT_WEIGHT_KG = 0.0

internal fun adjustWeight(weightKg: Double, deltaKg: Double): Double =
    (weightKg + deltaKg).coerceAtLeast(0.0).roundToQuarterKg()

private fun Double.roundToQuarterKg(): Double = (this * 4).roundToInt() / 4.0

internal fun adjustReps(reps: Int, delta: Int): Int = (reps + delta).coerceAtLeast(0)
