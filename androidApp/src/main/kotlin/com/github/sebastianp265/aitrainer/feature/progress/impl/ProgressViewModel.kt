package com.github.sebastianp265.aitrainer.feature.progress.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sebastianp265.aitrainer.integration.health.HealthService
import com.github.sebastianp265.aitrainer.integration.workout.WorkoutService
import com.github.sebastianp265.aitrainer.shared.state.WhileUiSubscribed
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

internal data class ProgressUiState(
    val range: ProgressRange = ProgressRange.FOUR_WEEKS,
    val cards: ProgressCards? = null,
)

internal class ProgressViewModel(
    private val healthService: HealthService,
    private val workoutService: WorkoutService,
    private val clock: Clock,
) : ViewModel() {

    private val range = MutableStateFlow(ProgressRange.FOUR_WEEKS)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ProgressUiState> = range.flatMapLatest { selected ->
        val today = clock.todayIn(TimeZone.currentSystemDefault())
        val firstDay = today.minus(DatePeriod(days = selected.days - 1))
        combine(healthService.observeDays(firstDay, today), workoutService.observeFinishedSessions(firstDay, today)) { days, sessions ->
            ProgressUiState(range = selected, cards = ProgressCalculator.calculate(days, sessions.size, selected, today))
        }
    }.stateIn(viewModelScope, WhileUiSubscribed, ProgressUiState())

    fun selectRange(selected: ProgressRange) {
        range.value = selected
    }

    fun syncAfterConnecting() {
        viewModelScope.launch { healthService.sync() }
    }
}
