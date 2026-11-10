package com.github.sebastianp265.aitrainer.feature.progress.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sebastianp265.aitrainer.integration.health.HealthDay
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
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

internal class MetricViewModel(
    healthService: HealthService,
    clock: Clock,
) : ViewModel() {

    val today: LocalDate = clock.todayIn(TimeZone.currentSystemDefault())

    val days: StateFlow<List<HealthDay>?> = healthService
        .observeDays(today.minus(DatePeriod(days = ProgressRange.YEAR.days - 1)), today)
        .map<List<HealthDay>, List<HealthDay>?> { it }
        .stateIn(viewModelScope, WhileUiSubscribed, null)
}

internal data class DayDetailUiState(
    val date: LocalDate,
    val day: HealthDay?,
    val externalWorkouts: List<HealthWorkout>,
    val gymSessions: List<SessionSummary>,
)

internal class DayDetailViewModel(
    private val healthService: HealthService,
    private val workoutService: WorkoutService,
) : ViewModel() {

    private val shownDate = MutableStateFlow<LocalDate?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DayDetailUiState?> = shownDate.filterNotNull().flatMapLatest { date ->
        combine(
            healthService.observeDays(date, date),
            healthService.observeWorkouts(),
            workoutService.observeFinishedSessions(date, date),
        ) { days, workouts, sessions ->
            val zone = TimeZone.currentSystemDefault()
            DayDetailUiState(
                date = date,
                day = days.singleOrNull(),
                externalWorkouts = workouts.filter { it.startedAt.toLocalDateTime(zone).date == date },
                gymSessions = sessions,
            )
        }
    }.stateIn(viewModelScope, WhileUiSubscribed, null)

    fun show(date: LocalDate) {
        shownDate.value = date
    }
}
