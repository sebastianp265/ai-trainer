package com.github.sebastianp265.aitrainer.feature.workout.impl

import com.github.sebastianp265.aitrainer.integration.workout.LastTimeGroup
import com.github.sebastianp265.aitrainer.integration.workout.SetEntry

internal fun lastTimeGroups(sets: List<SetEntry>): List<LastTimeGroup> =
    sets.filter { !it.isWarmup }
        .sortedBy { it.positionInSession }
        .fold(mutableListOf<MutableList<SetEntry>>()) { runs, set ->
            val lastRun = runs.lastOrNull()
            if (lastRun != null && lastRun.first().weightKg == set.weightKg && lastRun.first().reps == set.reps) {
                lastRun.add(set)
            } else {
                runs.add(mutableListOf(set))
            }
            runs
        }
        .map { run ->
            val rpes = run.mapNotNull { it.rpe }
            LastTimeGroup(run.size, run.first().reps, run.first().weightKg, rpes.minOrNull(), rpes.maxOrNull())
        }
