package com.github.sebastianp265.aitrainer.feature.app.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sebastianp265.aitrainer.integration.settings.SettingsService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

internal class CoachEnabledViewModel(settingsService: SettingsService) : ViewModel() {
    val enabled: StateFlow<Boolean> = settingsService.observeCoachEnabled().stateIn(viewModelScope, SharingStarted.Eagerly, true)
}
