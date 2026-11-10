package com.github.sebastianp265.aitrainer.shared.volume

import com.github.sebastianp265.aitrainer.integration.exercise.MuscleGroup
import com.github.sebastianp265.aitrainer.integration.volume.WeeklySets
import com.github.sebastianp265.aitrainer.integration.volume.WeeklyTarget

internal data class VolumeRow(
    val group: MuscleGroup,
    val done: Double,
    val planned: Double,
    val minSets: Int,
    val maxSets: Int,
) {
    val total: Double get() = done + planned
}

internal enum class TargetStatus { BELOW, IN_TARGET, ABOVE }

internal fun VolumeRow.status(): TargetStatus = when {
    total < minSets -> TargetStatus.BELOW
    total > maxSets -> TargetStatus.ABOVE
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
    planned: Map<String, Double> = emptyMap(),
): List<VolumeRow> {
    val targetsByGroup = targets.associateBy { it.groupKey }
    return groups.mapNotNull { group ->
        val target = targetsByGroup[group.key] ?: return@mapNotNull null
        VolumeRow(group, done[group.key] ?: 0.0, planned[group.key] ?: 0.0, target.minSets, target.maxSets)
    }.sortedByDescending { it.total }
}

private const val DAYS_PER_WEEK = 7
