package com.github.sebastianp265.aitrainer.domain.workout

import com.github.sebastianp265.aitrainer.domain.model.Exercise
import com.github.sebastianp265.aitrainer.domain.model.SetEntry
import kotlin.math.roundToInt

data class ExerciseBlock(
    val exercise: Exercise,
    val sets: List<SetEntry>,
)

fun groupSetsByExerciseInLoggingOrder(sets: List<SetEntry>, exercises: Map<Long, Exercise>): List<ExerciseBlock> =
    sets.sortedBy { it.positionInSession }
        .groupBy { it.exerciseId }
        .mapNotNull { (exerciseId, exerciseSets) ->
            exercises[exerciseId]?.let { ExerciseBlock(it, exerciseSets) }
        }

const val WEIGHT_STEP_KG = 2.5

fun adjustWeight(weightKg: Double, deltaKg: Double): Double =
    (weightKg + deltaKg).coerceAtLeast(0.0).roundToQuarterKg()

private fun Double.roundToQuarterKg(): Double = (this * 4).roundToInt() / 4.0

fun adjustReps(reps: Int, delta: Int): Int = (reps + delta).coerceAtLeast(0)
