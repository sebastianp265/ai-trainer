package com.github.sebastianp265.aitrainer.feature.workout.impl

import com.github.sebastianp265.aitrainer.integration.workout.BestSet
import com.github.sebastianp265.aitrainer.integration.workout.ExerciseHistory
import com.github.sebastianp265.aitrainer.integration.workout.ExerciseSessionEntry
import com.github.sebastianp265.aitrainer.integration.workout.SetEntry
import com.github.sebastianp265.aitrainer.shared.workout.HistorySetRow
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

private const val CHANGE_WINDOW_MONTHS = 3

internal fun exerciseHistory(exerciseKey: String, rows: List<HistorySetRow>, today: LocalDate): ExerciseHistory {
    val bySession = rows.groupBy { it.sessionId }.values.toList()
    val oldestFirst = bySession.mapIndexed { index, sets ->
        val earlierBest = bySession.take(index).mapNotNull { sessionOneRepMax(it) }.maxOrNull()
        val estimate = sessionOneRepMax(sets)
        ExerciseSessionEntry(
            sessionId = sets.first().sessionId,
            date = sets.first().date,
            planName = sets.first().planName,
            groups = lastTimeGroups(sets.map { it.toSetEntry(exerciseKey) }),
            estimatedOneRepMaxKg = estimate,
            averageRpe = sets.mapNotNull { it.rpe }.takeIf { it.isNotEmpty() }?.average(),
            isRecord = estimate != null && earlierBest != null && estimate > earlierBest,
        )
    }
    val estimates = oldestFirst.filter { it.estimatedOneRepMaxKg != null }
    val windowStart = today.minus(DatePeriod(months = CHANGE_WINDOW_MONTHS))
    val inWindow = estimates.filter { it.date >= windowStart }
    return ExerciseHistory(
        exerciseKey = exerciseKey,
        sessionCount = oldestFirst.size,
        estimatedOneRepMaxKg = estimates.lastOrNull()?.estimatedOneRepMaxKg,
        bestSet = rows.filter { isEstimable(it.weightKg, it.reps) }.maxByOrNull { estimatedOneRepMax(it.weightKg, it.reps) }
            ?.let { BestSet(it.weightKg, it.reps) },
        oneRepMaxChangeKg = if (inWindow.size >= 2) inWindow.last().estimatedOneRepMaxKg!! - inWindow.first().estimatedOneRepMaxKg!! else null,
        sessions = oldestFirst.reversed(),
    )
}

private fun sessionOneRepMax(sets: List<HistorySetRow>): Double? =
    sets.filter { isEstimable(it.weightKg, it.reps) }.maxOfOrNull { estimatedOneRepMax(it.weightKg, it.reps) }

private fun HistorySetRow.toSetEntry(exerciseKey: String) =
    SetEntry(0, sessionId, exerciseKey, positionInSession, reps, weightKg, rpe, isWarmup = false)
