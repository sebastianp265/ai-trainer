package com.github.sebastianp265.aitrainer.feature.workout.impl

import com.github.sebastianp265.aitrainer.shared.localization.formatDate
import com.github.sebastianp265.aitrainer.integration.workout.WorkoutReport
import com.github.sebastianp265.aitrainer.integration.coach.TopicKind
import com.github.sebastianp265.aitrainer.integration.coach.CoachTopic
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.shared.design.Tag
import com.github.sebastianp265.aitrainer.shared.design.extendedColors
import com.github.sebastianp265.aitrainer.shared.localization.displayName
import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
import com.github.sebastianp265.aitrainer.integration.workout.ReportExercise
import com.github.sebastianp265.aitrainer.integration.workout.SetEntry
import com.github.sebastianp265.aitrainer.shared.localization.formatWeight
import com.github.sebastianp265.aitrainer.shared.state.LocalCoachEnabled
import com.github.sebastianp265.aitrainer.shared.state.LocalOpenCoach
import com.github.sebastianp265.aitrainer.shared.workout.PlanLine
import com.github.sebastianp265.aitrainer.shared.workout.planExercises
import com.github.sebastianp265.aitrainer.shared.workout.planWorkingRpeText
import com.github.sebastianp265.aitrainer.shared.workout.planWorkingText
import com.github.sebastianp265.aitrainer.shared.design.DesignIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WorkoutDetailContent(
    state: WorkoutReportUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val report = state.report
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (report != null) {
                        Column {
                            Text(report.plan?.name ?: stringResource(R.string.workout_empty_title), style = MaterialTheme.typography.titleLarge)
                            Text(reportDetailSubtitle(report), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(DesignIcons.Back, contentDescription = stringResource(R.string.back)) }
                },
                actions = {
                    if (state.canEdit) {
                        IconButton(onClick = onEdit, Modifier.testTag(ReportTags.EDIT)) {
                            Icon(DesignIcons.Edit, contentDescription = stringResource(R.string.workout_edit_description))
                        }
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(DesignIcons.Delete, contentDescription = stringResource(R.string.workout_delete_description))
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading || report == null) {
            Box(Modifier.padding(padding).fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
            return@Scaffold
        }
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ReportStats(report)
            report.exercises.forEach { ExerciseReport(it, state.exercises) }
            if (LocalCoachEnabled.current) {
                val openCoach = LocalOpenCoach.current
                val topic = workoutTopic(report)
                FilledTonalButton(onClick = { openCoach(topic) }, Modifier.fillMaxWidth().height(48.dp).testTag(ReportTags.ASK_COACH)) {
                    Icon(DesignIcons.Coach, contentDescription = null, Modifier.padding(end = 8.dp))
                    Text(stringResource(R.string.detail_ask_coach))
                }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.workout_delete_title)) },
            text = { Text(stringResource(R.string.workout_delete_text)) },
            confirmButton = {
                TextButton(
                    onClick = { confirmDelete = false; onDelete() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.remove)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun ExerciseReport(exercise: ReportExercise, names: Map<String, Exercise>) {
    val locale = LocalConfiguration.current.locales[0]
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(names.getValue(exercise.exerciseKey).displayName(locale), style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp))
            if (exercise.plannedSets.isNotEmpty()) {
                Text(
                    stringResource(R.string.detail_plan_line, plannedLine(exercise)),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            var workingNumber = 0
            exercise.sets.forEach { set ->
                SetLine(if (set.isWarmup) null else ++workingNumber, set)
            }
            val difference = exercise.difference
            if (difference != null && !difference.addedToPlan) {
                val worse = differenceIsWorse(difference)
                Tag(
                    stringResource(R.string.detail_diff_vs_plan, differenceText(difference)),
                    containerColor = if (worse) extendedColors.warningContainer else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (worse) extendedColors.onWarningContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                )
                missingSetsText(exercise)?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable
private fun missingSetsText(exercise: ReportExercise): String? {
    val plannedWorking = exercise.plannedSets.count { !it.isWarmup }
    val doneWorking = exercise.sets.count { !it.isWarmup }
    return when {
        exercise.sets.isEmpty() -> stringResource(R.string.detail_exercise_not_done)
        doneWorking < plannedWorking -> stringResource(R.string.detail_set_not_done, doneWorking + 1)
        else -> null
    }
}

@Composable
internal fun workoutTopic(report: WorkoutReport): CoachTopic =
    CoachTopic(TopicKind.WORKOUT, report.session.id.toString(), report.plan?.name ?: formatDate(report.session.date))

@Composable
private fun plannedLine(exercise: ReportExercise): String =
    planExercises(exercise.plannedSets).flatMap { it.lines }.filterIsInstance<PlanLine.Working>().map { line ->
        listOf(planWorkingText(line), planWorkingRpeText(line)).filter { it.isNotEmpty() }.joinToString(" · ")
    }.joinToString(" · ")

@Composable
private fun SetLine(workingNumber: Int?, set: SetEntry) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag(ReportTags.SET_LINE),
    ) {
        Text(
            workingNumber?.toString() ?: stringResource(R.string.warmup_short),
            Modifier.widthIn(min = 28.dp),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp),
            color = if (workingNumber == null) extendedColors.warning else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            if (set.weightKg > 0) stringResource(R.string.detail_row_load, formatWeight(set.weightKg), set.reps)
            else pluralStringResource(R.plurals.detail_row_reps, set.reps, set.reps),
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
        )
        Text(
            set.rpe?.let { stringResource(R.string.plan_rpe, formatWeight(it)) } ?: "–",
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
