package com.github.sebastianp265.aitrainer.feature.history.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.github.sebastianp265.aitrainer.shared.design.DesignFilterChip
import com.github.sebastianp265.aitrainer.shared.design.ListRow
import com.github.sebastianp265.aitrainer.shared.design.SectionHeader
import com.github.sebastianp265.aitrainer.shared.design.Tag
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
internal fun HistoryContent(state: HistoryUiState, onOpenWorkout: (Long) -> Unit, onOpenExternal: (HealthWorkout) -> Unit) {
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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        modifier = Modifier.fillMaxSize().testTag(HistoryTags.HISTORY),
    ) {
        item { FilterRow(filter) { filter = it } }
        if (shown.isEmpty()) item { Text(stringResource(R.string.history_empty), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        shown.groupBy { it.startedAt.toLocalDateTime(zone).date.let { date -> date.year to date.month } }.forEach { (month, items) ->
            item { SectionHeader(formatMonth(month.first, month.second), Modifier.padding(top = 4.dp, bottom = 4.dp)) }
            items.forEach { item ->
                item(key = "${item::class.simpleName}:${item.startedAt.toEpochMilliseconds()}") {
                    when (item) {
                        is HistoryItem.Gym -> GymRow(item.session, zone) { onOpenWorkout(item.session.id) }
                        is HistoryItem.External -> ExternalRow(item.workout, zone) { onOpenExternal(item.workout) }
                    }
                }
            }
        }
        item {
            Text(
                stringResource(R.string.history_footnote),
                Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FilterRow(selected: HistoryFilter, onSelected: (HistoryFilter) -> Unit) {
    FlowRow(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HistoryFilter.entries.forEach { filter ->
            DesignFilterChip(
                selected = filter == selected,
                onClick = { onSelected(filter) },
                label = stringResource(
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

@Composable
private fun GymRow(session: SessionSummary, zone: TimeZone, onClick: () -> Unit) {
    val minutes = session.finishedAt?.let { ((it - session.startedAt).inWholeMinutes).toInt() }
    val parts = listOfNotNull(
        formatWeekdayDate(session.startedAt.toLocalDateTime(zone).date),
        pluralStringResource(R.plurals.exercise_count, session.exerciseCount, session.exerciseCount),
        pluralStringResource(R.plurals.set_count, session.setCount, session.setCount),
        minutes?.let { stringResource(R.string.value_min, it.toString()) },
    )
    ListRow(onClick = onClick) {
        RowText(session.planName ?: stringResource(R.string.history_workout), parts.joinToString(" · "))
        if (session.planId != null) Tag(stringResource(R.string.from_plan))
    }
}

@Composable
private fun RowScope.RowText(title: String, detail: String) {
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ExternalRow(workout: HealthWorkout, zone: TimeZone, onClick: () -> Unit) {
    ListRow(Modifier.testTag(HistoryTags.external(workout.startedAt)), onClick = onClick) {
        RowText(externalWorkoutTitle(workout), "${formatWeekdayDate(workout.startedAt.toLocalDateTime(zone).date)} · ${externalWorkoutDetails(workout)}")
        Tag(
            stringResource(R.string.health_connect),
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
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
                            SessionSummary(1, LocalDate(2026, 11, 7), Instant.parse("2026-11-07T17:00:00Z"), Instant.parse("2026-11-07T18:01:00Z"), 4, 15, null, null),
                        ),
                    ),
                ),
                onOpenWorkout = {},
                onOpenExternal = {},
            )
        }
    }
}
