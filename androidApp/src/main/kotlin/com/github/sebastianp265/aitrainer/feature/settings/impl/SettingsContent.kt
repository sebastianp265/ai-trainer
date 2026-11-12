package com.github.sebastianp265.aitrainer.feature.settings.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
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
import com.github.sebastianp265.aitrainer.shared.design.DesignIcons
import com.github.sebastianp265.aitrainer.shared.design.GroupCard
import com.github.sebastianp265.aitrainer.shared.design.GroupRow
import com.github.sebastianp265.aitrainer.shared.design.SectionHeader
import com.github.sebastianp265.aitrainer.shared.design.extendedColors
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon

@Composable
internal fun SettingsContent(
    state: SettingsUiState,
    actions: SettingsActions,
    healthDataSection: @Composable () -> Unit,
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
        item { SectionHeader(stringResource(R.string.settings_ai_trainer)) }
        item {
            GroupCard {
                CoachSwitchRow(state.isCoachEnabled, divider = state.isCoachEnabled, onChange = actions::setCoachEnabled)
                if (state.isCoachEnabled) {
                    LinkRow(
                        title = stringResource(R.string.settings_claude_api_key),
                        tag = SettingsTags.API_KEY_ROW,
                        action = stringResource(R.string.settings_change),
                        onClick = actions::openApiKey,
                    ) {
                        if (state.isClaudeApiKeyStored) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(DesignIcons.Check, contentDescription = null, Modifier.size(16.dp), tint = extendedColors.success)
                                Text(stringResource(R.string.settings_api_key_saved), style = MaterialTheme.typography.bodyMedium, color = extendedColors.success)
                            }
                        } else {
                            Subtitle(stringResource(R.string.settings_api_key_missing))
                        }
                    }
                    LinkRow(
                        title = stringResource(R.string.settings_claude_model),
                        tag = SettingsTags.MODEL_ROW,
                        action = stringResource(R.string.settings_change),
                        onClick = actions::openModel,
                    ) { Subtitle(modelLabel(state.claudeModel)) }
                    LinkRow(
                        title = stringResource(R.string.settings_coach_memory),
                        tag = SettingsTags.MEMORY_ROW,
                        action = null,
                        divider = false,
                        onClick = actions::openMemory,
                    ) { Subtitle(pluralStringResource(R.plurals.settings_notes_count, state.noteCount, state.noteCount)) }
                }
            }
        }
        if (!state.isCoachEnabled) {
            item {
                Text(
                    stringResource(R.string.settings_coach_off_explanation),
                    Modifier.padding(horizontal = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item { SectionHeader(stringResource(if (state.isCoachEnabled) R.string.settings_coach_data_access else R.string.settings_health_data)) }
        item { healthDataSection() }
        item { SectionHeader(stringResource(R.string.settings_advanced)) }
        item {
            GroupCard {
                LinkRow(
                    title = stringResource(R.string.settings_backup),
                    tag = SettingsTags.BACKUP_ROW,
                    action = null,
                    divider = false,
                    onClick = actions::openBackup,
                ) { Subtitle(stringResource(R.string.settings_backup_text)) }
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
private fun CoachSwitchRow(enabled: Boolean, divider: Boolean, onChange: (Boolean) -> Unit) {
    val description = stringResource(R.string.settings_coach_title)
    GroupRow(divider = divider) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.settings_coach_title), style = MaterialTheme.typography.bodyLarge)
            Subtitle(stringResource(if (enabled) R.string.settings_coach_on_text else R.string.settings_coach_off))
        }
        Switch(
            checked = enabled,
            onCheckedChange = onChange,
            modifier = Modifier.padding(end = 12.dp).semantics { contentDescription = description }.testTag(SettingsTags.COACH_SWITCH),
        )
    }
}

@Composable
private fun LinkRow(
    title: String,
    tag: String,
    action: String?,
    onClick: () -> Unit,
    divider: Boolean = true,
    subtitle: @Composable () -> Unit,
) {
    GroupRow(Modifier.testTag(tag), onClick = onClick, divider = divider) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle()
        }
        if (action != null) {
            Text(action, Modifier.padding(end = 12.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        } else {
            Icon(DesignIcons.ChevronRight, contentDescription = null, Modifier.padding(end = 12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Subtitle(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

internal interface SettingsActions {
    fun setCoachEnabled(enabled: Boolean)
    fun openApiKey()
    fun openModel()
    fun openMemory()
    fun openBackup()
}

internal object SettingsTags {
    const val COACH_SWITCH = "settings_coach_switch"
    const val API_KEY_ROW = "settings_api_key_row"
    const val MODEL_ROW = "settings_model_row"
    const val MEMORY_ROW = "settings_memory_row"
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
                    override fun openBackup() = Unit
                },
                healthDataSection = {},
            )
        }
    }
}
