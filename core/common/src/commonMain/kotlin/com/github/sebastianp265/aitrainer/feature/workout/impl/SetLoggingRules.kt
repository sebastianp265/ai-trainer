package com.github.sebastianp265.aitrainer.feature.workout.impl

import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
import com.github.sebastianp265.aitrainer.integration.workout.ExerciseBlock
import com.github.sebastianp265.aitrainer.integration.workout.SetEntry

internal fun groupSetsByExerciseInLoggingOrder(sets: List<SetEntry>, exercises: Map<String, Exercise>): List<ExerciseBlock> =
    sets.sortedBy { it.positionInSession }
        .groupBy { it.exerciseKey }
        .map { (exerciseKey, exerciseSets) -> ExerciseBlock(exercises.getValue(exerciseKey), exerciseSets) }

internal const val DEFAULT_REPS = 10
internal const val DEFAULT_WEIGHT_KG = 0.0

internal fun adjustReps(reps: Int, delta: Int): Int = (reps + delta).coerceAtLeast(0)
