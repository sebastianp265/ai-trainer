package com.github.sebastianp265.aitrainer.feature.coach.impl

import com.github.sebastianp265.aitrainer.integration.plan.PlannedSet

internal sealed interface PlanChange {
    data class Added(val exerciseKey: String) : PlanChange

    data class Removed(val exerciseKey: String, val setCount: Int) : PlanChange

    data class SetsChanged(val exerciseKey: String, val from: Int, val to: Int) : PlanChange

    data class WeightChanged(val exerciseKey: String, val deltaKg: Double) : PlanChange
}

internal fun planChanges(current: List<PlannedSet>, proposed: List<PlannedSet>): List<PlanChange> {
    val before = current.filter { !it.isWarmup }.groupBy { it.exerciseKey }
    val after = proposed.filter { !it.isWarmup }.groupBy { it.exerciseKey }
    val removed = (before.keys - after.keys).map { PlanChange.Removed(it, before.getValue(it).size) }
    val added = (after.keys - before.keys).map { PlanChange.Added(it) }
    val changed = (after.keys intersect before.keys).flatMap { key ->
        val old = before.getValue(key)
        val new = after.getValue(key)
        buildList {
            if (old.size != new.size) add(PlanChange.SetsChanged(key, old.size, new.size))
            val weightDelta = new.map { it.weightKg }.average() - old.map { it.weightKg }.average()
            if (kotlin.math.abs(weightDelta) >= MIN_WEIGHT_CHANGE_KG) add(PlanChange.WeightChanged(key, weightDelta))
        }
    }
    return removed + changed + added
}

private const val MIN_WEIGHT_CHANGE_KG = 0.05
