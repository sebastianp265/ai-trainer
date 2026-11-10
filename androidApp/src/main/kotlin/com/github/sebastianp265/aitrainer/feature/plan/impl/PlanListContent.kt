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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.feature.exercise.displayName
import com.github.sebastianp265.aitrainer.integration.exercise.Equipment
import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
import com.github.sebastianp265.aitrainer.integration.plan.Plan
import com.github.sebastianp265.aitrainer.integration.plan.PlanAuthor
import com.github.sebastianp265.aitrainer.integration.plan.PlannedSet
import com.github.sebastianp265.aitrainer.shared.localization.formatLongWeekdayDate
import com.github.sebastianp265.aitrainer.shared.localization.formatWeight
import kotlinx.datetime.LocalDate

@Composable
internal fun PlanListContent(
    state: PlanListUiState,
    onOpenPlan: (Long) -> Unit,
    onNewPlan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            if (state.plans.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onNewPlan,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
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
                else -> PlanList(state, onOpenPlan)
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
        Text(stringResource(R.string.plan_empty_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.plan_empty_text), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Button(onClick = onNewPlan, Modifier.testTag(PlanTags.NEW_PLAN)) { Text(stringResource(R.string.plan_write_own)) }
    }
}

@Composable
private fun PlanList(state: PlanListUiState, onOpenPlan: (Long) -> Unit) {
    val dated = state.plans.filter { it.date != null }
    val undated = state.plans.filter { it.date == null }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (dated.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.plan_upcoming)) }
            items(dated, key = { it.id }) { PlanCard(it, state.exercises) { onOpenPlan(it.id) } }
        }
        if (undated.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.plan_undated)) }
            items(undated, key = { it.id }) { PlanCard(it, state.exercises) { onOpenPlan(it.id) } }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun PlanCard(plan: Plan, exercises: Map<String, Exercise>, onClick: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    Card(onClick = onClick, Modifier.fillMaxWidth().testTag(PlanTags.card(plan.name))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(plan.name, style = MaterialTheme.typography.titleMedium)
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(authorLabel(plan.author), Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium)
                }
            }
            Text(planMeta(plan), style = MaterialTheme.typography.bodyMedium)
            val summary = planSummary(plan.sets, exercises, locale)
            if (summary.isNotEmpty()) Text(summary, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
        }
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
    const val START = "plan_start"
    const val ADD_EXERCISE = "plan_add_exercise"
    const val SET_WEIGHT = "plan_set_weight"
    const val SET_REPS = "plan_set_reps"

    fun card(name: String) = "plan_card:$name"
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
        )
    }
}
