package com.github.sebastianp265.aitrainer.feature.history.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sebastianp265.aitrainer.integration.health.HealthService
import com.github.sebastianp265.aitrainer.integration.health.HealthWorkout
import com.github.sebastianp265.aitrainer.integration.workout.SessionSummary
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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Instant

internal enum class HistoryFilter { ALL, GYM, OTHER_APPS }

internal sealed interface HistoryItem {
    val startedAt: Instant

    data class Gym(val session: SessionSummary) : HistoryItem {
        override val startedAt: Instant get() = session.startedAt
    }

    data class External(val workout: HealthWorkout) : HistoryItem {
        override val startedAt: Instant get() = workout.startedAt
    }
}

internal data class HistoryUiState(
    val isLoading: Boolean = true,
    val items: List<HistoryItem> = emptyList(),
)

internal class HistoryViewModel(
    workoutService: WorkoutService,
    healthService: HealthService,
    clock: Clock,
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = combine(
        workoutService.observeFinishedSessions(EARLIEST_DAY, clock.todayIn(TimeZone.currentSystemDefault())),
        healthService.observeWorkouts(),
    ) { sessions, workouts ->
        val items = sessions.map<SessionSummary, HistoryItem> { HistoryItem.Gym(it) } + workouts.map { HistoryItem.External(it) }
        HistoryUiState(isLoading = false, items = items.sortedByDescending { it.startedAt })
    }.stateIn(viewModelScope, WhileUiSubscribed, HistoryUiState())

    private companion object {
        val EARLIEST_DAY = LocalDate(2000, 1, 1)
    }
}

internal data class ExternalWorkoutUiState(val isLoading: Boolean = true, val workout: HealthWorkout? = null)

internal class ExternalWorkoutViewModel(
    private val healthService: HealthService,
) : ViewModel() {

    private val shownStart = MutableStateFlow<Long?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ExternalWorkoutUiState> = shownStart.filterNotNull().flatMapLatest { startedAtMillis ->
        healthService.observeWorkouts().map { workouts ->
            ExternalWorkoutUiState(
                isLoading = false,
                workout = workouts.firstOrNull { it.startedAt.toEpochMilliseconds() == startedAtMillis },
            )
        }
    }.stateIn(viewModelScope, WhileUiSubscribed, ExternalWorkoutUiState())

    fun show(startedAtMillis: Long) {
        shownStart.value = startedAtMillis
    }
}
