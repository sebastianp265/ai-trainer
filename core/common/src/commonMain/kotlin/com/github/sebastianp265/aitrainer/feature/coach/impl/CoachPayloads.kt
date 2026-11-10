package com.github.sebastianp265.aitrainer.feature.coach.impl

import com.github.sebastianp265.aitrainer.integration.plan.PlannedSet
import com.github.sebastianp265.aitrainer.integration.coach.ProposedPlan
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal val coachJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

@Serializable
internal data class PlannedSetPayload(
    val exerciseKey: String,
    val reps: Int,
    val weightKg: Double = 0.0,
    val rpe: Double? = null,
    val warmup: Boolean = false,
)

@Serializable
internal data class PlanPayload(
    val planId: Long? = null,
    val name: String,
    val date: String? = null,
    val sets: List<PlannedSetPayload>,
)

@Serializable
internal data class ProposalPayload(
    val plans: List<PlanPayload> = emptyList(),
    val groupKey: String? = null,
    val minSets: Int? = null,
    val maxSets: Int? = null,
    val reason: String? = null,
    val previousMinSets: Int? = null,
    val previousMaxSets: Int? = null,
)

internal fun PlanPayload.toProposedPlan() = ProposedPlan(
    planId = planId,
    name = name,
    date = date?.let(LocalDate::parse),
    sets = sets.map { PlannedSet(it.exerciseKey, it.reps, it.weightKg, it.rpe, it.warmup) },
)
