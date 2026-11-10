package com.github.sebastianp265.aitrainer.feature.exercise

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.sebastianp265.aitrainer.feature.exercise.impl.ExerciseHistoryContent
import com.github.sebastianp265.aitrainer.feature.exercise.impl.ExerciseHistoryViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun ExerciseHistoryScreen(exerciseKey: String, onOpenWorkout: (sessionId: Long) -> Unit) {
    val viewModel: ExerciseHistoryViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(exerciseKey) { viewModel.show(exerciseKey) }
    ExerciseHistoryContent(state, onOpenWorkout)
}
