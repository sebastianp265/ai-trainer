package com.github.sebastianp265.aitrainer.feature.workout.impl

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.integration.exercise.Equipment
import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
import com.github.sebastianp265.aitrainer.integration.plan.Plan
import com.github.sebastianp265.aitrainer.integration.plan.PlanAuthor
import com.github.sebastianp265.aitrainer.integration.plan.PlannedSet
import com.github.sebastianp265.aitrainer.integration.workout.SessionSummary
import com.github.sebastianp265.aitrainer.shared.design.DesignIcons
import com.github.sebastianp265.aitrainer.shared.design.Tag
import com.github.sebastianp265.aitrainer.shared.design.scrollbarWhileScrolling
import com.github.sebastianp265.aitrainer.shared.localization.displayName
import com.github.sebastianp265.aitrainer.shared.localization.formatDayMonth
import com.github.sebastianp265.aitrainer.shared.localization.formatMonth
import com.github.sebastianp265.aitrainer.shared.localization.formatMonthName
import com.github.sebastianp265.aitrainer.shared.localization.formatWeekdayDate
import com.github.sebastianp265.aitrainer.shared.localization.formatWeekdayShort
import com.github.sebastianp265.aitrainer.shared.state.LocalCoachEnabled
import com.github.sebastianp265.aitrainer.shared.state.LocalOpenCoach
import com.github.sebastianp265.aitrainer.shared.workout.exerciseCount
import com.github.sebastianp265.aitrainer.shared.workout.workingSetCount
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlin.time.Instant

internal interface TrainingActions {
    fun startEmpty()
    fun startPlan(planId: Long)
    fun openPlan(planId: Long)
    fun editPlan(planId: Long)
    fun newPlan()
    fun resumeWorkout()
    fun openWorkout(sessionId: Long)
    fun editWorkout(sessionId: Long)
}

internal enum class TrainingTab { PLANS, HISTORY }

@Composable
internal fun LogWorkoutContent(
    state: LogUiState,
    actions: TrainingActions,
    modifier: Modifier = Modifier,
    banner: @Composable () -> Unit = {},
) {
    var tab by rememberSaveable { mutableStateOf(TrainingTab.PLANS) }
    val coachEnabled = LocalCoachEnabled.current
    Box(modifier.fillMaxSize()) {
        if (state.isLoading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
            return@Box
        }
        val activeWorkout = state.activeWorkout
        val nextPlan = state.plans.firstOrNull().takeIf { activeWorkout == null }
        val listedPlans = if (nextPlan == null) state.plans else state.plans.drop(1)
        val listState = rememberLazyListState()
        LazyColumn(
            Modifier.fillMaxSize().scrollbarWhileScrolling(listState).testTag(LogWorkoutTags.LIST),
            state = listState,
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Box(ItemPadding) { banner() } }
            if (coachEnabled && state.plans.isEmpty() && state.historyMonths.isEmpty()) item { MeetCoachCard() }
            when {
                activeWorkout != null -> item(key = "active") { ActiveWorkoutCard(activeWorkout, actions) }
                nextPlan != null -> item(key = "next") { NextPlanCard(nextPlan, actions) }
            }
            item { StartButtons(actions, canStart = activeWorkout == null) }
            stickyHeader { TabSwitch(tab, onSelect = { tab = it }) }
            when (tab) {
                TrainingTab.PLANS -> plansTab(listedPlans, noPlans = state.plans.isEmpty(), canStart = activeWorkout == null, state.exercises, actions)
                TrainingTab.HISTORY -> historyTab(state.historyMonths, actions)
            }
        }
    }
}

private val ItemPadding = Modifier.padding(horizontal = 16.dp)

private val CardShape = RoundedCornerShape(20.dp)

private fun LazyListScope.plansTab(
    plans: List<Plan>,
    noPlans: Boolean,
    canStart: Boolean,
    exercises: Map<String, Exercise>,
    actions: TrainingActions,
) {
    if (noPlans) item { HintText(stringResource(R.string.training_no_plans)) }
    items(plans, key = { it.id }) { plan -> PlanRow(plan, canStart, exercises, actions) }
}

private fun LazyListScope.historyTab(months: List<HistoryMonth>, actions: TrainingActions) {
    if (months.isEmpty()) {
        item { NoWorkoutsYet() }
        return
    }
    months.forEach { month ->
        item(key = "month-${month.year}-${month.month}") { MonthHeader(month) }
        items(month.sessions, key = { it.id }) { session -> SessionRow(session, actions) }
    }
}

@Composable
private fun MeetCoachCard() {
    Card(ItemPadding.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.training_meet_coach_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.training_meet_coach_text), style = MaterialTheme.typography.bodyMedium)
            val openCoach = LocalOpenCoach.current
            Button(onClick = { openCoach(null) }, Modifier.testTag(LogWorkoutTags.MEET_COACH)) { Text(stringResource(R.string.training_meet_coach_button)) }
        }
    }
}

@Composable
private fun NextPlanCard(plan: Plan, actions: TrainingActions) {
    HeroCard(
        label = stringResource(if (plan.author == PlanAuthor.COACH) R.string.training_next_from_coach else R.string.training_next_own),
        title = plan.name,
        subtitle = planCounts(plan),
        buttonText = stringResource(R.string.start_workout),
        onClick = { actions.openPlan(plan.id) },
        onButtonClick = { actions.startPlan(plan.id) },
        modifier = Modifier.testTag(LogWorkoutTags.nextPlan(plan.name)),
        buttonModifier = Modifier.testTag(LogWorkoutTags.NEXT_PLAN_START),
        labelAction = {
            IconButton(onClick = { actions.editPlan(plan.id) }) {
                Icon(DesignIcons.Edit, contentDescription = stringResource(R.string.training_edit_plan_description, plan.name))
            }
        },
    )
}

@Composable
private fun ActiveWorkoutCard(active: ActiveWorkout, actions: TrainingActions) {
    val session = active.session
    val isEdit = session.editedSessionId != null
    HeroCard(
        label = stringResource(if (isEdit) R.string.training_active_edit else R.string.training_active_now),
        title = active.planName ?: stringResource(R.string.workout_empty_title),
        subtitle = if (isEdit) formatWeekdayDate(session.date) else formatElapsed(rememberElapsedSeconds(session.startedAt)),
        buttonText = stringResource(R.string.training_active_resume),
        onClick = actions::resumeWorkout,
        onButtonClick = actions::resumeWorkout,
        modifier = Modifier.testTag(LogWorkoutTags.ACTIVE_WORKOUT),
        buttonModifier = Modifier.testTag(LogWorkoutTags.ACTIVE_WORKOUT_RESUME),
    )
}

@Composable
private fun HeroCard(
    label: String,
    title: String,
    subtitle: String,
    buttonText: String,
    onClick: () -> Unit,
    onButtonClick: () -> Unit,
    modifier: Modifier,
    buttonModifier: Modifier,
    labelAction: @Composable () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = ItemPadding.fillMaxWidth().then(modifier),
        shape = RoundedCornerShape(28.dp),
        color = colors.primary,
        contentColor = colors.onPrimary,
    ) {
        Column(Modifier.padding(start = 20.dp, top = 8.dp, end = 8.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row {
                Column(Modifier.weight(1f).padding(top = 12.dp, end = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(label, Modifier.padding(bottom = 2.dp), style = MaterialTheme.typography.labelLarge)
                    Text(title, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium)
                }
                labelAction()
            }
            Button(
                onClick = onButtonClick,
                modifier = Modifier.padding(end = 12.dp).fillMaxWidth().height(56.dp).then(buttonModifier),
                colors = ButtonDefaults.buttonColors(containerColor = colors.onPrimary, contentColor = colors.primary),
            ) {
                Icon(DesignIcons.Play, contentDescription = null, Modifier.padding(end = 8.dp))
                Text(buttonText, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun planCounts(plan: Plan): String {
    val exercises = exerciseCount(plan.sets)
    val sets = workingSetCount(plan.sets)
    return stringResource(
        R.string.session_summary,
        pluralStringResource(R.plurals.exercise_count, exercises, exercises),
        pluralStringResource(R.plurals.set_count, sets, sets),
    )
}

@Composable
private fun StartButtons(actions: TrainingActions, canStart: Boolean) {
    Row(ItemPadding, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (canStart) {
            OutlinedRowButton(
                stringResource(R.string.training_without_plan),
                actions::startEmpty,
                Modifier.weight(1f).testTag(LogWorkoutTags.START_EMPTY),
            )
        }
        OutlinedRowButton(
            stringResource(R.string.plan_new_title),
            actions::newPlan,
            Modifier.weight(1f).testTag(LogWorkoutTags.NEW_PLAN),
        )
    }
}

@Composable
private fun OutlinedRowButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(DesignIcons.Add, contentDescription = null, Modifier.padding(end = 8.dp))
            Text(text, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TabSwitch(selected: TrainingTab, onSelect: (TrainingTab) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.fillMaxWidth().background(colors.background).padding(horizontal = 16.dp, vertical = 6.dp)) {
        Surface(shape = RoundedCornerShape(24.dp), color = colors.surfaceContainer, border = BorderStroke(1.dp, colors.outlineVariant)) {
            Row(Modifier.padding(4.dp).selectableGroup()) {
                TrainingTab.entries.forEach { tab ->
                    val isSelected = tab == selected
                    Box(
                        Modifier.weight(1f).height(44.dp)
                            .background(if (isSelected) colors.secondaryContainer else colors.surfaceContainer, RoundedCornerShape(20.dp))
                            .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(tab) })
                            .testTag(LogWorkoutTags.tab(tab)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(if (tab == TrainingTab.PLANS) R.string.training_tab_plans else R.string.training_tab_history),
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isSelected) colors.onSecondaryContainer else colors.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanRow(plan: Plan, canStart: Boolean, exercises: Map<String, Exercise>, actions: TrainingActions) {
    val locale = LocalConfiguration.current.locales[0]
    val exerciseNames = plan.sets.map { it.exerciseKey }.distinct().joinToString(", ") { exercises.getValue(it).displayName(locale) }
    ListCard(onClick = { actions.openPlan(plan.id) }, modifier = Modifier.testTag(LogWorkoutTags.planRow(plan.name))) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(plan.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                AuthorTag(plan.author)
            }
            Text(exerciseNames, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = { actions.editPlan(plan.id) }) {
            Icon(DesignIcons.Edit, contentDescription = stringResource(R.string.training_edit_plan_description, plan.name), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (canStart) {
            FilledTonalIconButton(onClick = { actions.startPlan(plan.id) }, Modifier.size(48.dp)) {
                Icon(DesignIcons.Play, contentDescription = stringResource(R.string.training_start_plan_description, plan.name))
            }
        }
    }
}

@Composable
private fun AuthorTag(author: PlanAuthor) {
    val colors = MaterialTheme.colorScheme
    when (author) {
        PlanAuthor.COACH -> Tag(stringResource(R.string.plan_author_coach), containerColor = colors.primaryContainer, contentColor = colors.onPrimaryContainer)
        PlanAuthor.USER -> Tag(stringResource(R.string.plan_author_you), containerColor = colors.tertiaryContainer, contentColor = colors.onTertiaryContainer)
    }
}

@Composable
private fun MonthHeader(month: HistoryMonth) {
    Text(
        if (month.isCurrentYear) formatMonthName(month.month) else formatMonth(month.year, month.month),
        ItemPadding.padding(top = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SessionRow(session: SessionSummary, actions: TrainingActions) {
    val colors = MaterialTheme.colorScheme
    val minutes = (checkNotNull(session.finishedAt) - session.startedAt).inWholeMinutes.toInt()
    ListCard(onClick = { actions.openWorkout(session.id) }, modifier = Modifier.testTag(LogWorkoutTags.SESSION_ROW)) {
        DayBadge(session.date)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    session.planName ?: stringResource(R.string.history_workout),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (session.planId != null) {
                    Tag(stringResource(R.string.from_plan), containerColor = colors.primaryContainer, contentColor = colors.onPrimaryContainer)
                } else {
                    Tag(stringResource(R.string.training_no_plan), containerColor = colors.tertiaryContainer, contentColor = colors.onTertiaryContainer)
                }
            }
            Text(
                stringResource(
                    R.string.session_summary,
                    stringResource(R.string.value_minutes, minutes),
                    pluralStringResource(R.plurals.set_count, session.setCount, session.setCount),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }
        IconButton(onClick = { actions.editWorkout(session.id) }) {
            Icon(
                DesignIcons.Edit,
                contentDescription = stringResource(R.string.training_edit_workout_description, formatDayMonth(session.date)),
                tint = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DayBadge(date: LocalDate) {
    Column(
        Modifier.size(48.dp, 52.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(14.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(date.day.toString(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Text(formatWeekdayShort(date), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ListCard(onClick: () -> Unit, modifier: Modifier, content: @Composable RowScope.() -> Unit) {
    Surface(
        onClick = onClick,
        modifier = ItemPadding.then(modifier).fillMaxWidth(),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            Modifier.padding(start = 14.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
private fun HintText(text: String) {
    Text(text, ItemPadding, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun NoWorkoutsYet() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(stringResource(R.string.no_workouts_yet), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(stringResource(R.string.no_workouts_yet_text), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    }
}

internal object LogWorkoutTags {
    const val LIST = "training_list"
    const val MEET_COACH = "training_meet_coach"
    const val START_EMPTY = "training_start_empty"
    const val NEXT_PLAN_START = "training_next_plan_start"
    const val ACTIVE_WORKOUT = "training_active_workout"
    const val ACTIVE_WORKOUT_RESUME = "training_active_workout_resume"
    const val NEW_PLAN = "training_new_plan"
    const val SESSION_ROW = "training_session_row"

    fun nextPlan(name: String) = "training_next_plan:$name"

    fun planRow(name: String) = "training_plan_row:$name"

    fun tab(tab: TrainingTab) = "training_tab:${tab.name}"
}

@PreviewLightDark
@Composable
private fun LogWorkoutPreview() {
    val start = Instant.parse("2026-11-10T17:00:00Z")
    val bench = Exercise("BENCH", mapOf("en" to "Bench press"), Equipment("BARBELL", mapOf("en" to "Barbell")), emptyList(), emptyList())
    MaterialTheme {
        Surface {
            LogWorkoutContent(
                state = LogUiState(
                    isLoading = false,
                    plans = listOf(
                        Plan(1, "Push A", LocalDate(2026, 11, 12), PlanAuthor.COACH, listOf(PlannedSet("BENCH", 8, 80.0, null, false))),
                        Plan(2, "Push B", null, PlanAuthor.USER, listOf(PlannedSet("BENCH", 8, 70.0, null, false))),
                    ),
                    historyMonths = listOf(
                        HistoryMonth(2026, Month.NOVEMBER, isCurrentYear = true, sessions = listOf(
                            SessionSummary(1, LocalDate(2026, 11, 10), start, start.plus(kotlin.time.Duration.parse("48m")), 4, 19, 1, "Pull A"),
                        )),
                    ),
                    exercises = mapOf("BENCH" to bench),
                ),
                actions = object : TrainingActions {
                    override fun startEmpty() = Unit
                    override fun startPlan(planId: Long) = Unit
                    override fun openPlan(planId: Long) = Unit
                    override fun editPlan(planId: Long) = Unit
                    override fun newPlan() = Unit
                    override fun resumeWorkout() = Unit
                    override fun openWorkout(sessionId: Long) = Unit
                    override fun editWorkout(sessionId: Long) = Unit
                },
            )
        }
    }
}
