package com.github.sebastianp265.aitrainer.feature.settings.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sebastianp265.aitrainer.integration.coach.CoachService
import com.github.sebastianp265.aitrainer.integration.settings.SettingsService
import com.github.sebastianp265.aitrainer.shared.state.WhileUiSubscribed
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class SettingsUiState(
    val isLoading: Boolean = true,
    val claudeModel: String = SettingsService.DEFAULT_CLAUDE_MODEL,
    val isClaudeApiKeyStored: Boolean = false,
    val isCoachEnabled: Boolean = true,
    val noteCount: Int = 0,
)

internal class SettingsViewModel(
    private val settingsService: SettingsService,
    coachService: CoachService,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsService.observeClaudeModel(),
        settingsService.observeIsClaudeApiKeyStored(),
        settingsService.observeCoachEnabled(),
        coachService.observeNotes().map { it.size },
    ) { claudeModel, isKeyStored, coachEnabled, noteCount ->
        SettingsUiState(isLoading = false, claudeModel = claudeModel, isClaudeApiKeyStored = isKeyStored, isCoachEnabled = coachEnabled, noteCount = noteCount)
    }.stateIn(viewModelScope, WhileUiSubscribed, SettingsUiState())

    fun setCoachEnabled(enabled: Boolean) {
        viewModelScope.launch { withContext(NonCancellable) { settingsService.setCoachEnabled(enabled) } }
    }
}
