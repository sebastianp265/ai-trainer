package com.github.sebastianp265.aitrainer.feature.workout

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.sebastianp265.aitrainer.feature.workout.impl.ActiveSessionState
import com.github.sebastianp265.aitrainer.feature.workout.impl.ActiveSessionViewModel
import com.github.sebastianp265.aitrainer.feature.workout.impl.ActiveWorkoutActions
import com.github.sebastianp265.aitrainer.feature.workout.impl.ActiveWorkoutContent
import com.github.sebastianp265.aitrainer.feature.workout.impl.ActiveWorkoutViewModel
import com.github.sebastianp265.aitrainer.feature.workout.impl.LogWorkoutContent
import com.github.sebastianp265.aitrainer.feature.workout.impl.WorkoutDetailContent
import com.github.sebastianp265.aitrainer.feature.workout.impl.WorkoutReportViewModel
import com.github.sebastianp265.aitrainer.feature.workout.impl.WorkoutSummaryContent
import com.github.sebastianp265.aitrainer.feature.workout.impl.LogWorkoutViewModel
import com.github.sebastianp265.aitrainer.feature.workout.impl.TrainingActions
import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
import com.github.sebastianp265.aitrainer.shared.coach.PendingProposalBanner
import com.github.sebastianp265.aitrainer.shared.coach.ProposalScope
import com.github.sebastianp265.aitrainer.integration.workout.ExerciseBlock
import com.github.sebastianp265.aitrainer.integration.workout.SetChange
import com.github.sebastianp265.aitrainer.integration.workout.SetEntry
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.runtime.getValue

@Composable
internal fun LogWorkoutScreen(
    onOpenWorkout: (Long) -> Unit,
    onOpenProposal: (Long) -> Unit,
    onOpenCoach: () -> Unit,
    onOpenPlan: (Long) -> Unit,
    onEditPlan: (Long) -> Unit,
    onNewPlan: () -> Unit,
    onResumeWorkout: () -> Unit,
) {
    val viewModel: LogWorkoutViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actions = remember(viewModel) {
        object : TrainingActions {
            override fun startEmpty() = viewModel.startEmptyWorkout()
            override fun startPlan(planId: Long) = viewModel.startWorkoutFromPlan(planId)
            override fun openPlan(planId: Long) = onOpenPlan(planId)
            override fun editPlan(planId: Long) = onEditPlan(planId)
            override fun newPlan() = onNewPlan()
            override fun resumeWorkout() = onResumeWorkout()
            override fun openWorkout(sessionId: Long) = onOpenWorkout(sessionId)
            override fun editWorkout(sessionId: Long) = viewModel.editWorkout(sessionId)
        }
    }
    LogWorkoutContent(
        state = state,
        actions = actions,
        banner = { PendingProposalBanner(ProposalScope.PLANS, onOpenProposal, onOpenCoach) },
    )
}

@Composable
internal fun ActiveWorkoutScreen(
    onFinished: (sessionId: Long) -> Unit,
    onOpenExerciseHistory: (String) -> Unit,
    onOpenExercisePicker: () -> Unit,
    pickedExerciseKey: String?,
    onPickedExerciseHandled: () -> Unit,
) {
    val viewModel: ActiveWorkoutViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(pickedExerciseKey) {
        pickedExerciseKey?.let {
            viewModel.addExercise(it)
            onPickedExerciseHandled()
        }
    }
    val actions = remember(viewModel) {
        object : ActiveWorkoutActions {
            override fun finishSession() = viewModel.finishSession(onFinished)
            override fun discardSession() = viewModel.discardSession()
            override fun saveEdit() = viewModel.saveEdit()
            override fun addExercise(exerciseKey: String) = viewModel.addExercise(exerciseKey)
            override fun removeExercise(exercise: Exercise) = viewModel.removeExercise(exercise)
            override fun addSet(block: ExerciseBlock) = viewModel.addSet(block)
            override fun changeSet(set: SetEntry, change: SetChange) = viewModel.changeSet(set, change)
            override fun deleteSet(set: SetEntry) = viewModel.deleteSet(set)
        }
    }
    ActiveWorkoutContent(
        state = state,
        actions = actions,
        onOpenExerciseHistory = onOpenExerciseHistory,
        onOpenExercisePicker = onOpenExercisePicker,
    )
}

@Composable
internal fun ActiveWorkoutRedirect(isActiveWorkoutOpen: () -> Boolean, onSessionStarted: () -> Unit, onSessionEnded: () -> Unit) {
    val viewModel: ActiveSessionViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var previousState by rememberSaveable { mutableStateOf(ActiveSessionState.Loading) }
    LaunchedEffect(state) {
        when (state) {
            ActiveSessionState.Loading -> return@LaunchedEffect
            ActiveSessionState.Active -> if (previousState != ActiveSessionState.Active && !isActiveWorkoutOpen()) onSessionStarted()
            ActiveSessionState.None -> if (isActiveWorkoutOpen()) onSessionEnded()
        }
        previousState = state
    }
}

@Composable
internal fun WorkoutSummaryScreen(sessionId: Long, onDone: () -> Unit, onShowAllSets: () -> Unit) {
    val viewModel: WorkoutReportViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(sessionId) { viewModel.show(sessionId) }
    BackHandler(onBack = onDone)
    LaunchedEffect(state) { if (!state.isLoading && state.report == null) onDone() }
    WorkoutSummaryContent(state, onDone, onShowAllSets)
}

@Composable
internal fun WorkoutDetailScreen(sessionId: Long, onBack: () -> Unit, onDeleted: () -> Unit) {
    val viewModel: WorkoutReportViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(sessionId) { viewModel.show(sessionId) }
    LaunchedEffect(state) { if (!state.isLoading && state.report == null) onBack() }
    WorkoutDetailContent(
        state = state,
        onBack = onBack,
        onEdit = viewModel::edit,
        onDelete = { viewModel.delete(onDeleted) },
    )
}
