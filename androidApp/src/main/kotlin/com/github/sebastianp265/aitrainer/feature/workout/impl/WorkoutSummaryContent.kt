package com.github.sebastianp265.aitrainer.feature.workout.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.shared.design.CardLabel
import com.github.sebastianp265.aitrainer.shared.design.CoachButton
import com.github.sebastianp265.aitrainer.shared.design.DesignIcons
import com.github.sebastianp265.aitrainer.shared.design.StatTile
import com.github.sebastianp265.aitrainer.shared.design.Tag
import com.github.sebastianp265.aitrainer.shared.design.extendedColors
import com.github.sebastianp265.aitrainer.shared.localization.displayName
import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
import com.github.sebastianp265.aitrainer.integration.workout.PreviousWorkout
import com.github.sebastianp265.aitrainer.integration.workout.RecordKind
import com.github.sebastianp265.aitrainer.integration.workout.ReportExercise
import com.github.sebastianp265.aitrainer.integration.workout.WorkoutRecord
import com.github.sebastianp265.aitrainer.integration.workout.WorkoutReport
import com.github.sebastianp265.aitrainer.shared.localization.formatNumber
import com.github.sebastianp265.aitrainer.shared.state.LocalCoachEnabled
import com.github.sebastianp265.aitrainer.shared.state.LocalOpenCoach
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
            Column(Modifier.heightIn(min = 64.dp), verticalArrangement = Arrangement.Center) {
                Text(stringResource(R.string.summary_title), style = MaterialTheme.typography.titleLarge)
                Text(reportSubtitle(report), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ReportStats(report)
            if (report.records.isNotEmpty()) RecordsCard(report.records, state.exercises)
            if (report.plan != null) PlanComparisonCard(report.exercises, state.exercises)
            report.previousWithSamePlan?.let { PreviousLine(it) }
            if (LocalCoachEnabled.current) {
                CoachButton(stringResource(R.string.summary_discuss_with_coach), LocalOpenCoach.current, Modifier.testTag(ReportTags.ASK_COACH))
            }
            Button(onClick = onDone, Modifier.fillMaxWidth().height(48.dp).testTag(ReportTags.DONE)) { Text(stringResource(R.string.summary_done)) }
            TextButton(onClick = onShowAllSets, Modifier.align(Alignment.CenterHorizontally)) { Text(stringResource(R.string.summary_all_sets)) }
        }
    }
}

@Composable
internal fun ReportStats(report: WorkoutReport) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        report.durationMinutes?.let { StatTile(stringResource(R.string.value_minutes, it), stringResource(R.string.summary_time), Modifier.weight(1f)) }
        StatTile(report.workingSetCount.toString(), stringResource(R.string.summary_sets), Modifier.weight(1f))
        report.averageRpe?.let { StatTile(formatNumber(it, 1), stringResource(R.string.summary_avg_rpe), Modifier.weight(1f)) }
    }
}

@Composable
private fun ReportRow(title: String, detail: String, divider: Color?, trailing: @Composable () -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp))
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            trailing()
        }
        if (divider != null) HorizontalDivider(color = divider)
    }
}

@Composable
private fun RecordsCard(records: List<WorkoutRecord>, exercises: Map<String, Exercise>) {
    val locale = LocalConfiguration.current.locales[0]
    val labels = rememberLastTimeLabels()
    Card(
        Modifier.fillMaxWidth().testTag(ReportTags.RECORDS),
        colors = CardDefaults.cardColors(containerColor = extendedColors.recordContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CardLabel(stringResource(R.string.summary_records))
            records.forEachIndexed { index, record ->
                ReportRow(
                    title = exercises.getValue(record.exerciseKey).displayName(locale),
                    detail = recordDetail(record, labels.formatNumber),
                    divider = extendedColors.warningContainer.takeIf { index < records.lastIndex },
                ) {
                    Tag(
                        recordBadge(record),
                        containerColor = extendedColors.warningContainer,
                        contentColor = extendedColors.onWarningContainer,
                    )
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
            CardLabel(stringResource(R.string.summary_vs_plan))
            val compared = exercises.filter { it.difference != null }
            compared.forEachIndexed { index, exercise ->
                val difference = exercise.difference!!
                ReportRow(
                    title = names.getValue(exercise.exerciseKey).displayName(locale),
                    detail = if (exercise.groups.isEmpty()) stringResource(R.string.summary_not_done) else exercise.groups.joinToString(" · ") { it.text(labels) },
                    divider = MaterialTheme.colorScheme.surfaceVariant.takeIf { index < compared.lastIndex },
                ) {
                    val worse = differenceIsWorse(difference)
                    val color = if (worse) extendedColors.warning else extendedColors.success
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (!worse) Icon(DesignIcons.Check, contentDescription = null, Modifier.size(16.dp), tint = color)
                        Text(
                            differenceText(difference),
                            style = MaterialTheme.typography.bodyMedium,
                            color = color,
                            modifier = Modifier.testTag(ReportTags.DIFFERENCE),
                        )
                    }
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
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

internal object ReportTags {
    const val DONE = "report_done"
    const val RECORDS = "report_records"
    const val PLAN_COMPARISON = "report_plan_comparison"
    const val DIFFERENCE = "report_difference"
    const val EDIT = "report_edit"
    const val ASK_COACH = "report_ask_coach"
    const val SET_LINE = "report_set_line"
}
