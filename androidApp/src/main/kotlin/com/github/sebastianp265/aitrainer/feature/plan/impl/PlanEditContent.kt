package com.github.sebastianp265.aitrainer.feature.plan.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.sebastianp265.aitrainer.shared.design.DashedButton
import com.github.sebastianp265.aitrainer.shared.design.DesignTextField
import com.github.sebastianp265.aitrainer.shared.design.InfoNote
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.shared.localization.displayName
import com.github.sebastianp265.aitrainer.integration.exercise.Equipment
import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
import com.github.sebastianp265.aitrainer.shared.workout.EditedSet
import com.github.sebastianp265.aitrainer.shared.workout.ExerciseSetsCard
import com.github.sebastianp265.aitrainer.shared.workout.SetRow
import com.github.sebastianp265.aitrainer.shared.workout.SetRowActions
import com.github.sebastianp265.aitrainer.shared.state.LocalCoachEnabled
import com.github.sebastianp265.aitrainer.shared.state.LocalOpenCoach
import com.github.sebastianp265.aitrainer.shared.design.DesignIcons

internal interface PlanEditActions {
    fun changeName(name: String)
    fun addExercise()
    fun removeExercise(exerciseKey: String)
    fun addSet(exerciseKey: String)
    fun removeSet(setId: Long)
    fun changeWeight(setId: Long, kg: Double)
    fun changeReps(setId: Long, reps: Int)
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
            NameField(state, actions)
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
private fun NameField(state: PlanEditUiState, actions: PlanEditActions) {
    DesignTextField(
        value = state.name,
        onValueChange = actions::changeName,
        label = stringResource(R.string.plan_name_label),
        placeholder = stringResource(R.string.plan_name_hint),
        modifier = Modifier.fillMaxWidth(),
        fieldModifier = Modifier.testTag(PlanTags.NAME_FIELD),
    )
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
    ExerciseSetsCard(
        name = exercise.displayName(LocalConfiguration.current.locales[0]),
        setCount = sets.size,
        onRemove = { actions.removeExercise(exercise.key) },
        onAddSet = { actions.addSet(exercise.key) },
    ) {
        var workingNumber = 0
        sets.forEach { set ->
            if (!set.isWarmup) workingNumber++
            SetRow(EditedSet(set.id, if (set.isWarmup) null else workingNumber, set.weightKg, set.reps, set.rpe), setActions(set.id, actions))
        }
    }
}

private fun setActions(setId: Long, actions: PlanEditActions) = object : SetRowActions {
    override fun toggleWarmup() = actions.toggleWarmup(setId)
    override fun changeWeight(kg: Double) = actions.changeWeight(setId, kg)
    override fun changeReps(reps: Int) = actions.changeReps(setId, reps)
    override fun changeRpe(rpe: Double?) = actions.changeRpe(setId, rpe)
    override fun delete() = actions.removeSet(setId)
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
                sets = listOf(EditableSet(1, "BENCH", 40.0, 10, null, true), EditableSet(2, "BENCH", 82.5, 8, 8.0, false)),
                exercises = mapOf("BENCH" to bench),
            ),
            actions = object : PlanEditActions {
                override fun changeName(name: String) = Unit
                override fun addExercise() = Unit
                override fun removeExercise(exerciseKey: String) = Unit
                override fun addSet(exerciseKey: String) = Unit
                override fun removeSet(setId: Long) = Unit
                override fun changeWeight(setId: Long, kg: Double) = Unit
                override fun changeReps(setId: Long, reps: Int) = Unit
                override fun changeRpe(setId: Long, rpe: Double?) = Unit
                override fun toggleWarmup(setId: Long) = Unit
                override fun save() = Unit
                override fun delete() = Unit
                override fun close() = Unit
            },
        )
    }
}
