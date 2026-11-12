package com.github.sebastianp265.aitrainer.feature.plan.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
import com.github.sebastianp265.aitrainer.integration.exercise.ExerciseService
import com.github.sebastianp265.aitrainer.integration.plan.Plan
import com.github.sebastianp265.aitrainer.integration.plan.PlanAuthor
import com.github.sebastianp265.aitrainer.integration.plan.PlanDraft
import com.github.sebastianp265.aitrainer.integration.plan.PlanService
import com.github.sebastianp265.aitrainer.integration.plan.PlannedSet
import com.github.sebastianp265.aitrainer.integration.workout.WorkoutService
import com.github.sebastianp265.aitrainer.shared.state.WhileUiSubscribed
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

internal const val NEW_PLAN_ID = -1L

internal data class PlanDetailUiState(
    val isLoading: Boolean = true,
    val plan: Plan? = null,
    val exercises: Map<String, Exercise> = emptyMap(),
    val canStart: Boolean = false,
)

internal class PlanDetailViewModel(
    private val planService: PlanService,
    private val workoutService: WorkoutService,
    exerciseService: ExerciseService,
) : ViewModel() {

    private val planId = MutableStateFlow<Long?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<PlanDetailUiState> = combine(
        planId.filterNotNull().flatMapLatest { planService.observePlan(it) },
        exerciseService.observeExercisesByKey(),
        workoutService.observeActiveSession(),
    ) { plan, exercises, activeSession -> PlanDetailUiState(false, plan, exercises, canStart = activeSession == null) }
        .stateIn(viewModelScope, WhileUiSubscribed, PlanDetailUiState())

    fun show(id: Long) {
        planId.value = id
    }

    fun start(onStarted: () -> Unit) {
        val id = planId.value ?: return
        viewModelScope.launch {
            workoutService.startSession(id)
            onStarted()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val id = planId.value ?: return
        viewModelScope.launch {
            planService.deletePlan(id)
            onDeleted()
        }
    }
}

internal data class EditableSet(
    val id: Long,
    val exerciseKey: String,
    val weight: String,
    val reps: String,
    val rpe: Double?,
    val isWarmup: Boolean,
)

internal data class PlanEditUiState(
    val isLoading: Boolean = true,
    val isNew: Boolean = true,
    val name: String = "",
    val date: LocalDate? = null,
    val sets: List<EditableSet> = emptyList(),
    val exercises: Map<String, Exercise> = emptyMap(),
) {
    val canSave: Boolean get() = name.isNotBlank() && sets.isNotEmpty() && sets.all { (it.reps.toIntOrNull() ?: 0) > 0 }
}

internal class PlanEditViewModel(
    private val planService: PlanService,
    private val workoutService: WorkoutService,
    private val exerciseService: ExerciseService,
) : ViewModel() {

    private companion object {
        const val DEFAULT_REPS = 10
    }

    private val state = MutableStateFlow(PlanEditUiState())
    val uiState: StateFlow<PlanEditUiState> = state

    private var loadedPlanId: Long? = null
    private var nextSetId = 1L

    init {
        viewModelScope.launch {
            exerciseService.observeExercisesByKey().collect { exercises -> state.update { it.copy(exercises = exercises) } }
        }
    }

    fun load(planId: Long) {
        if (loadedPlanId == planId) return
        loadedPlanId = planId
        viewModelScope.launch {
            if (planId == NEW_PLAN_ID) {
                state.update { it.copy(isLoading = false, isNew = true) }
                return@launch
            }
            val plan = planService.observePlan(planId).first() ?: return@launch
            state.update {
                it.copy(isLoading = false, isNew = false, name = plan.name, date = plan.date, sets = plan.sets.map { set -> set.toEditable() })
            }
        }
    }

    fun changeName(name: String) = state.update { it.copy(name = name) }

    fun changeDate(date: LocalDate?) = state.update { it.copy(date = date) }

    fun addExercise(exerciseKey: String) {
        viewModelScope.launch {
            val last = workoutService.lastWorkingSets(exerciseKey, excludingSessionId = -1L).firstOrNull()
            val set = EditableSet(
                id = nextSetId++,
                exerciseKey = exerciseKey,
                weight = last?.weightKg?.takeIf { it > 0 }?.let(::plainNumber).orEmpty(),
                reps = (last?.reps ?: DEFAULT_REPS).toString(),
                rpe = null,
                isWarmup = false,
            )
            state.update { it.copy(sets = it.sets + set) }
        }
    }

    fun removeExercise(exerciseKey: String) = state.update { it.copy(sets = it.sets.filter { set -> set.exerciseKey != exerciseKey }) }

    fun addSet(exerciseKey: String) = state.update { current ->
        val lastIndex = current.sets.indexOfLast { it.exerciseKey == exerciseKey }
        val template = current.sets[lastIndex]
        val added = template.copy(id = nextSetId++, isWarmup = false)
        current.copy(sets = current.sets.toMutableList().apply { add(lastIndex + 1, added) })
    }

    fun removeSet(setId: Long) = state.update { it.copy(sets = it.sets.filter { set -> set.id != setId }) }

    fun changeWeight(setId: Long, weight: String) = changeSet(setId) { it.copy(weight = weight) }

    fun changeReps(setId: Long, reps: String) = changeSet(setId) { it.copy(reps = reps) }

    fun changeRpe(setId: Long, rpe: Double?) = changeSet(setId) { it.copy(rpe = rpe) }

    fun toggleWarmup(setId: Long) = changeSet(setId) { it.copy(isWarmup = !it.isWarmup) }

    fun save(onSaved: (planId: Long) -> Unit) {
        val current = state.value
        if (!current.canSave) return
        val draft = PlanDraft(current.name.trim(), current.date, current.sets.map { it.toPlanned() })
        viewModelScope.launch {
            val id = loadedPlanId?.takeIf { it != NEW_PLAN_ID }
            if (id == null) onSaved(planService.createPlan(draft, PlanAuthor.USER))
            else {
                planService.updatePlan(id, draft)
                onSaved(id)
            }
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val id = loadedPlanId?.takeIf { it != NEW_PLAN_ID } ?: return
        viewModelScope.launch {
            planService.deletePlan(id)
            onDeleted()
        }
    }

    private fun changeSet(setId: Long, change: (EditableSet) -> EditableSet) =
        state.update { current -> current.copy(sets = current.sets.map { if (it.id == setId) change(it) else it }) }

    private fun PlannedSet.toEditable() = EditableSet(
        id = nextSetId++,
        exerciseKey = exerciseKey,
        weight = weightKg.takeIf { it > 0 }?.let(::plainNumber).orEmpty(),
        reps = reps.toString(),
        rpe = rpe,
        isWarmup = isWarmup,
    )

    private fun EditableSet.toPlanned() = PlannedSet(
        exerciseKey = exerciseKey,
        reps = reps.toInt(),
        weightKg = weight.trim().replace(',', '.').toDoubleOrNull() ?: 0.0,
        rpe = rpe,
        isWarmup = isWarmup,
    )

    private fun plainNumber(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
}
