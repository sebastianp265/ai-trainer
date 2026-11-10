package com.github.sebastianp265.aitrainer.feature.workout.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.integration.workout.SessionSummary
import com.github.sebastianp265.aitrainer.shared.localization.formatDate
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

@Composable
internal fun LogWorkoutContent(
    state: LogUiState,
    onStart: () -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        if (state.isLoading || state.hasActiveSession) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        } else {
            NoActiveSession(state.recentSessions, onStart, onOpenHistory)
        }
    }
}

@Composable
internal fun NoActiveSession(recent: List<SessionSummary>, onStart: () -> Unit, onOpenHistory: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.start_workout)) }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.recent_workouts), style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onOpenHistory) { Text(stringResource(R.string.history_all)) }
            }
        }
        if (recent.isEmpty()) {
            item { Text(stringResource(R.string.no_workouts_yet), style = MaterialTheme.typography.bodyMedium) }
        }
        items(recent, key = { it.id }) { session ->
            ListItem(
                headlineContent = { Text(formatDate(session.date)) },
                supportingContent = { Text(sessionSummary(session.exerciseCount, session.setCount)) },
            )
        }
    }
}

@Composable
private fun sessionSummary(exerciseCount: Int, setCount: Int): String = stringResource(
    R.string.session_summary,
    pluralStringResource(R.plurals.exercise_count, exerciseCount, exerciseCount),
    pluralStringResource(R.plurals.set_count, setCount, setCount),
)

@PreviewLightDark
@Composable
private fun LogWorkoutPreview() {
    val date = LocalDate(2026, 1, 5)
    val start = Instant.fromEpochMilliseconds(1_767_600_000_000)
    MaterialTheme {
        Surface {
            LogWorkoutContent(
                state = LogUiState(
                    isLoading = false,
                    recentSessions = listOf(
                        SessionSummary(1, date, start, start, exerciseCount = 4, setCount = 12),
                        SessionSummary(2, date, start, start, exerciseCount = 1, setCount = 1),
                    ),
                ),
                onStart = {},
                onOpenHistory = {},
            )
        }
    }
}
