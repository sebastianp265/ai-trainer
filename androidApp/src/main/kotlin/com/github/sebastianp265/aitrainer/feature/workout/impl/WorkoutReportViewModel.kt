package com.github.sebastianp265.aitrainer.feature.workout.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
import com.github.sebastianp265.aitrainer.integration.exercise.ExerciseService
import com.github.sebastianp265.aitrainer.integration.workout.WorkoutReport
import com.github.sebastianp265.aitrainer.integration.workout.WorkoutService
import com.github.sebastianp265.aitrainer.shared.state.WhileUiSubscribed
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal data class WorkoutReportUiState(
    val isLoading: Boolean = true,
    val report: WorkoutReport? = null,
    val exercises: Map<String, Exercise> = emptyMap(),
    val canEdit: Boolean = false,
)

internal class WorkoutReportViewModel(
    private val workoutService: WorkoutService,
    exerciseService: ExerciseService,
) : ViewModel() {

    private val sessionId = MutableStateFlow<Long?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<WorkoutReportUiState> = combine(
        sessionId.filterNotNull().flatMapLatest { workoutService.observeWorkoutReport(it) },
        exerciseService.observeExercisesByKey(),
        workoutService.observeActiveSession().map { it != null },
    ) { report, exercises, hasActiveSession ->
        WorkoutReportUiState(isLoading = false, report = report, exercises = exercises, canEdit = !hasActiveSession)
    }.stateIn(viewModelScope, WhileUiSubscribed, WorkoutReportUiState())

    fun show(id: Long) {
        sessionId.value = id
    }

    fun edit() {
        val id = sessionId.value ?: return
        viewModelScope.launch { workoutService.startEditing(id) }
    }

    fun delete(onDeleted: () -> Unit) {
        val id = sessionId.value ?: return
        viewModelScope.launch {
            workoutService.discardSession(id)
            onDeleted()
        }
    }
}
