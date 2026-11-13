package com.github.sebastianp265.aitrainer.feature.workout.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sebastianp265.aitrainer.integration.workout.WorkoutService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal enum class ActiveSessionState { Loading, None, Active }

internal class ActiveSessionViewModel(workoutService: WorkoutService) : ViewModel() {

    val state: StateFlow<ActiveSessionState> = workoutService.observeActiveSession()
        .map { if (it == null) ActiveSessionState.None else ActiveSessionState.Active }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ActiveSessionState.Loading)
}
