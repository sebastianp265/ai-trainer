package com.github.sebastianp265.aitrainer.feature.exercise

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.sebastianp265.aitrainer.feature.exercise.impl.ExercisePickerContent
import com.github.sebastianp265.aitrainer.feature.exercise.impl.ExercisePickerViewModel
import com.github.sebastianp265.aitrainer.integration.exercise.NewExercise
import org.koin.compose.viewmodel.koinViewModel

internal const val PICKED_EXERCISE_KEY = "picked_exercise"

@Composable
internal fun ExercisePickerScreen(onClose: () -> Unit, onPicked: (exerciseKey: String) -> Unit) {
    val viewModel: ExercisePickerViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ExercisePickerContent(
        state = state,
        onClose = onClose,
        onPick = onPicked,
        onCreate = { name: String, primary: String, secondary: List<String> ->
            viewModel.create(NewExercise(name, primary, secondary), onPicked)
        },
    )
}
