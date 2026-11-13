package com.github.sebastianp265.aitrainer.feature.coach.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
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
import com.github.sebastianp265.aitrainer.shared.state.WhileUiSubscribed
import com.github.sebastianp265.aitrainer.shared.workout.PlanLine
import com.github.sebastianp265.aitrainer.shared.workout.planExercises
import com.github.sebastianp265.aitrainer.shared.workout.planWorkingText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.github.sebastianp265.aitrainer.shared.design.DesignOutlinedButton
import com.github.sebastianp265.aitrainer.shared.design.InfoNote
import com.github.sebastianp265.aitrainer.shared.design.Tag
import com.github.sebastianp265.aitrainer.shared.design.extendedColors
import com.github.sebastianp265.aitrainer.shared.workout.PlanLineRows
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

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
                Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                    Row(
                        Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        DesignOutlinedButton(onClick = onReply, Modifier.weight(1f).height(48.dp)) { Text(stringResource(R.string.coach_reply)) }
                        Button(onClick = onAccept, Modifier.weight(1f).height(48.dp).testTag(CoachTags.ACCEPT)) { Text(stringResource(R.string.coach_accept)) }
                    }
                }
            }
        },
    ) { padding ->
        if (state.isLoading || proposal == null) {
            Box(Modifier.padding(padding).fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
            return@Scaffold
        }
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.heightIn(min = 64.dp), verticalArrangement = Arrangement.Center) {
                Text(stringResource(R.string.preview_title), style = MaterialTheme.typography.titleLarge)
                Text(previewSubtitle(proposal), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            InfoNote(stringResource(R.string.preview_what_changes), proposal.summary)
            proposal.plans.forEach { plan ->
                PlanPreview(plan, plan.planId?.let { state.currentPlans[it] }, state.exercises, showName = proposal.plans.size > 1)
            }
            Text(stringResource(R.string.preview_footer), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun previewSubtitle(proposal: CoachProposal): String {
    val first = proposal.plans.firstOrNull() ?: return ""
    val name = if (proposal.plans.size > 1) pluralStringResource(R.plurals.coach_plans_count, proposal.plans.size, proposal.plans.size) else first.name
    val effect = when (proposal.type) {
        ProposalType.CHANGE_PLAN -> stringResource(R.string.preview_replaces)
        else -> stringResource(R.string.preview_new)
    }
    return listOf(name, effect).joinToString(" · ")
}

@Composable
private fun PlanPreview(plan: ProposedPlan, current: Plan?, exercises: Map<String, Exercise>, showName: Boolean) {
    val locale = LocalConfiguration.current.locales[0]
    val changes = current?.let { planChanges(it.sets, plan.sets) }.orEmpty()
    if (showName) Text(plan.name, style = MaterialTheme.typography.titleMedium)
    planExercises(plan.sets).forEach { exercise ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        exercises[exercise.exerciseKey]?.displayName(locale) ?: exercise.exerciseKey,
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                    )
                    changes.filter { changeKey(it) == exercise.exerciseKey }.forEach { change ->
                        Tag(changeBadge(change), containerColor = extendedColors.warningContainer, contentColor = extendedColors.onWarningContainer)
                    }
                }
                PlanLineRows(exercise.lines)
            }
        }
    }
    changes.filterIsInstance<PlanChange.Removed>().forEach { removed ->
        val removedLines = current?.sets.orEmpty().filter { it.exerciseKey == removed.exerciseKey }
        Surface(
            Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = Color.Transparent,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        exercises[removed.exerciseKey]?.displayName(locale) ?: removed.exerciseKey,
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, textDecoration = TextDecoration.LineThrough),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Tag(stringResource(R.string.preview_removed), containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                }
                planExercises(removedLines).flatMap { it.lines }.filterIsInstance<PlanLine.Working>().firstOrNull()?.let { line ->
                    Text(stringResource(R.string.preview_was, planWorkingText(line)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    is PlanChange.SetsChanged -> pluralStringResource(R.plurals.preview_sets_changed, change.to, change.from, change.to)
    is PlanChange.WeightChanged -> stringResource(R.string.diff_kg, com.github.sebastianp265.aitrainer.shared.localization.formatNumber(change.deltaKg, 1, signed = true))
}
