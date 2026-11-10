package com.github.sebastianp265.aitrainer.feature.coach.impl

import com.github.sebastianp265.aitrainer.integration.coach.ClaudeClient
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeContent
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeMessage
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeResult
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeRole
import com.github.sebastianp265.aitrainer.integration.coach.CoachActivity
import com.github.sebastianp265.aitrainer.integration.coach.CoachLogEntry
import com.github.sebastianp265.aitrainer.integration.coach.CoachLogKind
import com.github.sebastianp265.aitrainer.integration.coach.CoachMessage
import com.github.sebastianp265.aitrainer.integration.coach.CoachNote
import com.github.sebastianp265.aitrainer.integration.coach.CoachProposal
import com.github.sebastianp265.aitrainer.integration.coach.CoachRole
import com.github.sebastianp265.aitrainer.integration.coach.CoachService
import com.github.sebastianp265.aitrainer.integration.coach.CoachUsage
import com.github.sebastianp265.aitrainer.integration.coach.ProposalState
import com.github.sebastianp265.aitrainer.integration.coach.ProposalType
import com.github.sebastianp265.aitrainer.integration.coach.ProposedTarget
import com.github.sebastianp265.aitrainer.integration.coach.WeeklyReviewSummary
import com.github.sebastianp265.aitrainer.integration.health.HealthService
import com.github.sebastianp265.aitrainer.integration.plan.PlanAuthor
import com.github.sebastianp265.aitrainer.integration.plan.PlanDraft
import com.github.sebastianp265.aitrainer.integration.plan.PlanService
import com.github.sebastianp265.aitrainer.integration.settings.SettingsService
import com.github.sebastianp265.aitrainer.integration.volume.TargetSource
import com.github.sebastianp265.aitrainer.integration.volume.VolumeService
import com.github.sebastianp265.aitrainer.integration.workout.WorkoutService
import com.github.sebastianp265.aitrainer.shared.coach.CoachDao
import com.github.sebastianp265.aitrainer.shared.coach.CoachLogEntity
import com.github.sebastianp265.aitrainer.shared.coach.CoachMessageEntity
import com.github.sebastianp265.aitrainer.shared.coach.CoachProfileEntity
import com.github.sebastianp265.aitrainer.shared.coach.CoachProposalEntity
import com.github.sebastianp265.aitrainer.shared.coach.CoachUsageEntity
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
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

internal class DefaultCoachService(
    private val claudeClient: () -> ClaudeClient,
    private val settings: SettingsService,
    private val dao: CoachDao,
    private val toolbox: CoachToolbox,
    private val workouts: WorkoutService,
    private val health: HealthService,
    private val plans: PlanService,
    private val volume: VolumeService,
    private val clock: Clock,
) : CoachService {

    private companion object {
        const val HISTORY_LIMIT = 40
        const val MAX_TOOL_ROUNDS = 8
        const val LOG_KEEP = 200
        const val SUMMARY_DAYS = 28
        const val SUMMARY_WORKOUTS = 5
    }

    private val activity = MutableStateFlow<CoachActivity>(CoachActivity.Idle)

    override fun observeActivity(): Flow<CoachActivity> = activity

    override fun observeMessages(): Flow<List<CoachMessage>> =
        combine(dao.observeMessages(), dao.observeProposals()) { messages, proposals ->
            val proposalsByMessage = proposals.mapNotNull { p -> p.messageId?.let { it to p.toModel() } }.groupBy({ it.first }, { it.second })
            messages.map { it.toModel(proposalsByMessage[it.id].orEmpty()) }
        }

    override suspend fun send(text: String) {
        dao.insertMessage(CoachMessageEntity(role = CoachRole.USER, text = text.trim(), createdAt = clock.now(), checked = null, review = null))
        converse()
    }

    override suspend fun retry() {
        converse()
    }

    override suspend fun reviewLastWeekIfDue() {
        val today = clock.todayIn(TimeZone.currentSystemDefault())
        val weekStart = weekBeforeToday(today)
        val lastReviewed = settings.observeLastReviewedWeek().first()
        if (lastReviewed != null && lastReviewed >= weekStart) return
        val weekEnd = weekStart.plus(DatePeriod(days = 6))
        val sessions = workouts.observeFinishedSessions(weekStart, weekEnd).first()
        val plannedUndone = plans.observeUpcoming().first().count { it.date != null && it.date!! in weekStart..weekEnd }
        val fromPlans = sessions.count { it.planId != null }
        val reports = sessions.mapNotNull { workouts.observeWorkoutReport(it.id).first() }
        val summary = reviewSummary(
            weekStart = weekStart,
            workouts = sessions.size,
            plannedWorkouts = (fromPlans + plannedUndone).takeIf { it > 0 },
            externalWorkouts = health.observeWorkouts().first().count { it.startedAt.toLocalDateTime(TimeZone.currentSystemDefault()).date in weekStart..weekEnd },
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
        val context = coachContext()
        val request = buildCoachRequest(context, transient)
        val checked = mutableListOf<String>()
        val proposalIds = mutableListOf<Long>()
        val conversation = request.messages.toMutableList()
        repeat(MAX_TOOL_ROUNDS) {
            log(CoachLogKind.REQUEST, "request ${conversation.size} messages", request.system.take(LOG_DETAIL_LIMIT))
            when (val result = claudeClient().send(request.copy(messages = conversation.toList()))) {
                is ClaudeResult.Failure -> {
                    log(CoachLogKind.ERROR, result.kind.name, result.message)
                    activity.value = CoachActivity.Failed(result.kind, result.message)
                    return false
                }
                is ClaudeResult.Success -> {
                    addUsage(result.response.inputTokens, result.response.outputTokens)
                    val toolUses = result.response.content.filterIsInstance<ClaudeContent.ToolUse>()
                    if (toolUses.isEmpty()) {
                        finish(result.response.content.filterIsInstance<ClaudeContent.Text>().joinToString("\n") { it.text }, checked, proposalIds, review)
                        return true
                    }
                    conversation += ClaudeMessage(ClaudeRole.ASSISTANT, result.response.content)
                    val results = toolUses.map { use ->
                        activity.value = CoachActivity.Working(use.name)
                        val run = toolbox.run(use.name, use.inputJson)
                        log(CoachLogKind.TOOL, use.name, "${use.inputJson}\n=> ${run.text}".take(LOG_DETAIL_LIMIT))
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
            today = today,
            profile = dao.profile().associate { it.field to it.value },
            notes = dao.notes(),
            targets = volume.observeTargets().first(),
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

    private suspend fun log(kind: CoachLogKind, summary: String, details: String) {
        dao.insertLog(CoachLogEntity(at = clock.now(), kind = kind, summary = summary, details = details))
        dao.trimLog(LOG_KEEP)
    }

    override fun observePendingProposals(): Flow<List<CoachProposal>> =
        dao.observeProposals().map { list -> list.filter { it.state == ProposalState.PENDING }.map { it.toModel() } }

    override fun observeProposal(id: Long): Flow<CoachProposal?> = dao.observeProposal(id).map { it?.toModel() }

    override suspend fun accept(proposalId: Long): Boolean {
        val entity = dao.getProposal(proposalId) ?: return false
        if (entity.state != ProposalState.PENDING) return false
        val payload = coachJson.decodeFromString(ProposalPayload.serializer(), entity.payload)
        when (entity.type) {
            ProposalType.NEW_PLAN, ProposalType.CHANGE_PLAN, ProposalType.MULTI_PLAN -> payload.plans.map { it.toProposedPlan() }.forEach { plan ->
                val draft = PlanDraft(plan.name, plan.date, plan.sets)
                if (plan.planId == null) plans.createPlan(draft, PlanAuthor.COACH) else plans.updatePlan(plan.planId, draft)
            }
            ProposalType.TARGET -> volume.setTarget(
                groupKey = checkNotNull(payload.groupKey),
                minSets = checkNotNull(payload.minSets),
                maxSets = checkNotNull(payload.maxSets),
                source = TargetSource.COACH,
                reason = payload.reason,
            )
        }
        dao.setProposalState(proposalId, ProposalState.ACCEPTED)
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

    override fun observeUsageThisMonth(): Flow<CoachUsage> =
        dao.observeUsage(currentMonth()).map { it?.let { u -> CoachUsage(u.messages, u.inputTokens, u.outputTokens) } ?: CoachUsage(0, 0, 0) }

    override fun observeLog(): Flow<List<CoachLogEntry>> =
        dao.observeLog(LOG_KEEP).map { list -> list.map { CoachLogEntry(it.id, it.at, it.kind, it.summary, it.details) } }

    override suspend fun clearLog() = dao.clearLog()
}

private const val LOG_DETAIL_LIMIT = 4000

private fun CoachMessageEntity.toModel(proposals: List<CoachProposal>) =
    CoachMessage(id, role, text, createdAt, checked?.split(',').orEmpty(), proposals, review?.let { coachJson.decodeFromString(ReviewPayload.serializer(), it).toSummary() })

private fun CoachProposalEntity.toModel(): CoachProposal {
    val payload = coachJson.decodeFromString(ProposalPayload.serializer(), payload)
    val target = payload.groupKey?.let {
        ProposedTarget(it, payload.minSets ?: 0, payload.maxSets ?: 0, payload.reason, payload.previousMinSets ?: 0, payload.previousMaxSets ?: 0)
    }
    return CoachProposal(id, type, state, summary, payload.plans.map { it.toProposedPlan() }, target, createdAt)
}
