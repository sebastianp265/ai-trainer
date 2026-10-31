package com.github.sebastianp265.aitrainer.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sebastianp265.aitrainer.core.secrets.Secret
import com.github.sebastianp265.aitrainer.core.secrets.SecretStore
import com.github.sebastianp265.aitrainer.core.ui.WhileUiSubscribed
import com.github.sebastianp265.aitrainer.feature.settings.domain.AppSettingsRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val isLoading: Boolean = true,
    val claudeModel: String = AppSettingsRepository.DEFAULT_CLAUDE_MODEL,
    val isClaudeApiKeyStored: Boolean = false,
)

class SettingsViewModel(
    private val appSettingsRepository: AppSettingsRepository,
    private val secretStore: SecretStore,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        appSettingsRepository.observeClaudeModel(),
        secretStore.observeIsStored(Secret.CLAUDE_API_KEY),
    ) { claudeModel, isKeyStored ->
        SettingsUiState(isLoading = false, claudeModel = claudeModel, isClaudeApiKeyStored = isKeyStored)
    }.stateIn(viewModelScope, WhileUiSubscribed, SettingsUiState())

    fun saveClaudeApiKey(apiKey: String) {
        if (apiKey.isBlank()) return
        saveEvenIfScreenIsLeft { secretStore.write(Secret.CLAUDE_API_KEY, apiKey.trim()) }
    }

    fun removeClaudeApiKey() = saveEvenIfScreenIsLeft { secretStore.delete(Secret.CLAUDE_API_KEY) }

    fun setClaudeModel(modelId: String) = saveEvenIfScreenIsLeft { appSettingsRepository.setClaudeModel(modelId) }

    private fun saveEvenIfScreenIsLeft(save: suspend () -> Unit) {
        viewModelScope.launch { withContext(NonCancellable) { save() } }
    }
}
