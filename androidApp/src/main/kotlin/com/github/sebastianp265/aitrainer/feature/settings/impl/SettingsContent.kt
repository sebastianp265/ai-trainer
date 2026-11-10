package com.github.sebastianp265.aitrainer.feature.settings.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.integration.settings.SettingsService

@Composable
internal fun SettingsContent(
    state: SettingsUiState,
    actions: SettingsActions,
    showDebug: Boolean,
    healthConnectRow: @Composable () -> Unit,
) {
    if (state.isLoading) {
        Box(Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { SectionTitle(R.string.settings_ai_trainer) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column {
                    CoachSwitchRow(state.isCoachEnabled, actions::setCoachEnabled)
                    if (state.isCoachEnabled) {
                        HorizontalDivider()
                        LinkRow(
                            title = stringResource(R.string.settings_claude_api_key),
                            subtitle = stringResource(if (state.isClaudeApiKeyStored) R.string.settings_api_key_saved else R.string.settings_api_key_missing),
                            action = stringResource(R.string.settings_change),
                            tag = SettingsTags.API_KEY_ROW,
                            onClick = actions::openApiKey,
                        )
                        HorizontalDivider()
                        LinkRow(
                            title = stringResource(R.string.settings_claude_model),
                            subtitle = modelLabel(state.claudeModel),
                            action = stringResource(R.string.settings_change),
                            tag = SettingsTags.MODEL_ROW,
                            onClick = actions::openModel,
                        )
                        HorizontalDivider()
                        LinkRow(
                            title = stringResource(R.string.settings_coach_memory),
                            subtitle = pluralStringResource(R.plurals.settings_notes_count, state.noteCount, state.noteCount),
                            action = null,
                            tag = SettingsTags.MEMORY_ROW,
                            onClick = actions::openMemory,
                        )
                    }
                }
            }
        }
        if (!state.isCoachEnabled) {
            item { Text(stringResource(R.string.settings_coach_off_explanation), style = MaterialTheme.typography.bodyMedium) }
        }
        item { SectionTitle(R.string.settings_data) }
        item { healthConnectRow() }
        item { SectionTitle(R.string.settings_advanced) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column {
                    LinkRow(
                        title = stringResource(R.string.settings_backup),
                        subtitle = stringResource(R.string.settings_backup_text),
                        action = null,
                        tag = SettingsTags.BACKUP_ROW,
                        onClick = actions::openBackup,
                    )
                    if (showDebug) {
                        HorizontalDivider()
                        LinkRow(
                            title = stringResource(R.string.settings_debug),
                            subtitle = stringResource(R.string.settings_debug_text),
                            action = null,
                            tag = SettingsTags.DEBUG_ROW,
                            onClick = actions::openDebug,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun modelLabel(model: String): String = when (model) {
    "claude-opus-5-5" -> stringResource(R.string.model_opus)
    "claude-sonnet-5-5" -> stringResource(R.string.model_sonnet)
    "claude-haiku-5-5" -> stringResource(R.string.model_haiku)
    else -> model
}

@Composable
private fun CoachSwitchRow(enabled: Boolean, onChange: (Boolean) -> Unit) {
    val description = stringResource(R.string.settings_coach_title)
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

@Composable
private fun LinkRow(title: String, subtitle: String, action: String?, tag: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
        }
        if (action != null) Text(action, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
internal fun SectionTitle(title: Int) {
    Text(stringResource(title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}

internal interface SettingsActions {
    fun setCoachEnabled(enabled: Boolean)
    fun openApiKey()
    fun openModel()
    fun openMemory()
    fun openDebug()
    fun openBackup()
}

internal object SettingsTags {
    const val COACH_SWITCH = "settings_coach_switch"
    const val API_KEY_ROW = "settings_api_key_row"
    const val MODEL_ROW = "settings_model_row"
    const val MEMORY_ROW = "settings_memory_row"
    const val DEBUG_ROW = "settings_debug_row"
    const val BACKUP_ROW = "settings_backup_row"
}

@PreviewLightDark
@Composable
private fun SettingsPreview() {
    MaterialTheme {
        Surface {
            SettingsContent(
                state = SettingsUiState(isLoading = false, isClaudeApiKeyStored = true, isCoachEnabled = true, noteCount = 3),
                actions = object : SettingsActions {
                    override fun setCoachEnabled(enabled: Boolean) = Unit
                    override fun openApiKey() = Unit
                    override fun openModel() = Unit
                    override fun openMemory() = Unit
                    override fun openDebug() = Unit
                    override fun openBackup() = Unit
                },
                showDebug = true,
                healthConnectRow = {},
            )
        }
    }
}
