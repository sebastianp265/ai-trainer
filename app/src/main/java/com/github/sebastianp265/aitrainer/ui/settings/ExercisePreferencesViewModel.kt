package com.github.sebastianp265.aitrainer.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sebastianp265.aitrainer.data.repo.ExerciseRepository
import com.github.sebastianp265.aitrainer.domain.model.Exercise
import com.github.sebastianp265.aitrainer.domain.model.ExercisePreference
import com.github.sebastianp265.aitrainer.ui.WhileUiSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ExercisePreferencesUiState(
    val isLoading: Boolean = true,
    val exercises: List<Exercise> = emptyList(),
)

class ExercisePreferencesViewModel(
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {

    val uiState: StateFlow<ExercisePreferencesUiState> = exerciseRepository.observeExercises()
        .map { ExercisePreferencesUiState(isLoading = false, exercises = it) }
        .stateIn(viewModelScope, WhileUiSubscribed, ExercisePreferencesUiState())

    fun setPreference(exercise: Exercise, preference: ExercisePreference) {
        viewModelScope.launch { exerciseRepository.setPreference(exercise.id, preference) }
    }
}
