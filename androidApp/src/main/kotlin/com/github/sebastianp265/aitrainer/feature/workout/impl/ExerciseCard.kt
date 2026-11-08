package com.github.sebastianp265.aitrainer.feature.workout.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.feature.exercise.displayName
import com.github.sebastianp265.aitrainer.integration.workout.ExerciseBlock
import com.github.sebastianp265.aitrainer.integration.workout.SetEntry

@Composable
internal fun ExerciseCard(block: ExerciseBlock, actions: LogWorkoutActions) {
    Card(Modifier.fillMaxWidth().testTag(LogWorkoutTags.exerciseCard(block.exercise.key))) {
        Column(Modifier.padding(12.dp)) {
            Text(block.exercise.displayName(LocalConfiguration.current.locales[0]), style = MaterialTheme.typography.titleMedium)
            block.sets.forEachIndexed { index, set ->
                if (index > 0) HorizontalDivider()
                SetRow(index + 1, set, actions)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { actions.addSet(block) }) { Text(stringResource(R.string.add_set)) }
                OutlinedButton(onClick = { actions.copySetsFromLastTime(block) }) { Text(stringResource(R.string.same_as_last_time)) }
            }
        }
    }
}

@Composable
internal fun SetRow(number: Int, set: SetEntry, actions: LogWorkoutActions) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        FilterChip(
            selected = set.isWarmup,
            onClick = { actions.toggleWarmup(set) },
            label = { Text(if (set.isWarmup) stringResource(R.string.warmup_short) else "$number") },
        )
        Spacer(Modifier.width(4.dp))
        Stepper(
            value = formatWeight(set.weightKg),
            valueTag = LogWorkoutTags.WEIGHT,
            unit = stringResource(R.string.unit_kg),
            decreaseDescription = stringResource(R.string.decrease_weight),
            increaseDescription = stringResource(R.string.increase_weight),
            onMinus = { actions.changeWeight(set, -WEIGHT_STEP_KG) },
            onPlus = { actions.changeWeight(set, WEIGHT_STEP_KG) },
            modifier = Modifier.weight(1f),
        )
        Stepper(
            value = set.reps.toString(),
            valueTag = LogWorkoutTags.REPS,
            unit = stringResource(R.string.unit_reps),
            decreaseDescription = stringResource(R.string.decrease_reps),
            increaseDescription = stringResource(R.string.increase_reps),
            onMinus = { actions.changeReps(set, -1) },
            onPlus = { actions.changeReps(set, 1) },
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { actions.deleteSet(set) }) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_set))
        }
    }
}

private const val WEIGHT_STEP_KG = 2.5

@Composable
internal fun Stepper(
    value: String,
    valueTag: String,
    unit: String,
    decreaseDescription: String,
    increaseDescription: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        FilledTonalIconButton(onClick = onMinus, modifier = Modifier.semantics { contentDescription = decreaseDescription }) {
            Text("−")
        }
        Column(Modifier.widthIn(min = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, Modifier.testTag(valueTag), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Text(unit, style = MaterialTheme.typography.labelSmall)
        }
        FilledTonalIconButton(onClick = onPlus, modifier = Modifier.semantics { contentDescription = increaseDescription }) {
            Text("+")
        }
    }
}
