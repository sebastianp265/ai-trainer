package com.github.sebastianp265.aitrainer.feature.settings.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import com.github.sebastianp265.aitrainer.shared.design.GroupCard
import com.github.sebastianp265.aitrainer.shared.design.GroupRow
import com.github.sebastianp265.aitrainer.shared.design.SectionHeader
import com.github.sebastianp265.aitrainer.shared.design.LinkRow
import com.github.sebastianp265.aitrainer.shared.design.Subtitle

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
                        title = stringResource(R.string.settings_ai),
                        tag = SettingsTags.AI_ROW,
                        action = null,
                        onClick = actions::openAi,
                    ) { Subtitle(stringResource(R.string.settings_ai_text)) }
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

internal interface SettingsActions {
    fun setCoachEnabled(enabled: Boolean)
    fun openAi()
    fun openMemory()
    fun openBackup()
}

internal object SettingsTags {
    const val COACH_SWITCH = "settings_coach_switch"
    const val AI_ROW = "settings_ai_row"
    const val MEMORY_ROW = "settings_memory_row"
    const val BACKUP_ROW = "settings_backup_row"
}

@PreviewLightDark
@Composable
private fun SettingsPreview() {
    MaterialTheme {
        Surface {
            SettingsContent(
                state = SettingsUiState(isLoading = false, isCoachEnabled = true, noteCount = 3),
                actions = object : SettingsActions {
                    override fun setCoachEnabled(enabled: Boolean) = Unit
                    override fun openAi() = Unit
                    override fun openMemory() = Unit
                    override fun openBackup() = Unit
                },
                healthDataSection = {},
            )
        }
    }
}
