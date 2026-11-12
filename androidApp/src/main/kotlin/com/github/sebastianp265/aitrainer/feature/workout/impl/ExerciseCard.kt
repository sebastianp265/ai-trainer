package com.github.sebastianp265.aitrainer.feature.workout.impl

import com.github.sebastianp265.aitrainer.integration.workout.ExerciseBlock
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import android.speech.RecognizerIntent
import android.content.Intent
import android.content.ActivityNotFoundException
import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.github.sebastianp265.aitrainer.shared.design.ChipButton
import com.github.sebastianp265.aitrainer.shared.design.DesignIcons
import com.github.sebastianp265.aitrainer.shared.design.extendedColors

@Composable
internal fun ExerciseCard(ui: ExerciseBlockUi, actions: ActiveWorkoutActions, onOpenHistory: () -> Unit) {
    val block = ui.block
    var expanded by rememberSaveable(block.exercise.key) { mutableStateOf(true) }
    Card(Modifier.fillMaxWidth().testTag(ActiveWorkoutTags.exerciseCard(block.exercise.key))) {
        Column(Modifier.padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    block.exercise.displayName(LocalConfiguration.current.locales[0]),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                )
                if (expanded) {
                    IconButton(onClick = { actions.removeExercise(block.exercise) }, Modifier.size(44.dp)) {
                        Icon(DesignIcons.Close, contentDescription = stringResource(R.string.remove_exercise), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Text(
                        pluralStringResource(R.plurals.set_count, block.sets.size, block.sets.size),
                        Modifier.padding(end = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            LastTimeLabel(ui, onOpenHistory)
            if (expanded) {
                var workingNumber = 0
                block.sets.forEach { set ->
                    if (!set.isWarmup) workingNumber++
                    SetRow(if (set.isWarmup) null else workingNumber, set, actions)
                }
                Row(Modifier.padding(start = 30.dp, top = 4.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChipButton(stringResource(R.string.add_set), onClick = { actions.addSet(block) })
                    SaySetButton(block, actions)
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

@Composable
private fun SaySetButton(block: ExerciseBlock, actions: ActiveWorkoutActions) {
    val prompt = stringResource(R.string.voice_prompt)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val alternatives = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).orEmpty()
        if (result.resultCode == Activity.RESULT_OK && alternatives.isNotEmpty()) actions.addSpokenSet(block, alternatives)
    }
    IconButton(
        onClick = {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
                .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            try {
                launcher.launch(intent)
            } catch (_: ActivityNotFoundException) {
                actions.voiceUnavailable()
            }
        },
        modifier = Modifier.size(36.dp).testTag(ActiveWorkoutTags.SAY_SET),
    ) { Icon(DesignIcons.Mic, contentDescription = stringResource(R.string.voice_add_set), tint = MaterialTheme.colorScheme.primary) }
}

@Composable
internal fun SetRow(workingNumber: Int?, set: SetEntry, actions: ActiveWorkoutActions) {
    var typing by rememberSaveable(set.id) { mutableStateOf<TypedField?>(null) }
    var pickingRpe by rememberSaveable(set.id) { mutableStateOf(false) }
    val decreaseDescription = stringResource(R.string.decrease_reps)
    val increaseDescription = stringResource(R.string.increase_reps)
    val weightDescription = stringResource(R.string.set_weight_description)
    Column {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 52.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                workingNumber?.toString() ?: stringResource(R.string.warmup_short),
                modifier = Modifier.width(24.dp).toggleable(value = set.isWarmup, role = Role.Checkbox) { actions.changeSet(set, SetChange.ToggleWarmup) }.testTag(ActiveWorkoutTags.WARMUP_TOGGLE),
                style = MaterialTheme.typography.labelLarge,
                color = if (set.isWarmup) extendedColors.warning else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            ValueBox(
                Modifier.width(72.dp).clickable { typing = TypedField.WEIGHT }.semantics { contentDescription = weightDescription },
            ) {
                Text(if (set.weightKg > 0) formatWeight(set.weightKg) else "–", Modifier.testTag(ActiveWorkoutTags.WEIGHT), style = valueStyle())
                UnitText(stringResource(R.string.unit_kg))
            }
            Row(
                Modifier.weight(1f).height(44.dp).background(MaterialTheme.colorScheme.surface, FieldShape).padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StepButton("−", decreaseDescription) { actions.changeSet(set, SetChange.Reps(-1)) }
                Column(
                    Modifier.weight(1f).fillMaxHeight().clickable { typing = TypedField.REPS },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(set.reps.toString(), Modifier.testTag(ActiveWorkoutTags.REPS), style = valueStyle())
                    UnitText(stringResource(R.string.unit_reps))
                }
                StepButton("+", increaseDescription) { actions.changeSet(set, SetChange.Reps(1)) }
            }
            RpeButton(set.rpe, picking = pickingRpe) { pickingRpe = !pickingRpe }
            IconButton(onClick = { actions.deleteSet(set) }, Modifier.size(32.dp, 44.dp)) {
                Icon(DesignIcons.Delete, contentDescription = stringResource(R.string.delete_set), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (pickingRpe) {
            val description = stringResource(R.string.set_rpe_description, workingNumber ?: 0)
            FlowRow(
                Modifier.fillMaxWidth().padding(start = 30.dp, top = 4.dp, bottom = 8.dp).semantics { contentDescription = description },
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                rpeValues.forEach { value ->
                    RpeChip(formatWeight(value), selected = set.rpe == value) {
                        actions.changeSet(set, SetChange.RpeTo(if (set.rpe == value) null else value))
                        pickingRpe = false
                    }
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

private val FieldShape = RoundedCornerShape(10.dp)

@Composable
private fun valueStyle() = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, lineHeight = 20.sp)

@Composable
private fun UnitText(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal), color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ValueBox(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.height(44.dp).background(MaterialTheme.colorScheme.surface, FieldShape),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content,
    )
}

@Composable
private fun StepButton(text: String, description: String, onClick: () -> Unit) {
    Box(
        Modifier.size(32.dp, 44.dp).clickable(onClick = onClick).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp), color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun RpeButton(rpe: Double?, picking: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val filled = rpe != null && picking
    val outline = colors.outline
    Box(
        Modifier.width(64.dp).height(44.dp)
            .then(
                when {
                    filled -> Modifier.background(colors.primary, FieldShape)
                    rpe != null -> Modifier.border(1.dp, outline, FieldShape)
                    else -> Modifier.drawBehind {
                        drawRoundRect(
                            color = outline,
                            style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
                            cornerRadius = CornerRadius(10.dp.toPx()),
                        )
                    }
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            rpe?.let { formatWeight(it) } ?: stringResource(R.string.rpe_label),
            Modifier.testTag(ActiveWorkoutTags.RPE),
            style = MaterialTheme.typography.labelLarge,
            color = when {
                filled -> colors.onPrimary
                rpe != null -> colors.onSurface
                else -> colors.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun RpeChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = Modifier.height(36.dp).widthIn(min = 40.dp),
        shape = MaterialTheme.shapes.small,
        color = if (selected) colors.primary else colors.surface,
        contentColor = if (selected) colors.onPrimary else colors.onSurface,
        border = if (selected) null else BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Box(Modifier.padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

private enum class TypedField { WEIGHT, REPS }

private fun plainNumber(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

private val rpeValues: List<Double> = generateSequence(RPE_MIN) { it + RPE_STEP }.takeWhile { it <= RPE_MAX }.toList()
