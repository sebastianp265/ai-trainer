package com.github.sebastianp265.aitrainer.feature.plan.impl

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.shared.localization.displayName
import com.github.sebastianp265.aitrainer.integration.exercise.Equipment
import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
import com.github.sebastianp265.aitrainer.integration.plan.Plan
import com.github.sebastianp265.aitrainer.integration.plan.PlanAuthor
import com.github.sebastianp265.aitrainer.integration.plan.PlannedSet
import com.github.sebastianp265.aitrainer.shared.localization.formatLongWeekdayDate
import com.github.sebastianp265.aitrainer.shared.state.LocalCoachEnabled
import com.github.sebastianp265.aitrainer.shared.state.LocalOpenCoach
import com.github.sebastianp265.aitrainer.shared.workout.PlanExercise
import com.github.sebastianp265.aitrainer.shared.workout.PlanLine
import com.github.sebastianp265.aitrainer.shared.workout.planExercises
import com.github.sebastianp265.aitrainer.shared.workout.planNumberText
import com.github.sebastianp265.aitrainer.shared.workout.planWarmupText
import com.github.sebastianp265.aitrainer.shared.workout.planWorkingRpeText
import com.github.sebastianp265.aitrainer.shared.workout.planWorkingText
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlanDetailContent(
    state: PlanDetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onStart: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val plan = state.plan
    LaunchedEffect(state.isLoading, plan) { if (!state.isLoading && plan == null) onBack() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (plan != null) {
                        Column {
                            Text(plan.name, style = MaterialTheme.typography.titleLarge)
                            Text(planSubtitle(plan), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) }
                },
                actions = {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.plan_edit)) }
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.plan_delete)) }
                },
            )
        },
    ) { padding ->
        if (state.isLoading || plan == null) {
            Box(Modifier.padding(padding).fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
            return@Scaffold
        }
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            planExercises(plan.sets).forEach { ExerciseLines(it, state.exercises) }
            Button(onClick = onStart, Modifier.fillMaxWidth().testTag(PlanTags.START)) { Text(stringResource(R.string.plan_start)) }
            if (LocalCoachEnabled.current) {
                FilledTonalButton(onClick = LocalOpenCoach.current, Modifier.fillMaxWidth().testTag(PlanTags.ASK_COACH)) { Text(stringResource(R.string.plan_change_with_coach)) }
            }
            OutlinedButton(onClick = onEdit, Modifier.fillMaxWidth()) { Text(stringResource(R.string.plan_edit_myself)) }
        }
        if (confirmDelete) {
            DeletePlanDialog(plan.name, onConfirm = { confirmDelete = false; onDelete() }, onDismiss = { confirmDelete = false })
        }
    }
}

@Composable
private fun planSubtitle(plan: Plan): String {
    val date = plan.date?.let { formatLongWeekdayDate(it) } ?: stringResource(R.string.plan_no_date)
    val author = when (plan.author) {
        PlanAuthor.USER -> stringResource(R.string.plan_written_by_you)
        PlanAuthor.COACH -> stringResource(R.string.plan_written_by_coach)
    }
    return stringResource(R.string.plan_subtitle, date, author)
}

@Composable
private fun ExerciseLines(exercise: PlanExercise, exercises: Map<String, Exercise>) {
    val locale = LocalConfiguration.current.locales[0]
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(exercises.getValue(exercise.exerciseKey).displayName(locale), style = MaterialTheme.typography.titleMedium)
            exercise.lines.forEach { line ->
                when (line) {
                    is PlanLine.Warmup -> LineRow("R", planWarmupText(line), stringResource(R.string.plan_warmup))
                    is PlanLine.Working -> LineRow(planNumberText(line), planWorkingText(line), planWorkingRpeText(line))
                }
            }
        }
    }
}

@Composable
private fun LineRow(number: String, text: String, trailing: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(number, Modifier.widthIn(min = 40.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(trailing, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
internal fun DeletePlanDialog(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.plan_delete_title)) },
        text = { Text(stringResource(R.string.plan_delete_text, name)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.remove)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@PreviewLightDark
@Composable
private fun PlanDetailPreview() {
    val bench = Exercise("BENCH", mapOf("en" to "Bench press"), Equipment("BARBELL", mapOf("en" to "Barbell")), emptyList(), emptyList())
    MaterialTheme {
        PlanDetailContent(
            state = PlanDetailUiState(
                isLoading = false,
                plan = Plan(
                    1, "Push A", LocalDate(2026, 11, 12), PlanAuthor.COACH,
                    listOf(
                        PlannedSet("BENCH", 10, 40.0, null, true),
                        PlannedSet("BENCH", 8, 82.5, 8.0, false),
                        PlannedSet("BENCH", 8, 82.5, 8.0, false),
                    ),
                ),
                exercises = mapOf("BENCH" to bench),
            ),
            onBack = {},
            onEdit = {},
            onStart = {},
            onDelete = {},
        )
    }
}
