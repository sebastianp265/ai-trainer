package com.github.sebastianp265.aitrainer.feature.plan.impl

import com.github.sebastianp265.aitrainer.integration.plan.PlannedSet

internal sealed interface PlanLine {
    data class Warmup(val reps: Int, val weightKg: Double) : PlanLine

    data class Working(
        val firstNumber: Int,
        val lastNumber: Int,
        val setCount: Int,
        val reps: Int,
        val weightKg: Double,
        val rpe: Double?,
    ) : PlanLine
}

internal data class PlanExercise(val exerciseKey: String, val lines: List<PlanLine>)

internal fun planExercises(sets: List<PlannedSet>): List<PlanExercise> =
    sets.groupBy { it.exerciseKey }.map { (exerciseKey, exerciseSets) -> PlanExercise(exerciseKey, planLines(exerciseSets)) }

private fun planLines(sets: List<PlannedSet>): List<PlanLine> {
    val lines = mutableListOf<PlanLine>()
    var workingNumber = 0
    sets.forEach { set ->
        if (set.isWarmup) {
            lines += PlanLine.Warmup(set.reps, set.weightKg)
            return@forEach
        }
        workingNumber++
        val previous = lines.lastOrNull() as? PlanLine.Working
        if (previous != null && previous.reps == set.reps && previous.weightKg == set.weightKg && previous.rpe == set.rpe) {
            lines[lines.lastIndex] = previous.copy(lastNumber = workingNumber, setCount = previous.setCount + 1)
        } else {
            lines += PlanLine.Working(workingNumber, workingNumber, 1, set.reps, set.weightKg, set.rpe)
        }
    }
    return lines
}

internal fun workingSetCount(sets: List<PlannedSet>): Int = sets.count { !it.isWarmup }

internal fun exerciseCount(sets: List<PlannedSet>): Int = sets.map { it.exerciseKey }.distinct().size
