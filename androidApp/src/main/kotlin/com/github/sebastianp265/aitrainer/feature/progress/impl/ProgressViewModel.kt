package com.github.sebastianp265.aitrainer.feature.progress.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sebastianp265.aitrainer.integration.health.HealthService
import com.github.sebastianp265.aitrainer.integration.health.HealthDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class ProgressUiState(
    val isLoading: Boolean = true,
    val healthDays: List<HealthDay>? = null,
)

internal class ProgressViewModel(
    private val healthService: HealthService,
) : ViewModel() {

    private val state = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val days = healthService.readRecentDays()
            state.update { it.copy(isLoading = false, healthDays = days) }
        }
    }
}
