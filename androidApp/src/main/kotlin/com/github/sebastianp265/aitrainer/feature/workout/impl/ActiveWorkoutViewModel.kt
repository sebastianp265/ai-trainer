package com.github.sebastianp265.aitrainer.feature.workout.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
import com.github.sebastianp265.aitrainer.integration.exercise.ExerciseService
import com.github.sebastianp265.aitrainer.integration.workout.CopyResult
import com.github.sebastianp265.aitrainer.integration.workout.ExerciseBlock
import com.github.sebastianp265.aitrainer.integration.workout.LastTimeGroup
import com.github.sebastianp265.aitrainer.integration.workout.SetChange
import com.github.sebastianp265.aitrainer.integration.workout.SetEntry
import com.github.sebastianp265.aitrainer.integration.workout.WorkoutService
import com.github.sebastianp265.aitrainer.integration.workout.WorkoutSession
import com.github.sebastianp265.aitrainer.shared.state.WhileUiSubscribed
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal data class ExerciseBlockUi(val block: ExerciseBlock, val lastTime: List<LastTimeGroup>)

internal sealed interface ActiveWorkoutMessage {
    data class NoPreviousSets(val exercise: Exercise) : ActiveWorkoutMessage
}

internal data class ActiveWorkoutUiState(
    val isLoading: Boolean = true,
    val session: WorkoutSession? = null,
    val blocks: List<ExerciseBlockUi> = emptyList(),
    val message: ActiveWorkoutMessage? = null,
)

internal class ActiveWorkoutViewModel(
    private val workoutService: WorkoutService,
    exerciseService: ExerciseService,
) : ViewModel() {

    private val exercisesByKey = flow { emit(exerciseService.exercises().associateBy { it.key }) }

    private val message = MutableStateFlow<ActiveWorkoutMessage?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val activeSessionWithSets = workoutService.observeActiveSession().flatMapLatest { session ->
        if (session == null) flowOf(null)
        else workoutService.observeSets(session.id).map { sets -> session to sets }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val lastTimeByExercise = activeSessionWithSets
        .map { active -> active?.let { (session, sets) -> session.id to sets.map { it.exerciseKey }.distinct() } }
        .distinctUntilChanged()
        .flatMapLatest { sessionAndKeys ->
            flow {
                emit(
                    sessionAndKeys?.let { (sessionId, keys) ->
                        keys.associateWith { key ->
                            workoutService.groupLastTimeSets(workoutService.lastWorkingSets(key, sessionId))
                        }
                    }.orEmpty(),
                )
            }
        }

    val uiState: StateFlow<ActiveWorkoutUiState> = combine(
        activeSessionWithSets,
        exercisesByKey,
        lastTimeByExercise,
        message,
    ) { active, exercisesByKey, lastTime, msg ->
        ActiveWorkoutUiState(
            isLoading = false,
            session = active?.first,
            blocks = active?.second?.let { sets ->
                workoutService.groupSetsByExercise(sets, exercisesByKey)
                    .map { ExerciseBlockUi(it, lastTime[it.exercise.key].orEmpty()) }
            }.orEmpty(),
            message = msg,
        )
    }.stateIn(viewModelScope, WhileUiSubscribed, ActiveWorkoutUiState())

    fun finishSession() = withSession { workoutService.finishSession(it.id) }

    fun discardSession() = withSession { workoutService.discardSession(it.id) }

    fun addExercise(exerciseKey: String) = withSession { workoutService.addExercise(it.id, exerciseKey) }

    fun removeExercise(exercise: Exercise) = withSession { workoutService.removeExercise(it.id, exercise.key) }

    fun addSet(block: ExerciseBlock) = withSession { workoutService.addSet(it.id, block) }

    fun copySetsFromLastTime(block: ExerciseBlock) = withSession { session ->
        if (workoutService.copySetsFromLastTime(session.id, block.exercise.key) == CopyResult.NO_PREVIOUS_SETS) {
            message.value = ActiveWorkoutMessage.NoPreviousSets(block.exercise)
        }
    }

    fun changeSet(set: SetEntry, change: SetChange) = launch { workoutService.changeSet(set.id, change) }

    fun deleteSet(set: SetEntry) = launch { workoutService.deleteSet(set.id) }

    fun messageShown() {
        message.value = null
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private fun withSession(block: suspend (WorkoutSession) -> Unit) = launch {
        uiState.value.session?.let { block(it) }
    }
}
