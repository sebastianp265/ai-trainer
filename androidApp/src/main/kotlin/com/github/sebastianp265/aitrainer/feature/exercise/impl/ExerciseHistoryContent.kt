package com.github.sebastianp265.aitrainer.feature.exercise.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.shared.design.CardLabel
import com.github.sebastianp265.aitrainer.shared.design.CoachButton
import com.github.sebastianp265.aitrainer.shared.design.StatTile
import com.github.sebastianp265.aitrainer.shared.design.extendedColors
import com.github.sebastianp265.aitrainer.shared.localization.displayName
import com.github.sebastianp265.aitrainer.integration.exercise.Equipment
import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
import com.github.sebastianp265.aitrainer.integration.exercise.MuscleGroup
import com.github.sebastianp265.aitrainer.integration.workout.BestSet
import com.github.sebastianp265.aitrainer.integration.workout.ExerciseHistory
import com.github.sebastianp265.aitrainer.integration.workout.ExerciseSessionEntry
import com.github.sebastianp265.aitrainer.integration.workout.LastTimeGroup
import com.github.sebastianp265.aitrainer.shared.charts.DailyPoint
import com.github.sebastianp265.aitrainer.shared.charts.LineChart
import com.github.sebastianp265.aitrainer.shared.localization.formatNumber
import com.github.sebastianp265.aitrainer.shared.localization.formatWeight
import com.github.sebastianp265.aitrainer.shared.localization.formatWeekdayDate
import com.github.sebastianp265.aitrainer.shared.state.LocalCoachEnabled
import com.github.sebastianp265.aitrainer.shared.state.LocalOpenCoach
import com.github.sebastianp265.aitrainer.shared.workout.rememberLastTimeLabels
import com.github.sebastianp265.aitrainer.shared.workout.text
import kotlinx.datetime.LocalDate

@Composable
internal fun ExerciseHistoryContent(state: ExerciseHistoryUiState?, onOpenWorkout: (Long) -> Unit) {
    if (state == null) {
        Box(Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
        return
    }
    val locale = LocalConfiguration.current.locales[0]
    val history = state.history
    val coachEnabled = LocalCoachEnabled.current
    val openCoach = LocalOpenCoach.current
    val oneRepMaxPoints = history.sessions.reversed().mapNotNull { entry -> entry.estimatedOneRepMaxKg?.let { DailyPoint(entry.date, it) } }
    val rpePoints = history.sessions.reversed().mapNotNull { entry -> entry.averageRpe?.let { DailyPoint(entry.date, it) } }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.heightIn(min = 56.dp), verticalArrangement = Arrangement.Center) {
                Text(state.exercise.displayName(locale), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(
                        R.string.exercise_history_subtitle,
                        state.exercise.primaryMuscles.joinToString { it.displayName(locale) },
                        pluralStringResource(R.plurals.workout_count, history.sessionCount, history.sessionCount),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (history.sessions.isEmpty()) {
            item { Text(stringResource(R.string.exercise_history_empty), style = MaterialTheme.typography.bodyLarge) }
            return@LazyColumn
        }
        item { Stats(history) }
        if (oneRepMaxPoints.size >= 2) {
            item {
                HistoryCard(stringResource(R.string.exercise_history_one_rep_max_chart)) {
                    LineChart(oneRepMaxPoints, oneRepMaxPoints)
                }
            }
        }
        if (rpePoints.size >= 2) {
            item {
                HistoryCard(stringResource(R.string.exercise_history_rpe_chart)) {
                    LineChart(rpePoints, rpePoints)
                    Text(stringResource(R.string.exercise_history_rpe_caption), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            HistoryCard(stringResource(R.string.exercise_history_sessions_title)) {
                history.sessions.forEach { SessionRow(it) { onOpenWorkout(it.sessionId) } }
            }
        }
        if (coachEnabled) {
            item {
                CoachButton(stringResource(R.string.history_ask_coach), openCoach)
            }
        }
    }
}

@Composable
private fun Stats(history: ExerciseHistory) {
    val estimate = history.estimatedOneRepMaxKg
    val bestSet = history.bestSet
    val change = history.oneRepMaxChangeKg
    if (estimate == null && bestSet == null && change == null) return
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        estimate?.let { StatTile(stringResource(R.string.value_kg, formatNumber(it)), stringResource(R.string.exercise_history_estimate), Modifier.weight(1f)) }
        bestSet?.let {
            StatTile(
                stringResource(R.string.exercise_history_best_set_value, formatWeight(it.weightKg), it.reps),
                stringResource(R.string.exercise_history_best_set),
                Modifier.weight(1f),
            )
        }
        change?.let { StatTile(stringResource(R.string.value_kg, formatNumber(it, 1, signed = true)), stringResource(R.string.exercise_history_change), Modifier.weight(1f)) }
    }
}

@Composable
private fun HistoryCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CardLabel(title)
            content()
        }
    }
}

@Composable
private fun SessionRow(entry: ExerciseSessionEntry, onClick: () -> Unit) {
    val labels = rememberLastTimeLabels()
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp).testTag(ExerciseHistoryTags.SESSION),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                listOfNotNull(formatWeekdayDate(entry.date), entry.planName).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (entry.isRecord) {
                Text(
                    "· " + stringResource(R.string.progress_record),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = extendedColors.success,
                )
            }
        }
        Text(entry.groups.joinToString(" · ") { it.text(labels) }, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium))
    }
}

internal object ExerciseHistoryTags {
    const val SESSION = "exercise_history_session"
}

@PreviewLightDark
@Composable
private fun ExerciseHistoryPreview() {
    val chest = MuscleGroup("CHEST", mapOf("en" to "Chest"))
    val exercise = Exercise("BENCH", mapOf("en" to "Bench press"), Equipment("BARBELL", mapOf("en" to "Barbell")), listOf(chest), emptyList())
    val sessions = listOf(
        ExerciseSessionEntry(2, LocalDate(2026, 11, 3), "Push A", listOf(LastTimeGroup(3, 8, 80.0, 7.5, 7.5)), 101.3, 7.5, true),
        ExerciseSessionEntry(1, LocalDate(2026, 10, 27), null, listOf(LastTimeGroup(2, 8, 77.5, 8.0, 8.0), LastTimeGroup(1, 7, 77.5, 9.0, 9.0)), 97.0, 8.3, false),
    )
    MaterialTheme {
        ExerciseHistoryContent(
            state = ExerciseHistoryUiState(exercise, ExerciseHistory("BENCH", 2, 101.3, BestSet(80.0, 8), 4.3, sessions)),
            onOpenWorkout = {},
        )
    }
}
