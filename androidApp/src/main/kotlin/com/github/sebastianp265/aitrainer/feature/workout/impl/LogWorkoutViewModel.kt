package com.github.sebastianp265.aitrainer.feature.workout.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
import com.github.sebastianp265.aitrainer.integration.exercise.ExerciseService
import com.github.sebastianp265.aitrainer.shared.state.WhileUiSubscribed
import com.github.sebastianp265.aitrainer.integration.workout.ExerciseBlock
import com.github.sebastianp265.aitrainer.integration.workout.SessionSummary
import com.github.sebastianp265.aitrainer.integration.workout.SetEntry
import com.github.sebastianp265.aitrainer.integration.workout.WorkoutSession
import com.github.sebastianp265.aitrainer.integration.workout.CopyResult
import com.github.sebastianp265.aitrainer.integration.workout.SetChange
import com.github.sebastianp265.aitrainer.integration.workout.WorkoutService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal data class ActiveSessionUi(
    val session: WorkoutSession,
    val blocks: List<ExerciseBlock>,
)

internal sealed interface LogMessage {
    data object WorkoutSaved : LogMessage
    data class NoPreviousSets(val exercise: Exercise) : LogMessage
}

internal data class LogUiState(
    val isLoading: Boolean = true,
    val activeSession: ActiveSessionUi? = null,
    val recentSessions: List<SessionSummary> = emptyList(),
    val message: LogMessage? = null,
)

internal class LogWorkoutViewModel(
    private val workoutService: WorkoutService,
    exerciseService: ExerciseService,
) : ViewModel() {

    private val exercisesByKey = flow { emit(exerciseService.exercises().associateBy { it.key }) }

    private val message = MutableStateFlow<LogMessage?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val activeSessionWithSets = workoutService.observeActiveSession().flatMapLatest { session ->
        if (session == null) flowOf(null)
        else workoutService.observeSets(session.id).map { sets -> session to sets }
    }

    val uiState: StateFlow<LogUiState> = combine(
        activeSessionWithSets,
        workoutService.observeRecentSessions(),
        exercisesByKey,
        message,
    ) { active, recent, exercisesByKey, msg ->
        LogUiState(
            isLoading = false,
            activeSession = active?.let { (session, sets) ->
                ActiveSessionUi(session, workoutService.groupSetsByExercise(sets, exercisesByKey))
            },
            recentSessions = recent,
            message = msg,
        )
    }.stateIn(
        scope = viewModelScope,
        started = WhileUiSubscribed,
        initialValue = LogUiState(),
    )

    fun startSession() = launch { workoutService.startSession() }

    fun finishSession() = withActiveSession { session ->
        workoutService.finishSession(session.id)
        message.value = LogMessage.WorkoutSaved
    }

    fun discardSession() = withActiveSession { session -> workoutService.discardSession(session.id) }

    fun addExercise(exercise: Exercise) = withActiveSession { session -> workoutService.addExercise(session.id, exercise.key) }

    fun addSet(block: ExerciseBlock) = withActiveSession { session -> workoutService.addSet(session.id, block) }

    fun copySetsFromLastTime(block: ExerciseBlock) = withActiveSession { session ->
        if (workoutService.copySetsFromLastTime(session.id, block.exercise.key) == CopyResult.NO_PREVIOUS_SETS) {
            message.value = LogMessage.NoPreviousSets(block.exercise)
        }
    }

    fun changeReps(set: SetEntry, delta: Int) = launch { workoutService.changeSet(set.id, SetChange.Reps(delta)) }

    fun changeWeight(set: SetEntry, deltaKg: Double) = launch { workoutService.changeSet(set.id, SetChange.Weight(deltaKg)) }

    fun toggleWarmup(set: SetEntry) = launch { workoutService.changeSet(set.id, SetChange.ToggleWarmup) }

    fun deleteSet(set: SetEntry) = launch { workoutService.deleteSet(set.id) }

    fun messageShown() {
        message.value = null
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private fun withActiveSession(block: suspend (WorkoutSession) -> Unit) = launch {
        uiState.value.activeSession?.session?.let { block(it) }
    }
}
