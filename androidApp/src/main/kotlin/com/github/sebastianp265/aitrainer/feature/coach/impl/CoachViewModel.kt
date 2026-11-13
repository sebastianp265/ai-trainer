package com.github.sebastianp265.aitrainer.feature.coach.impl

import kotlinx.coroutines.flow.MutableStateFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sebastianp265.aitrainer.integration.coach.CoachActivity
import com.github.sebastianp265.aitrainer.integration.coach.CoachMessage
import com.github.sebastianp265.aitrainer.integration.coach.CoachProposal
import com.github.sebastianp265.aitrainer.integration.coach.CoachReadiness
import com.github.sebastianp265.aitrainer.integration.coach.CoachService
import com.github.sebastianp265.aitrainer.integration.coach.CoachTopic
import com.github.sebastianp265.aitrainer.integration.coach.ProposalType
import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
import com.github.sebastianp265.aitrainer.integration.exercise.ExerciseService
import com.github.sebastianp265.aitrainer.integration.plan.Plan
import com.github.sebastianp265.aitrainer.integration.plan.PlanService
import com.github.sebastianp265.aitrainer.shared.state.WhileUiSubscribed
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class ProposalUi(
    val proposal: CoachProposal,
    val changes: List<PlanChange>,
    val exerciseCount: Int,
    val setCount: Int,
)

internal data class MessageUi(val message: CoachMessage, val proposals: List<ProposalUi>)

internal data class CoachUiState(
    val isLoading: Boolean = true,
    val readiness: CoachReadiness = CoachReadiness.NEEDS_SETUP,
    val messages: List<MessageUi> = emptyList(),
    val activity: CoachActivity = CoachActivity.Idle,
    val exercises: Map<String, Exercise> = emptyMap(),
    val pendingProposalIds: List<Long> = emptyList(),
    val topic: CoachTopic? = null,
)

internal class CoachViewModel(
    private val coachService: CoachService,
    planService: PlanService,
    exerciseService: ExerciseService,
) : ViewModel() {

    private val pendingTopic = MutableStateFlow<CoachTopic?>(null)
    private var topicTaken = false

    private val conversation = combine(
        coachService.observeMessages(),
        coachService.observeActivity(),
        planService.observeUpcoming(),
    ) { messages, activity, plans -> Triple(messages, activity, plans) }

    val uiState: StateFlow<CoachUiState> = combine(
        conversation,
        coachService.observeReadiness(),
        exerciseService.observeExercisesByKey(),
        pendingTopic,
    ) { (messages, activity, plans), readiness, exercises, topic ->
        val plansById = plans.associateBy { it.id }
        CoachUiState(
            isLoading = false,
            readiness = readiness,
            messages = messages.map { message -> MessageUi(message, message.proposals.map { it.toUi(plansById) }) },
            activity = activity,
            exercises = exercises,
            pendingProposalIds = messages.flatMap { it.proposals }.filter { it.state == com.github.sebastianp265.aitrainer.integration.coach.ProposalState.PENDING }.map { it.id },
            topic = topic,
        )
    }.stateIn(viewModelScope, WhileUiSubscribed, CoachUiState())

    fun send(text: String) {
        val topic = pendingTopic.value
        pendingTopic.value = null
        keepRunning { coachService.send(text, topic) }
    }

    fun startWith(topic: CoachTopic?) {
        if (topicTaken) return
        topicTaken = true
        pendingTopic.value = topic
    }

    fun clearTopic() {
        pendingTopic.value = null
    }

    fun retry() = keepRunning { coachService.retry() }

    fun deleteMessage(id: Long) {
        viewModelScope.launch { coachService.deleteMessage(id) }
    }

    fun undo(proposalId: Long) {
        viewModelScope.launch { coachService.undo(proposalId) }
    }

    fun accept(proposalId: Long, onAccepted: () -> Unit = {}) {
        viewModelScope.launch {
            if (coachService.accept(proposalId)) onAccepted()
        }
    }

    private fun keepRunning(block: suspend () -> Unit) {
        viewModelScope.launch { withContext(NonCancellable) { block() } }
    }
}

internal fun CoachProposal.toUi(currentPlans: Map<Long, Plan>): ProposalUi {
    val proposed = plans.flatMap { it.sets }
    val changes = if (type == ProposalType.CHANGE_PLAN) {
        val plan = plans.single()
        plan.planId?.let { currentPlans[it] }?.let { planChanges(it.sets, plan.sets) }.orEmpty()
    } else {
        emptyList()
    }
    return ProposalUi(this, changes, proposed.map { it.exerciseKey }.distinct().size, proposed.count { !it.isWarmup })
}
