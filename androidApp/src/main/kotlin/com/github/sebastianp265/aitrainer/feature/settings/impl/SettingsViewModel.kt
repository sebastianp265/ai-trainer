package com.github.sebastianp265.aitrainer.feature.settings.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sebastianp265.aitrainer.shared.state.WhileUiSubscribed
import com.github.sebastianp265.aitrainer.integration.settings.SettingsService
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class SettingsUiState(
    val isLoading: Boolean = true,
    val claudeModel: String = SettingsService.DEFAULT_CLAUDE_MODEL,
    val isClaudeApiKeyStored: Boolean = false,
    val isCoachEnabled: Boolean = false,
)

internal class SettingsViewModel(
    private val settingsService: SettingsService,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsService.observeClaudeModel(),
        settingsService.observeIsClaudeApiKeyStored(),
        settingsService.observeCoachEnabled(),
    ) { claudeModel, isKeyStored, coachEnabled ->
        SettingsUiState(isLoading = false, claudeModel = claudeModel, isClaudeApiKeyStored = isKeyStored, isCoachEnabled = coachEnabled)
    }.stateIn(viewModelScope, WhileUiSubscribed, SettingsUiState())

    fun saveClaudeApiKey(apiKey: String) {
        if (apiKey.isBlank()) return
        saveEvenIfScreenIsLeft { settingsService.saveClaudeApiKey(apiKey.trim()) }
    }

    fun removeClaudeApiKey() = saveEvenIfScreenIsLeft { settingsService.removeClaudeApiKey() }

    fun setCoachEnabled(enabled: Boolean) = saveEvenIfScreenIsLeft { settingsService.setCoachEnabled(enabled) }

    fun setClaudeModel(modelId: String) = saveEvenIfScreenIsLeft { settingsService.setClaudeModel(modelId) }

    private fun saveEvenIfScreenIsLeft(save: suspend () -> Unit) {
        viewModelScope.launch { withContext(NonCancellable) { save() } }
    }
}
