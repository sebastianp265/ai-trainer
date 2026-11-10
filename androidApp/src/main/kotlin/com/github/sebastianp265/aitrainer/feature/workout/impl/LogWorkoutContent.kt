package com.github.sebastianp265.aitrainer.feature.workout.impl

import android.content.res.Resources
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.shared.localization.formatDate
import com.github.sebastianp265.aitrainer.integration.exercise.Equipment
import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
import com.github.sebastianp265.aitrainer.feature.exercise.ExercisePickerDialog
import com.github.sebastianp265.aitrainer.feature.exercise.displayName
import com.github.sebastianp265.aitrainer.integration.exercise.MuscleGroup
import com.github.sebastianp265.aitrainer.integration.workout.ExerciseBlock
import com.github.sebastianp265.aitrainer.integration.workout.SessionSummary
import com.github.sebastianp265.aitrainer.integration.workout.SetEntry
import com.github.sebastianp265.aitrainer.integration.workout.WorkoutSession
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

@Composable
internal fun LogWorkoutContent(
    state: LogUiState,
    actions: LogWorkoutActions,
    onMessageShown: () -> Unit,
    onOpenHistory: () -> Unit,
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
                else -> NoActiveSession(state.recentSessions, actions::startSession, onOpenHistory)
            }
        }
    }

    if (showPicker) {
        ExercisePickerDialog(
            onPick = {
                actions.addExercise(it)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

private fun LogMessage.text(resources: Resources): String = when (this) {
    LogMessage.WorkoutSaved -> resources.getString(R.string.workout_saved)
    is LogMessage.NoPreviousSets -> resources.getString(R.string.no_previous_sets, exercise.displayName(resources.configuration.locales[0]))
}

@Composable
internal fun ActiveSession(active: ActiveSessionUi, actions: LogWorkoutActions) {
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
        items(active.blocks, key = { it.exercise.key }) { block -> ExerciseCard(block, actions) }
    }
    if (confirmDiscard) {
        DiscardWorkoutDialog(
            onConfirm = {
                confirmDiscard = false
                actions.discardSession()
            },
            onDismiss = { confirmDiscard = false },
        )
    }
}

@Composable
internal fun NoActiveSession(recent: List<SessionSummary>, onStart: () -> Unit, onOpenHistory: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.start_workout)) }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.recent_workouts), style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onOpenHistory) { Text(stringResource(R.string.history_all)) }
            }
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
private fun sessionSummary(exerciseCount: Int, setCount: Int): String = stringResource(
    R.string.session_summary,
    pluralStringResource(R.plurals.exercise_count, exerciseCount, exerciseCount),
    pluralStringResource(R.plurals.set_count, setCount, setCount),
)

@Composable
internal fun DiscardWorkoutDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.discard_workout_title)) },
        text = { Text(stringResource(R.string.discard_workout_text)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.discard)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

internal interface LogWorkoutActions {
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

internal object LogWorkoutTags {
    const val WEIGHT = "set_weight"
    const val REPS = "set_reps"

    fun exerciseCard(key: String) = "exercise_card:$key"
}

@PreviewLightDark
@Composable
private fun LogWorkoutWithoutActiveSessionPreview() {
    val date = LocalDate(2026, 1, 5)
    val start = Instant.fromEpochMilliseconds(1_767_600_000_000)
    MaterialTheme {
        Surface {
            LogWorkoutContent(
                state = LogUiState(
                    isLoading = false,
                    recentSessions = listOf(
                        SessionSummary(1, date, start, start, exerciseCount = 4, setCount = 12),
                        SessionSummary(2, date, start, start, exerciseCount = 1, setCount = 1),
                    ),
                ),
                actions = object : LogWorkoutActions {
                    override fun startSession() = Unit
                    override fun finishSession() = Unit
                    override fun discardSession() = Unit
                    override fun addExercise(exercise: Exercise) = Unit
                    override fun addSet(block: ExerciseBlock) = Unit
                    override fun copySetsFromLastTime(block: ExerciseBlock) = Unit
                    override fun changeReps(set: SetEntry, delta: Int) = Unit
                    override fun changeWeight(set: SetEntry, deltaKg: Double) = Unit
                    override fun toggleWarmup(set: SetEntry) = Unit
                    override fun deleteSet(set: SetEntry) = Unit
                },
                onMessageShown = {},
                onOpenHistory = {},
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun LogWorkoutWithActiveSessionPreview() {
    val date = LocalDate(2026, 1, 5)
    val start = Instant.fromEpochMilliseconds(1_767_600_000_000)
    MaterialTheme {
        Surface {
            LogWorkoutContent(
                state = LogUiState(
                    isLoading = false,
                    activeSession = ActiveSessionUi(
                        session = WorkoutSession(1, date, start, null, null, null),
                        blocks = listOf(
                            ExerciseBlock(
                                exercise = Exercise(
                                    key = "BARBELL_BENCH_PRESS",
                                    namesByLanguage = mapOf("en" to "Barbell bench press"),
                                    equipment = Equipment("BARBELL", mapOf("en" to "Barbell")),
                                    primaryMuscles = listOf(MuscleGroup("CHEST", mapOf("en" to "Chest"))),
                                    secondaryMuscles = listOf(MuscleGroup("TRICEPS", mapOf("en" to "Triceps"))),
                                ),
                                sets = listOf(
                                    SetEntry(1, 1, "BARBELL_BENCH_PRESS", 1, reps = 8, weightKg = 60.0, rpe = null, isWarmup = true),
                                    SetEntry(2, 1, "BARBELL_BENCH_PRESS", 2, reps = 8, weightKg = 80.0, rpe = 8.0, isWarmup = false),
                                ),
                            ),
                        ),
                    ),
                ),
                actions = object : LogWorkoutActions {
                    override fun startSession() = Unit
                    override fun finishSession() = Unit
                    override fun discardSession() = Unit
                    override fun addExercise(exercise: Exercise) = Unit
                    override fun addSet(block: ExerciseBlock) = Unit
                    override fun copySetsFromLastTime(block: ExerciseBlock) = Unit
                    override fun changeReps(set: SetEntry, delta: Int) = Unit
                    override fun changeWeight(set: SetEntry, deltaKg: Double) = Unit
                    override fun toggleWarmup(set: SetEntry) = Unit
                    override fun deleteSet(set: SetEntry) = Unit
                },
                onMessageShown = {},
                onOpenHistory = {},
            )
        }
    }
}
