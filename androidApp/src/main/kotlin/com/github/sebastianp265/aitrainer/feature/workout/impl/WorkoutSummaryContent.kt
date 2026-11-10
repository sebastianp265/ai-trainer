package com.github.sebastianp265.aitrainer.feature.workout.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.shared.localization.displayName
import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
import com.github.sebastianp265.aitrainer.integration.workout.PreviousWorkout
import com.github.sebastianp265.aitrainer.integration.workout.RecordKind
import com.github.sebastianp265.aitrainer.integration.workout.ReportExercise
import com.github.sebastianp265.aitrainer.integration.workout.WorkoutRecord
import com.github.sebastianp265.aitrainer.integration.workout.WorkoutReport
import com.github.sebastianp265.aitrainer.shared.localization.formatNumber
import com.github.sebastianp265.aitrainer.shared.localization.formatWeekdayDate
import com.github.sebastianp265.aitrainer.shared.workout.rememberLastTimeLabels
import com.github.sebastianp265.aitrainer.shared.workout.text

@Composable
internal fun WorkoutSummaryContent(
    state: WorkoutReportUiState,
    onDone: () -> Unit,
    onShowAllSets: () -> Unit,
) {
    val report = state.report
    Scaffold { padding ->
        if (state.isLoading || report == null) {
            Box(Modifier.padding(padding).fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
            return@Scaffold
        }
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column {
                Text(stringResource(R.string.summary_title), style = MaterialTheme.typography.headlineSmall)
                Text(reportSubtitle(report), style = MaterialTheme.typography.bodyMedium)
            }
            ReportStats(report)
            if (report.records.isNotEmpty()) RecordsCard(report.records, state.exercises)
            if (report.plan != null) PlanComparisonCard(report.exercises, state.exercises)
            report.previousWithSamePlan?.let { PreviousLine(it) }
            Button(onClick = onDone, Modifier.fillMaxWidth().testTag(ReportTags.DONE)) { Text(stringResource(R.string.summary_done)) }
            TextButton(onClick = onShowAllSets, Modifier.align(Alignment.CenterHorizontally)) { Text(stringResource(R.string.summary_all_sets)) }
        }
    }
}

@Composable
internal fun ReportStats(report: WorkoutReport) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        report.durationMinutes?.let { StatCard(stringResource(R.string.value_minutes, it), stringResource(R.string.summary_time), Modifier.weight(1f)) }
        StatCard(report.workingSetCount.toString(), stringResource(R.string.summary_sets), Modifier.weight(1f))
        report.averageRpe?.let { StatCard(formatNumber(it, 1), stringResource(R.string.summary_avg_rpe), Modifier.weight(1f)) }
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun RecordsCard(records: List<WorkoutRecord>, exercises: Map<String, Exercise>) {
    val locale = LocalConfiguration.current.locales[0]
    val labels = rememberLastTimeLabels()
    Card(Modifier.fillMaxWidth().testTag(ReportTags.RECORDS)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.summary_records), style = MaterialTheme.typography.labelLarge)
            records.forEach { record ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(exercises.getValue(record.exerciseKey).displayName(locale), style = MaterialTheme.typography.bodyLarge)
                        Text(recordDetail(record, labels.formatNumber), style = MaterialTheme.typography.bodyMedium)
                    }
                    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.tertiaryContainer) {
                        Text(recordBadge(record), Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun recordDetail(record: WorkoutRecord, number: (Double) -> String): String = when (record.kind) {
    RecordKind.ONE_REP_MAX -> stringResource(
        R.string.record_one_rep_max_detail,
        stringResource(R.string.last_time_set_format, record.reps, number(record.weightKg)),
        number(record.estimatedOneRepMaxKg),
    )
    RecordKind.REPS_AT_WEIGHT -> stringResource(R.string.record_reps_detail, number(record.weightKg), record.reps)
}

@Composable
private fun recordBadge(record: WorkoutRecord): String = when (record.kind) {
    RecordKind.ONE_REP_MAX -> stringResource(R.string.diff_kg, formatNumber(record.improvement, 1, signed = true))
    RecordKind.REPS_AT_WEIGHT -> stringResource(R.string.diff_reps, formatNumber(record.improvement, signed = true))
}

@Composable
private fun PlanComparisonCard(exercises: List<ReportExercise>, names: Map<String, Exercise>) {
    val locale = LocalConfiguration.current.locales[0]
    val labels = rememberLastTimeLabels()
    Card(Modifier.fillMaxWidth().testTag(ReportTags.PLAN_COMPARISON)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.summary_vs_plan), style = MaterialTheme.typography.labelLarge)
            exercises.forEach { exercise ->
                val difference = exercise.difference ?: return@forEach
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(names.getValue(exercise.exerciseKey).displayName(locale), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            if (exercise.groups.isEmpty()) stringResource(R.string.summary_not_done) else exercise.groups.joinToString(" · ") { it.text(labels) },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Text(
                        differenceText(difference),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (differenceIsWorse(difference)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag(ReportTags.DIFFERENCE),
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviousLine(previous: PreviousWorkout) {
    val parts = listOfNotNull(
        previous.durationMinutes?.let { stringResource(R.string.value_minutes, it) },
        pluralStringResource(R.plurals.set_count, previous.workingSetCount, previous.workingSetCount),
        previous.averageRpe?.let { stringResource(R.string.summary_previous_rpe, formatNumber(it, 1)) },
    )
    Text(
        stringResource(R.string.summary_previous, previous.name, formatWeekdayDate(previous.date), parts.joinToString(" · ")),
        style = MaterialTheme.typography.bodyMedium,
    )
}

internal object ReportTags {
    const val DONE = "report_done"
    const val RECORDS = "report_records"
    const val PLAN_COMPARISON = "report_plan_comparison"
    const val DIFFERENCE = "report_difference"
    const val EDIT = "report_edit"
    const val SET_LINE = "report_set_line"
}
