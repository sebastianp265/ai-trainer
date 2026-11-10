package com.github.sebastianp265.aitrainer.feature.settings.impl

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.github.sebastianp265.aitrainer.R

@Composable
internal fun SettingsContent(state: SettingsUiState, actions: SettingsActions, healthConnectRow: @Composable () -> Unit) {
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
        item { CoachSwitchRow(state.isCoachEnabled, actions::setCoachEnabled) }
        if (state.isCoachEnabled) {
            item { ClaudeApiKeyField(state.isClaudeApiKeyStored, actions::saveClaudeApiKey, actions::removeClaudeApiKey) }
            item { ClaudeModelField(state.claudeModel, actions::setClaudeModel) }
        } else {
            item { Text(stringResource(R.string.settings_coach_off_explanation), style = MaterialTheme.typography.bodyMedium) }
        }
        item { SectionTitle(R.string.settings_data) }
        item { healthConnectRow() }
    }
}

@Composable
internal fun CoachSwitchRow(enabled: Boolean, onChange: (Boolean) -> Unit) {
    val description = stringResource(R.string.settings_coach_title)
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_coach_title), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(if (enabled) R.string.settings_coach_on_text else R.string.settings_coach_off),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onChange,
                modifier = Modifier.semantics { contentDescription = description }.testTag(SettingsTags.COACH_SWITCH),
            )
        }
    }
}

@Composable
internal fun SectionTitle(@StringRes title: Int) {
    Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
}

@Composable
internal fun ClaudeApiKeyField(isStored: Boolean, onSave: (String) -> Unit, onRemove: () -> Unit) {
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
internal fun ClaudeModelField(savedModel: String, onModelChange: (String) -> Unit) {
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

internal interface SettingsActions {
    fun saveClaudeApiKey(apiKey: String)
    fun removeClaudeApiKey()
    fun setClaudeModel(modelId: String)
    fun setCoachEnabled(enabled: Boolean)
}

internal object SettingsTags {
    const val CLAUDE_API_KEY = "settings_claude_api_key"
    const val CLAUDE_MODEL = "settings_claude_model"
    const val COACH_SWITCH = "settings_coach_switch"
}

@PreviewLightDark
@Composable
private fun SettingsPreview() {
    MaterialTheme {
        Surface {
            SettingsContent(
                state = SettingsUiState(isLoading = false, isClaudeApiKeyStored = true, isCoachEnabled = true),
                actions = object : SettingsActions {
                    override fun saveClaudeApiKey(apiKey: String) = Unit
                    override fun removeClaudeApiKey() = Unit
                    override fun setClaudeModel(modelId: String) = Unit
                    override fun setCoachEnabled(enabled: Boolean) = Unit
                },
                healthConnectRow = {},
            )
        }
    }
}
