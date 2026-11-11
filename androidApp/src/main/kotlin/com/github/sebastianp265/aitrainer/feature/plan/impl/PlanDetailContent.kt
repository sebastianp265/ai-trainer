package com.github.sebastianp265.aitrainer.feature.plan.impl

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.sp
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
import com.github.sebastianp265.aitrainer.shared.workout.PlanLineRows
import com.github.sebastianp265.aitrainer.shared.workout.planExercises
import kotlinx.datetime.LocalDate
import com.github.sebastianp265.aitrainer.shared.design.DesignIcons
import com.github.sebastianp265.aitrainer.shared.design.DesignOutlinedButton

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
                            Text(planSubtitle(plan), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(DesignIcons.Back, contentDescription = stringResource(R.string.back)) }
                },
                actions = {
                    IconButton(onClick = onEdit) { Icon(DesignIcons.Edit, contentDescription = stringResource(R.string.plan_edit)) }
                    IconButton(onClick = { confirmDelete = true }) { Icon(DesignIcons.Delete, contentDescription = stringResource(R.string.plan_delete)) }
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
            val coachEnabled = LocalCoachEnabled.current
            plan.coachNote?.takeIf { coachEnabled }?.let { CoachNote(it) }
            planExercises(plan.sets).forEach { ExerciseLines(it, state.exercises) }
            Button(onClick = onStart, Modifier.fillMaxWidth().height(48.dp).testTag(PlanTags.START)) { Text(stringResource(R.string.plan_start)) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (coachEnabled) {
                    val openCoach = LocalOpenCoach.current
                    Button(
                        onClick = { openCoach(CoachTopic(TopicKind.PLAN, plan.id.toString(), plan.name)) },
                        modifier = Modifier.weight(1f).height(48.dp).testTag(PlanTags.ASK_COACH),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        ),
                    ) { Text(stringResource(R.string.plan_change_with_coach)) }
                }
                DesignOutlinedButton(onClick = onEdit, Modifier.weight(1f).height(48.dp)) { Text(stringResource(R.string.plan_edit_myself)) }
            }
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
private fun CoachNote(note: String) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(DesignIcons.Coach, contentDescription = null)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.plan_coach_note), style = MaterialTheme.typography.titleSmall)
                Text(note, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ExerciseLines(exercise: PlanExercise, exercises: Map<String, Exercise>) {
    val locale = LocalConfiguration.current.locales[0]
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(exercises.getValue(exercise.exerciseKey).displayName(locale), style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp))
            PlanLineRows(exercise.lines)
        }
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
