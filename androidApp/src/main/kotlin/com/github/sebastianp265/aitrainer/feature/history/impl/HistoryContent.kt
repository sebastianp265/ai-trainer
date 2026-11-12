package com.github.sebastianp265.aitrainer.feature.history.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.shared.design.ListRow
import com.github.sebastianp265.aitrainer.shared.design.SectionHeader
import com.github.sebastianp265.aitrainer.shared.design.Tag
import com.github.sebastianp265.aitrainer.integration.workout.SessionSummary
import com.github.sebastianp265.aitrainer.shared.localization.formatMonth
import com.github.sebastianp265.aitrainer.shared.localization.formatWeekdayDate
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@Composable
internal fun HistoryContent(state: HistoryUiState, onOpenWorkout: (Long) -> Unit) {
    if (state.isLoading) {
        Box(Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
        return
    }
    val zone = TimeZone.currentSystemDefault()
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        modifier = Modifier.fillMaxSize().testTag(HistoryTags.HISTORY),
    ) {
        if (state.sessions.isEmpty()) item { Text(stringResource(R.string.history_empty), Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        state.sessions.groupBy { it.startedAt.toLocalDateTime(zone).date.let { date -> date.year to date.month } }.forEach { (month, sessions) ->
            item { SectionHeader(formatMonth(month.first, month.second), Modifier.padding(top = 4.dp, bottom = 4.dp)) }
            sessions.forEach { session ->
                item(key = session.id) { GymRow(session, zone) { onOpenWorkout(session.id) } }
            }
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

internal object HistoryTags {
    const val HISTORY = "history"
}

@PreviewLightDark
@Composable
private fun HistoryContentPreview() {
    MaterialTheme {
        Surface {
            HistoryContent(
                state = HistoryUiState(
                    isLoading = false,
                    sessions = listOf(
                        SessionSummary(1, LocalDate(2026, 11, 7), Instant.parse("2026-11-07T17:00:00Z"), Instant.parse("2026-11-07T18:01:00Z"), 4, 15, null, null),
                    ),
                ),
                onOpenWorkout = {},
            )
        }
    }
}
