package com.github.sebastianp265.aitrainer.shared.workout

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.shared.design.ChipButton
import com.github.sebastianp265.aitrainer.shared.design.DesignIcons
import com.github.sebastianp265.aitrainer.shared.design.extendedColors
import com.github.sebastianp265.aitrainer.shared.localization.formatWeight
import com.github.sebastianp265.aitrainer.shared.workout.impl.NumberInputDialog
import com.github.sebastianp265.aitrainer.shared.workout.impl.rpeScale

internal class EditedSet(
    val key: Long,
    val workingNumber: Int?,
    val weightKg: Double,
    val reps: Int,
    val rpe: Double?,
)

internal interface SetRowActions {
    fun toggleWarmup()
    fun changeWeight(kg: Double)
    fun changeReps(reps: Int)
    fun changeRpe(rpe: Double?)
    fun delete()
}

@Composable
internal fun ExerciseSetsCard(
    name: String,
    setCount: Int,
    onRemove: () -> Unit,
    onAddSet: () -> Unit,
    modifier: Modifier = Modifier,
    belowName: @Composable () -> Unit = {},
    sets: @Composable () -> Unit,
) {
    var expanded by rememberSaveable(name) { mutableStateOf(true) }
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp))
                if (expanded) {
                    IconButton(onClick = onRemove, Modifier.size(44.dp)) {
                        Icon(DesignIcons.Close, contentDescription = stringResource(R.string.remove_exercise), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Text(
                        pluralStringResource(R.plurals.set_count, setCount, setCount),
                        Modifier.padding(end = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            belowName()
            if (expanded) {
                sets()
                Row(Modifier.padding(start = 30.dp, top = 4.dp, bottom = 4.dp)) {
                    ChipButton(stringResource(R.string.add_set), onClick = onAddSet)
                }
            }
        }
    }
}

@Composable
internal fun SetRow(set: EditedSet, actions: SetRowActions) {
    var typing by rememberSaveable(set.key) { mutableStateOf<TypedField?>(null) }
    var pickingRpe by rememberSaveable(set.key) { mutableStateOf(false) }
    val isWarmup = set.workingNumber == null
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
                set.workingNumber?.toString() ?: stringResource(R.string.warmup_short),
                modifier = Modifier.width(24.dp).toggleable(value = isWarmup, role = Role.Checkbox) { actions.toggleWarmup() }.testTag(SetRowTags.WARMUP_TOGGLE),
                style = MaterialTheme.typography.labelLarge,
                color = if (isWarmup) extendedColors.warning else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            ValueBox(Modifier.width(72.dp).clickable { typing = TypedField.WEIGHT }.semantics { contentDescription = weightDescription }) {
                Text(if (set.weightKg > 0) formatWeight(set.weightKg) else "–", Modifier.testTag(SetRowTags.WEIGHT), style = valueStyle())
                UnitText(stringResource(R.string.unit_kg))
            }
            Row(
                Modifier.weight(1f).height(44.dp).background(MaterialTheme.colorScheme.surface, FieldShape).padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StepButton("−", decreaseDescription) { actions.changeReps((set.reps - 1).coerceAtLeast(0)) }
                Column(
                    Modifier.weight(1f).fillMaxHeight().clickable { typing = TypedField.REPS },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(set.reps.toString(), Modifier.testTag(SetRowTags.REPS), style = valueStyle())
                    UnitText(stringResource(R.string.unit_reps))
                }
                StepButton("+", increaseDescription) { actions.changeReps(set.reps + 1) }
            }
            if (isWarmup) {
                Spacer(Modifier.width(RpeButtonWidth))
            } else {
                RpeButton(set.rpe, picking = pickingRpe) { pickingRpe = !pickingRpe }
            }
            IconButton(onClick = actions::delete, Modifier.size(32.dp, 44.dp)) {
                Icon(DesignIcons.Delete, contentDescription = stringResource(R.string.delete_set), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (pickingRpe && set.workingNumber != null) {
            val description = stringResource(R.string.set_rpe_description, set.workingNumber)
            Column(Modifier.padding(start = 30.dp, top = 4.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.rpe_hint), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(
                    Modifier.fillMaxWidth().semantics { contentDescription = description },
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    rpeScale.forEach { value ->
                        RpeChip(formatWeight(value), selected = set.rpe == value) {
                            actions.changeRpe(if (set.rpe == value) null else value)
                            pickingRpe = false
                        }
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
                actions.changeWeight(it)
                typing = null
            },
            onDismiss = { typing = null },
        )
        TypedField.REPS -> NumberInputDialog(
            title = stringResource(R.string.set_reps_dialog_title),
            initial = set.reps.toString(),
            allowDecimals = false,
            onConfirm = {
                actions.changeReps(it.toInt())
                typing = null
            },
            onDismiss = { typing = null },
        )
        null -> Unit
    }
}

internal object SetRowTags {
    const val WEIGHT = "set_weight"
    const val REPS = "set_reps"
    const val RPE = "set_rpe"
    const val WARMUP_TOGGLE = "set_warmup_toggle"
}

private val FieldShape = RoundedCornerShape(10.dp)

private val RpeButtonWidth = 64.dp

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
        Modifier.width(RpeButtonWidth).height(44.dp)
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
            Modifier.testTag(SetRowTags.RPE),
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

@PreviewLightDark
@Composable
private fun SetEditingPreview() {
    val actions = object : SetRowActions {
        override fun toggleWarmup() = Unit
        override fun changeWeight(kg: Double) = Unit
        override fun changeReps(reps: Int) = Unit
        override fun changeRpe(rpe: Double?) = Unit
        override fun delete() = Unit
    }
    MaterialTheme {
        Surface {
            ExerciseSetsCard("Bench press", setCount = 2, onRemove = {}, onAddSet = {}, modifier = Modifier.padding(16.dp)) {
                SetRow(EditedSet(1, workingNumber = null, weightKg = 40.0, reps = 10, rpe = null), actions)
                SetRow(EditedSet(2, workingNumber = 1, weightKg = 82.5, reps = 8, rpe = 8.0), actions)
            }
        }
    }
}
