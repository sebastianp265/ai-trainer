package com.github.sebastianp265.aitrainer.ui.log

import android.content.res.Resources
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.domain.model.Equipment
import com.github.sebastianp265.aitrainer.domain.model.Exercise
import com.github.sebastianp265.aitrainer.domain.model.ExercisePreference
import com.github.sebastianp265.aitrainer.domain.model.MuscleGroup
import com.github.sebastianp265.aitrainer.domain.model.SessionSummary
import com.github.sebastianp265.aitrainer.domain.model.SetEntry
import com.github.sebastianp265.aitrainer.domain.model.WorkoutSession
import com.github.sebastianp265.aitrainer.domain.workout.ExerciseBlock
import com.github.sebastianp265.aitrainer.domain.workout.WEIGHT_STEP_KG
import com.github.sebastianp265.aitrainer.ui.displayName
import com.github.sebastianp265.aitrainer.ui.formatDate
import com.github.sebastianp265.aitrainer.ui.formatWeight
import com.github.sebastianp265.aitrainer.ui.labelRes
import com.github.sebastianp265.aitrainer.ui.theme.AITrainerTheme
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Instant

object LogWorkoutTags {
    const val WEIGHT = "set_weight"
    const val REPS = "set_reps"

    fun exerciseCard(exerciseName: String) = "exercise_card:$exerciseName"
}

interface LogWorkoutActions {
    fun startSession()
    fun finishSession()
    fun discardSession()
    fun addExercise(exercise: Exercise)
    fun addSet(block: ExerciseBlock)
    fun copySetsFromLastTime(block: ExerciseBlock)
    fun changeReps(set: SetEntry, delta: Int)
    fun changeWeight(set: SetEntry, deltaKg: Double)
    fun toggleWarmup(set: SetEntry)
    fun deleteSet(set: SetEntry)
}

@Composable
fun LogWorkoutRoute(viewModel: LogWorkoutViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actions = remember(viewModel) {
        object : LogWorkoutActions {
            override fun startSession() = viewModel.startSession()
            override fun finishSession() = viewModel.finishSession()
            override fun discardSession() = viewModel.discardSession()
            override fun addExercise(exercise: Exercise) = viewModel.addExercise(exercise)
            override fun addSet(block: ExerciseBlock) = viewModel.addSet(block)
            override fun copySetsFromLastTime(block: ExerciseBlock) = viewModel.copySetsFromLastTime(block)
            override fun changeReps(set: SetEntry, delta: Int) = viewModel.changeReps(set, delta)
            override fun changeWeight(set: SetEntry, deltaKg: Double) = viewModel.changeWeight(set, deltaKg)
            override fun toggleWarmup(set: SetEntry) = viewModel.toggleWarmup(set)
            override fun deleteSet(set: SetEntry) = viewModel.deleteSet(set)
        }
    }
    LogWorkoutScreen(state = state, actions = actions, onMessageShown = viewModel::messageShown)
}

@Composable
fun LogWorkoutScreen(
    state: LogUiState,
    actions: LogWorkoutActions,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it.text(resources))
            onMessageShown()
        }
    }
    var showPicker by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (state.activeSession != null) {
                ExtendedFloatingActionButton(
                    onClick = { showPicker = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_exercise)) },
                    text = { Text(stringResource(R.string.exercise_button)) },
                )
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.activeSession != null -> ActiveSession(state.activeSession, actions)
                else -> NoActiveSession(state.recentSessions, actions::startSession)
            }
        }
    }

    if (showPicker) {
        ExercisePickerDialog(
            exercises = state.exercises,
            onPick = {
                actions.addExercise(it)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun NoActiveSession(recent: List<SessionSummary>, onStart: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.start_workout)) }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.recent_workouts), style = MaterialTheme.typography.titleMedium)
        }
        if (recent.isEmpty()) {
            item { Text(stringResource(R.string.no_workouts_yet), style = MaterialTheme.typography.bodyMedium) }
        }
        items(recent, key = { it.id }) { session ->
            ListItem(
                headlineContent = { Text(formatDate(session.date)) },
                supportingContent = { Text(sessionSummary(session.exerciseCount, session.setCount)) },
            )
        }
    }
}

@Composable
private fun ActiveSession(active: ActiveSessionUi, actions: LogWorkoutActions) {
    var confirmDiscard by remember { mutableStateOf(false) }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.workout_title, formatDate(active.session.date)),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { confirmDiscard = true }) { Text(stringResource(R.string.discard)) }
                Button(onClick = actions::finishSession) { Text(stringResource(R.string.finish)) }
            }
        }
        if (active.blocks.isEmpty()) {
            item { Text(stringResource(R.string.add_first_exercise_hint)) }
        }
        items(active.blocks, key = { it.exercise.id }) { block -> ExerciseCard(block, actions) }
    }
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.discard_workout_title)) },
            text = { Text(stringResource(R.string.discard_workout_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    actions.discardSession()
                }) { Text(stringResource(R.string.discard)) }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun ExerciseCard(block: ExerciseBlock, actions: LogWorkoutActions) {
    Card(Modifier.fillMaxWidth().testTag(LogWorkoutTags.exerciseCard(block.exercise.name))) {
        Column(Modifier.padding(12.dp)) {
            Text(block.exercise.displayName(LocalResources.current), style = MaterialTheme.typography.titleMedium)
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
private fun SetRow(number: Int, set: SetEntry, actions: LogWorkoutActions) {
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

@Composable
private fun Stepper(
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

@Composable
private fun ExercisePickerDialog(exercises: List<Exercise>, onPick: (Exercise) -> Unit, onDismiss: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val resources = LocalResources.current
    val locale = LocalConfiguration.current.locales[0]
    val items = remember(exercises, resources, locale) { sortedPickerItems(exercises, { it.displayName(resources) }, locale) }
    val filtered = items.searchByDisplayOrEnglishName(query)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_exercise)) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.search)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(Modifier.height(360.dp)) {
                    items(filtered, key = { it.exercise.id }) { item ->
                        ListItem(
                            headlineContent = { Text(item.displayName) },
                            supportingContent = {
                                Text(item.exercise.primaryMuscles.joinToString { resources.getString(it.labelRes()) })
                            },
                            modifier = Modifier.clickable { onPick(item.exercise) },
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun sessionSummary(exerciseCount: Int, setCount: Int): String = stringResource(
    R.string.session_summary,
    pluralStringResource(R.plurals.exercise_count, exerciseCount, exerciseCount),
    pluralStringResource(R.plurals.set_count, setCount, setCount),
)

private fun LogMessage.text(resources: Resources): String = when (this) {
    LogMessage.WorkoutSaved -> resources.getString(R.string.workout_saved)
    is LogMessage.NoPreviousSets -> resources.getString(R.string.no_previous_sets, exercise.displayName(resources))
}

private object NoOpActions : LogWorkoutActions {
    override fun startSession() {}
    override fun finishSession() {}
    override fun discardSession() {}
    override fun addExercise(exercise: Exercise) {}
    override fun addSet(block: ExerciseBlock) {}
    override fun copySetsFromLastTime(block: ExerciseBlock) {}
    override fun changeReps(set: SetEntry, delta: Int) {}
    override fun changeWeight(set: SetEntry, deltaKg: Double) {}
    override fun toggleWarmup(set: SetEntry) {}
    override fun deleteSet(set: SetEntry) {}
}

private val previewBench = Exercise(1, "Barbell Bench Press", listOf(MuscleGroup.CHEST), listOf(MuscleGroup.TRICEPS), Equipment.BARBELL, ExercisePreference.FAVOURITE)

@Preview(showBackground = true)
@Preview(name = "Polski", locale = "pl", showBackground = true)
@Composable
private fun ActiveSessionPreview() {
    val sets = listOf(
        SetEntry(1, 1, 1, 0, 10, 40.0, null, isWarmup = true),
        SetEntry(2, 1, 1, 1, 8, 80.0, null, isWarmup = false),
        SetEntry(3, 1, 1, 2, 7, 82.5, null, isWarmup = false),
    )
    val session = WorkoutSession(1, LocalDate(2026, 10, 25), Instant.fromEpochMilliseconds(0), null, null, null)
    AITrainerTheme {
        LogWorkoutScreen(
            state = LogUiState(isLoading = false, activeSession = ActiveSessionUi(session, listOf(ExerciseBlock(previewBench, sets)))),
            actions = NoOpActions,
            onMessageShown = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NoSessionPreview() {
    AITrainerTheme {
        LogWorkoutScreen(
            state = LogUiState(
                isLoading = false,
                recentSessions = listOf(
                    SessionSummary(1, LocalDate(2026, 10, 23), Instant.fromEpochMilliseconds(0), Instant.fromEpochMilliseconds(1), 5, 18),
                ),
            ),
            actions = NoOpActions,
            onMessageShown = {},
        )
    }
}
