package com.github.sebastianp265.aitrainer.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.sebastianp265.aitrainer.feature.health.HealthConnectSettingsRow
import com.github.sebastianp265.aitrainer.feature.settings.impl.SettingsActions
import com.github.sebastianp265.aitrainer.feature.settings.impl.SettingsContent
import com.github.sebastianp265.aitrainer.feature.settings.impl.SettingsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun SettingsScreen(onOpenHealthConnect: () -> Unit) {
    val viewModel: SettingsViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actions = remember(viewModel) {
        object : SettingsActions {
            override fun saveClaudeApiKey(apiKey: String) = viewModel.saveClaudeApiKey(apiKey)
            override fun removeClaudeApiKey() = viewModel.removeClaudeApiKey()
            override fun setClaudeModel(modelId: String) = viewModel.setClaudeModel(modelId)
        }
    }
    SettingsContent(state, actions, healthConnectRow = { HealthConnectSettingsRow(onOpenHealthConnect) })
}
