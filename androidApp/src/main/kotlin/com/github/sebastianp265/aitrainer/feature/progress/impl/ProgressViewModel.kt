package com.github.sebastianp265.aitrainer.feature.progress.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sebastianp265.aitrainer.integration.health.HealthDay
import com.github.sebastianp265.aitrainer.integration.health.HealthService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

internal data class ProgressUiState(
    val isLoading: Boolean = true,
    val healthDays: List<HealthDay> = emptyList(),
)

internal class ProgressViewModel(
    private val healthService: HealthService,
    clock: Clock,
) : ViewModel() {

    private val state = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = state.asStateFlow()

    init {
        val today = clock.todayIn(TimeZone.currentSystemDefault())
        viewModelScope.launch {
            healthService.observeDays(today.minus(DatePeriod(days = DAYS - 1)), today).collect { days ->
                state.update { it.copy(isLoading = false, healthDays = days) }
            }
        }
    }

    fun syncAfterConnecting() {
        viewModelScope.launch { healthService.sync() }
    }

    private companion object {
        const val DAYS = 30
    }
}
