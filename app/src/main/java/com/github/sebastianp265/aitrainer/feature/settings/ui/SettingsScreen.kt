package com.github.sebastianp265.aitrainer.feature.settings.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.core.ui.theme.AITrainerTheme
import org.koin.compose.viewmodel.koinViewModel

@Preview(showBackground = true)
@Preview(name = "Polski", locale = "pl", showBackground = true)
@Composable
private fun SettingsPreview() {
    AITrainerTheme {
        SettingsContent(SettingsUiState(isLoading = false, isClaudeApiKeyStored = true), NoOpSettingsActions)
    }
}

private object NoOpSettingsActions : SettingsActions {
    override fun saveClaudeApiKey(apiKey: String) {}
    override fun removeClaudeApiKey() {}
    override fun setClaudeModel(modelId: String) {}
}

object SettingsTags {
    const val CLAUDE_API_KEY = "settings_claude_api_key"
    const val CLAUDE_MODEL = "settings_claude_model"
}

interface SettingsActions {
    fun saveClaudeApiKey(apiKey: String)
    fun removeClaudeApiKey()
    fun setClaudeModel(modelId: String)
}

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actions = remember(viewModel) {
        object : SettingsActions {
            override fun saveClaudeApiKey(apiKey: String) = viewModel.saveClaudeApiKey(apiKey)
            override fun removeClaudeApiKey() = viewModel.removeClaudeApiKey()
            override fun setClaudeModel(modelId: String) = viewModel.setClaudeModel(modelId)
        }
    }
    SettingsContent(state, actions)
}

@Composable
fun SettingsContent(state: SettingsUiState, actions: SettingsActions) {
    if (state.isLoading) {
        Box(Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { SectionTitle(R.string.settings_ai_trainer) }
        item { ClaudeApiKey(state.isClaudeApiKeyStored, actions::saveClaudeApiKey, actions::removeClaudeApiKey) }
        item { ClaudeModelField(state.claudeModel, actions::setClaudeModel) }
    }
}

@Composable
private fun SectionTitle(@StringRes title: Int) {
    Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
}

@Composable
private fun ClaudeApiKey(isStored: Boolean, onSave: (String) -> Unit, onRemove: () -> Unit) {
    var apiKey by rememberSaveable { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(if (isStored) R.string.settings_api_key_saved else R.string.settings_api_key_missing),
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text(stringResource(R.string.settings_claude_api_key)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth().testTag(SettingsTags.CLAUDE_API_KEY),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    onSave(apiKey)
                    apiKey = ""
                },
                enabled = apiKey.isNotBlank(),
            ) { Text(stringResource(R.string.save)) }
            if (isStored) {
                OutlinedButton(onClick = onRemove) { Text(stringResource(R.string.remove)) }
            }
        }
    }
}

@Composable
private fun ClaudeModelField(savedModel: String, onModelChange: (String) -> Unit) {
    var model by rememberSaveable { mutableStateOf(savedModel) }
    OutlinedTextField(
        value = model,
        onValueChange = {
            model = it
            onModelChange(it)
        },
        label = { Text(stringResource(R.string.settings_claude_model)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag(SettingsTags.CLAUDE_MODEL),
    )
}
