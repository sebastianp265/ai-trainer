package com.github.sebastianp265.aitrainer.feature.workout.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
import com.github.sebastianp265.aitrainer.integration.exercise.ExerciseService
import com.github.sebastianp265.aitrainer.integration.plan.Plan
import com.github.sebastianp265.aitrainer.integration.plan.PlanService
import com.github.sebastianp265.aitrainer.integration.workout.SessionSummary
import com.github.sebastianp265.aitrainer.integration.workout.WorkoutService
import com.github.sebastianp265.aitrainer.shared.state.WhileUiSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal data class LogUiState(
    val isLoading: Boolean = true,
    val plans: List<Plan> = emptyList(),
    val recentSessions: List<SessionSummary> = emptyList(),
    val exercises: Map<String, Exercise> = emptyMap(),
)

internal class LogWorkoutViewModel(
    private val workoutService: WorkoutService,
    planService: PlanService,
    exerciseService: ExerciseService,
) : ViewModel() {

    val uiState: StateFlow<LogUiState> = combine(
        planService.observeUpcoming(),
        workoutService.observeRecentSessions(),
        exerciseService.observeExercisesByKey(),
    ) { plans, recent, exercises ->
        LogUiState(isLoading = false, plans = plans, recentSessions = recent, exercises = exercises)
    }.stateIn(viewModelScope, WhileUiSubscribed, LogUiState())

    fun startEmptyWorkout() {
        viewModelScope.launch { workoutService.startSession() }
    }

    fun startWorkoutFromPlan(planId: Long) {
        viewModelScope.launch { workoutService.startSession(planId) }
    }
}
