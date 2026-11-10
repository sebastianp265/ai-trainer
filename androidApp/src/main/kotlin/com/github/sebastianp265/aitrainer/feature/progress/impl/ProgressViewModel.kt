package com.github.sebastianp265.aitrainer.feature.progress.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sebastianp265.aitrainer.integration.health.HealthService
import com.github.sebastianp265.aitrainer.shared.state.WhileUiSubscribed
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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
    private val clock: Clock,
) : ViewModel() {

    private val range = MutableStateFlow(ProgressRange.FOUR_WEEKS)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ProgressUiState> = range.flatMapLatest { selected ->
        val today = clock.todayIn(TimeZone.currentSystemDefault())
        healthService.observeDays(today.minus(DatePeriod(days = selected.days - 1)), today).map { days ->
            ProgressUiState(range = selected, cards = ProgressCalculator.calculate(days, selected, today))
        }
    }.stateIn(viewModelScope, WhileUiSubscribed, ProgressUiState())

    fun selectRange(selected: ProgressRange) {
        range.value = selected
    }

    fun syncAfterConnecting() {
        viewModelScope.launch { healthService.sync() }
    }
}
