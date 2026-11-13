package com.github.sebastianp265.aitrainer.feature.coach.impl

import com.github.sebastianp265.aitrainer.integration.coach.ProposalDesk
import com.github.sebastianp265.aitrainer.integration.coach.ProposalDraft
import com.github.sebastianp265.aitrainer.integration.coach.ProposalOutcome
import com.github.sebastianp265.aitrainer.integration.coach.ProposalState
import com.github.sebastianp265.aitrainer.integration.coach.ProposalType
import com.github.sebastianp265.aitrainer.integration.coach.ProposedPlan
import com.github.sebastianp265.aitrainer.integration.exercise.ExerciseService
import com.github.sebastianp265.aitrainer.integration.nutrition.NutritionService
import com.github.sebastianp265.aitrainer.integration.plan.PlannedSet
import com.github.sebastianp265.aitrainer.integration.volume.VolumeService
import com.github.sebastianp265.aitrainer.integration.workout.RPE_MAX
import com.github.sebastianp265.aitrainer.integration.workout.RPE_MIN
import com.github.sebastianp265.aitrainer.integration.workout.RPE_STEP
import com.github.sebastianp265.aitrainer.integration.workout.WorkoutService
import com.github.sebastianp265.aitrainer.shared.coach.CoachDao
import com.github.sebastianp265.aitrainer.shared.coach.CoachProposalEntity
import kotlinx.coroutines.flow.first
import kotlin.time.Clock

internal class DefaultProposalDesk(
    private val workouts: WorkoutService,
    private val volume: VolumeService,
    private val exercises: ExerciseService,
    private val nutrition: NutritionService,
    private val dao: CoachDao,
    private val clock: Clock,
) : ProposalDesk {

    override suspend fun propose(draft: ProposalDraft): ProposalOutcome = try {
        ProposalOutcome.Saved(save(draft))
    } catch (e: IllegalArgumentException) {
        ProposalOutcome.Rejected(e.message.orEmpty())
    }

    private suspend fun save(draft: ProposalDraft): Long = when (draft) {
        is ProposalDraft.Plan -> savePlan(draft)
        is ProposalDraft.Plans -> savePlans(draft)
        is ProposalDraft.Target -> saveTarget(draft)
        is ProposalDraft.WorkoutChange -> saveWorkoutChange(draft)
        is ProposalDraft.Exercise -> saveExercise(draft)
        is ProposalDraft.Nutrition -> saveNutrition(draft)
    }

    private suspend fun savePlan(draft: ProposalDraft.Plan): Long {
        val plan = draft.plan
        validate(plan)
        return dao.insertSupersedingProposal(
            entity(
                if (plan.planId == null) ProposalType.NEW_PLAN else ProposalType.CHANGE_PLAN,
                draft.summary,
                ProposalPayload(plans = listOf(plan.toPayload())),
                subjectKey = plan.planId?.let { "plan:$it" },
            ),
        )
    }

    private suspend fun savePlans(draft: ProposalDraft.Plans): Long {
        require(draft.plans.isNotEmpty()) { "plans must not be empty" }
        draft.plans.forEach { validate(it) }
        return dao.insertProposal(entity(ProposalType.MULTI_PLAN, draft.summary, ProposalPayload(plans = draft.plans.map { it.toPayload() }), subjectKey = null))
    }

    private suspend fun saveTarget(draft: ProposalDraft.Target): Long {
        require(draft.minSets in 0..draft.maxSets) { "minSets must be between 0 and maxSets" }
        val current = volume.observeTargets().first().firstOrNull { it.groupKey == draft.groupKey }
            ?: throw IllegalArgumentException("unknown muscle group ${draft.groupKey}")
        val payload = ProposalPayload(
            groupKey = draft.groupKey, minSets = draft.minSets, maxSets = draft.maxSets, reason = draft.reason,
            previousMinSets = current.minSets, previousMaxSets = current.maxSets,
        )
        return dao.insertSupersedingProposal(entity(ProposalType.TARGET, draft.summary, payload, subjectKey = "target:${draft.groupKey}"))
    }

    private suspend fun saveWorkoutChange(draft: ProposalDraft.WorkoutChange): Long {
        val session = (draft.sessionId?.let { workouts.findSession(it) } ?: workouts.observeActiveSession().first())
            ?: throw IllegalArgumentException("no running workout; give the id of a finished workout")
        val exerciseKeys = (draft.sets.map { it.exerciseKey } + draft.removeExerciseKeys).distinct()
        require(exerciseKeys.isNotEmpty()) { "nothing to change" }
        val known = exerciseNames().keys
        exerciseKeys.forEach { require(it in known) { "unknown exerciseKey $it" } }
        draft.sets.forEach(::validate)
        val payload = ProposalPayload(sessionId = session.id, exerciseKeys = exerciseKeys, workoutSets = draft.sets.map { it.toPayload() })
        return dao.insertSupersedingProposal(entity(ProposalType.WORKOUT_CHANGE, draft.summary, payload, subjectKey = "workout:${session.id}"))
    }

    private suspend fun saveExercise(draft: ProposalDraft.Exercise): Long {
        val exercise = draft.exercise
        require(exercise.name.isNotBlank()) { "name is empty" }
        val groups = exercises.observeMuscleGroups().first().map { it.key }
        (exercise.secondaryGroupKeys + exercise.primaryGroupKey).forEach { require(it in groups) { "unknown muscle group $it; use one of ${groups.joinToString()}" } }
        exerciseNames().entries.firstOrNull { it.value.equals(exercise.name.trim(), ignoreCase = true) }?.let {
            throw IllegalArgumentException("this exercise already exists with key ${it.key}")
        }
        val payload = ProposalPayload(newExercise = NewExercisePayload(exercise.name.trim(), exercise.primaryGroupKey, exercise.secondaryGroupKeys))
        return dao.insertSupersedingProposal(entity(ProposalType.NEW_EXERCISE, draft.summary, payload, subjectKey = "exercise:${exercise.name.trim().lowercase()}"))
    }

    private suspend fun saveNutrition(draft: ProposalDraft.Nutrition): Long {
        require(draft.kcal != null || draft.proteinG != null) { "give kcal, proteinG or both" }
        draft.kcal?.let { require(it in 1000..6000) { "kcal must be between 1000 and 6000" } }
        draft.proteinG?.let { require(it in 30..400) { "proteinG must be between 30 and 400" } }
        val current = nutrition.observeTarget().first()
        val payload = ProposalPayload(
            reason = draft.reason,
            nutrition = NutritionPayload(draft.kcal, draft.proteinG, current?.kcal, current?.proteinG, current?.reason),
        )
        return dao.insertSupersedingProposal(entity(ProposalType.NUTRITION_TARGET, draft.summary, payload, subjectKey = "nutrition"))
    }

    private fun entity(type: ProposalType, summary: String, payload: ProposalPayload, subjectKey: String?) = CoachProposalEntity(
        type = type, state = ProposalState.PENDING, summary = summary,
        payload = coachJson.encodeToString(ProposalPayload.serializer(), payload),
        subjectKey = subjectKey, messageId = null, createdAt = clock.now(),
    )

    private suspend fun validate(plan: ProposedPlan) {
        require(plan.name.isNotBlank()) { "plan name is empty" }
        require(plan.sets.isNotEmpty()) { "plan has no sets" }
        val known = exerciseNames().keys
        plan.sets.forEach {
            require(it.exerciseKey in known) { "unknown exerciseKey ${it.exerciseKey}" }
            validate(it)
        }
    }

    private fun validate(set: PlannedSet) {
        require(set.reps > 0) { "reps must be positive" }
        set.rpe?.let { require(isOnRpeScale(it)) { "RPE must be $RPE_SCALE" } }
    }

    private suspend fun exerciseNames(): Map<String, String> =
        exercises.exercises().associate { it.key to (it.namesByLanguage["en"] ?: it.key) }
}

private const val RPE_SCALE = "from $RPE_MIN to $RPE_MAX in steps of $RPE_STEP"

private fun isOnRpeScale(rpe: Double): Boolean = rpe in RPE_MIN..RPE_MAX && (rpe - RPE_MIN) % RPE_STEP == 0.0

private fun ProposedPlan.toPayload() = PlanPayload(planId, name, sets.map { it.toPayload() })
