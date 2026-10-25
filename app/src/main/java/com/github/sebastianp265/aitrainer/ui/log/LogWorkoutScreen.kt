package com.github.sebastianp265.aitrainer.ui.log

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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.sebastianp265.aitrainer.domain.model.Equipment
import com.github.sebastianp265.aitrainer.domain.model.Exercise
import com.github.sebastianp265.aitrainer.domain.model.ExercisePreference
import com.github.sebastianp265.aitrainer.domain.model.MuscleGroup
import com.github.sebastianp265.aitrainer.domain.model.SessionSummary
import com.github.sebastianp265.aitrainer.domain.model.SetEntry
import com.github.sebastianp265.aitrainer.domain.model.WorkoutSession
import com.github.sebastianp265.aitrainer.domain.workout.ExerciseBlock
import com.github.sebastianp265.aitrainer.domain.workout.WEIGHT_STEP_KG
import com.github.sebastianp265.aitrainer.ui.theme.AITrainerTheme
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Instant

interface LogWorkoutActions {
    fun startSession()
    fun finishSession()
    fun discardSession()
    fun addExercise(exercise: Exercise)
    fun addSet(block: ExerciseBlock)
    fun sameAsLastTime(block: ExerciseBlock)
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
            override fun sameAsLastTime(block: ExerciseBlock) = viewModel.sameAsLastTime(block)
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
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
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
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add exercise") },
                    text = { Text("Exercise") },
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
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("Start workout") }
            Spacer(Modifier.height(16.dp))
            Text("Recent workouts", style = MaterialTheme.typography.titleMedium)
        }
        if (recent.isEmpty()) {
            item { Text("No workouts logged yet.", style = MaterialTheme.typography.bodyMedium) }
        }
        items(recent, key = { it.id }) { session ->
            ListItem(
                headlineContent = { Text(session.date.toString()) },
                supportingContent = { Text("${session.exerciseCount} exercises · ${session.setCount} sets") },
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
                    "Workout · ${active.session.date}",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { confirmDiscard = true }) { Text("Discard") }
                Button(onClick = actions::finishSession) { Text("Finish") }
            }
        }
        if (active.blocks.isEmpty()) {
            item { Text("Add your first exercise with the button below.") }
        }
        items(active.blocks, key = { it.exercise.id }) { block -> ExerciseCard(block, actions) }
    }
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard workout?") },
            text = { Text("All sets logged in this session will be deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    actions.discardSession()
                }) { Text("Discard") }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ExerciseCard(block: ExerciseBlock, actions: LogWorkoutActions) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(block.exercise.name, style = MaterialTheme.typography.titleMedium)
            block.sets.forEachIndexed { index, set ->
                if (index > 0) HorizontalDivider()
                SetRow(index + 1, set, actions)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { actions.addSet(block) }) { Text("+ Set") }
                OutlinedButton(onClick = { actions.sameAsLastTime(block) }) { Text("Same as last time") }
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
            label = { Text(if (set.isWarmup) "W" else "$number") },
        )
        Spacer(Modifier.width(4.dp))
        Stepper(
            value = formatKg(set.weightKg),
            unit = "kg",
            label = "weight",
            onMinus = { actions.changeWeight(set, -WEIGHT_STEP_KG) },
            onPlus = { actions.changeWeight(set, WEIGHT_STEP_KG) },
            modifier = Modifier.weight(1f),
        )
        Stepper(
            value = set.reps.toString(),
            unit = "reps",
            label = "reps",
            onMinus = { actions.changeReps(set, -1) },
            onPlus = { actions.changeReps(set, 1) },
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { actions.deleteSet(set) }) {
            Icon(Icons.Default.Delete, contentDescription = "Delete set")
        }
    }
}

@Composable
private fun Stepper(
    value: String,
    unit: String,
    label: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        FilledTonalIconButton(onClick = onMinus, modifier = Modifier.semantics { contentDescription = "Decrease $label" }) {
            Text("−")
        }
        Column(Modifier.widthIn(min = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Text(unit, style = MaterialTheme.typography.labelSmall)
        }
        FilledTonalIconButton(onClick = onPlus, modifier = Modifier.semantics { contentDescription = "Increase $label" }) {
            Text("+")
        }
    }
}

@Composable
private fun ExercisePickerDialog(exercises: List<Exercise>, onPick: (Exercise) -> Unit, onDismiss: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = exercises.filter { it.name.contains(query.trim(), ignoreCase = true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add exercise") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(Modifier.height(360.dp)) {
                    items(filtered, key = { it.id }) { exercise ->
                        ListItem(
                            headlineContent = { Text(exercise.name) },
                            supportingContent = {
                                Text(exercise.primaryMuscles.joinToString { it.displayName() })
                            },
                            modifier = Modifier.clickable { onPick(exercise) },
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun formatKg(kg: Double): String = if (kg % 1.0 == 0.0) kg.toInt().toString() else kg.toString()

private fun MuscleGroup.displayName(): String = name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

private object NoOpActions : LogWorkoutActions {
    override fun startSession() {}
    override fun finishSession() {}
    override fun discardSession() {}
    override fun addExercise(exercise: Exercise) {}
    override fun addSet(block: ExerciseBlock) {}
    override fun sameAsLastTime(block: ExerciseBlock) {}
    override fun changeReps(set: SetEntry, delta: Int) {}
    override fun changeWeight(set: SetEntry, deltaKg: Double) {}
    override fun toggleWarmup(set: SetEntry) {}
    override fun deleteSet(set: SetEntry) {}
}

private val previewBench = Exercise(1, "Barbell Bench Press", listOf(MuscleGroup.CHEST), listOf(MuscleGroup.TRICEPS), Equipment.BARBELL, ExercisePreference.FAVOURITE)

@Preview(showBackground = true)
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
