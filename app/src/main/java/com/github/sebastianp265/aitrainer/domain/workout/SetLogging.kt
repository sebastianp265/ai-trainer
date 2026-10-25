package com.github.sebastianp265.aitrainer.domain.workout

import com.github.sebastianp265.aitrainer.domain.model.Exercise
import com.github.sebastianp265.aitrainer.domain.model.SetEntry
import kotlin.math.roundToInt

data class ExerciseBlock(
    val exercise: Exercise,
    val sets: List<SetEntry>,
)

/** Blocks are ordered by when each exercise was first logged. */
fun groupSetsByExercise(sets: List<SetEntry>, exercises: Map<Long, Exercise>): List<ExerciseBlock> =
    sets.sortedBy { it.order }
        .groupBy { it.exerciseId }
        .mapNotNull { (exerciseId, exerciseSets) ->
            exercises[exerciseId]?.let { ExerciseBlock(it, exerciseSets) }
        }

const val WEIGHT_STEP_KG = 2.5

/** Rounded to 0.25 kg so repeated steps don't accumulate floating point drift. */
fun adjustWeight(weightKg: Double, deltaKg: Double): Double =
    (((weightKg + deltaKg).coerceAtLeast(0.0)) * 4).roundToInt() / 4.0

fun adjustReps(reps: Int, delta: Int): Int = (reps + delta).coerceAtLeast(0)
