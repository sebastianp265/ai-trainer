package com.github.sebastianp265.aitrainer.feature.workout.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.sebastianp265.aitrainer.R
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
                            Text(reportDetailSubtitle(report), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) }
                },
                actions = {
                    if (state.canEdit) {
                        IconButton(onClick = onEdit, Modifier.testTag(ReportTags.EDIT)) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.workout_edit_description))
                        }
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.workout_delete_description))
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
                FilledTonalButton(onClick = LocalOpenCoach.current, Modifier.fillMaxWidth().testTag(ReportTags.ASK_COACH)) { Text(stringResource(R.string.detail_ask_coach)) }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.workout_delete_title)) },
            text = { Text(stringResource(R.string.workout_delete_text)) },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text(stringResource(R.string.remove)) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun ExerciseReport(exercise: ReportExercise, names: Map<String, Exercise>) {
    val locale = LocalConfiguration.current.locales[0]
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(names.getValue(exercise.exerciseKey).displayName(locale), style = MaterialTheme.typography.titleMedium)
            if (exercise.plannedSets.isNotEmpty()) {
                Text(stringResource(R.string.detail_plan_line, plannedLine(exercise)), style = MaterialTheme.typography.bodyMedium)
            }
            var workingNumber = 0
            exercise.sets.forEach { set ->
                SetLine(if (set.isWarmup) null else ++workingNumber, set)
            }
            val difference = exercise.difference
            if (difference != null && !difference.addedToPlan) {
                Text(
                    stringResource(R.string.detail_diff_vs_plan, differenceText(difference)),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (differenceIsWorse(difference)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
                missingSetsText(exercise)?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
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
private fun plannedLine(exercise: ReportExercise): String =
    planExercises(exercise.plannedSets).flatMap { it.lines }.filterIsInstance<PlanLine.Working>().map { line ->
        listOf(planWorkingText(line), planWorkingRpeText(line)).filter { it.isNotEmpty() }.joinToString(" · ")
    }.joinToString(" · ")

@Composable
private fun SetLine(workingNumber: Int?, set: SetEntry) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().testTag(ReportTags.SET_LINE)) {
        Text(workingNumber?.toString() ?: stringResource(R.string.warmup_short), Modifier.widthIn(min = 32.dp), color = MaterialTheme.colorScheme.primary)
        Text(
            if (set.weightKg > 0) stringResource(R.string.detail_row_load, formatWeight(set.weightKg), set.reps)
            else stringResource(R.string.detail_row_reps, set.reps),
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(set.rpe?.let { stringResource(R.string.plan_rpe, formatWeight(it)) } ?: "–", style = MaterialTheme.typography.bodyMedium)
    }
}
