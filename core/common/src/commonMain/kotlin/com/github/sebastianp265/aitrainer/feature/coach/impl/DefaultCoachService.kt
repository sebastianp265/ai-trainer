package com.github.sebastianp265.aitrainer.feature.coach.impl

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.github.sebastianp265.aitrainer.integration.plan.PlannedSet
import com.github.sebastianp265.aitrainer.integration.workout.SetEntry
import com.github.sebastianp265.aitrainer.integration.coach.ProposedNutritionTarget
import com.github.sebastianp265.aitrainer.integration.nutrition.NutritionTarget
import com.github.sebastianp265.aitrainer.integration.nutrition.NutritionService
import com.github.sebastianp265.aitrainer.integration.exercise.NewExercise
import com.github.sebastianp265.aitrainer.integration.exercise.ExerciseService
import com.github.sebastianp265.aitrainer.integration.coach.CoachActivity
import com.github.sebastianp265.aitrainer.integration.coach.CoachReadiness
import com.github.sebastianp265.aitrainer.integration.coach.CoachReply
import com.github.sebastianp265.aitrainer.integration.coach.CoachRequest
import com.github.sebastianp265.aitrainer.integration.coach.CoachResponder
import com.github.sebastianp265.aitrainer.integration.coach.CoachTurn
import com.github.sebastianp265.aitrainer.integration.coach.ProposalDesk
import com.github.sebastianp265.aitrainer.integration.coach.ProposalDraft
import com.github.sebastianp265.aitrainer.integration.coach.ProposalOutcome
import com.github.sebastianp265.aitrainer.integration.coach.CoachMessage
import com.github.sebastianp265.aitrainer.integration.coach.CoachProposal
import com.github.sebastianp265.aitrainer.integration.coach.CoachRole
import com.github.sebastianp265.aitrainer.integration.coach.CoachService
import com.github.sebastianp265.aitrainer.integration.coach.CoachTopic
import com.github.sebastianp265.aitrainer.integration.coach.ProposalState
import com.github.sebastianp265.aitrainer.integration.coach.ProposalType
import com.github.sebastianp265.aitrainer.integration.coach.ProposedTarget
import com.github.sebastianp265.aitrainer.integration.coach.ProposedWorkoutChange
import com.github.sebastianp265.aitrainer.integration.coach.WeeklyReviewSummary
import com.github.sebastianp265.aitrainer.integration.health.HealthService
import com.github.sebastianp265.aitrainer.integration.plan.Plan
import com.github.sebastianp265.aitrainer.integration.plan.PlanAuthor
import com.github.sebastianp265.aitrainer.integration.plan.PlanDraft
import com.github.sebastianp265.aitrainer.integration.plan.PlanService
import com.github.sebastianp265.aitrainer.integration.settings.SettingsService
import com.github.sebastianp265.aitrainer.integration.volume.TargetSource
import com.github.sebastianp265.aitrainer.integration.volume.VolumeService
import com.github.sebastianp265.aitrainer.integration.volume.WeeklyTarget
import com.github.sebastianp265.aitrainer.integration.workout.WorkoutService
import com.github.sebastianp265.aitrainer.shared.coach.CoachDao
import com.github.sebastianp265.aitrainer.shared.coach.CoachMessageEntity
import com.github.sebastianp265.aitrainer.shared.coach.CoachProposalEntity
import com.github.sebastianp265.aitrainer.shared.coach.CoachTopicColumns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

internal class DefaultCoachService(
    private val responder: CoachResponder,
    private val settings: SettingsService,
    private val dao: CoachDao,
    private val desk: ProposalDesk,
    private val workouts: WorkoutService,
    private val health: HealthService,
    private val plans: PlanService,
    private val volume: VolumeService,
    private val exercises: ExerciseService,
    private val nutrition: NutritionService,
    private val clock: Clock,
) : CoachService {

    private companion object {
        const val HISTORY_LIMIT = 40
    }

    private val activity = MutableStateFlow<CoachActivity>(CoachActivity.Idle)

    override fun observeActivity(): Flow<CoachActivity> = activity

    override fun observeReadiness(): Flow<CoachReadiness> = responder.observeReadiness()

    override fun observeMessages(): Flow<List<CoachMessage>> =
        combine(dao.observeMessages(), dao.observeProposals(), observeUndoContext()) { messages, proposals, undoContext ->
            val proposalsByMessage = proposals.mapNotNull { p -> p.messageId?.let { it to p.toModel(undoContext) } }.groupBy({ it.first }, { it.second })
            messages.map { it.toModel(proposalsByMessage[it.id].orEmpty()) }
        }

    override suspend fun send(text: String, topic: CoachTopic?) {
        val columns = topic?.let { CoachTopicColumns(it.kind, it.id, it.label) }
        dao.insertMessage(CoachMessageEntity(role = CoachRole.USER, text = text.trim(), createdAt = clock.now(), checked = null, review = null, topic = columns))
        converse()
    }

    override suspend fun retry() {
        converse()
    }

    override suspend fun deleteMessage(id: Long) = dao.deleteMessage(id)

    override suspend fun reviewLastWeekIfDue() {
        val today = clock.todayIn(TimeZone.currentSystemDefault())
        val weekStart = weekBeforeToday(today)
        val lastReviewed = settings.observeLastReviewedWeek().first()
        if (lastReviewed != null && lastReviewed >= weekStart) return
        val weekEnd = weekStart.plus(DatePeriod(days = 6))
        val sessions = workouts.observeFinishedSessions(weekStart, weekEnd).first()
        val reports = sessions.mapNotNull { workouts.observeWorkoutReport(it.id).first() }
        val summary = reviewSummary(
            weekStart = weekStart,
            workouts = sessions.size,
            records = reports.sumOf { it.records.size },
            thisWeek = health.observeDays(weekStart, weekEnd).first(),
            weekBefore = health.observeDays(weekStart.minus(DatePeriod(days = 7)), weekStart.minus(DatePeriod(days = 1))).first(),
        )
        if (!summary.hasAnything()) {
            settings.setLastReviewedWeek(weekStart)
            return
        }
        if (converse(review = summary)) settings.setLastReviewedWeek(weekStart)
    }

    private suspend fun converse(review: WeeklyReviewSummary? = null): Boolean {
        activity.value = CoachActivity.Working(step = null)
        val proposalIds = mutableListOf<Long>()
        val turns = dao.lastMessages(HISTORY_LIMIT).sortedBy { it.id }.map { CoachTurn(it.role, it.text, it.topic?.let { topic -> CoachTopic(topic.kind, topic.id, topic.label) }) }
        val trackingDesk = object : ProposalDesk {
            override suspend fun propose(draft: ProposalDraft): ProposalOutcome =
                desk.propose(draft).also { if (it is ProposalOutcome.Saved) proposalIds += it.proposalId }
        }
        return when (val reply = responder.reply(CoachRequest(turns, review), trackingDesk) { step -> activity.value = CoachActivity.Working(step) }) {
            is CoachReply.Failure -> {
                activity.value = CoachActivity.Failed(reply.kind, reply.message)
                false
            }
            is CoachReply.Answer -> {
                finish(reply.text, reply.checked, proposalIds, review)
                true
            }
        }
    }

    private suspend fun finish(text: String, checked: List<String>, proposalIds: List<Long>, review: WeeklyReviewSummary?) {
        val messageId = dao.insertMessage(
            CoachMessageEntity(
                role = CoachRole.COACH, text = text, createdAt = clock.now(), checked = checked.takeIf { it.isNotEmpty() }?.joinToString(","),
                review = review?.let { coachJson.encodeToString(ReviewPayload.serializer(), it.toPayload()) },
            ),
        )
        if (proposalIds.isNotEmpty()) dao.attachProposals(messageId, proposalIds)
        activity.value = CoachActivity.Idle
    }

    override fun observePendingProposals(): Flow<List<CoachProposal>> =
        combine(dao.observeProposals(), observeUndoContext()) { list, undoContext ->
            list.filter { it.state == ProposalState.PENDING }.map { it.toModel(undoContext) }
        }

    override fun observeProposal(id: Long): Flow<CoachProposal?> =
        combine(dao.observeProposal(id), observeUndoContext()) { proposal, undoContext -> proposal?.toModel(undoContext) }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeUndoContext(): Flow<UndoContext> {
        val activeWithSets = workouts.observeActiveSession().flatMapLatest { session ->
            if (session == null) flowOf(null to emptyList()) else workouts.observeSets(session.id).map { session to it }
        }
        return combine(plans.observeUpcoming(), activeWithSets, volume.observeTargets(), nutrition.observeTarget()) { upcoming, (active, activeSets), targets, nutritionTarget ->
            UndoContext(
                upcomingPlans = upcoming.associateBy { it.id },
                activeSessionId = active?.id,
                activePlanId = active?.planId,
                activeSets = activeSets,
                targets = targets.associateBy { it.groupKey },
                nutrition = nutritionTarget,
            )
        }
    }

    override suspend fun accept(proposalId: Long): Boolean {
        val entity = dao.getProposal(proposalId) ?: return false
        if (entity.state != ProposalState.PENDING) return false
        val payload = coachJson.decodeFromString(ProposalPayload.serializer(), entity.payload)
        val undo = when (entity.type) {
            ProposalType.NEW_PLAN, ProposalType.CHANGE_PLAN, ProposalType.MULTI_PLAN -> acceptPlans(payload, entity.summary)
            ProposalType.TARGET -> acceptTarget(payload)
            ProposalType.WORKOUT_CHANGE -> acceptWorkoutChange(payload) ?: return false
            ProposalType.NUTRITION_TARGET -> {
                val target = checkNotNull(payload.nutrition)
                nutrition.setTarget(target.kcal, target.proteinG, entity.summary)
                UndoPayload()
            }
            ProposalType.NEW_EXERCISE -> {
                val exercise = checkNotNull(payload.newExercise)
                exercises.createExercise(NewExercise(exercise.name, exercise.primaryGroupKey, exercise.secondaryGroupKeys))
                UndoPayload()
            }
        }
        dao.markAccepted(proposalId, coachJson.encodeToString(UndoPayload.serializer(), undo))
        return true
    }

    private suspend fun acceptPlans(payload: ProposalPayload, note: String): UndoPayload {
        val created = mutableListOf<CreatedPlan>()
        val previous = mutableListOf<PreviousPlan>()
        payload.plans.map { it.toProposedPlan() }.forEachIndexed { index, plan ->
            val draft = PlanDraft(plan.name, plan.sets, coachNote = note)
            val existing = plan.planId?.let { plans.observePlan(it).first() }
            if (existing == null) {
                created += CreatedPlan(plans.createPlan(draft, PlanAuthor.COACH), index)
            } else {
                previous += PreviousPlan(existing.toPayload(), existing.coachNote)
                plans.updatePlan(existing.id, draft)
            }
        }
        return UndoPayload(createdPlans = created, previousPlans = previous)
    }

    private suspend fun acceptTarget(payload: ProposalPayload): UndoPayload {
        val groupKey = checkNotNull(payload.groupKey)
        val before = volume.observeTargets().first().first { it.groupKey == groupKey }
        volume.setTarget(groupKey, checkNotNull(payload.minSets), checkNotNull(payload.maxSets), TargetSource.COACH, payload.reason)
        return UndoPayload(previousTarget = PreviousTarget(groupKey, before.minSets, before.maxSets, before.source, before.reason))
    }

    private suspend fun acceptWorkoutChange(payload: ProposalPayload): UndoPayload? {
        val sessionId = checkNotNull(payload.sessionId)
        val session = workouts.findSession(sessionId) ?: return null
        val before = workouts.observeSets(sessionId).first().filter { it.exerciseKey in payload.exerciseKeys }
        workouts.replaceExerciseSets(sessionId, payload.exerciseKeys, payload.workoutSets.map { it.toPlannedSet() })
        return UndoPayload(
            previousWorkoutSets = before.map { PlannedSetPayload(it.exerciseKey, it.reps, it.weightKg, it.rpe, it.isWarmup) },
            wasRunningWorkout = session.finishedAt == null,
        )
    }

    override suspend fun undo(proposalId: Long): Boolean {
        val entity = dao.getProposal(proposalId) ?: return false
        if (!entity.toModel(observeUndoContext().first()).isUndoable) return false
        val undo = coachJson.decodeFromString(UndoPayload.serializer(), checkNotNull(entity.undo))
        undo.createdPlans.forEach { plans.deletePlan(it.planId) }
        undo.previousPlans.forEach { (plan, note) ->
            plans.updatePlan(checkNotNull(plan.planId), PlanDraft(plan.name, plan.toProposedPlan().sets, note))
        }
        undo.previousTarget?.let { volume.setTarget(it.groupKey, it.minSets, it.maxSets, it.source, it.reason) }
        if (entity.type == ProposalType.NUTRITION_TARGET) {
            val target = checkNotNull(coachJson.decodeFromString(ProposalPayload.serializer(), entity.payload).nutrition)
            nutrition.setTarget(target.previousKcal, target.previousProteinG, target.previousReason)
        }
        if (entity.type == ProposalType.WORKOUT_CHANGE) {
            val payload = coachJson.decodeFromString(ProposalPayload.serializer(), entity.payload)
            val sessionId = checkNotNull(payload.sessionId)
            if (workouts.findSession(sessionId) == null) return false
            if (!payload.matchesSetsOf(workouts.observeSets(sessionId).first())) return false
            workouts.replaceExerciseSets(sessionId, payload.exerciseKeys, undo.previousWorkoutSets.map { it.toPlannedSet() })
        }
        dao.setProposalState(proposalId, ProposalState.UNDONE)
        return true
    }

    override suspend fun acceptAll(proposalIds: List<Long>) {
        proposalIds.forEach { accept(it) }
    }
}

private fun CoachMessageEntity.toModel(proposals: List<CoachProposal>) =
    CoachMessage(
        id, role, text, createdAt, checked?.split(',').orEmpty(), proposals,
        review?.let { coachJson.decodeFromString(ReviewPayload.serializer(), it).toSummary() },
        topic?.let { CoachTopic(it.kind, it.id, it.label) },
    )

private class UndoContext(
    val upcomingPlans: Map<Long, Plan>,
    val activeSessionId: Long?,
    val activePlanId: Long?,
    val activeSets: List<SetEntry>,
    val targets: Map<String, WeeklyTarget>,
    val nutrition: NutritionTarget?,
)

private fun CoachProposalEntity.toModel(undoContext: UndoContext): CoachProposal {
    val payload = coachJson.decodeFromString(ProposalPayload.serializer(), payload)
    val target = payload.groupKey?.let {
        ProposedTarget(it, payload.minSets ?: 0, payload.maxSets ?: 0, payload.reason, payload.previousMinSets ?: 0, payload.previousMaxSets ?: 0)
    }
    val workoutChange = payload.sessionId?.let { ProposedWorkoutChange(it, payload.exerciseKeys, payload.workoutSets.map { set -> set.toPlannedSet() }) }
    val newExercise = payload.newExercise?.let { NewExercise(it.name, it.primaryGroupKey, it.secondaryGroupKeys) }
    val nutrition = payload.nutrition?.let { ProposedNutritionTarget(it.kcal, it.proteinG, it.previousKcal, it.previousProteinG, payload.reason) }
    return CoachProposal(
        id, type, state, summary, payload.plans.map { it.toProposedPlan() }, target, workoutChange, newExercise, nutrition, createdAt, isUndoable(payload, undoContext),
    )
}

private fun CoachProposalEntity.isUndoable(payload: ProposalPayload, context: UndoContext): Boolean {
    if (state != ProposalState.ACCEPTED || type == ProposalType.NEW_EXERCISE) return false
    val undo = this.undo?.let { coachJson.decodeFromString(UndoPayload.serializer(), it) } ?: return false
    val acceptedPlans = undo.createdPlans.map { it.planId to payload.plans[it.proposedIndex] } +
        undo.previousPlans.map { previous -> checkNotNull(previous.plan.planId).let { id -> id to payload.plans.first { it.planId == id } } }
    val plansUntouched = acceptedPlans.all { (id, accepted) -> id != context.activePlanId && context.upcomingPlans[id]?.isAsAccepted(accepted) == true }
    val targetUnchanged = undo.previousTarget?.let { previous ->
        val current = context.targets[previous.groupKey]
        current != null && current.source == TargetSource.COACH && current.minSets == payload.minSets && current.maxSets == payload.maxSets
    } ?: true
    val workoutStillOpen = !undo.wasRunningWorkout || payload.sessionId == context.activeSessionId && payload.matchesSetsOf(context.activeSets)
    val nutritionUnchanged = payload.nutrition?.let { context.nutrition?.kcal == it.kcal && context.nutrition?.proteinG == it.proteinG } ?: true
    return plansUntouched && targetUnchanged && workoutStillOpen && nutritionUnchanged
}

private fun Plan.isAsAccepted(accepted: PlanPayload) =
    name == accepted.name.trim() && sets == accepted.sets.map { it.toPlannedSet() }

private fun ProposalPayload.matchesSetsOf(current: List<SetEntry>) = exerciseKeys.all { key ->
    current.filter { it.exerciseKey == key }.sortedBy { it.positionInSession }.map { PlannedSet(it.exerciseKey, it.reps, it.weightKg, it.rpe, it.isWarmup) } ==
        workoutSets.filter { it.exerciseKey == key }.map { it.toPlannedSet() }
}

private fun Plan.toPayload() = PlanPayload(
    planId = id,
    name = name,
    sets = sets.map { it.toPayload() },
)
