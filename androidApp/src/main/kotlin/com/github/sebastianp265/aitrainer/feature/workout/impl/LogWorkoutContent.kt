package com.github.sebastianp265.aitrainer.feature.workout.impl

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.shared.design.IconCircle
import com.github.sebastianp265.aitrainer.shared.design.ListRow
import com.github.sebastianp265.aitrainer.shared.workout.exerciseCount
import com.github.sebastianp265.aitrainer.shared.workout.workingSetCount
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.github.sebastianp265.aitrainer.shared.design.SectionHeader
import com.github.sebastianp265.aitrainer.shared.design.Tag
import com.github.sebastianp265.aitrainer.integration.plan.Plan
import com.github.sebastianp265.aitrainer.integration.plan.PlanAuthor
import com.github.sebastianp265.aitrainer.integration.plan.PlannedSet
import com.github.sebastianp265.aitrainer.integration.workout.SessionSummary
import com.github.sebastianp265.aitrainer.shared.state.LocalCoachEnabled
import com.github.sebastianp265.aitrainer.shared.state.LocalOpenCoach
import com.github.sebastianp265.aitrainer.shared.localization.formatWeekdayDate
import kotlinx.datetime.LocalDate
import kotlin.time.Instant
import com.github.sebastianp265.aitrainer.shared.design.DesignIcons
import androidx.compose.runtime.setValue

@Composable
internal fun LogWorkoutContent(
    state: LogUiState,
    onStartEmpty: () -> Unit,
    onStartFromPlan: (Long) -> Unit,
    onOpenPlan: (Long) -> Unit,
    onNewPlan: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenWorkout: (Long) -> Unit,
    modifier: Modifier = Modifier,
    week: @Composable () -> Unit = {},
    banner: @Composable () -> Unit = {},
) {
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    val coachEnabled = LocalCoachEnabled.current
    Box(modifier.fillMaxSize()) {
        if (state.isLoading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { week() }
                item { banner() }
                if (coachEnabled && state.plans.isEmpty() && state.recentSessions.isEmpty()) item { MeetCoachCard() }
                item {
                    Button(onClick = { sheetOpen = true }, Modifier.fillMaxWidth().height(56.dp).testTag(LogWorkoutTags.START_WORKOUT)) {
                        Text(stringResource(R.string.start_workout))
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
            onOpenPlan = { sheetOpen = false; onOpenPlan(it) },
            onNewPlan = { sheetOpen = false; onNewPlan() },
            onDismiss = { sheetOpen = false },
        )
    }
}

private const val RECENT_SHOWN = 5

@Composable
private fun MeetCoachCard() {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.training_meet_coach_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.training_meet_coach_text), style = MaterialTheme.typography.bodyMedium)
            val openCoach = LocalOpenCoach.current
            Button(onClick = { openCoach(null) }, Modifier.testTag(LogWorkoutTags.MEET_COACH)) { Text(stringResource(R.string.training_meet_coach_button)) }
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
    SectionHeader(stringResource(R.string.recent_workouts)) {
        TextButton(onClick = onOpenHistory) { Text(stringResource(R.string.history_all)) }
    }
}

@Composable
private fun RecentSessionRow(session: SessionSummary, onClick: () -> Unit) {
    ListRow(Modifier.testTag(LogWorkoutTags.RECENT_SESSION), onClick = onClick) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(session.planName ?: stringResource(R.string.workout_empty_title), style = MaterialTheme.typography.bodyLarge)
            Text(sessionLine(session), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (session.planId != null) Tag(stringResource(R.string.from_plan))
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
private fun StartSheet(
    plans: List<Plan>,
    onStartFromPlan: (Long) -> Unit,
    onStartEmpty: () -> Unit,
    onOpenPlan: (Long) -> Unit,
    onNewPlan: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Text(stringResource(R.string.start_workout), Modifier.padding(horizontal = 24.dp).padding(bottom = 8.dp), style = MaterialTheme.typography.headlineSmall)
            if (plans.isNotEmpty()) {
                Text(
                    stringResource(R.string.from_a_plan),
                    Modifier.padding(horizontal = 24.dp).padding(top = 12.dp, bottom = 4.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                plans.forEachIndexed { index, plan ->
                    SheetOption(
                        title = plan.name,
                        subtitle = sheetPlanLine(plan),
                        badge = if (plan.author == PlanAuthor.USER) stringResource(R.string.plan_author_you) else stringResource(R.string.plan_author_coach),
                        selected = index == 0,
                        icon = DesignIcons.Plan,
                        modifier = Modifier.testTag(LogWorkoutTags.sheetPlan(plan.name)),
                        trailing = {
                            IconButton(onClick = { onOpenPlan(plan.id) }, Modifier.testTag(LogWorkoutTags.sheetPlanDetails(plan.name))) {
                                Icon(DesignIcons.Info, contentDescription = stringResource(R.string.plan_details))
                            }
                        },
                    ) { onStartFromPlan(plan.id) }
                }
                HorizontalDivider(Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            }
            SheetOption(
                title = stringResource(R.string.empty_workout),
                subtitle = stringResource(R.string.empty_workout_text),
                badge = null,
                selected = false,
                icon = DesignIcons.Add,
                modifier = Modifier.testTag(LogWorkoutTags.SHEET_EMPTY),
                onClick = onStartEmpty,
            )
            SheetOption(
                title = stringResource(R.string.sheet_new_plan),
                subtitle = stringResource(R.string.sheet_new_plan_text),
                badge = null,
                selected = false,
                icon = DesignIcons.Edit,
                modifier = Modifier.testTag(LogWorkoutTags.SHEET_NEW_PLAN),
                onClick = onNewPlan,
            )
        }
    }
}

@Composable
private fun sheetPlanLine(plan: Plan): String {
    val exerciseCount = exerciseCount(plan.sets)
    val setCount = workingSetCount(plan.sets)
    val counts = stringResource(
        R.string.session_summary,
        pluralStringResource(R.plurals.exercise_count, exerciseCount, exerciseCount),
        pluralStringResource(R.plurals.set_count, setCount, setCount),
    )
    val date = plan.date?.let { formatWeekdayDate(it) } ?: stringResource(R.string.plan_no_date)
    return stringResource(R.string.session_summary, date, counts)
}

@Composable
private fun SheetOption(
    title: String,
    subtitle: String,
    badge: String?,
    selected: Boolean,
    icon: ImageVector,
    modifier: Modifier,
    trailing: @Composable () -> Unit = {},
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
    ) {
        Row(
            Modifier.heightIn(min = 56.dp).padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconCircle(icon)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (badge != null) {
                Tag(badge, containerColor = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.secondaryContainer)
            }
            trailing()
        }
    }
}

internal object LogWorkoutTags {
    const val MEET_COACH = "training_meet_coach"
    const val START_WORKOUT = "training_start_workout"
    const val SHEET_EMPTY = "training_sheet_empty"
    const val SHEET_NEW_PLAN = "training_sheet_new_plan"
    const val RECENT_SESSION = "training_recent_session"

    fun sheetPlan(name: String) = "training_sheet_plan:$name"

    fun sheetPlanDetails(name: String) = "training_sheet_plan_details:$name"
}

@PreviewLightDark
@Composable
private fun LogWorkoutPreview() {
    val start = Instant.fromEpochMilliseconds(1_767_600_000_000)
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
                ),
                onStartEmpty = {},
                onStartFromPlan = {},
                onOpenPlan = {},
                onNewPlan = {},
                onOpenHistory = {},
                onOpenWorkout = {},
            )
        }
    }
}
