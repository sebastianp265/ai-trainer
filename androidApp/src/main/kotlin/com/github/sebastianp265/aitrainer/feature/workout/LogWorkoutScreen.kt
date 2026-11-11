package com.github.sebastianp265.aitrainer.feature.workout

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
import com.github.sebastianp265.aitrainer.shared.coach.PendingProposalBanner
import com.github.sebastianp265.aitrainer.shared.coach.ProposalScope
import com.github.sebastianp265.aitrainer.integration.workout.ExerciseBlock
import com.github.sebastianp265.aitrainer.integration.workout.SetChange
import com.github.sebastianp265.aitrainer.integration.workout.SetEntry
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun LogWorkoutScreen(onOpenHistory: () -> Unit, onOpenWorkout: (Long) -> Unit, onOpenProposal: (Long) -> Unit, onOpenCoach: () -> Unit) {
    val viewModel: LogWorkoutViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LogWorkoutContent(
        state = state,
        onStartEmpty = viewModel::startEmptyWorkout,
        onStartFromPlan = viewModel::startWorkoutFromPlan,
        onOpenHistory = onOpenHistory,
        onOpenWorkout = onOpenWorkout,
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
    val activity = checkNotNull(LocalActivity.current)
    BackHandler { activity.moveTaskToBack(true) }
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
            override fun addExercise(exerciseKey: String) = viewModel.addExercise(exerciseKey)
            override fun removeExercise(exercise: Exercise) = viewModel.removeExercise(exercise)
            override fun addSet(block: ExerciseBlock) = viewModel.addSet(block)
            override fun copySetsFromLastTime(block: ExerciseBlock) = viewModel.copySetsFromLastTime(block)
            override fun addSpokenSet(block: ExerciseBlock, alternatives: List<String>) = viewModel.addSpokenSet(block, alternatives)
            override fun voiceUnavailable() = viewModel.voiceUnavailable()
            override fun changeSet(set: SetEntry, change: SetChange) = viewModel.changeSet(set, change)
            override fun deleteSet(set: SetEntry) = viewModel.deleteSet(set)
        }
    }
    ActiveWorkoutContent(
        state = state,
        actions = actions,
        onMessageShown = viewModel::messageShown,
        onOpenExerciseHistory = onOpenExerciseHistory,
        onOpenExercisePicker = onOpenExercisePicker,
    )
}

@Composable
internal fun ActiveWorkoutRedirect(isActiveWorkoutOpen: () -> Boolean, onSessionStarted: () -> Unit, onSessionEnded: () -> Unit) {
    val viewModel: ActiveSessionViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state) {
        when (state) {
            ActiveSessionState.Loading -> Unit
            ActiveSessionState.Active -> if (!isActiveWorkoutOpen()) onSessionStarted()
            ActiveSessionState.None -> if (isActiveWorkoutOpen()) onSessionEnded()
        }
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
        onEdit = { viewModel.edit(onReopened = {}) },
        onDelete = { viewModel.delete(onDeleted) },
    )
}
