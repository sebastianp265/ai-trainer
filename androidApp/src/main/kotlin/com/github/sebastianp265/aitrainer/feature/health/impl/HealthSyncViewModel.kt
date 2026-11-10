package com.github.sebastianp265.aitrainer.feature.health.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sebastianp265.aitrainer.integration.health.HealthService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class HealthSyncViewModel(
    private val healthService: HealthService,
) : ViewModel() {

    private val syncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = syncing.asStateFlow()

    fun sync() {
        if (!syncing.compareAndSet(expect = false, update = true)) return
        viewModelScope.launch {
            try {
                healthService.sync()
            } finally {
                syncing.value = false
            }
        }
    }
}
