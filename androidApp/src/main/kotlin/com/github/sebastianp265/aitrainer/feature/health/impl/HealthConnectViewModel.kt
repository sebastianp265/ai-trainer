package com.github.sebastianp265.aitrainer.feature.health.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sebastianp265.aitrainer.integration.health.HealthConnectAccess
import com.github.sebastianp265.aitrainer.integration.health.HealthService
import com.github.sebastianp265.aitrainer.integration.health.HealthStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Instant

internal data class HealthConnectUiState(
    val status: HealthStatus? = null,
    val lastSync: Instant? = null,
    val isSyncing: Boolean = false,
    val syncFailed: Boolean = false,
    val requiredPermissions: Set<String> = emptySet(),
) {
    val access: HealthConnectAccess? get() = status?.access
}

internal class HealthConnectViewModel(
    private val healthService: HealthService,
) : ViewModel() {

    private val state = MutableStateFlow(HealthConnectUiState(requiredPermissions = healthService.permissionsToRequest))
    val uiState: StateFlow<HealthConnectUiState> = state.asStateFlow()

    init {
        viewModelScope.launch {
            healthService.observeLastSync().collect { lastSync -> state.update { it.copy(lastSync = lastSync) } }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val status = healthService.status()
            state.update { it.copy(status = status) }
        }
    }

    fun syncNow() {
        if (state.value.isSyncing) return
        state.update { it.copy(isSyncing = true, syncFailed = false) }
        viewModelScope.launch {
            val failed = try {
                healthService.sync()
                false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                true
            }
            state.update { it.copy(isSyncing = false, syncFailed = failed) }
            refresh()
        }
    }
}
