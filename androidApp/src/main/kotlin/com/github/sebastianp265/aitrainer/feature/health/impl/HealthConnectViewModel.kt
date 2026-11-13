package com.github.sebastianp265.aitrainer.feature.health.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sebastianp265.aitrainer.integration.health.HealthConnectAccess
import com.github.sebastianp265.aitrainer.integration.health.HealthDataType
import com.github.sebastianp265.aitrainer.integration.health.HealthService
import com.github.sebastianp265.aitrainer.integration.health.HealthStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class HealthConnectUiState(
    val status: HealthStatus? = null,
    val permissionsToRequest: Set<String> = emptySet(),
) {
    val access: HealthConnectAccess? get() = status?.access
}

internal class HealthConnectViewModel(
    private val healthService: HealthService,
) : ViewModel() {

    private val state = MutableStateFlow(HealthConnectUiState())
    val uiState: StateFlow<HealthConnectUiState> = state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val status = healthService.status()
            state.update { it.copy(status = status) }
        }
    }

    fun turnOn(type: HealthDataType) {
        viewModelScope.launch { requestOrRefresh(healthService.enable(type)) }
    }

    fun turnOff(type: HealthDataType) {
        viewModelScope.launch {
            healthService.disable(type)
            refresh()
        }
    }

    fun allowHistory() {
        viewModelScope.launch { requestOrRefresh(healthService.missingHistoryPermissions()) }
    }

    fun permissionsRequested() {
        state.update { it.copy(permissionsToRequest = emptySet()) }
    }

    fun permissionsAnswered() {
        healthService.reload()
        refresh()
    }

    private fun requestOrRefresh(missing: Set<String>) {
        if (missing.isEmpty()) refresh() else state.update { it.copy(permissionsToRequest = missing) }
    }
}
