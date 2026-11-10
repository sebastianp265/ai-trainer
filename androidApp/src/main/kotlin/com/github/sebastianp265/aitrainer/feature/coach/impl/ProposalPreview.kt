package com.github.sebastianp265.aitrainer.feature.coach.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.integration.coach.CoachProposal
import com.github.sebastianp265.aitrainer.integration.coach.CoachService
import com.github.sebastianp265.aitrainer.integration.coach.ProposalState
import com.github.sebastianp265.aitrainer.integration.coach.ProposalType
import com.github.sebastianp265.aitrainer.integration.coach.ProposedPlan
import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
import com.github.sebastianp265.aitrainer.integration.exercise.ExerciseService
import com.github.sebastianp265.aitrainer.integration.plan.Plan
import com.github.sebastianp265.aitrainer.integration.plan.PlanService
import com.github.sebastianp265.aitrainer.shared.localization.displayName
import com.github.sebastianp265.aitrainer.shared.localization.formatLongWeekdayDate
import com.github.sebastianp265.aitrainer.shared.state.WhileUiSubscribed
import com.github.sebastianp265.aitrainer.shared.workout.PlanLine
import com.github.sebastianp265.aitrainer.shared.workout.planExercises
import com.github.sebastianp265.aitrainer.shared.workout.planNumberText
import com.github.sebastianp265.aitrainer.shared.workout.planWarmupText
import com.github.sebastianp265.aitrainer.shared.workout.planWorkingRpeText
import com.github.sebastianp265.aitrainer.shared.workout.planWorkingText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal data class PreviewUiState(
    val isLoading: Boolean = true,
    val proposal: CoachProposal? = null,
    val currentPlans: Map<Long, Plan> = emptyMap(),
    val exercises: Map<String, Exercise> = emptyMap(),
)

internal class ProposalPreviewViewModel(
    private val coachService: CoachService,
    planService: PlanService,
    exerciseService: ExerciseService,
) : ViewModel() {

    private val proposalId = MutableStateFlow<Long?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<PreviewUiState> = combine(
        proposalId.filterNotNull().flatMapLatest { coachService.observeProposal(it) },
        planService.observeUpcoming(),
        exerciseService.observeExercisesByKey(),
    ) { proposal, plans, exercises -> PreviewUiState(false, proposal, plans.associateBy { it.id }, exercises) }
        .stateIn(viewModelScope, WhileUiSubscribed, PreviewUiState())

    fun show(id: Long) {
        proposalId.value = id
    }

    fun accept(onAccepted: () -> Unit) {
        val id = proposalId.value ?: return
        viewModelScope.launch {
            if (coachService.accept(id)) onAccepted()
        }
    }
}

@Composable
internal fun ProposalPreviewContent(state: PreviewUiState, onAccept: () -> Unit, onReply: () -> Unit) {
    val proposal = state.proposal
    LaunchedEffect(state.isLoading, proposal) { if (!state.isLoading && proposal == null) onReply() }
    Scaffold(
        bottomBar = {
            if (proposal?.state == ProposalState.PENDING) {
                Surface(tonalElevation = 3.dp) {
                    Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = onReply, Modifier.weight(1f)) { Text(stringResource(R.string.coach_reply)) }
                        Button(onClick = onAccept, Modifier.weight(1f).testTag(CoachTags.ACCEPT)) { Text(stringResource(R.string.coach_accept)) }
                    }
                }
            }
        },
    ) { padding ->
        if (state.isLoading || proposal == null) {
            Box(Modifier.padding(padding).fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
            return@Scaffold
        }
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column {
                Text(stringResource(R.string.preview_title), style = MaterialTheme.typography.headlineSmall)
                Text(previewSubtitle(proposal), style = MaterialTheme.typography.bodyMedium)
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(14.dp)) {
                    Text(stringResource(R.string.preview_what_changes), style = MaterialTheme.typography.titleSmall)
                    Text(proposal.summary, style = MaterialTheme.typography.bodyMedium)
                }
            }
            proposal.plans.forEach { plan ->
                PlanPreview(plan, plan.planId?.let { state.currentPlans[it] }, state.exercises, showName = proposal.plans.size > 1)
            }
            Text(stringResource(R.string.preview_footer), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun previewSubtitle(proposal: CoachProposal): String {
    val first = proposal.plans.firstOrNull() ?: return ""
    val name = if (proposal.plans.size > 1) stringResource(R.string.coach_plans_count, proposal.plans.size) else first.name
    val date = first.date?.takeIf { proposal.plans.size == 1 }?.let { formatLongWeekdayDate(it) }
    val effect = when (proposal.type) {
        ProposalType.CHANGE_PLAN -> stringResource(R.string.preview_replaces)
        else -> stringResource(R.string.preview_new)
    }
    return listOfNotNull(name, date, effect).joinToString(" · ")
}

@Composable
private fun PlanPreview(plan: ProposedPlan, current: Plan?, exercises: Map<String, Exercise>, showName: Boolean) {
    val locale = LocalConfiguration.current.locales[0]
    val changes = current?.let { planChanges(it.sets, plan.sets) }.orEmpty()
    if (showName) Text(plan.name, style = MaterialTheme.typography.titleMedium)
    planExercises(plan.sets).forEach { exercise ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(exercises[exercise.exerciseKey]?.displayName(locale) ?: exercise.exerciseKey, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    changes.filter { changeKey(it) == exercise.exerciseKey }.forEach { change ->
                        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.tertiaryContainer) {
                            Text(changeBadge(change), Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                exercise.lines.forEach { line ->
                    when (line) {
                        is PlanLine.Warmup -> PreviewLine("R", planWarmupText(line), stringResource(R.string.plan_warmup))
                        is PlanLine.Working -> PreviewLine(planNumberText(line), planWorkingText(line), planWorkingRpeText(line))
                    }
                }
            }
        }
    }
    changes.filterIsInstance<PlanChange.Removed>().forEach { removed ->
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(exercises[removed.exerciseKey]?.displayName(locale) ?: removed.exerciseKey, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.errorContainer) {
                    Text(stringResource(R.string.preview_removed), Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

private fun changeKey(change: PlanChange): String = when (change) {
    is PlanChange.Added -> change.exerciseKey
    is PlanChange.Removed -> change.exerciseKey
    is PlanChange.SetsChanged -> change.exerciseKey
    is PlanChange.WeightChanged -> change.exerciseKey
}

@Composable
private fun changeBadge(change: PlanChange): String = when (change) {
    is PlanChange.Added -> stringResource(R.string.preview_added)
    is PlanChange.Removed -> stringResource(R.string.preview_removed)
    is PlanChange.SetsChanged -> stringResource(R.string.preview_sets_changed, change.from, change.to)
    is PlanChange.WeightChanged -> stringResource(R.string.diff_kg, com.github.sebastianp265.aitrainer.shared.localization.formatNumber(change.deltaKg, 1, signed = true))
}

@Composable
private fun PreviewLine(number: String, text: String, trailing: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(number, Modifier.widthIn(min = 40.dp), color = MaterialTheme.colorScheme.primary)
        Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(trailing, style = MaterialTheme.typography.bodyMedium)
    }
}
