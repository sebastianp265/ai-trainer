package com.github.sebastianp265.aitrainer.feature.plan.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.shared.localization.displayName
import com.github.sebastianp265.aitrainer.integration.exercise.Equipment
import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
import com.github.sebastianp265.aitrainer.integration.workout.RPE_MAX
import com.github.sebastianp265.aitrainer.integration.workout.RPE_MIN
import com.github.sebastianp265.aitrainer.integration.workout.RPE_STEP
import com.github.sebastianp265.aitrainer.shared.localization.formatDate
import com.github.sebastianp265.aitrainer.shared.state.LocalCoachEnabled
import com.github.sebastianp265.aitrainer.shared.state.LocalOpenCoach
import com.github.sebastianp265.aitrainer.shared.localization.formatWeight
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

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
                    IconButton(onClick = actions::close) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close)) }
                },
                actions = {
                    TextButton(onClick = actions::save, enabled = state.canSave, modifier = Modifier.testTag(PlanTags.SAVE)) {
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
                Text(stringResource(R.string.plan_save_hint), style = MaterialTheme.typography.bodyMedium)
                if (state.isNew && LocalCoachEnabled.current) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.plan_prefer_ready_title), style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(R.string.plan_prefer_ready_text), style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = LocalOpenCoach.current) { Text(stringResource(R.string.plan_go_to_coach)) }
                        }
                    }
                }
            } else {
                val setsByExercise = state.sets.groupBy { it.exerciseKey }
                setsByExercise.forEach { (exerciseKey, sets) -> state.exercises[exerciseKey]?.let { ExerciseEditor(it, sets, actions) } }
                OutlinedButton(onClick = actions::addExercise, Modifier.fillMaxWidth().testTag(PlanTags.ADD_EXERCISE)) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(stringResource(R.string.add_exercise))
                }
            }
            if (!state.isNew) {
                TextButton(onClick = { confirmDelete = true }, Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.plan_delete), color = MaterialTheme.colorScheme.error)
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
        OutlinedTextField(
            value = state.name,
            onValueChange = actions::changeName,
            label = { Text(stringResource(R.string.plan_name_label)) },
            placeholder = { Text(stringResource(R.string.plan_name_hint)) },
            singleLine = true,
            modifier = Modifier.weight(1f).testTag(PlanTags.NAME_FIELD),
        )
        Box(Modifier.weight(1f)) {
            OutlinedTextField(
                value = state.date?.let { formatDate(it) }.orEmpty(),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.plan_date_label)) },
                placeholder = { Text(stringResource(R.string.plan_no_date)) },
                trailingIcon = {
                    if (state.date != null) {
                        IconButton(onClick = { actions.changeDate(null) }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.plan_clear_date))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Box(Modifier.matchParentSize().padding(end = 48.dp).testTag(PlanTags.DATE_FIELD).clickable { pickingDate = true })
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
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.plan_no_exercises_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.plan_no_exercises_text), style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onAddExercise, Modifier.testTag(PlanTags.ADD_EXERCISE)) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(stringResource(R.string.add_exercise))
            }
        }
    }
}

@Composable
private fun ExerciseEditor(exercise: Exercise, sets: List<EditableSet>, actions: PlanEditActions) {
    val locale = LocalConfiguration.current.locales[0]
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(exercise.displayName(locale), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { actions.removeExercise(exercise.key) }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove_exercise))
                }
            }
            Row(Modifier.fillMaxWidth()) {
                Box(Modifier.size(56.dp))
                Text(stringResource(R.string.unit_kg), Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                Text(stringResource(R.string.unit_reps), Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                Text(stringResource(R.string.rpe_label), Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                Box(Modifier.size(48.dp))
            }
            var workingNumber = 0
            var warmupNumber = 0
            sets.forEach { set ->
                val label = if (set.isWarmup) stringResource(R.string.plan_warmup_n, ++warmupNumber) else stringResource(R.string.plan_set_n, ++workingNumber)
                SetEditor(label, if (set.isWarmup) null else workingNumber, set, actions)
            }
            AssistChip(onClick = { actions.addSet(exercise.key) }, label = { Text(stringResource(R.string.add_set)) })
        }
    }
}

@Composable
private fun SetEditor(label: String, workingNumber: Int?, set: EditableSet, actions: PlanEditActions) {
    var rpeMenuOpen by rememberSaveable(set.id) { mutableStateOf(false) }
    val kgDescription = stringResource(R.string.plan_field_description, label, stringResource(R.string.unit_kg))
    val repsDescription = stringResource(R.string.plan_field_description, label, stringResource(R.string.unit_reps))
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        FilterChip(
            selected = set.isWarmup,
            onClick = { actions.toggleWarmup(set.id) },
            label = { Text(workingNumber?.toString() ?: stringResource(R.string.warmup_short)) },
            modifier = Modifier.size(width = 56.dp, height = 40.dp),
        )
        OutlinedTextField(
            value = set.weight,
            onValueChange = { actions.changeWeight(set.id, it) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f).padding(horizontal = 2.dp).semantics { contentDescription = kgDescription }.testTag(PlanTags.SET_WEIGHT),
        )
        OutlinedTextField(
            value = set.reps,
            onValueChange = { actions.changeReps(set.id, it) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f).padding(horizontal = 2.dp).semantics { contentDescription = repsDescription }.testTag(PlanTags.SET_REPS),
        )
        Box(Modifier.weight(1f)) {
            TextButton(onClick = { rpeMenuOpen = true }) { Text(set.rpe?.let { formatWeight(it) } ?: "–") }
            DropdownMenu(expanded = rpeMenuOpen, onDismissRequest = { rpeMenuOpen = false }) {
                DropdownMenuItem(text = { Text("–") }, onClick = { actions.changeRpe(set.id, null); rpeMenuOpen = false })
                rpeChoices.forEach { value ->
                    DropdownMenuItem(text = { Text(formatWeight(value)) }, onClick = { actions.changeRpe(set.id, value); rpeMenuOpen = false })
                }
            }
        }
        IconButton(onClick = { actions.removeSet(set.id) }) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_set))
        }
    }
}

private val rpeChoices: List<Double> = generateSequence(RPE_MIN) { it + RPE_STEP }.takeWhile { it <= RPE_MAX }.toList()

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
