package com.github.sebastianp265.aitrainer.feature.workout.impl

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import com.github.sebastianp265.aitrainer.shared.state.LocalOpenCoach
import com.github.sebastianp265.aitrainer.shared.state.LocalCoachEnabled
import com.github.sebastianp265.aitrainer.integration.coach.TopicKind
import com.github.sebastianp265.aitrainer.integration.coach.CoachTopic
import android.content.res.Resources
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.shared.localization.formatWeekdayDate
import com.github.sebastianp265.aitrainer.integration.exercise.Equipment
import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
import com.github.sebastianp265.aitrainer.integration.exercise.MuscleGroup
import com.github.sebastianp265.aitrainer.integration.plan.Plan
import com.github.sebastianp265.aitrainer.integration.plan.PlanAuthor
import com.github.sebastianp265.aitrainer.integration.workout.ExerciseBlock
import com.github.sebastianp265.aitrainer.integration.workout.LastTimeGroup
import com.github.sebastianp265.aitrainer.integration.workout.SetChange
import com.github.sebastianp265.aitrainer.integration.workout.SetEntry
import com.github.sebastianp265.aitrainer.integration.workout.WorkoutSession
import kotlinx.datetime.LocalDate
import kotlin.time.Instant
import com.github.sebastianp265.aitrainer.shared.design.DashedButton
import com.github.sebastianp265.aitrainer.shared.design.DesignIcons

@Composable
internal fun ActiveWorkoutContent(
    state: ActiveWorkoutUiState,
    actions: ActiveWorkoutActions,
    onMessageShown: () -> Unit,
    onOpenExerciseHistory: (String) -> Unit,
    onOpenExercisePicker: () -> Unit,
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
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    val isEditing = state.session?.editedSessionId != null

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = { confirmDiscard = true },
                        modifier = Modifier.weight(1f).height(48.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    ) { Text(stringResource(if (isEditing) R.string.cancel else R.string.discard)) }
                    Button(
                        onClick = if (isEditing) actions::saveEdit else actions::finishSession,
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) { Text(stringResource(if (isEditing) R.string.save else R.string.finish)) }
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            val session = state.session
            if (state.isLoading || session == null) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    Modifier.statusBarsPadding(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item { WorkoutHeader(session, state.plan) }
                    if (state.blocks.isEmpty()) {
                        item { Text(stringResource(R.string.add_first_exercise_hint), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    items(state.blocks, key = { it.block.exercise.key }) { ui ->
                        ExerciseCard(ui, actions, onOpenHistory = { onOpenExerciseHistory(ui.block.exercise.key) })
                    }
                    item { DashedButton(stringResource(R.string.add_exercise), DesignIcons.Add, onClick = onOpenExercisePicker) }
                }
            }
        }
    }

    if (confirmDiscard && isEditing) {
        CancelEditDialog(
            onConfirm = {
                confirmDiscard = false
                actions.discardSession()
            },
            onDismiss = { confirmDiscard = false },
        )
    } else if (confirmDiscard) {
        DiscardWorkoutDialog(
            planName = state.plan?.name,
            onConfirm = {
                confirmDiscard = false
                actions.discardSession()
            },
            onDismiss = { confirmDiscard = false },
        )
    }
}

@Composable
private fun WorkoutHeader(session: WorkoutSession, plan: Plan?) {
    val elapsedSeconds = rememberElapsedSeconds(session.startedAt)
    val title = plan?.name ?: stringResource(R.string.workout_empty_title)
    Row(verticalAlignment = Alignment.CenterVertically) {
    Column(Modifier.weight(1f).heightIn(min = 64.dp).padding(vertical = 8.dp), verticalArrangement = Arrangement.Center) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        val origin = when (plan?.author) {
            null -> stringResource(R.string.workout_empty_subtitle)
            PlanAuthor.USER -> stringResource(R.string.workout_from_own_plan)
            PlanAuthor.COACH -> stringResource(R.string.workout_from_coach_plan)
        }
        Text(
            stringResource(R.string.workout_subtitle, origin, if (session.editedSessionId != null) formatWeekdayDate(session.date) else formatElapsed(elapsedSeconds)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(ActiveWorkoutTags.SUBTITLE),
        )
    }
    if (LocalCoachEnabled.current) {
        val openCoach = LocalOpenCoach.current
        FilledTonalIconButton(
            onClick = { openCoach(CoachTopic(TopicKind.ACTIVE_WORKOUT, session.id.toString(), title)) },
            modifier = Modifier.size(48.dp).testTag(ActiveWorkoutTags.ASK_COACH),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            ),
        ) { Icon(DesignIcons.Coach, contentDescription = stringResource(R.string.workout_ask_coach)) }
    }
    }
}

private fun ActiveWorkoutMessage.text(resources: Resources): String = when (this) {
    is ActiveWorkoutMessage.NotUnderstood -> resources.getString(R.string.voice_not_understood, text)
    ActiveWorkoutMessage.VoiceUnavailable -> resources.getString(R.string.voice_unavailable)
}

@Composable
private fun CancelEditDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cancel_edit_title)) },
        text = { Text(stringResource(R.string.cancel_edit_text)) },
        confirmButton = {
            TextButton(onClick = onConfirm, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Text(stringResource(R.string.cancel_edit_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.keep_editing)) } },
    )
}

@Composable
internal fun DiscardWorkoutDialog(planName: String?, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.discard_workout_title)) },
        text = {
            Text(
                if (planName == null) stringResource(R.string.discard_workout_text)
                else stringResource(R.string.discard_workout_text_plan, planName),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Text(stringResource(R.string.discard))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.back_to_workout)) } },
    )
}

internal interface ActiveWorkoutActions {
    fun finishSession()
    fun discardSession()
    fun saveEdit()
    fun addExercise(exerciseKey: String)
    fun removeExercise(exercise: Exercise)
    fun addSet(block: ExerciseBlock)
    fun addSpokenSet(block: ExerciseBlock, alternatives: List<String>)
    fun voiceUnavailable()
    fun changeSet(set: SetEntry, change: SetChange)
    fun deleteSet(set: SetEntry)
}

internal object ActiveWorkoutTags {
    const val ASK_COACH = "workout_ask_coach"
    const val SAY_SET = "workout_say_set"
    const val WEIGHT = "set_weight"
    const val REPS = "set_reps"
    const val RPE = "set_rpe"
    const val WARMUP_TOGGLE = "set_warmup_toggle"
    const val NUMBER_INPUT = "number_input"
    const val LAST_TIME = "last_time"
    const val SUBTITLE = "workout_subtitle"

    fun exerciseCard(key: String) = "exercise_card:$key"
}

@PreviewLightDark
@Composable
private fun ActiveWorkoutPreview() {
    val date = LocalDate(2026, 1, 5)
    val start = Instant.fromEpochMilliseconds(1_767_600_000_000)
    val bench = Exercise(
        key = "BARBELL_BENCH_PRESS",
        namesByLanguage = mapOf("en" to "Barbell bench press"),
        equipment = Equipment("BARBELL", mapOf("en" to "Barbell")),
        primaryMuscles = listOf(MuscleGroup("CHEST", mapOf("en" to "Chest"))),
        secondaryMuscles = listOf(MuscleGroup("ARMS", mapOf("en" to "Arms"))),
    )
    MaterialTheme {
        Surface {
            ActiveWorkoutContent(
                state = ActiveWorkoutUiState(
                    isLoading = false,
                    session = WorkoutSession(1, date, start, null, null, null, null),
                    blocks = listOf(
                        ExerciseBlockUi(
                            ExerciseBlock(
                                exercise = bench,
                                sets = listOf(
                                    SetEntry(1, 1, bench.key, 1, reps = 8, weightKg = 60.0, rpe = null, isWarmup = true),
                                    SetEntry(2, 1, bench.key, 2, reps = 8, weightKg = 80.0, rpe = 8.0, isWarmup = false),
                                ),
                            ),
                            lastTime = listOf(LastTimeGroup(3, 8, 80.0, 7.5, 8.0)),
                        ),
                    ),
                ),
                actions = object : ActiveWorkoutActions {
                    override fun finishSession() = Unit
                    override fun discardSession() = Unit
                    override fun saveEdit() = Unit
                    override fun addExercise(exerciseKey: String) = Unit
                    override fun removeExercise(exercise: Exercise) = Unit
                    override fun addSet(block: ExerciseBlock) = Unit
                    override fun addSpokenSet(block: ExerciseBlock, alternatives: List<String>) = Unit
                    override fun voiceUnavailable() = Unit
                    override fun changeSet(set: SetEntry, change: SetChange) = Unit
                    override fun deleteSet(set: SetEntry) = Unit
                },
                onMessageShown = {},
                onOpenExerciseHistory = {},
                onOpenExercisePicker = {},
            )
        }
    }
}
