package com.github.sebastianp265.aitrainer.feature.workout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
import com.github.sebastianp265.aitrainer.feature.workout.impl.LogWorkoutActions
import com.github.sebastianp265.aitrainer.feature.workout.impl.LogWorkoutContent
import com.github.sebastianp265.aitrainer.feature.workout.impl.LogWorkoutViewModel
import com.github.sebastianp265.aitrainer.integration.workout.ExerciseBlock
import com.github.sebastianp265.aitrainer.integration.workout.SetEntry
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun LogWorkoutScreen(onOpenHistory: () -> Unit) {
    val viewModel: LogWorkoutViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actions = remember(viewModel) {
        object : LogWorkoutActions {
            override fun startSession() = viewModel.startSession()
            override fun finishSession() = viewModel.finishSession()
            override fun discardSession() = viewModel.discardSession()
            override fun addExercise(exercise: Exercise) = viewModel.addExercise(exercise)
            override fun addSet(block: ExerciseBlock) = viewModel.addSet(block)
            override fun copySetsFromLastTime(block: ExerciseBlock) = viewModel.copySetsFromLastTime(block)
            override fun changeReps(set: SetEntry, delta: Int) = viewModel.changeReps(set, delta)
            override fun changeWeight(set: SetEntry, deltaKg: Double) = viewModel.changeWeight(set, deltaKg)
            override fun toggleWarmup(set: SetEntry) = viewModel.toggleWarmup(set)
            override fun deleteSet(set: SetEntry) = viewModel.deleteSet(set)
        }
    }
    LogWorkoutContent(state = state, actions = actions, onMessageShown = viewModel::messageShown, onOpenHistory = onOpenHistory)
}
