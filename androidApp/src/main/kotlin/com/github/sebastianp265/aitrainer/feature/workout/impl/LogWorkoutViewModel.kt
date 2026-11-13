package com.github.sebastianp265.aitrainer.feature.workout.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
import com.github.sebastianp265.aitrainer.integration.exercise.ExerciseService
import com.github.sebastianp265.aitrainer.integration.plan.Plan
import com.github.sebastianp265.aitrainer.integration.plan.PlanService
import com.github.sebastianp265.aitrainer.integration.workout.SessionSummary
import com.github.sebastianp265.aitrainer.integration.workout.WorkoutService
import com.github.sebastianp265.aitrainer.integration.workout.WorkoutSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import com.github.sebastianp265.aitrainer.shared.state.WhileUiSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

internal data class ActiveWorkout(val session: WorkoutSession, val planName: String?)

internal data class LogUiState(
    val isLoading: Boolean = true,
    val activeWorkout: ActiveWorkout? = null,
    val plans: List<Plan> = emptyList(),
    val historyMonths: List<HistoryMonth> = emptyList(),
    val exercises: Map<String, Exercise> = emptyMap(),
)

internal data class HistoryMonth(val year: Int, val month: Month, val isCurrentYear: Boolean, val sessions: List<SessionSummary>)

internal fun historyMonths(sessions: List<SessionSummary>, today: LocalDate): List<HistoryMonth> =
    sessions.sortedByDescending { it.startedAt }
        .groupBy { it.date.year to it.date.month }
        .map { (yearAndMonth, monthSessions) ->
            val (year, month) = yearAndMonth
            HistoryMonth(year, month, isCurrentYear = year == today.year, sessions = monthSessions)
        }

internal class LogWorkoutViewModel(
    private val workoutService: WorkoutService,
    private val planService: PlanService,
    exerciseService: ExerciseService,
    clock: Clock,
) : ViewModel() {

    private companion object {
        val EARLIEST_DAY = LocalDate(2000, 1, 1)
    }

    private val today = clock.todayIn(TimeZone.currentSystemDefault())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val activeWorkout = workoutService.observeActiveSession().flatMapLatest { session ->
        val planId = session?.planId
        when {
            session == null -> flowOf(null)
            planId == null -> flowOf(ActiveWorkout(session, planName = null))
            else -> planService.observePlan(planId).map { plan -> ActiveWorkout(session, plan?.name) }
        }
    }

    val uiState: StateFlow<LogUiState> = combine(
        activeWorkout,
        planService.observeUpcoming(),
        workoutService.observeFinishedSessions(EARLIEST_DAY, today),
        exerciseService.observeExercisesByKey(),
    ) { active, plans, sessions, exercises ->
        LogUiState(isLoading = false, activeWorkout = active, plans = plans, historyMonths = historyMonths(sessions, today), exercises = exercises)
    }.stateIn(viewModelScope, WhileUiSubscribed, LogUiState())

    fun startEmptyWorkout() {
        viewModelScope.launch { workoutService.startSession() }
    }

    fun startWorkoutFromPlan(planId: Long) {
        viewModelScope.launch { workoutService.startSession(planId) }
    }

    fun movePlan(planId: Long, toIndex: Int) {
        viewModelScope.launch { planService.movePlan(planId, toIndex) }
    }
    fun editWorkout(sessionId: Long) {
        viewModelScope.launch { workoutService.startEditing(sessionId) }
    }
}
