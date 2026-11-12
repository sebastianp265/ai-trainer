package com.github.sebastianp265.aitrainer.feature.plan.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
import com.github.sebastianp265.aitrainer.integration.exercise.ExerciseService
import com.github.sebastianp265.aitrainer.integration.plan.Plan
import com.github.sebastianp265.aitrainer.integration.settings.SettingsService
import com.github.sebastianp265.aitrainer.integration.volume.VolumeService
import com.github.sebastianp265.aitrainer.shared.localization.mondayOf
import com.github.sebastianp265.aitrainer.shared.volume.VolumeRow
import com.github.sebastianp265.aitrainer.shared.volume.volumeRows
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock
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

internal data class PlanListUiState(
    val isLoading: Boolean = true,
    val plans: List<Plan> = emptyList(),
    val exercises: Map<String, Exercise> = emptyMap(),
    val week: WeekUi? = null,
    val volumeRows: List<VolumeRow> = emptyList(),
    val showTargets: Boolean = true,
)

internal class PlanListViewModel(
    planService: PlanService,
    workoutService: WorkoutService,
    volumeService: VolumeService,
    settingsService: SettingsService,
    exerciseService: ExerciseService,
    clock: Clock,
) : ViewModel() {

    private val zone = TimeZone.currentSystemDefault()
    private val monday = mondayOf(clock.todayIn(zone))
    private val sunday = monday.plus(DatePeriod(days = 6))
    private val today = clock.todayIn(zone)

    private val weekAndPlans = combine(
        planService.observeUpcoming(),
        workoutService.observeFinishedSessions(monday, sunday),
    ) { plans, sessions ->
        val doneDays = sessions.map { it.date }.toSet()
        val plannedDays = plans.mapNotNull { it.date }.filter { it in monday..sunday }
        plans to WeekUi(
            days = (0..6).map { offset ->
                val date = monday.plus(DatePeriod(days = offset))
                WeekDay(date, date == today, date in doneDays, date in plannedDays)
            },
            doneCount = sessions.size,
            plannedCount = plannedDays.size,
        )
    }

    private val volume = combine(
        volumeService.observeWeeklySets(monday, sunday),
        volumeService.observePlannedSets(monday, sunday),
        volumeService.observeTargets(),
        exerciseService.observeMuscleGroups(),
        settingsService.observeShowTargets(),
    ) { weeks, planned, targets, groups, showTargets ->
        VolumeState(volumeRows(groups, targets, weeks.firstOrNull()?.setsByGroup.orEmpty(), planned), showTargets)
    }

    val uiState: StateFlow<PlanListUiState> = combine(weekAndPlans, volume, exerciseService.observeExercisesByKey()) { (plans, week), volumeState, exercises ->
        PlanListUiState(false, plans, exercises, week, volumeState.rows, volumeState.showTargets)
    }.stateIn(viewModelScope, WhileUiSubscribed, PlanListUiState())
}

private class VolumeState(val rows: List<VolumeRow>, val showTargets: Boolean)

internal data class PlanDetailUiState(val isLoading: Boolean = true, val plan: Plan? = null, val exercises: Map<String, Exercise> = emptyMap())

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
    ) { plan, exercises -> PlanDetailUiState(false, plan, exercises) }
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
