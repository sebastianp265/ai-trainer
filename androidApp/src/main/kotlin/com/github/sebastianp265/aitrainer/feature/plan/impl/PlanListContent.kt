package com.github.sebastianp265.aitrainer.feature.plan.impl

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
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.sebastianp265.aitrainer.shared.design.CoachButton
import com.github.sebastianp265.aitrainer.shared.design.SectionHeader
import androidx.compose.foundation.layout.height
import com.github.sebastianp265.aitrainer.shared.design.Tag
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
import com.github.sebastianp265.aitrainer.shared.localization.formatWeight
import com.github.sebastianp265.aitrainer.shared.workout.PlanLine
import com.github.sebastianp265.aitrainer.shared.workout.exerciseCount
import com.github.sebastianp265.aitrainer.shared.workout.planExercises
import com.github.sebastianp265.aitrainer.shared.workout.workingSetCount
import kotlinx.datetime.LocalDate
import com.github.sebastianp265.aitrainer.shared.design.DesignIcons
import com.github.sebastianp265.aitrainer.shared.design.DesignOutlinedButton

@Composable
internal fun PlanListContent(
    state: PlanListUiState,
    onOpenPlan: (Long) -> Unit,
    onNewPlan: () -> Unit,
    onOpenTargets: () -> Unit,
    modifier: Modifier = Modifier,
    banner: @Composable () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            if (state.plans.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onNewPlan,
                    icon = { Icon(DesignIcons.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.plan_new_fab)) },
                    modifier = Modifier.testTag(PlanTags.NEW_PLAN),
                )
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.plans.isEmpty() -> EmptyPlans(onNewPlan)
                else -> PlanList(state, onOpenPlan, onOpenTargets, banner)
            }
        }
    }
}

@Composable
private fun EmptyPlans(onNewPlan: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.plan_empty_title), style = MaterialTheme.typography.titleMedium)
        val coachEnabled = LocalCoachEnabled.current
        Text(
            stringResource(if (coachEnabled) R.string.plan_empty_text_with_coach else R.string.plan_empty_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (coachEnabled) {
            val openCoach = LocalOpenCoach.current
            CoachButton(stringResource(R.string.plan_ask_coach), { openCoach(null) }, Modifier.testTag(PlanTags.ASK_COACH))
            DesignOutlinedButton(onClick = onNewPlan, Modifier.fillMaxWidth().height(48.dp).testTag(PlanTags.NEW_PLAN)) { Text(stringResource(R.string.plan_write_own)) }
        } else {
            Button(onClick = onNewPlan, Modifier.fillMaxWidth().height(48.dp).testTag(PlanTags.NEW_PLAN)) { Text(stringResource(R.string.plan_write_own)) }
        }
    }
}

@Composable
private fun PlanList(state: PlanListUiState, onOpenPlan: (Long) -> Unit, onOpenTargets: () -> Unit, banner: @Composable () -> Unit) {
    val dated = state.plans.filter { it.date != null }
    val undated = state.plans.filter { it.date == null }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.week?.let { week ->
            item { WeekHeader(week) }
            item { WeekStrip(week) }
        }
        item { banner() }
        if (state.volumeRows.isNotEmpty()) item { WeekVolumeCard(state.volumeRows, state.showTargets, onOpenTargets) }
        if (dated.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.plan_upcoming)) }
            items(dated, key = { it.id }) { PlanCard(it, state.exercises, isNext = it == dated.first()) { onOpenPlan(it.id) } }
        }
        if (undated.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.plan_undated)) }
            items(undated, key = { it.id }) { PlanCard(it, state.exercises, isNext = false) { onOpenPlan(it.id) } }
        }
    }
}

@Composable
private fun PlanCard(plan: Plan, exercises: Map<String, Exercise>, isNext: Boolean, onClick: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val colors = MaterialTheme.colorScheme
    val detailColor = if (isNext) colors.onPrimaryContainer else colors.onSurfaceVariant
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().testTag(PlanTags.card(plan.name)),
        colors = if (isNext) CardDefaults.cardColors(containerColor = colors.primaryContainer, contentColor = colors.onPrimaryContainer) else CardDefaults.cardColors(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Text(plan.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp))
                AuthorTag(plan.author)
            }
            Text(planMeta(plan), style = MaterialTheme.typography.bodyMedium, color = detailColor)
            val summary = planSummary(plan.sets, exercises, locale)
            if (summary.isNotEmpty()) Text(summary, style = MaterialTheme.typography.bodyMedium, color = detailColor, maxLines = 2)
        }
    }
}

@Composable
internal fun AuthorTag(author: PlanAuthor) {
    when (author) {
        PlanAuthor.COACH -> Tag(authorLabel(author))
        PlanAuthor.USER -> Tag(authorLabel(author), containerColor = MaterialTheme.colorScheme.surfaceDim, contentColor = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
internal fun authorLabel(author: PlanAuthor): String = when (author) {
    PlanAuthor.USER -> stringResource(R.string.plan_author_you)
    PlanAuthor.COACH -> stringResource(R.string.plan_author_coach)
}

@Composable
internal fun planMeta(plan: Plan): String {
    val counts = stringResource(
        R.string.plan_counts,
        pluralStringResource(R.plurals.exercise_count, exerciseCount(plan.sets), exerciseCount(plan.sets)),
        pluralStringResource(R.plurals.set_count, workingSetCount(plan.sets), workingSetCount(plan.sets)),
    )
    val date = plan.date
    return if (date != null) stringResource(R.string.plan_dated_counts, formatLongWeekdayDate(date), counts) else counts
}

@Composable
private fun planSummary(sets: List<PlannedSet>, exercises: Map<String, Exercise>, locale: java.util.Locale): String {
    val planned = planExercises(sets)
    val shown = planned.take(SUMMARY_EXERCISES).mapIndexed { index, exercise ->
        val name = exercises.getValue(exercise.exerciseKey).displayName(locale)
        val working = exercise.lines.filterIsInstance<PlanLine.Working>().firstOrNull()
        if (index == 0 && working != null && working.weightKg > 0) {
            stringResource(R.string.plan_summary_first, name, working.setCount, working.reps, formatWeight(working.weightKg))
        } else {
            name
        }
    }
    return shown.joinToString(", ") + if (planned.size > SUMMARY_EXERCISES) "…" else ""
}

private const val SUMMARY_EXERCISES = 3

internal object PlanTags {
    const val NEW_PLAN = "plan_new"
    const val NAME_FIELD = "plan_name"
    const val DATE_FIELD = "plan_date"
    const val SAVE = "plan_save"
    const val ASK_COACH = "plan_ask_coach"
    const val WEEK_STRIP = "plan_week_strip"
    const val WEEK_VOLUME = "plan_week_volume"
    const val WEEK_TARGETS_LINK = "plan_week_targets_link"
    const val START = "plan_start"
    const val ADD_EXERCISE = "plan_add_exercise"
    const val SET_WEIGHT = "plan_set_weight"
    const val SET_REPS = "plan_set_reps"

    fun card(name: String) = "plan_card:$name"

    fun weekDay(date: LocalDate) = "plan_week_day:$date"
}

@PreviewLightDark
@Composable
private fun PlanListPreview() {
    val bench = Exercise("BENCH", mapOf("en" to "Bench press"), Equipment("BARBELL", mapOf("en" to "Barbell")), emptyList(), emptyList())
    MaterialTheme {
        PlanListContent(
            state = PlanListUiState(
                isLoading = false,
                plans = listOf(
                    Plan(1, "Push A", LocalDate(2026, 11, 12), PlanAuthor.COACH, listOf(PlannedSet("BENCH", 8, 82.5, 8.0, false), PlannedSet("BENCH", 8, 82.5, 8.0, false))),
                    Plan(2, "Arms extra", null, PlanAuthor.USER, listOf(PlannedSet("BENCH", 10, 0.0, null, false))),
                ),
                exercises = mapOf("BENCH" to bench),
            ),
            onOpenPlan = {},
            onNewPlan = {},
            onOpenTargets = {},
        )
    }
}
