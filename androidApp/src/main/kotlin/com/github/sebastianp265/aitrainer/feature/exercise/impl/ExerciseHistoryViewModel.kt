package com.github.sebastianp265.aitrainer.feature.exercise.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
import com.github.sebastianp265.aitrainer.integration.exercise.ExerciseService
import com.github.sebastianp265.aitrainer.integration.workout.ExerciseHistory
import com.github.sebastianp265.aitrainer.integration.workout.WorkoutService
import com.github.sebastianp265.aitrainer.shared.state.WhileUiSubscribed
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal data class ExerciseHistoryUiState(val exercise: Exercise, val history: ExerciseHistory)

internal class ExerciseHistoryViewModel(
    private val exerciseService: ExerciseService,
    private val workoutService: WorkoutService,
) : ViewModel() {

    private val exerciseKey = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ExerciseHistoryUiState?> = exerciseKey.filterNotNull().flatMapLatest { key ->
        val exercise = exerciseService.exercises().first { it.key == key }
        workoutService.observeExerciseHistory(key).map { ExerciseHistoryUiState(exercise, it) }
    }.stateIn(viewModelScope, WhileUiSubscribed, null)

    fun show(key: String) {
        exerciseKey.value = key
    }
}
