package com.github.sebastianp265.aitrainer.feature.workout.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.shared.localization.displayName
import com.github.sebastianp265.aitrainer.integration.exercise.Equipment
import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
import com.github.sebastianp265.aitrainer.integration.plan.Plan
import com.github.sebastianp265.aitrainer.integration.plan.PlanAuthor
import com.github.sebastianp265.aitrainer.integration.plan.PlannedSet
import com.github.sebastianp265.aitrainer.integration.workout.SessionSummary
import com.github.sebastianp265.aitrainer.shared.localization.formatLongWeekdayDate
import com.github.sebastianp265.aitrainer.shared.localization.formatWeekdayDate
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

@Composable
internal fun LogWorkoutContent(
    state: LogUiState,
    onStartEmpty: () -> Unit,
    onStartFromPlan: (Long) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenWorkout: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    Box(modifier.fillMaxSize()) {
        if (state.isLoading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val nextPlan = state.plans.firstOrNull()
                item {
                    if (nextPlan != null) {
                        NextPlanCard(nextPlan, state.exercises, onStartFromPlan = { onStartFromPlan(nextPlan.id) }, onOtherWorkout = { sheetOpen = true })
                    } else {
                        StartOnYourOwnCard(onStartEmpty)
                    }
                }
                item { RecentHeader(onOpenHistory) }
                if (state.recentSessions.isEmpty()) {
                    item { NoWorkoutsYet() }
                } else {
                    items(state.recentSessions.take(RECENT_SHOWN), key = { it.id }) { session ->
                        RecentSessionRow(session) { onOpenWorkout(session.id) }
                    }
                }
            }
        }
    }
    if (sheetOpen) {
        StartSheet(
            plans = state.plans,
            onStartFromPlan = { sheetOpen = false; onStartFromPlan(it) },
            onStartEmpty = { sheetOpen = false; onStartEmpty() },
            onDismiss = { sheetOpen = false },
        )
    }
}

private const val RECENT_SHOWN = 5

@Composable
private fun NextPlanCard(plan: Plan, exercises: Map<String, Exercise>, onStartFromPlan: () -> Unit, onOtherWorkout: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val label = plan.date?.let { stringResource(R.string.next_in_plan_dated, formatLongWeekdayDate(it).uppercase(locale)) }
        ?: stringResource(R.string.next_in_plan)
    val names = plan.sets.map { it.exerciseKey }.distinct().joinToString(", ") { exercises.getValue(it).displayName(locale) }
    Card(
        Modifier.fillMaxWidth().testTag(LogWorkoutTags.NEXT_PLAN),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(plan.name, style = MaterialTheme.typography.titleLarge)
            Text(names, style = MaterialTheme.typography.bodyMedium)
            Text(planCountsWithAuthor(plan), style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStartFromPlan, Modifier.testTag(LogWorkoutTags.START_NEXT_PLAN)) { Text(stringResource(R.string.start_next_plan)) }
                OutlinedButton(onClick = onOtherWorkout, Modifier.testTag(LogWorkoutTags.OTHER_WORKOUT)) { Text(stringResource(R.string.other_workout)) }
            }
        }
    }
}

@Composable
private fun planCountsWithAuthor(plan: Plan): String {
    val exerciseCount = plan.sets.map { it.exerciseKey }.distinct().size
    val setCount = plan.sets.count { !it.isWarmup }
    val author = when (plan.author) {
        PlanAuthor.USER -> stringResource(R.string.plan_written_by_you)
        PlanAuthor.COACH -> stringResource(R.string.plan_written_by_coach)
    }
    return stringResource(
        R.string.plan_counts_with_author,
        pluralStringResource(R.plurals.exercise_count, exerciseCount, exerciseCount),
        pluralStringResource(R.plurals.set_count, setCount, setCount),
        author,
    )
}

@Composable
private fun StartOnYourOwnCard(onStartEmpty: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.start_on_your_own), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.start_on_your_own_text), style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onStartEmpty, Modifier.testTag(LogWorkoutTags.START_EMPTY)) { Text(stringResource(R.string.empty_workout)) }
        }
    }
}

@Composable
private fun NoWorkoutsYet() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(stringResource(R.string.no_workouts_yet), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(stringResource(R.string.no_workouts_yet_text), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    }
}

@Composable
private fun RecentHeader(onOpenHistory: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.recent_workouts), style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = onOpenHistory) { Text(stringResource(R.string.history_all)) }
    }
}

@Composable
private fun RecentSessionRow(session: SessionSummary, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp).testTag(LogWorkoutTags.RECENT_SESSION),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(session.planName ?: stringResource(R.string.workout_empty_title), style = MaterialTheme.typography.bodyLarge)
            Text(sessionLine(session), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (session.planId != null) {
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer) {
                Text(stringResource(R.string.from_plan), Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun sessionLine(session: SessionSummary): String {
    val minutes = session.finishedAt?.let { (it - session.startedAt).inWholeMinutes }
    val counts = stringResource(
        R.string.session_summary,
        pluralStringResource(R.plurals.exercise_count, session.exerciseCount, session.exerciseCount),
        pluralStringResource(R.plurals.set_count, session.setCount, session.setCount),
    )
    val withDate = stringResource(R.string.session_summary, formatWeekdayDate(session.date), counts)
    return if (minutes != null) stringResource(R.string.session_summary, withDate, stringResource(R.string.value_minutes, minutes)) else withDate
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartSheet(plans: List<Plan>, onStartFromPlan: (Long) -> Unit, onStartEmpty: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.start_workout), style = MaterialTheme.typography.titleLarge)
            if (plans.isNotEmpty()) {
                Text(stringResource(R.string.from_a_plan), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                plans.forEachIndexed { index, plan ->
                    SheetOption(
                        title = plan.name,
                        subtitle = sheetPlanLine(plan),
                        badge = if (plan.author == PlanAuthor.USER) stringResource(R.string.plan_author_you) else stringResource(R.string.plan_author_coach),
                        selected = index == 0,
                        modifier = Modifier.testTag(LogWorkoutTags.sheetPlan(plan.name)),
                    ) { onStartFromPlan(plan.id) }
                }
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
            }
            SheetOption(
                title = stringResource(R.string.empty_workout),
                subtitle = stringResource(R.string.empty_workout_text),
                badge = null,
                selected = false,
                modifier = Modifier.testTag(LogWorkoutTags.SHEET_EMPTY),
                onClick = onStartEmpty,
            )
        }
    }
}

@Composable
private fun sheetPlanLine(plan: Plan): String {
    val exerciseCount = plan.sets.map { it.exerciseKey }.distinct().size
    val setCount = plan.sets.count { !it.isWarmup }
    val counts = stringResource(
        R.string.session_summary,
        pluralStringResource(R.plurals.exercise_count, exerciseCount, exerciseCount),
        pluralStringResource(R.plurals.set_count, setCount, setCount),
    )
    val date = plan.date?.let { formatWeekdayDate(it) } ?: stringResource(R.string.plan_no_date)
    return stringResource(R.string.session_summary, date, counts)
}

@Composable
private fun SheetOption(title: String, subtitle: String, badge: String?, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
            if (badge != null) Text(badge, style = MaterialTheme.typography.labelMedium)
        }
    }
}

internal object LogWorkoutTags {
    const val NEXT_PLAN = "training_next_plan"
    const val START_NEXT_PLAN = "training_start_next_plan"
    const val START_EMPTY = "training_start_empty"
    const val OTHER_WORKOUT = "training_other_workout"
    const val SHEET_EMPTY = "training_sheet_empty"
    const val RECENT_SESSION = "training_recent_session"

    fun sheetPlan(name: String) = "training_sheet_plan:$name"
}

@PreviewLightDark
@Composable
private fun LogWorkoutPreview() {
    val start = Instant.fromEpochMilliseconds(1_767_600_000_000)
    val bench = Exercise("BENCH", mapOf("en" to "Bench press"), Equipment("BARBELL", mapOf("en" to "Barbell")), emptyList(), emptyList())
    MaterialTheme {
        Surface {
            LogWorkoutContent(
                state = LogUiState(
                    isLoading = false,
                    plans = listOf(Plan(1, "Push A", LocalDate(2026, 11, 12), PlanAuthor.USER, listOf(PlannedSet("BENCH", 8, 80.0, null, false)))),
                    recentSessions = listOf(
                        SessionSummary(1, LocalDate(2026, 1, 5), start, start.plus(kotlin.time.Duration.parse("52m")), 4, 12, 1, "Push A"),
                        SessionSummary(2, LocalDate(2026, 1, 3), start, start.plus(kotlin.time.Duration.parse("48m")), 1, 1, null, null),
                    ),
                    exercises = mapOf("BENCH" to bench),
                ),
                onStartEmpty = {},
                onStartFromPlan = {},
                onOpenHistory = {},
                onOpenWorkout = {},
            )
        }
    }
}
