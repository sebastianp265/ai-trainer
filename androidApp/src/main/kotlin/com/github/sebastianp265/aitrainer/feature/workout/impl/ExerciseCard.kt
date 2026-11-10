package com.github.sebastianp265.aitrainer.feature.workout.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.shared.localization.displayName
import com.github.sebastianp265.aitrainer.integration.workout.RPE_MAX
import com.github.sebastianp265.aitrainer.integration.workout.RPE_MIN
import com.github.sebastianp265.aitrainer.integration.workout.RPE_STEP
import com.github.sebastianp265.aitrainer.integration.workout.SetChange
import com.github.sebastianp265.aitrainer.integration.workout.SetEntry
import com.github.sebastianp265.aitrainer.shared.localization.formatWeight
import com.github.sebastianp265.aitrainer.shared.workout.hiddenSetCount
import com.github.sebastianp265.aitrainer.shared.workout.rememberLastTimeLabels
import com.github.sebastianp265.aitrainer.shared.workout.text

@Composable
internal fun ExerciseCard(ui: ExerciseBlockUi, actions: ActiveWorkoutActions, onOpenHistory: () -> Unit) {
    val block = ui.block
    var expanded by rememberSaveable(block.exercise.key) { mutableStateOf(true) }
    Card(Modifier.fillMaxWidth().testTag(ActiveWorkoutTags.exerciseCard(block.exercise.key))) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(block.exercise.displayName(LocalConfiguration.current.locales[0]), style = MaterialTheme.typography.titleMedium)
                    if (!expanded) {
                        Text(
                            pluralStringResource(R.plurals.set_count, block.sets.size, block.sets.size),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                IconButton(onClick = { actions.removeExercise(block.exercise) }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove_exercise))
                }
            }
            LastTimeLabel(ui, onOpenHistory)
            if (expanded) {
                var workingNumber = 0
                block.sets.forEach { set ->
                    if (!set.isWarmup) workingNumber++
                    SetRow(if (set.isWarmup) null else workingNumber, set, actions)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = { actions.addSet(block) }, label = { Text(stringResource(R.string.add_set)) })
                    AssistChip(onClick = { actions.copySetsFromLastTime(block) }, label = { Text(stringResource(R.string.same_as_last_time)) })
                }
            }
        }
    }
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
    Text(
        label,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 2,
        onTextLayout = { if (it.hasVisualOverflow && shownCount > 1) shownCount-- },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenHistory).testTag(ActiveWorkoutTags.LAST_TIME),
    )
}

private const val NO_BREAK_SPACE = ' '

@Composable
internal fun SetRow(workingNumber: Int?, set: SetEntry, actions: ActiveWorkoutActions) {
    var typing by rememberSaveable(set.id) { mutableStateOf<TypedField?>(null) }
    var pickingRpe by rememberSaveable(set.id) { mutableStateOf(false) }
    val decreaseDescription = stringResource(R.string.decrease_reps)
    val increaseDescription = stringResource(R.string.increase_reps)
    val weightDescription = stringResource(R.string.set_weight_description)
    Column {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            FilterChip(
                selected = set.isWarmup,
                onClick = { actions.changeSet(set, SetChange.ToggleWarmup) },
                label = { Text(workingNumber?.toString() ?: stringResource(R.string.warmup_short)) },
                modifier = Modifier.testTag(ActiveWorkoutTags.WARMUP_TOGGLE),
            )
            TextButton(
                onClick = { typing = TypedField.WEIGHT },
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier.semantics { contentDescription = weightDescription },
            ) {
                Text(if (set.weightKg > 0) formatWeight(set.weightKg) else "–", Modifier.testTag(ActiveWorkoutTags.WEIGHT))
                Text(" " + stringResource(R.string.unit_kg), style = MaterialTheme.typography.labelSmall)
            }
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                IconButton(
                    onClick = { actions.changeSet(set, SetChange.Reps(-1)) },
                    modifier = Modifier.size(36.dp).semantics { contentDescription = decreaseDescription },
                ) { Text("−") }
                TextButton(onClick = { typing = TypedField.REPS }, contentPadding = PaddingValues(horizontal = 4.dp)) {
                    Text(set.reps.toString(), Modifier.testTag(ActiveWorkoutTags.REPS))
                }
                IconButton(
                    onClick = { actions.changeSet(set, SetChange.Reps(1)) },
                    modifier = Modifier.size(36.dp).semantics { contentDescription = increaseDescription },
                ) { Text("+") }
            }
            TextButton(onClick = { pickingRpe = !pickingRpe }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text(set.rpe?.let { formatWeight(it) } ?: stringResource(R.string.rpe_label), Modifier.testTag(ActiveWorkoutTags.RPE))
            }
            IconButton(onClick = { actions.deleteSet(set) }) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_set))
            }
        }
        if (pickingRpe) {
            val description = stringResource(R.string.set_rpe_description, workingNumber ?: 0)
            FlowRow(
                Modifier.fillMaxWidth().semantics { contentDescription = description },
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                rpeValues.forEach { value ->
                    FilterChip(
                        selected = set.rpe == value,
                        onClick = {
                            actions.changeSet(set, SetChange.RpeTo(if (set.rpe == value) null else value))
                            pickingRpe = false
                        },
                        label = { Text(formatWeight(value)) },
                    )
                }
            }
        }
    }
    when (typing) {
        TypedField.WEIGHT -> NumberInputDialog(
            title = stringResource(R.string.set_weight_dialog_title),
            initial = if (set.weightKg > 0) plainNumber(set.weightKg) else "",
            allowDecimals = true,
            onConfirm = {
                actions.changeSet(set, SetChange.WeightTo(it))
                typing = null
            },
            onDismiss = { typing = null },
        )
        TypedField.REPS -> NumberInputDialog(
            title = stringResource(R.string.set_reps_dialog_title),
            initial = set.reps.toString(),
            allowDecimals = false,
            onConfirm = {
                actions.changeSet(set, SetChange.RepsTo(it.toInt()))
                typing = null
            },
            onDismiss = { typing = null },
        )
        null -> Unit
    }
}

private enum class TypedField { WEIGHT, REPS }

private fun plainNumber(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

private val rpeValues: List<Double> = generateSequence(RPE_MIN) { it + RPE_STEP }.takeWhile { it <= RPE_MAX }.toList()
