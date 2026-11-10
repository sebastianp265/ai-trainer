package com.github.sebastianp265.aitrainer.feature.coach.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.integration.coach.CoachLogEntry
import com.github.sebastianp265.aitrainer.integration.coach.CoachLogKind
import com.github.sebastianp265.aitrainer.integration.coach.CoachService
import com.github.sebastianp265.aitrainer.integration.debug.DebugCounts
import com.github.sebastianp265.aitrainer.integration.debug.DebugService
import com.github.sebastianp265.aitrainer.integration.health.HealthService
import com.github.sebastianp265.aitrainer.integration.settings.SettingsService
import com.github.sebastianp265.aitrainer.shared.localization.formatDateTime
import com.github.sebastianp265.aitrainer.shared.localization.formatTime
import com.github.sebastianp265.aitrainer.shared.state.WhileUiSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Instant

internal data class DebugUiState(
    val log: List<CoachLogEntry> = emptyList(),
    val counts: DebugCounts = DebugCounts(0, 0, 0, 0, 0),
    val lastSync: Instant? = null,
    val fakeClaude: Boolean = false,
    val fakeHealth: Boolean = false,
)

internal class DebugViewModel(
    private val coachService: CoachService,
    private val settingsService: SettingsService,
    debugService: DebugService,
    healthService: HealthService,
) : ViewModel() {

    private val data = combine(coachService.observeLog(), debugService.observeCounts(), healthService.observeLastSync()) { log, counts, sync -> Triple(log, counts, sync) }

    val uiState: StateFlow<DebugUiState> = combine(
        data,
        settingsService.observeUseFakeClaude(),
        settingsService.observeUseFakeHealth(),
    ) { (log, counts, sync), fakeClaude, fakeHealth -> DebugUiState(log, counts, sync, fakeClaude, fakeHealth) }
        .stateIn(viewModelScope, WhileUiSubscribed, DebugUiState())

    fun setFakeClaude(use: Boolean) {
        viewModelScope.launch { settingsService.setUseFakeClaude(use) }
    }

    fun setFakeHealth(use: Boolean) {
        viewModelScope.launch { settingsService.setUseFakeHealth(use) }
    }

    fun clearLog() {
        viewModelScope.launch { coachService.clearLog() }
    }
}

@Composable
internal fun DebugContent(state: DebugUiState, onFakeClaude: (Boolean) -> Unit, onFakeHealth: (Boolean) -> Unit, onClearLog: () -> Unit) {
    var showDetails by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SwitchRow(R.string.debug_fake_claude, state.fakeClaude, onFakeClaude, DebugTags.FAKE_CLAUDE)
                SwitchRow(R.string.debug_fake_health, state.fakeHealth, onFakeHealth, DebugTags.FAKE_HEALTH)
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.debug_claude_log), style = MaterialTheme.typography.labelLarge)
                if (state.log.isEmpty()) Text(stringResource(R.string.debug_log_empty), style = MaterialTheme.typography.bodyMedium)
                state.log.forEach { entry -> LogRow(entry, showDetails) }
                Row {
                    TextButton(onClick = { showDetails = !showDetails }, Modifier.testTag(DebugTags.DETAILS)) { Text(stringResource(R.string.debug_show_details)) }
                    TextButton(onClick = onClearLog) { Text(stringResource(R.string.debug_clear_log)) }
                }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.debug_sync), style = MaterialTheme.typography.labelLarge)
                Text(state.lastSync?.let { formatDateTime(it) } ?: stringResource(R.string.debug_never_synced), style = MaterialTheme.typography.bodyMedium)
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.debug_database), style = MaterialTheme.typography.labelLarge)
                CountRow(R.string.debug_schema, state.counts.schemaVersion)
                CountRow(R.string.debug_workouts, state.counts.workouts)
                CountRow(R.string.debug_plans, state.counts.plans)
                CountRow(R.string.debug_health_days, state.counts.healthDays)
                CountRow(R.string.debug_notes, state.counts.coachNotes)
            }
        }
        Text(stringResource(R.string.debug_footer), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SwitchRow(label: Int, checked: Boolean, onChange: (Boolean) -> Unit, tag: String) {
    val description = stringResource(label)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(description, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange, modifier = Modifier.semantics { contentDescription = description }.testTag(tag))
    }
}

@Composable
private fun LogRow(entry: CoachLogEntry, showDetails: Boolean) {
    Column(Modifier.testTag(DebugTags.LOG_ENTRY)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(formatTime(entry.at), style = MaterialTheme.typography.bodySmall)
            Text(
                entry.summary,
                style = MaterialTheme.typography.bodySmall,
                color = if (entry.kind == CoachLogKind.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
        }
        if (showDetails) Text(entry.details, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun CountRow(label: Int, value: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(label), style = MaterialTheme.typography.bodyMedium)
        Text(value.toString(), style = MaterialTheme.typography.bodyMedium)
    }
}

internal object DebugTags {
    const val FAKE_CLAUDE = "debug_fake_claude"
    const val FAKE_HEALTH = "debug_fake_health"
    const val DETAILS = "debug_details"
    const val LOG_ENTRY = "debug_log_entry"
}
