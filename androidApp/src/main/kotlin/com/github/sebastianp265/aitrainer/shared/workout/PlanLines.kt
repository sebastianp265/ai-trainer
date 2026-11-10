package com.github.sebastianp265.aitrainer.shared.workout

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.integration.plan.PlannedSet
import com.github.sebastianp265.aitrainer.shared.localization.formatWeight

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

@Composable
internal fun planNumberText(line: PlanLine.Working): String =
    if (line.firstNumber == line.lastNumber) line.firstNumber.toString() else stringResource(R.string.plan_set_range, line.firstNumber, line.lastNumber)

@Composable
internal fun planWarmupText(line: PlanLine.Warmup): String =
    if (line.weightKg > 0) stringResource(R.string.plan_line_warmup, line.reps, formatWeight(line.weightKg))
    else stringResource(R.string.plan_line_warmup_bodyweight, line.reps, stringResource(R.string.bodyweight_label))

@Composable
internal fun planWorkingText(line: PlanLine.Working): String =
    if (line.weightKg > 0) stringResource(R.string.plan_line_load, line.setCount, line.reps, formatWeight(line.weightKg))
    else stringResource(R.string.plan_line_load_bodyweight, line.setCount, line.reps, stringResource(R.string.bodyweight_label))

@Composable
internal fun planWorkingRpeText(line: PlanLine.Working): String =
    line.rpe?.let { stringResource(R.string.plan_rpe, formatWeight(it)) }.orEmpty()
