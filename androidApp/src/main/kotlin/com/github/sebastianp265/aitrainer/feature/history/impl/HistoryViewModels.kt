package com.github.sebastianp265.aitrainer.feature.history.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sebastianp265.aitrainer.integration.workout.SessionSummary
import com.github.sebastianp265.aitrainer.integration.workout.WorkoutService
import com.github.sebastianp265.aitrainer.shared.state.WhileUiSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

internal data class HistoryUiState(
    val isLoading: Boolean = true,
    val sessions: List<SessionSummary> = emptyList(),
)

internal class HistoryViewModel(
    workoutService: WorkoutService,
    clock: Clock,
) : ViewModel() {

    private companion object {
        val EARLIEST_DAY = LocalDate(2000, 1, 1)
    }

    val uiState: StateFlow<HistoryUiState> = workoutService
        .observeFinishedSessions(EARLIEST_DAY, clock.todayIn(TimeZone.currentSystemDefault()))
        .map { sessions -> HistoryUiState(isLoading = false, sessions = sessions.sortedByDescending { it.startedAt }) }
        .stateIn(viewModelScope, WhileUiSubscribed, HistoryUiState())
}
