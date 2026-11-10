package com.github.sebastianp265.aitrainer.feature.volume.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.shared.localization.displayName
import com.github.sebastianp265.aitrainer.integration.exercise.MuscleGroup
import com.github.sebastianp265.aitrainer.integration.volume.DEFAULT_MAX_WEEKLY_SETS
import com.github.sebastianp265.aitrainer.integration.volume.DEFAULT_MIN_WEEKLY_SETS
import com.github.sebastianp265.aitrainer.integration.volume.TargetSource
import com.github.sebastianp265.aitrainer.integration.volume.WeeklyTarget
import com.github.sebastianp265.aitrainer.shared.localization.formatDayMonth
import kotlinx.datetime.LocalDate

@Composable
internal fun VolumeTargetsContent(
    state: VolumeTargetsUiState,
    onShowTargetsChange: (Boolean) -> Unit,
    onTargetChange: (groupKey: String, minSets: Int, maxSets: Int) -> Unit,
    onRestoreDefaults: () -> Unit,
    banner: @Composable () -> Unit = {},
) {
    if (state.isLoading) {
        Box(Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
        return
    }
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column {
            Text(stringResource(R.string.targets_title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.targets_subtitle), style = MaterialTheme.typography.bodyMedium)
        }
        banner()
        ShowTargetsRow(state.showTargets, onShowTargetsChange)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                state.rows.forEach { row -> TargetRowEditor(row, onTargetChange) }
            }
        }
        OutlinedButton(onClick = onRestoreDefaults, Modifier.fillMaxWidth().testTag(VolumeTags.RESTORE_DEFAULTS)) {
            Text(stringResource(R.string.targets_restore, DEFAULT_MIN_WEEKLY_SETS, DEFAULT_MAX_WEEKLY_SETS))
        }
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.targets_why_title, DEFAULT_MIN_WEEKLY_SETS, DEFAULT_MAX_WEEKLY_SETS), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.targets_why_text), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.targets_why_fractions), style = MaterialTheme.typography.bodyMedium)
            }
        }
        Text(stringResource(R.string.targets_footer), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ShowTargetsRow(showTargets: Boolean, onChange: (Boolean) -> Unit) {
    val description = stringResource(R.string.targets_show)
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.targets_show), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.targets_show_text), style = MaterialTheme.typography.bodyMedium)
            }
            Switch(
                checked = showTargets,
                onCheckedChange = onChange,
                modifier = Modifier.semantics { contentDescription = description }.testTag(VolumeTags.SHOW_TARGETS),
            )
        }
    }
}

@Composable
private fun TargetRowEditor(row: TargetRow, onTargetChange: (String, Int, Int) -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val name = row.group.displayName(locale)
    var minText by rememberSaveable(row.target.minSets) { mutableStateOf(row.target.minSets.toString()) }
    var maxText by rememberSaveable(row.target.maxSets) { mutableStateOf(row.target.maxSets.toString()) }
    val fromDescription = stringResource(R.string.targets_from, name)
    val toDescription = stringResource(R.string.targets_to, name)

    fun commit(min: String, max: String) {
        val from = min.toIntOrNull() ?: return
        val to = max.toIntOrNull() ?: return
        if (from in 0..to && (from != row.target.minSets || to != row.target.maxSets)) onTargetChange(row.target.groupKey, from, to)
    }

    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge)
            Text(sourceText(row.target), style = MaterialTheme.typography.bodySmall)
        }
        OutlinedTextField(
            value = minText,
            onValueChange = { minText = it; commit(it, maxText) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(72.dp).semantics { contentDescription = fromDescription }.testTag(VolumeTags.min(row.target.groupKey)),
        )
        Text("–", Modifier.padding(horizontal = 6.dp))
        OutlinedTextField(
            value = maxText,
            onValueChange = { maxText = it; commit(minText, it) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(72.dp).semantics { contentDescription = toDescription }.testTag(VolumeTags.max(row.target.groupKey)),
        )
    }
}

@Composable
private fun sourceText(target: WeeklyTarget): String {
    val date = target.date?.let { formatDayMonth(it) }.orEmpty()
    return when (target.source) {
        TargetSource.DEFAULT -> stringResource(R.string.targets_source_default)
        TargetSource.USER -> stringResource(R.string.targets_source_user, date)
        TargetSource.COACH ->
            if (target.reason != null) stringResource(R.string.targets_source_coach, date, target.reason!!)
            else stringResource(R.string.targets_source_coach_no_reason, date)
    }
}

internal object VolumeTags {
    const val SHOW_TARGETS = "targets_show"
    const val RESTORE_DEFAULTS = "targets_restore_defaults"

    fun min(groupKey: String) = "target_min:$groupKey"

    fun max(groupKey: String) = "target_max:$groupKey"
}

@PreviewLightDark
@Composable
private fun VolumeTargetsPreview() {
    fun group(key: String, name: String) = MuscleGroup(key, mapOf("en" to name))
    MaterialTheme {
        VolumeTargetsContent(
            state = VolumeTargetsUiState(
                isLoading = false,
                rows = listOf(
                    TargetRow(group("CHEST", "Chest"), WeeklyTarget("CHEST", 10, 16, TargetSource.COACH, LocalDate(2026, 11, 2), "progression every 2 weeks")),
                    TargetRow(group("LEGS", "Legs"), WeeklyTarget("LEGS", 10, 20, TargetSource.DEFAULT, null, null)),
                    TargetRow(group("ABS", "Abs"), WeeklyTarget("ABS", 6, 10, TargetSource.USER, LocalDate(2026, 10, 20), null)),
                ),
            ),
            onShowTargetsChange = {},
            onTargetChange = { _, _, _ -> },
            onRestoreDefaults = {},
        )
    }
}
