package com.github.sebastianp265.aitrainer.feature.volume.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sebastianp265.aitrainer.integration.exercise.ExerciseService
import com.github.sebastianp265.aitrainer.integration.exercise.MuscleGroup
import com.github.sebastianp265.aitrainer.integration.settings.SettingsService
import com.github.sebastianp265.aitrainer.integration.volume.VolumeService
import com.github.sebastianp265.aitrainer.integration.volume.WeeklyTarget
import com.github.sebastianp265.aitrainer.shared.state.WhileUiSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal data class TargetRow(val group: MuscleGroup, val target: WeeklyTarget)

internal data class VolumeTargetsUiState(
    val isLoading: Boolean = true,
    val showTargets: Boolean = true,
    val rows: List<TargetRow> = emptyList(),
)

internal class VolumeTargetsViewModel(
    private val volumeService: VolumeService,
    private val settingsService: SettingsService,
    exerciseService: ExerciseService,
) : ViewModel() {

    val uiState: StateFlow<VolumeTargetsUiState> = combine(
        volumeService.observeTargets(),
        exerciseService.observeMuscleGroups(),
        settingsService.observeShowTargets(),
    ) { targets, groups, showTargets ->
        val groupsByKey = groups.associateBy { it.key }
        VolumeTargetsUiState(false, showTargets, targets.map { TargetRow(groupsByKey.getValue(it.groupKey), it) })
    }.stateIn(viewModelScope, WhileUiSubscribed, VolumeTargetsUiState())

    fun setShowTargets(show: Boolean) {
        viewModelScope.launch { settingsService.setShowTargets(show) }
    }

    fun setTarget(groupKey: String, minSets: Int, maxSets: Int) {
        viewModelScope.launch { volumeService.setTarget(groupKey, minSets, maxSets) }
    }

    fun restoreDefaults() {
        viewModelScope.launch { volumeService.restoreDefaultTargets() }
    }
}
