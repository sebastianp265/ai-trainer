package com.github.sebastianp265.aitrainer.domain.stats

import com.github.sebastianp265.aitrainer.domain.model.LoggedSet
import com.github.sebastianp265.aitrainer.domain.model.MuscleGroup

data class MuscleGroupVolume(val muscleGroup: MuscleGroup, val sets: Double)

private const val PRIMARY_MUSCLE_SET_SHARE = 1.0
private const val SECONDARY_MUSCLE_SET_SHARE = 0.5

fun setsPerMuscleGroup(sets: List<LoggedSet>): List<MuscleGroupVolume> {
    val totals = mutableMapOf<MuscleGroup, Double>()
    sets.filterNot { it.isWarmup }.forEach { set ->
        set.primaryMuscles.forEach { totals.merge(it, PRIMARY_MUSCLE_SET_SHARE, Double::plus) }
        set.secondaryMuscles.forEach { totals.merge(it, SECONDARY_MUSCLE_SET_SHARE, Double::plus) }
    }
    return totals.map { (muscleGroup, total) -> MuscleGroupVolume(muscleGroup, total) }
        .sortedWith(compareByDescending<MuscleGroupVolume> { it.sets }.thenBy { it.muscleGroup.ordinal })
}
