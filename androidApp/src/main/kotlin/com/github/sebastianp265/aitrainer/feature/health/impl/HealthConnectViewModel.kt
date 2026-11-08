package com.github.sebastianp265.aitrainer.feature.health.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sebastianp265.aitrainer.integration.health.HealthService
import com.github.sebastianp265.aitrainer.integration.health.HealthConnectAccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class HealthConnectUiState(
    val access: HealthConnectAccess? = null,
    val requiredPermissions: Set<String> = emptySet(),
)

internal class HealthConnectViewModel(
    private val healthService: HealthService,
) : ViewModel() {

    private val state = MutableStateFlow(HealthConnectUiState(requiredPermissions = healthService.requiredPermissions))
    val uiState: StateFlow<HealthConnectUiState> = state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val access = healthService.checkAccess()
            state.update { it.copy(access = access) }
        }
    }
}
