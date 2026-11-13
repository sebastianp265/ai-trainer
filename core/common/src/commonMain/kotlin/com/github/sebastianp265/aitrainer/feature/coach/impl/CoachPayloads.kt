package com.github.sebastianp265.aitrainer.feature.coach.impl

import com.github.sebastianp265.aitrainer.integration.plan.PlannedSet
import com.github.sebastianp265.aitrainer.integration.coach.ProposedPlan
import com.github.sebastianp265.aitrainer.integration.volume.TargetSource
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
    val sessionId: Long? = null,
    val exerciseKeys: List<String> = emptyList(),
    val workoutSets: List<PlannedSetPayload> = emptyList(),
    val newExercise: NewExercisePayload? = null,
    val nutrition: NutritionPayload? = null,
)

@Serializable
internal data class NutritionPayload(val kcal: Int?, val proteinG: Int?, val previousKcal: Int?, val previousProteinG: Int?, val previousReason: String?)

@Serializable
internal data class NewExercisePayload(val name: String, val primaryGroupKey: String, val secondaryGroupKeys: List<String> = emptyList())

@Serializable
internal data class UndoPayload(
    val createdPlans: List<CreatedPlan> = emptyList(),
    val previousPlans: List<PreviousPlan> = emptyList(),
    val previousTarget: PreviousTarget? = null,
    val previousWorkoutSets: List<PlannedSetPayload> = emptyList(),
    val wasRunningWorkout: Boolean = false,
)

@Serializable
internal data class CreatedPlan(val planId: Long, val proposedIndex: Int)

@Serializable
internal data class PreviousPlan(val plan: PlanPayload, val coachNote: String?)

@Serializable
internal data class PreviousTarget(
    val groupKey: String,
    val minSets: Int,
    val maxSets: Int,
    val source: TargetSource,
    val reason: String?,
)

internal fun PlanPayload.toProposedPlan() = ProposedPlan(
    planId = planId,
    name = name,
    sets = sets.map { it.toPlannedSet() },
)

internal fun PlannedSetPayload.toPlannedSet() = PlannedSet(exerciseKey, reps, weightKg, rpe, warmup)

internal fun PlannedSet.toPayload() = PlannedSetPayload(exerciseKey, reps, weightKg, rpe, isWarmup)
