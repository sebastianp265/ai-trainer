package com.github.sebastianp265.aitrainer.feature.progress.impl

import com.github.sebastianp265.aitrainer.integration.exercise.MuscleGroup
import com.github.sebastianp265.aitrainer.integration.volume.WeeklySets
import com.github.sebastianp265.aitrainer.integration.volume.WeeklyTarget

internal data class VolumeRow(
    val group: MuscleGroup,
    val done: Double,
    val minSets: Int,
    val maxSets: Int,
)

internal enum class TargetStatus { BELOW, IN_TARGET, ABOVE }

internal fun VolumeRow.status(): TargetStatus = when {
    done < minSets -> TargetStatus.BELOW
    done > maxSets -> TargetStatus.ABOVE
    else -> TargetStatus.IN_TARGET
}

internal fun averageSetsPerWeek(weeks: List<WeeklySets>, rangeDays: Int): Map<String, Double> {
    val weekCount = rangeDays / DAYS_PER_WEEK.toDouble()
    return weeks.flatMap { it.setsByGroup.entries }
        .groupBy({ it.key }, { it.value })
        .mapValues { it.value.sum() / weekCount }
}

internal fun volumeRows(
    groups: List<MuscleGroup>,
    targets: List<WeeklyTarget>,
    done: Map<String, Double>,
): List<VolumeRow> {
    val targetsByGroup = targets.associateBy { it.groupKey }
    return groups.mapNotNull { group ->
        val target = targetsByGroup[group.key] ?: return@mapNotNull null
        VolumeRow(group, done[group.key] ?: 0.0, target.minSets, target.maxSets)
    }.sortedByDescending { it.done }
}

private const val DAYS_PER_WEEK = 7
