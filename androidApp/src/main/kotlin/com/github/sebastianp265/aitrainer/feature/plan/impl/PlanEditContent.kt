package com.github.sebastianp265.aitrainer.feature.plan.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.sebastianp265.aitrainer.shared.design.ChipButton
import com.github.sebastianp265.aitrainer.shared.design.DashedButton
import com.github.sebastianp265.aitrainer.shared.design.DesignTextField
import com.github.sebastianp265.aitrainer.shared.design.InfoNote
import com.github.sebastianp265.aitrainer.shared.design.extendedColors
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.shared.localization.displayName
import com.github.sebastianp265.aitrainer.integration.exercise.Equipment
import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
import com.github.sebastianp265.aitrainer.shared.workout.rpeScale
import com.github.sebastianp265.aitrainer.shared.localization.formatDate
import com.github.sebastianp265.aitrainer.shared.state.LocalCoachEnabled
import com.github.sebastianp265.aitrainer.shared.state.LocalOpenCoach
import com.github.sebastianp265.aitrainer.shared.localization.formatWeight
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant
import com.github.sebastianp265.aitrainer.shared.design.DesignIcons

internal interface PlanEditActions {
    fun changeName(name: String)
    fun changeDate(date: LocalDate?)
    fun addExercise()
    fun removeExercise(exerciseKey: String)
    fun addSet(exerciseKey: String)
    fun removeSet(setId: Long)
    fun changeWeight(setId: Long, weight: String)
    fun changeReps(setId: Long, reps: String)
    fun changeRpe(setId: Long, rpe: Double?)
    fun toggleWarmup(setId: Long)
    fun save()
    fun delete()
    fun close()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlanEditContent(state: PlanEditUiState, actions: PlanEditActions) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isNew) R.string.plan_new_title else R.string.plan_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = actions::close) { Icon(DesignIcons.Close, contentDescription = stringResource(R.string.close)) }
                },
                actions = {
                    Button(
                        onClick = actions::save,
                        enabled = state.canSave,
                        modifier = Modifier.padding(end = 8.dp).height(40.dp).testTag(PlanTags.SAVE),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                    ) {
                        Text(stringResource(R.string.save))
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.padding(padding).fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
            return@Scaffold
        }
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NameAndDate(state, actions)
            if (state.sets.isEmpty()) {
                EmptyExercises(actions::addExercise)
                Text(stringResource(R.string.plan_save_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (state.isNew && LocalCoachEnabled.current) {
                    val openCoach = LocalOpenCoach.current
                    InfoNote(stringResource(R.string.plan_prefer_ready_title), stringResource(R.string.plan_prefer_ready_text)) {
                        TextButton(
                            onClick = { openCoach(null) },
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onTertiaryContainer),
                        ) { Text(stringResource(R.string.plan_go_to_coach)) }
                    }
                }
            } else {
                val setsByExercise = state.sets.groupBy { it.exerciseKey }
                setsByExercise.forEach { (exerciseKey, sets) -> state.exercises[exerciseKey]?.let { ExerciseEditor(it, sets, actions) } }
                DashedButton(stringResource(R.string.add_exercise), DesignIcons.Add, onClick = actions::addExercise, modifier = Modifier.testTag(PlanTags.ADD_EXERCISE))
            }
            if (!state.isNew) {
                TextButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally).height(44.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.plan_delete), style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp))
                }
            }
        }
        if (confirmDelete) {
            DeletePlanDialog(state.name, onConfirm = { confirmDelete = false; actions.delete() }, onDismiss = { confirmDelete = false })
        }
    }
}

@Composable
private fun NameAndDate(state: PlanEditUiState, actions: PlanEditActions) {
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        DesignTextField(
            value = state.name,
            onValueChange = actions::changeName,
            label = stringResource(R.string.plan_name_label),
            placeholder = stringResource(R.string.plan_name_hint),
            modifier = Modifier.weight(1f),
            fieldModifier = Modifier.testTag(PlanTags.NAME_FIELD),
        )
        Box(Modifier.weight(1f)) {
            DesignTextField(
                value = state.date?.let { formatDate(it) }.orEmpty(),
                onValueChange = {},
                readOnly = true,
                label = stringResource(R.string.plan_date_label),
                placeholder = stringResource(R.string.plan_no_date),
                trailingIcon = {
                    if (state.date != null) {
                        IconButton(onClick = { actions.changeDate(null) }) {
                            Icon(DesignIcons.Close, contentDescription = stringResource(R.string.plan_clear_date))
                        }
                    }
                },
            )
            Box(Modifier.matchParentSize().padding(top = 20.dp, end = 48.dp).testTag(PlanTags.DATE_FIELD).clickable { pickingDate = true })
        }
    }
    if (pickingDate) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = state.date?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds())
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        actions.changeDate(Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date)
                    }
                    pickingDate = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(pickerState) }
    }
}

@Composable
private fun EmptyExercises(onAddExercise: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.plan_no_exercises_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.plan_no_exercises_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onAddExercise, Modifier.height(44.dp).testTag(PlanTags.ADD_EXERCISE)) {
                Icon(DesignIcons.Add, contentDescription = null, Modifier.padding(end = 8.dp))
                Text(stringResource(R.string.add_exercise))
            }
        }
    }
}

@Composable
private fun ExerciseEditor(exercise: Exercise, sets: List<EditableSet>, actions: PlanEditActions) {
    val locale = LocalConfiguration.current.locales[0]
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 14.dp, top = 10.dp, end = 6.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(exercise.displayName(locale), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp))
                IconButton(onClick = { actions.removeExercise(exercise.key) }) {
                    Icon(DesignIcons.Close, contentDescription = stringResource(R.string.remove_exercise), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.width(28.dp))
                ColumnHeader(stringResource(R.string.unit_kg), Modifier.weight(1f))
                ColumnHeader(stringResource(R.string.unit_reps), Modifier.weight(1f))
                ColumnHeader(stringResource(R.string.rpe_label), Modifier.weight(1f))
                Box(Modifier.width(44.dp))
            }
            var workingNumber = 0
            var warmupNumber = 0
            sets.forEach { set ->
                val label = if (set.isWarmup) stringResource(R.string.plan_warmup_n, ++warmupNumber) else stringResource(R.string.plan_set_n, ++workingNumber)
                SetEditor(label, if (set.isWarmup) null else workingNumber, set, actions)
            }
            ChipButton(stringResource(R.string.add_set), onClick = { actions.addSet(exercise.key) }, modifier = Modifier.padding(start = 34.dp))
        }
    }
}

@Composable
private fun ColumnHeader(text: String, modifier: Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
}

@Composable
private fun CellField(value: String, onValueChange: (String) -> Unit, keyboardType: KeyboardType, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.onSurface, textAlign = TextAlign.Center),
        cursorBrush = SolidColor(colors.primary),
        modifier = modifier,
        decorationBox = { field ->
            Box(Modifier.height(44.dp).background(colors.surface, MaterialTheme.shapes.small), contentAlignment = Alignment.Center) { field() }
        },
    )
}

@Composable
private fun SetEditor(label: String, workingNumber: Int?, set: EditableSet, actions: PlanEditActions) {
    var rpeMenuOpen by rememberSaveable(set.id) { mutableStateOf(false) }
    val kgDescription = stringResource(R.string.plan_field_description, label, stringResource(R.string.unit_kg))
    val repsDescription = stringResource(R.string.plan_field_description, label, stringResource(R.string.unit_reps))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            workingNumber?.toString() ?: stringResource(R.string.warmup_short),
            Modifier.width(28.dp).toggleable(value = set.isWarmup, role = Role.Checkbox) { actions.toggleWarmup(set.id) },
            style = MaterialTheme.typography.labelLarge,
            color = if (set.isWarmup) extendedColors.warning else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        CellField(
            set.weight,
            { actions.changeWeight(set.id, it) },
            KeyboardType.Decimal,
            Modifier.weight(1f).semantics { contentDescription = kgDescription }.testTag(PlanTags.SET_WEIGHT),
        )
        CellField(
            set.reps,
            { actions.changeReps(set.id, it) },
            KeyboardType.Number,
            Modifier.weight(1f).semantics { contentDescription = repsDescription }.testTag(PlanTags.SET_REPS),
        )
        if (set.isWarmup) {
            Spacer(Modifier.weight(1f))
        } else {
            Box(Modifier.weight(1f).height(44.dp).background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small).clickable { rpeMenuOpen = true }, contentAlignment = Alignment.Center) {
                Text(set.rpe?.let { formatWeight(it) } ?: "–", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium))
                DropdownMenu(expanded = rpeMenuOpen, onDismissRequest = { rpeMenuOpen = false }) {
                    Text(
                        stringResource(R.string.rpe_hint),
                        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    DropdownMenuItem(text = { Text("–") }, onClick = { actions.changeRpe(set.id, null); rpeMenuOpen = false })
                    rpeScale.forEach { value ->
                        DropdownMenuItem(text = { Text(formatWeight(value)) }, onClick = { actions.changeRpe(set.id, value); rpeMenuOpen = false })
                    }
                }
            }
        }
        IconButton(onClick = { actions.removeSet(set.id) }, Modifier.size(44.dp)) {
            Icon(DesignIcons.Delete, contentDescription = stringResource(R.string.delete_set), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}


@PreviewLightDark
@Composable
private fun PlanEditPreview() {
    val bench = Exercise("BENCH", mapOf("en" to "Bench press"), Equipment("BARBELL", mapOf("en" to "Barbell")), emptyList(), emptyList())
    MaterialTheme {
        PlanEditContent(
            state = PlanEditUiState(
                isLoading = false,
                isNew = false,
                name = "Push A",
                date = LocalDate(2026, 11, 12),
                sets = listOf(EditableSet(1, "BENCH", "40", "10", null, true), EditableSet(2, "BENCH", "82.5", "8", 8.0, false)),
                exercises = mapOf("BENCH" to bench),
            ),
            actions = object : PlanEditActions {
                override fun changeName(name: String) = Unit
                override fun changeDate(date: LocalDate?) = Unit
                override fun addExercise() = Unit
                override fun removeExercise(exerciseKey: String) = Unit
                override fun addSet(exerciseKey: String) = Unit
                override fun removeSet(setId: Long) = Unit
                override fun changeWeight(setId: Long, weight: String) = Unit
                override fun changeReps(setId: Long, reps: String) = Unit
                override fun changeRpe(setId: Long, rpe: Double?) = Unit
                override fun toggleWarmup(setId: Long) = Unit
                override fun save() = Unit
                override fun delete() = Unit
                override fun close() = Unit
            },
        )
    }
}
