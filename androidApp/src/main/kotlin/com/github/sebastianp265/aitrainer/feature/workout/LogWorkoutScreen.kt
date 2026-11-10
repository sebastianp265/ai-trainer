package com.github.sebastianp265.aitrainer.feature.workout

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.sebastianp265.aitrainer.feature.workout.impl.ActiveSessionState
import com.github.sebastianp265.aitrainer.feature.workout.impl.ActiveSessionViewModel
import com.github.sebastianp265.aitrainer.feature.workout.impl.ActiveWorkoutActions
import com.github.sebastianp265.aitrainer.feature.workout.impl.ActiveWorkoutContent
import com.github.sebastianp265.aitrainer.feature.workout.impl.ActiveWorkoutViewModel
import com.github.sebastianp265.aitrainer.feature.workout.impl.LogWorkoutContent
import com.github.sebastianp265.aitrainer.feature.workout.impl.LogWorkoutViewModel
import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
import com.github.sebastianp265.aitrainer.integration.workout.ExerciseBlock
import com.github.sebastianp265.aitrainer.integration.workout.SetChange
import com.github.sebastianp265.aitrainer.integration.workout.SetEntry
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun LogWorkoutScreen(onOpenHistory: () -> Unit) {
    val viewModel: LogWorkoutViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LogWorkoutContent(state = state, onStart = viewModel::startSession, onOpenHistory = onOpenHistory)
}

@Composable
internal fun ActiveWorkoutScreen(onOpenExerciseHistory: (String) -> Unit) {
    val viewModel: ActiveWorkoutViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalContext.current as Activity
    BackHandler { activity.moveTaskToBack(true) }
    val actions = remember(viewModel) {
        object : ActiveWorkoutActions {
            override fun finishSession() = viewModel.finishSession()
            override fun discardSession() = viewModel.discardSession()
            override fun addExercise(exercise: Exercise) = viewModel.addExercise(exercise)
            override fun removeExercise(exercise: Exercise) = viewModel.removeExercise(exercise)
            override fun addSet(block: ExerciseBlock) = viewModel.addSet(block)
            override fun copySetsFromLastTime(block: ExerciseBlock) = viewModel.copySetsFromLastTime(block)
            override fun changeSet(set: SetEntry, change: SetChange) = viewModel.changeSet(set, change)
            override fun deleteSet(set: SetEntry) = viewModel.deleteSet(set)
        }
    }
    ActiveWorkoutContent(
        state = state,
        actions = actions,
        onMessageShown = viewModel::messageShown,
        onOpenExerciseHistory = onOpenExerciseHistory,
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
