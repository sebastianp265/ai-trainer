package com.github.sebastianp265.aitrainer.feature.history.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.integration.health.HealthWorkout
import com.github.sebastianp265.aitrainer.integration.workout.SessionSummary
import com.github.sebastianp265.aitrainer.shared.health.externalWorkoutDetails
import com.github.sebastianp265.aitrainer.shared.health.externalWorkoutTitle
import com.github.sebastianp265.aitrainer.shared.localization.formatMonth
import com.github.sebastianp265.aitrainer.shared.localization.formatWeekdayDate
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@Composable
internal fun HistoryContent(state: HistoryUiState, onOpenExternal: (HealthWorkout) -> Unit) {
    if (state.isLoading) {
        Box(Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
        return
    }
    var filter by rememberSaveable { mutableStateOf(HistoryFilter.ALL) }
    val zone = TimeZone.currentSystemDefault()
    val shown = remember(state.items, filter) {
        state.items.filter { item ->
            when (filter) {
                HistoryFilter.ALL -> true
                HistoryFilter.GYM -> item is HistoryItem.Gym
                HistoryFilter.OTHER_APPS -> item is HistoryItem.External
            }
        }
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize().testTag(HistoryTags.HISTORY),
    ) {
        item { Text(stringResource(R.string.history_title), style = MaterialTheme.typography.headlineSmall) }
        item { FilterRow(filter) { filter = it } }
        if (shown.isEmpty()) item { Text(stringResource(R.string.history_empty)) }
        shown.groupBy { it.startedAt.toLocalDateTime(zone).date.let { date -> date.year to date.month } }.forEach { (month, items) ->
            item { Text(formatMonth(month.first, month.second), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp)) }
            items.forEach { item ->
                item(key = "${item::class.simpleName}:${item.startedAt.toEpochMilliseconds()}") {
                    when (item) {
                        is HistoryItem.Gym -> GymRow(item.session, zone)
                        is HistoryItem.External -> ExternalRow(item.workout, zone) { onOpenExternal(item.workout) }
                    }
                }
            }
        }
        item { Text(stringResource(R.string.history_footnote), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp)) }
    }
}

@Composable
private fun FilterRow(selected: HistoryFilter, onSelected: (HistoryFilter) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        HistoryFilter.entries.forEachIndexed { index, filter ->
            SegmentedButton(
                selected = filter == selected,
                onClick = { onSelected(filter) },
                shape = SegmentedButtonDefaults.itemShape(index, HistoryFilter.entries.size),
            ) {
                Text(
                    stringResource(
                        when (filter) {
                            HistoryFilter.ALL -> R.string.history_all
                            HistoryFilter.GYM -> R.string.history_gym
                            HistoryFilter.OTHER_APPS -> R.string.history_other_apps
                        },
                    ),
                )
            }
        }
    }
}

@Composable
private fun GymRow(session: SessionSummary, zone: TimeZone) {
    val minutes = session.finishedAt?.let { ((it - session.startedAt).inWholeMinutes).toInt() }
    val parts = listOfNotNull(
        formatWeekdayDate(session.startedAt.toLocalDateTime(zone).date),
        pluralStringResource(R.plurals.exercise_count, session.exerciseCount, session.exerciseCount),
        pluralStringResource(R.plurals.set_count, session.setCount, session.setCount),
        minutes?.let { stringResource(R.string.value_min, it.toString()) },
    )
    ListItem(
        headlineContent = { Text(stringResource(R.string.history_workout)) },
        supportingContent = { Text(parts.joinToString(" · ")) },
    )
}

@Composable
private fun ExternalRow(workout: HealthWorkout, zone: TimeZone, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(externalWorkoutTitle(workout)) },
        supportingContent = {
            Column {
                Text("${formatWeekdayDate(workout.startedAt.toLocalDateTime(zone).date)} · ${externalWorkoutDetails(workout)}")
                Text(stringResource(R.string.health_connect), style = MaterialTheme.typography.labelSmall)
            }
        },
        modifier = Modifier.clickable(onClick = onClick).testTag(HistoryTags.external(workout.startedAt)),
    )
}

internal object HistoryTags {
    const val HISTORY = "history"
    const val EXTERNAL_WORKOUT = "history_external_workout"
    fun external(startedAt: Instant) = "history_external:${startedAt.toEpochMilliseconds()}"
}

@PreviewLightDark
@Composable
private fun HistoryContentPreview() {
    MaterialTheme {
        Surface {
            HistoryContent(
                state = HistoryUiState(
                    isLoading = false,
                    items = listOf(
                        HistoryItem.External(HealthWorkout("running", Instant.parse("2026-11-08T07:40:00Z"), 42, 6800.0, 480, 148)),
                        HistoryItem.Gym(
                            SessionSummary(1, LocalDate(2026, 11, 7), Instant.parse("2026-11-07T17:00:00Z"), Instant.parse("2026-11-07T18:01:00Z"), 4, 15),
                        ),
                    ),
                ),
                onOpenExternal = {},
            )
        }
    }
}
