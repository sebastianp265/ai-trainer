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
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeClient
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeContent
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeMessage
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeResponse
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeResult
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeRole
import com.github.sebastianp265.aitrainer.integration.coach.CoachActivity
import com.github.sebastianp265.aitrainer.integration.coach.CoachMessage
import com.github.sebastianp265.aitrainer.integration.coach.CoachNote
import com.github.sebastianp265.aitrainer.integration.coach.CoachProposal
import com.github.sebastianp265.aitrainer.integration.coach.CoachRole
import com.github.sebastianp265.aitrainer.integration.coach.CoachRun
import com.github.sebastianp265.aitrainer.integration.coach.CoachService
import com.github.sebastianp265.aitrainer.integration.coach.CoachTopic
import com.github.sebastianp265.aitrainer.integration.coach.CoachUsage
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
import com.github.sebastianp265.aitrainer.shared.coach.CoachProfileEntity
import com.github.sebastianp265.aitrainer.shared.coach.CoachProposalEntity
import com.github.sebastianp265.aitrainer.shared.coach.CoachTopicColumns
import com.github.sebastianp265.aitrainer.shared.coach.CoachUsageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration

internal class DefaultCoachService(
    private val claudeClient: () -> ClaudeClient,
    private val settings: SettingsService,
    private val dao: CoachDao,
    private val toolbox: CoachToolbox,
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
        const val MAX_TOOL_ROUNDS = 8
        const val SUMMARY_DAYS = 28
        const val SUMMARY_WORKOUTS = 5
    }

    private val activity = MutableStateFlow<CoachActivity>(CoachActivity.Idle)
    private val lastRun = MutableStateFlow<CoachRun?>(null)

    override fun observeActivity(): Flow<CoachActivity> = activity

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
        val plannedUndone = plans.observeUpcoming().first().count { it.date != null && it.date in weekStart..weekEnd }
        val fromPlans = sessions.count { it.planId != null }
        val reports = sessions.mapNotNull { workouts.observeWorkoutReport(it.id).first() }
        val summary = reviewSummary(
            weekStart = weekStart,
            workouts = sessions.size,
            plannedWorkouts = (fromPlans + plannedUndone).takeIf { it > 0 },
            records = reports.sumOf { it.records.size },
            thisWeek = health.observeDays(weekStart, weekEnd).first(),
            weekBefore = health.observeDays(weekStart.minus(DatePeriod(days = 7)), weekStart.minus(DatePeriod(days = 1))).first(),
        )
        if (!summary.hasAnything()) {
            settings.setLastReviewedWeek(weekStart)
            return
        }
        if (converse(transient = summary.asPrompt(), review = summary)) settings.setLastReviewedWeek(weekStart)
    }

    private suspend fun converse(transient: String? = null, review: WeeklyReviewSummary? = null): Boolean {
        activity.value = CoachActivity.Working(tool = null)
        val startedAt = clock.now()
        var run = CoachRun(rounds = 0, inputTokens = 0, cacheReadTokens = 0, cacheWriteTokens = 0, outputTokens = 0, duration = Duration.ZERO, stopReason = null)
        val context = coachContext()
        val request = buildCoachRequest(context, transient)
        val checked = mutableListOf<String>()
        val proposalIds = mutableListOf<Long>()
        val conversation = request.messages.toMutableList()
        repeat(MAX_TOOL_ROUNDS) {
            when (val result = claudeClient().send(request.copy(messages = conversation.toList()))) {
                is ClaudeResult.Failure -> {
                    activity.value = CoachActivity.Failed(result.kind, result.message)
                    return false
                }
                is ClaudeResult.Success -> {
                    val response = result.response
                    addUsage(response.inputTokens + response.cacheReadTokens + response.cacheWriteTokens, response.outputTokens)
                    run = run.after(response, clock.now() - startedAt)
                    lastRun.value = run
                    val toolUses = result.response.content.filterIsInstance<ClaudeContent.ToolUse>()
                    if (toolUses.isEmpty()) {
                        finish(result.response.content.filterIsInstance<ClaudeContent.Text>().joinToString("\n") { it.text }, checked, proposalIds, review)
                        return true
                    }
                    conversation += ClaudeMessage(ClaudeRole.ASSISTANT, result.response.content)
                    val results = toolUses.map { use ->
                        activity.value = CoachActivity.Working(use.name)
                        val run = toolbox.run(use.name, use.inputJson)
                        run.checked?.let { if (it !in checked) checked += it }
                        run.proposalId?.let { proposalIds += it }
                        ClaudeContent.ToolResult(use.id, run.text, run.isError)
                    }
                    conversation += ClaudeMessage(ClaudeRole.USER, results)
                }
            }
        }
        finish("", checked, proposalIds, review)
        return true
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

    private suspend fun coachContext(): CoachContext {
        val today = clock.todayIn(TimeZone.currentSystemDefault())
        val sessions = workouts.observeFinishedSessions(today.minus(DatePeriod(days = SUMMARY_DAYS)), today).first().take(SUMMARY_WORKOUTS)
        return CoachContext(
            model = settings.observeClaudeModel().first(),
            tuning = settings.observeClaudeTuning().first(),
            today = today,
            profile = dao.profile().associate { it.field to it.value },
            notes = dao.notes(),
            targets = volume.observeTargets().first(),
            nutritionTarget = nutrition.observeTarget().first(),
            recentWorkouts = sessions.map { "${it.date} ${it.planName ?: "Workout"}: ${it.exerciseCount} exercises, ${it.setCount} working sets" },
            healthDays = health.observeDays(today.minus(DatePeriod(days = SUMMARY_DAYS)), today).first(),
            history = dao.lastMessages(HISTORY_LIMIT),
            tools = toolbox.definitions,
        )
    }

    private suspend fun addUsage(inputTokens: Int, outputTokens: Int) {
        val month = currentMonth()
        val current = dao.usage(month) ?: CoachUsageEntity(month, 0, 0, 0)
        dao.upsertUsage(current.copy(messages = current.messages + 1, inputTokens = current.inputTokens + inputTokens, outputTokens = current.outputTokens + outputTokens))
    }

    private fun currentMonth(): String {
        val date = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return "${date.year}-${(date.month.ordinal + 1).toString().padStart(2, '0')}"
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
            val draft = PlanDraft(plan.name, plan.date, plan.sets, coachNote = note)
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
            plans.updatePlan(checkNotNull(plan.planId), PlanDraft(plan.name, plan.date?.let(LocalDate::parse), plan.toProposedPlan().sets, note))
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

    override fun observeProfile(): Flow<Map<String, String>> = dao.observeProfile().map { list -> list.associate { it.field to it.value } }

    override suspend fun setProfileField(field: String, value: String) {
        require(field in CoachService.PROFILE_FIELDS)
        if (value.isBlank()) dao.deleteProfileField(field) else dao.upsertProfile(CoachProfileEntity(field, value.trim()))
    }

    override fun observeNotes(): Flow<List<CoachNote>> = dao.observeNotes().map { list -> list.map { CoachNote(it.id, it.text, it.createdAt) } }

    override suspend fun updateNote(id: Long, text: String) {
        dao.updateNote(id, text.trim())
    }

    override suspend fun deleteNote(id: Long) {
        dao.deleteNote(id)
    }

    override fun observeLastRun(): Flow<CoachRun?> = lastRun

    override fun observeUsageThisMonth(): Flow<CoachUsage> =
        dao.observeUsage(currentMonth()).map { it?.let { u -> CoachUsage(u.messages, u.inputTokens, u.outputTokens) } ?: CoachUsage(0, 0, 0) }
}

private fun CoachRun.after(response: ClaudeResponse, elapsed: Duration) = CoachRun(
    rounds = rounds + 1,
    inputTokens = inputTokens + response.inputTokens,
    cacheReadTokens = cacheReadTokens + response.cacheReadTokens,
    cacheWriteTokens = cacheWriteTokens + response.cacheWriteTokens,
    outputTokens = outputTokens + response.outputTokens,
    duration = elapsed,
    stopReason = response.stopReason,
)

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
    name == accepted.name.trim() && date == accepted.date?.let(LocalDate::parse) && sets == accepted.sets.map { it.toPlannedSet() }

private fun ProposalPayload.matchesSetsOf(current: List<SetEntry>) = exerciseKeys.all { key ->
    current.filter { it.exerciseKey == key }.sortedBy { it.positionInSession }.map { PlannedSet(it.exerciseKey, it.reps, it.weightKg, it.rpe, it.isWarmup) } ==
        workoutSets.filter { it.exerciseKey == key }.map { it.toPlannedSet() }
}

private fun Plan.toPayload() = PlanPayload(
    planId = id,
    name = name,
    date = date?.toString(),
    sets = sets.map { it.toPayload() },
)
