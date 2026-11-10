package com.github.sebastianp265.aitrainer.feature.workout.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sebastianp265.aitrainer.integration.workout.SessionSummary
import com.github.sebastianp265.aitrainer.integration.workout.WorkoutService
import com.github.sebastianp265.aitrainer.shared.state.WhileUiSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal data class LogUiState(
    val isLoading: Boolean = true,
    val hasActiveSession: Boolean = false,
    val recentSessions: List<SessionSummary> = emptyList(),
)

internal class LogWorkoutViewModel(private val workoutService: WorkoutService) : ViewModel() {

    val uiState: StateFlow<LogUiState> = combine(
        workoutService.observeActiveSession().map { it != null },
        workoutService.observeRecentSessions(),
    ) { hasActive, recent ->
        LogUiState(isLoading = false, hasActiveSession = hasActive, recentSessions = recent)
    }.stateIn(viewModelScope, WhileUiSubscribed, LogUiState())

    fun startSession() {
        viewModelScope.launch { workoutService.startSession() }
    }
}
