package com.github.sebastianp265.aitrainer.feature.workout.impl

import com.github.sebastianp265.aitrainer.integration.plan.PlannedSet
import com.github.sebastianp265.aitrainer.integration.workout.PlanDifference
import com.github.sebastianp265.aitrainer.integration.workout.RecordKind
import com.github.sebastianp265.aitrainer.integration.workout.SetEntry
import com.github.sebastianp265.aitrainer.integration.workout.WorkoutRecord
import com.github.sebastianp265.aitrainer.shared.workout.WorkingSetRow

internal fun workingSets(sets: List<SetEntry>): List<SetEntry> = sets.filter { !it.isWarmup }

internal fun averageRpe(sets: List<SetEntry>): Double? =
    workingSets(sets).mapNotNull { it.rpe }.takeIf { it.isNotEmpty() }?.average()

internal fun planDifference(planned: List<PlannedSet>, done: List<SetEntry>): PlanDifference {
    val plannedWorking = planned.filter { !it.isWarmup }
    val doneWorking = workingSets(done)
    if (plannedWorking.isEmpty()) return PlanDifference(0, 0.0, 0, addedToPlan = true)
    return PlanDifference(
        setCountDelta = doneWorking.size - plannedWorking.size,
        weightDeltaKg = if (doneWorking.isEmpty()) 0.0 else doneWorking.map { it.weightKg }.average() - plannedWorking.map { it.weightKg }.average(),
        repsDelta = doneWorking.sumOf { it.reps } - plannedWorking.sumOf { it.reps },
        addedToPlan = false,
    )
}

internal fun workoutRecords(sessionSets: List<SetEntry>, earlier: List<WorkingSetRow>): List<WorkoutRecord> =
    workingSets(sessionSets).groupBy { it.exerciseKey }.flatMap { (exerciseKey, sets) ->
        val before = earlier.filter { it.exerciseKey == exerciseKey }
        if (before.isEmpty()) return@flatMap emptyList()
        listOfNotNull(oneRepMaxRecord(exerciseKey, sets, before), repsAtWeightRecord(exerciseKey, sets, before))
    }

private fun estimable(weightKg: Double, reps: Int) = weightKg > 0 && reps in 1..MAX_REPS_FOR_ESTIMATE

private fun oneRepMaxRecord(exerciseKey: String, sets: List<SetEntry>, before: List<WorkingSetRow>): WorkoutRecord? {
    val best = sets.filter { estimable(it.weightKg, it.reps) }.maxByOrNull { estimatedOneRepMax(it.weightKg, it.reps) } ?: return null
    val previousBest = before.filter { estimable(it.weightKg, it.reps) }.maxOfOrNull { estimatedOneRepMax(it.weightKg, it.reps) } ?: return null
    val estimate = estimatedOneRepMax(best.weightKg, best.reps)
    if (estimate <= previousBest) return null
    return WorkoutRecord(exerciseKey, RecordKind.ONE_REP_MAX, best.weightKg, best.reps, estimate, improvement = estimate - previousBest)
}

private fun repsAtWeightRecord(exerciseKey: String, sets: List<SetEntry>, before: List<WorkingSetRow>): WorkoutRecord? =
    sets.filter { it.weightKg > 0 }
        .groupBy { it.weightKg }
        .mapNotNull { (weightKg, atWeight) ->
            val previousMax = before.filter { it.weightKg == weightKg }.maxOfOrNull { it.reps } ?: return@mapNotNull null
            val reps = atWeight.maxOf { it.reps }
            if (reps <= previousMax) null
            else WorkoutRecord(exerciseKey, RecordKind.REPS_AT_WEIGHT, weightKg, reps, estimatedOneRepMax(weightKg, reps), improvement = (reps - previousMax).toDouble())
        }
        .maxByOrNull { it.weightKg }
