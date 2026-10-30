package com.github.sebastianp265.aitrainer.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sebastianp265.aitrainer.data.healthconnect.HealthDataSource
import com.github.sebastianp265.aitrainer.domain.model.HealthDay
import com.github.sebastianp265.aitrainer.domain.usecase.CheckHealthConnectAccessUseCase
import com.github.sebastianp265.aitrainer.domain.usecase.HealthConnectAccess
import com.github.sebastianp265.aitrainer.domain.usecase.ReadHealthConnectDaysUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProgressUiState(
    val healthConnectAccess: HealthConnectAccess? = null,
    val healthConnectPermissions: Set<String> = emptySet(),
    val healthDays: List<HealthDay>? = null,
)

class ProgressViewModel(
    private val checkHealthConnectAccess: CheckHealthConnectAccessUseCase,
    private val readHealthConnectDays: ReadHealthConnectDaysUseCase,
    healthDataSource: HealthDataSource,
) : ViewModel() {

    private val state = MutableStateFlow(ProgressUiState(healthConnectPermissions = healthDataSource.requiredPermissions))
    val uiState: StateFlow<ProgressUiState> = state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val access = checkHealthConnectAccess()
            val days = if (access == HealthConnectAccess.CONNECTED) readHealthConnectDays() else null
            state.update { it.copy(healthConnectAccess = access, healthDays = days) }
        }
    }
}
