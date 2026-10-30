package com.github.sebastianp265.aitrainer.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sebastianp265.aitrainer.data.repo.ExerciseRepository
import com.github.sebastianp265.aitrainer.data.repo.WorkoutRepository
import com.github.sebastianp265.aitrainer.domain.model.Exercise
import com.github.sebastianp265.aitrainer.domain.model.SessionSummary
import com.github.sebastianp265.aitrainer.domain.model.SetEntry
import com.github.sebastianp265.aitrainer.domain.model.WorkoutSession
import com.github.sebastianp265.aitrainer.domain.usecase.AddExerciseToWorkoutUseCase
import com.github.sebastianp265.aitrainer.domain.usecase.AddSetUseCase
import com.github.sebastianp265.aitrainer.domain.usecase.ChangeSetUseCase
import com.github.sebastianp265.aitrainer.domain.usecase.CopySetsFromLastTimeUseCase
import com.github.sebastianp265.aitrainer.domain.usecase.CopySetsFromLastTimeUseCase.CopyResult
import com.github.sebastianp265.aitrainer.domain.usecase.SetChange
import com.github.sebastianp265.aitrainer.domain.workout.ExerciseBlock
import com.github.sebastianp265.aitrainer.domain.workout.groupSetsByExerciseInLoggingOrder
import com.github.sebastianp265.aitrainer.ui.WhileUiSubscribed
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ActiveSessionUi(
    val session: WorkoutSession,
    val blocks: List<ExerciseBlock>,
)

sealed interface LogMessage {
    data object WorkoutSaved : LogMessage
    data class NoPreviousSets(val exercise: Exercise) : LogMessage
}

data class LogUiState(
    val isLoading: Boolean = true,
    val activeSession: ActiveSessionUi? = null,
    val recentSessions: List<SessionSummary> = emptyList(),
    val exercises: List<Exercise> = emptyList(),
    val message: LogMessage? = null,
)

class LogWorkoutViewModel(
    private val workoutRepository: WorkoutRepository,
    exerciseRepository: ExerciseRepository,
    private val addExerciseToWorkout: AddExerciseToWorkoutUseCase,
    private val addSetToBlock: AddSetUseCase,
    private val copyLastSets: CopySetsFromLastTimeUseCase,
    private val changeSet: ChangeSetUseCase,
) : ViewModel() {

    private val message = MutableStateFlow<LogMessage?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val activeSessionWithSets = workoutRepository.observeActiveSession().flatMapLatest { session ->
        if (session == null) flowOf(null)
        else workoutRepository.observeSets(session.id).map { sets -> session to sets }
    }

    val uiState: StateFlow<LogUiState> = combine(
        activeSessionWithSets,
        workoutRepository.observeRecentSessions(),
        exerciseRepository.observeExercises(),
        message,
    ) { active, recent, exercises, msg ->
        val byId = exercises.associateBy { it.id }
        LogUiState(
            isLoading = false,
            activeSession = active?.let { (session, sets) ->
                ActiveSessionUi(session, groupSetsByExerciseInLoggingOrder(sets, byId))
            },
            recentSessions = recent,
            exercises = exercises,
            message = msg,
        )
    }.stateIn(
        scope = viewModelScope,
        started = WhileUiSubscribed,
        initialValue = LogUiState(),
    )

    fun startSession() = launch { workoutRepository.startSession() }

    fun finishSession() = withActiveSession { session ->
        workoutRepository.finishSession(session.id)
        message.value = LogMessage.WorkoutSaved
    }

    fun discardSession() = withActiveSession { session -> workoutRepository.discardSession(session.id) }

    fun addExercise(exercise: Exercise) = withActiveSession { session -> addExerciseToWorkout(session.id, exercise.id) }

    fun addSet(block: ExerciseBlock) = withActiveSession { session -> addSetToBlock(session.id, block) }

    fun copySetsFromLastTime(block: ExerciseBlock) = withActiveSession { session ->
        if (copyLastSets(session.id, block.exercise.id) == CopyResult.NO_PREVIOUS_SETS) {
            message.value = LogMessage.NoPreviousSets(block.exercise)
        }
    }

    fun changeReps(set: SetEntry, delta: Int) = launch { changeSet(set.id, SetChange.Reps(delta)) }

    fun changeWeight(set: SetEntry, deltaKg: Double) = launch { changeSet(set.id, SetChange.Weight(deltaKg)) }

    fun toggleWarmup(set: SetEntry) = launch { changeSet(set.id, SetChange.ToggleWarmup) }

    fun deleteSet(set: SetEntry) = launch { workoutRepository.deleteSet(set.id) }

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
