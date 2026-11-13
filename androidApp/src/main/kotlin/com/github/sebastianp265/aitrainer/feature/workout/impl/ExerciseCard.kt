package com.github.sebastianp265.aitrainer.feature.workout.impl

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.integration.workout.SetChange
import com.github.sebastianp265.aitrainer.integration.workout.SetEntry
import com.github.sebastianp265.aitrainer.shared.localization.displayName
import com.github.sebastianp265.aitrainer.shared.workout.EditedSet
import com.github.sebastianp265.aitrainer.shared.workout.ExerciseSetsCard
import com.github.sebastianp265.aitrainer.shared.workout.SetRow
import com.github.sebastianp265.aitrainer.shared.workout.SetRowActions
import com.github.sebastianp265.aitrainer.shared.workout.hiddenSetCount
import com.github.sebastianp265.aitrainer.shared.workout.rememberLastTimeLabels
import com.github.sebastianp265.aitrainer.shared.workout.text

@Composable
internal fun ExerciseCard(ui: ExerciseBlockUi, actions: ActiveWorkoutActions, onOpenHistory: () -> Unit) {
    val block = ui.block
    ExerciseSetsCard(
        name = block.exercise.displayName(LocalConfiguration.current.locales[0]),
        setCount = block.sets.size,
        onRemove = { actions.removeExercise(block.exercise) },
        onAddSet = { actions.addSet(block) },
        modifier = Modifier.testTag(ActiveWorkoutTags.exerciseCard(block.exercise.key)),
        belowName = { LastTimeLabel(ui, onOpenHistory) },
    ) {
        var workingNumber = 0
        block.sets.forEach { set ->
            if (!set.isWarmup) workingNumber++
            SetRow(EditedSet(set.id, if (set.isWarmup) null else workingNumber, set.weightKg, set.reps, set.rpe), setActions(set, actions))
        }
    }
}

private fun setActions(set: SetEntry, actions: ActiveWorkoutActions) = object : SetRowActions {
    override fun toggleWarmup() = actions.changeSet(set, SetChange.ToggleWarmup)
    override fun changeWeight(kg: Double) = actions.changeSet(set, SetChange.WeightTo(kg))
    override fun changeReps(reps: Int) = actions.changeSet(set, SetChange.RepsTo(reps))
    override fun changeRpe(rpe: Double?) = actions.changeSet(set, SetChange.RpeTo(rpe))
    override fun delete() = actions.deleteSet(set)
}

@Composable
private fun LastTimeLabel(ui: ExerciseBlockUi, onOpenHistory: () -> Unit) {
    if (ui.lastTime.isEmpty()) return
    val labels = rememberLastTimeLabels()
    val prefix = stringResource(R.string.last_time_prefix)
    val groupTexts = ui.lastTime.map { it.text(labels).replace(' ', NO_BREAK_SPACE) }
    var shownCount by remember(ui.lastTime) { mutableIntStateOf(groupTexts.size) }
    val hidden = hiddenSetCount(ui.lastTime, shownCount)
    val moreText = if (hidden > 0) stringResource(R.string.last_time_more, hidden) else null
    val label = (listOf(prefix + NO_BREAK_SPACE + groupTexts.first()) + groupTexts.drop(1).take(shownCount - 1) + listOfNotNull(moreText))
        .let { items -> if (shownCount == 1) listOf(prefix + NO_BREAK_SPACE + groupTexts.first()) + listOfNotNull(moreText) else items }
        .joinToString(" · ")
    Surface(
        onClick = onOpenHistory,
        modifier = Modifier.padding(end = 8.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
            maxLines = 2,
            onTextLayout = { if (it.hasVisualOverflow && shownCount > 1) shownCount-- },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).testTag(ActiveWorkoutTags.LAST_TIME),
        )
    }
}

private const val NO_BREAK_SPACE = ' '
