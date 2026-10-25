package com.github.sebastianp265.aitrainer.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sebastianp265.aitrainer.data.repo.ExerciseRepository
import com.github.sebastianp265.aitrainer.data.repo.WorkoutRepository
import com.github.sebastianp265.aitrainer.domain.model.Exercise
import com.github.sebastianp265.aitrainer.domain.model.SessionSummary
import com.github.sebastianp265.aitrainer.domain.model.SetEntry
import com.github.sebastianp265.aitrainer.domain.model.WorkoutSession
import com.github.sebastianp265.aitrainer.domain.workout.ExerciseBlock
import com.github.sebastianp265.aitrainer.domain.workout.adjustReps
import com.github.sebastianp265.aitrainer.domain.workout.adjustWeight
import com.github.sebastianp265.aitrainer.domain.workout.groupSetsByExerciseInLoggingOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
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
        started = SharingStarted.WhileSubscribed(KEEP_QUERIES_ALIVE_AFTER_UI_STOPS_MS),
        initialValue = LogUiState(),
    )

    fun startSession() = launch { workoutRepository.startSession() }

    fun finishSession() = withActiveSession { session ->
        workoutRepository.finishSession(session.id)
        message.value = LogMessage.WorkoutSaved
    }

    fun discardSession() = withActiveSession { session -> workoutRepository.discardSession(session.id) }

    fun addExercise(exercise: Exercise) = withActiveSession { session ->
        val previous = workoutRepository.lastSetsFor(exercise.id, session.id).firstOrNull { !it.isWarmup }
        workoutRepository.addSet(
            sessionId = session.id,
            exerciseId = exercise.id,
            reps = previous?.reps ?: DEFAULT_REPS,
            weightKg = previous?.weightKg ?: 0.0,
        )
    }

    fun addSet(block: ExerciseBlock) = withActiveSession { session ->
        val last = block.sets.lastOrNull()
        workoutRepository.addSet(session.id, block.exercise.id, last?.reps ?: DEFAULT_REPS, last?.weightKg ?: 0.0)
    }

    fun copySetsFromLastTime(block: ExerciseBlock) = withActiveSession { session ->
        val previous = workoutRepository.lastSetsFor(block.exercise.id, session.id)
        if (previous.isEmpty()) {
            message.value = LogMessage.NoPreviousSets(block.exercise)
            return@withActiveSession
        }
        previous.forEach { workoutRepository.addSet(session.id, it.exerciseId, it.reps, it.weightKg, it.isWarmup) }
    }

    fun changeReps(set: SetEntry, delta: Int) = launch {
        workoutRepository.modifySet(set.id) { it.copy(reps = adjustReps(it.reps, delta)) }
    }

    fun changeWeight(set: SetEntry, deltaKg: Double) = launch {
        workoutRepository.modifySet(set.id) { it.copy(weightKg = adjustWeight(it.weightKg, deltaKg)) }
    }

    fun toggleWarmup(set: SetEntry) = launch {
        workoutRepository.modifySet(set.id) { it.copy(isWarmup = !it.isWarmup) }
    }

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

    private companion object {
        const val DEFAULT_REPS = 10
        const val KEEP_QUERIES_ALIVE_AFTER_UI_STOPS_MS = 5_000L
    }
}
