package com.github.sebastianp265.aitrainer.feature.coach.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeClient
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeFailureKind
import com.github.sebastianp265.aitrainer.integration.coach.CoachService
import com.github.sebastianp265.aitrainer.integration.coach.CoachUsage
import com.github.sebastianp265.aitrainer.integration.settings.SettingsService
import com.github.sebastianp265.aitrainer.shared.localization.formatNumber
import com.github.sebastianp265.aitrainer.shared.localization.formatTime
import com.github.sebastianp265.aitrainer.shared.state.WhileUiSubscribed
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Instant

internal enum class KeyCheck { IDLE, CHECKING, REJECTED, OFFLINE, OTHER }

internal data class ApiKeyUiState(
    val isStored: Boolean = false,
    val checkedAt: Instant? = null,
    val check: KeyCheck = KeyCheck.IDLE,
)

internal class ApiKeyViewModel(
    private val settingsService: SettingsService,
    private val claude: ClaudeClient,
    private val clock: Clock,
) : ViewModel() {

    private val check = MutableStateFlow(KeyCheck.IDLE)

    val uiState: StateFlow<ApiKeyUiState> = combine(
        settingsService.observeIsClaudeApiKeyStored(),
        settingsService.observeApiKeyCheckedAt(),
        check,
    ) { stored, checkedAt, check -> ApiKeyUiState(stored, checkedAt, check) }
        .stateIn(viewModelScope, WhileUiSubscribed, ApiKeyUiState())

    fun saveAndCheck(apiKey: String) {
        if (apiKey.isBlank()) return
        viewModelScope.launch {
            withContext(NonCancellable) {
                val key = apiKey.trim()
                settingsService.saveClaudeApiKey(key)
                check.value = KeyCheck.CHECKING
                when (claude.checkApiKey(key)) {
                    null -> {
                        settingsService.setApiKeyCheckedAt(clock.now())
                        check.value = KeyCheck.IDLE
                    }
                    ClaudeFailureKind.KEY_REJECTED -> {
                        settingsService.setApiKeyCheckedAt(null)
                        check.value = KeyCheck.REJECTED
                    }
                    ClaudeFailureKind.NO_NETWORK -> check.value = KeyCheck.OFFLINE
                    else -> check.value = KeyCheck.OTHER
                }
            }
        }
    }

    fun remove() {
        viewModelScope.launch {
            withContext(NonCancellable) {
                settingsService.removeClaudeApiKey()
                settingsService.setApiKeyCheckedAt(null)
                check.value = KeyCheck.IDLE
            }
        }
    }
}

@Composable
internal fun ApiKeyContent(state: ApiKeyUiState, onSave: (String) -> Unit, onRemove: () -> Unit) {
    var apiKey by rememberSaveable { mutableStateOf("") }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text(stringResource(R.string.settings_claude_api_key)) },
            placeholder = { Text(stringResource(if (state.isStored) R.string.api_key_paste_new else R.string.api_key_paste)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth().testTag(CoachApiKeyTags.FIELD),
        )
        KeyStatus(state)
        Button(
            onClick = {
                onSave(apiKey)
                apiKey = ""
            },
            enabled = apiKey.isNotBlank() && state.check != KeyCheck.CHECKING,
            modifier = Modifier.fillMaxWidth().testTag(CoachApiKeyTags.SAVE),
        ) { Text(stringResource(R.string.api_key_save_and_check)) }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.api_key_where), style = MaterialTheme.typography.labelLarge)
                Text(stringResource(R.string.api_key_step_1), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.api_key_step_2), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.api_key_step_3), style = MaterialTheme.typography.bodyMedium)
            }
        }
        Text(stringResource(R.string.api_key_privacy), style = MaterialTheme.typography.bodyMedium)
        if (state.isStored) {
            TextButton(onClick = onRemove, Modifier.align(Alignment.CenterHorizontally).testTag(CoachApiKeyTags.REMOVE)) {
                Text(stringResource(R.string.api_key_remove), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun KeyStatus(state: ApiKeyUiState) {
    val checkedAt = state.checkedAt
    val (text, error) = when {
        state.check == KeyCheck.CHECKING -> stringResource(R.string.api_key_checking) to false
        state.check == KeyCheck.REJECTED -> stringResource(R.string.api_key_rejected) to true
        state.check == KeyCheck.OFFLINE -> stringResource(R.string.api_key_offline) to true
        state.check == KeyCheck.OTHER -> stringResource(R.string.api_key_other) to true
        checkedAt != null -> stringResource(R.string.api_key_works, formatTime(checkedAt)) to false
        state.isStored -> stringResource(R.string.settings_api_key_saved) to false
        else -> stringResource(R.string.settings_api_key_missing) to false
    }
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        modifier = Modifier.testTag(CoachApiKeyTags.STATUS),
    )
}

internal object CoachApiKeyTags {
    const val FIELD = "api_key_field"
    const val SAVE = "api_key_save"
    const val REMOVE = "api_key_remove"
    const val STATUS = "api_key_status"
}

internal data class ModelUiState(val model: String = SettingsService.DEFAULT_CLAUDE_MODEL, val usage: CoachUsage = CoachUsage(0, 0, 0))

internal class ModelPickerViewModel(
    private val settingsService: SettingsService,
    coachService: CoachService,
) : ViewModel() {

    val uiState: StateFlow<ModelUiState> = combine(
        settingsService.observeClaudeModel(),
        coachService.observeUsageThisMonth(),
    ) { model, usage -> ModelUiState(model, usage) }
        .stateIn(viewModelScope, WhileUiSubscribed, ModelUiState())

    fun select(model: String) {
        viewModelScope.launch { withContext(NonCancellable) { settingsService.setClaudeModel(model) } }
    }
}

@Composable
internal fun ModelPickerContent(state: ModelUiState, onSelect: (String) -> Unit) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(vertical = 4.dp)) {
                SettingsService.CLAUDE_MODELS.forEach { model ->
                    Row(
                        Modifier.fillMaxWidth()
                            .selectable(selected = state.model == model, onClick = { onSelect(model) }, role = Role.RadioButton)
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .testTag(CoachModelTags.option(model)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = state.model == model, onClick = null)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(modelName(model), style = MaterialTheme.typography.bodyLarge)
                            Text(modelHint(model), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
        Text(stringResource(R.string.model_change_note), style = MaterialTheme.typography.bodyMedium)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.model_usage_title), style = MaterialTheme.typography.labelLarge)
                UsageRow(stringResource(R.string.model_usage_messages), formatNumber(state.usage.messages.toDouble()), CoachModelTags.USAGE_MESSAGES)
                UsageRow(stringResource(R.string.model_usage_tokens), formatTokens(state.usage.inputTokens + state.usage.outputTokens), CoachModelTags.USAGE_TOKENS)
                Text(stringResource(R.string.model_usage_cost), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun UsageRow(label: String, value: String, tag: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag(tag))
    }
}

@Composable
private fun formatTokens(tokens: Long): String =
    if (tokens >= MILLION) stringResource(R.string.model_tokens_million, formatNumber(tokens / MILLION.toDouble(), 1)) else formatNumber(tokens.toDouble())

private const val MILLION = 1_000_000L

@Composable
internal fun modelName(model: String): String = when (model) {
    "claude-opus-5-5" -> stringResource(R.string.model_opus)
    "claude-sonnet-5-5" -> stringResource(R.string.model_sonnet)
    "claude-haiku-5-5" -> stringResource(R.string.model_haiku)
    else -> model
}

@Composable
private fun modelHint(model: String): String = when (model) {
    "claude-opus-5-5" -> stringResource(R.string.model_opus_hint)
    "claude-sonnet-5-5" -> stringResource(R.string.model_sonnet_hint)
    "claude-haiku-5-5" -> stringResource(R.string.model_haiku_hint)
    else -> ""
}

internal object CoachModelTags {
    const val USAGE_MESSAGES = "model_usage_messages"
    const val USAGE_TOKENS = "model_usage_tokens"

    fun option(model: String) = "model_option:$model"
}
