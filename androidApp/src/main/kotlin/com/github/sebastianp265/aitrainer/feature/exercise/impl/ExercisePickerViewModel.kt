package com.github.sebastianp265.aitrainer.feature.exercise.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
import com.github.sebastianp265.aitrainer.integration.exercise.ExerciseService
import com.github.sebastianp265.aitrainer.integration.exercise.MuscleGroup
import com.github.sebastianp265.aitrainer.integration.exercise.NewExercise
import com.github.sebastianp265.aitrainer.integration.workout.RecentExercise
import com.github.sebastianp265.aitrainer.integration.workout.WorkoutService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal data class ExercisePickerUiState(
    val isLoading: Boolean = true,
    val exercises: List<Exercise> = emptyList(),
    val groups: List<MuscleGroup> = emptyList(),
    val recent: List<RecentExercise> = emptyList(),
)

internal class ExercisePickerViewModel(
    private val exerciseService: ExerciseService,
    private val workoutService: WorkoutService,
) : ViewModel() {

    private companion object {
        const val RECENT_LIMIT = 5
    }

    private val state = MutableStateFlow(ExercisePickerUiState())
    val uiState: StateFlow<ExercisePickerUiState> = state

    init {
        viewModelScope.launch {
            val exercises = exerciseService.exercises()
            state.value = ExercisePickerUiState(
                isLoading = false,
                exercises = exercises,
                groups = exercises.flatMap { it.primaryMuscles }.distinctBy { it.key }.sortedBy { muscleGroupOrder(it.key) },
                recent = workoutService.recentExercises(RECENT_LIMIT),
            )
        }
    }

    fun create(newExercise: NewExercise, onCreated: (String) -> Unit) {
        viewModelScope.launch { onCreated(exerciseService.createExercise(newExercise)) }
    }
}
