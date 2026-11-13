package com.github.sebastianp265.aitrainer.feature.ai

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.sebastianp265.aitrainer.feature.ai.impl.AiSettingsActions
import com.github.sebastianp265.aitrainer.feature.ai.impl.AiSettingsContent
import com.github.sebastianp265.aitrainer.feature.ai.impl.AiSettingsViewModel
import com.github.sebastianp265.aitrainer.feature.ai.impl.ApiKeyContent
import com.github.sebastianp265.aitrainer.feature.ai.impl.ApiKeyViewModel
import com.github.sebastianp265.aitrainer.feature.ai.impl.ModelPickerContent
import com.github.sebastianp265.aitrainer.feature.ai.impl.ModelPickerViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun AiSettingsScreen(onOpenApiKey: () -> Unit, onOpenModel: () -> Unit) {
    val viewModel: AiSettingsViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actions = remember(onOpenApiKey, onOpenModel) {
        object : AiSettingsActions {
            override fun openApiKey() = onOpenApiKey()
            override fun openModel() = onOpenModel()
        }
    }
    AiSettingsContent(state, actions)
}

@Composable
internal fun ApiKeyScreen() {
    val viewModel: ApiKeyViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ApiKeyContent(state, onSave = viewModel::saveAndCheck, onRemove = viewModel::remove)
}

@Composable
internal fun ModelPickerScreen() {
    val viewModel: ModelPickerViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ModelPickerContent(state, onSelect = viewModel::select, onTune = viewModel::tune)
}
