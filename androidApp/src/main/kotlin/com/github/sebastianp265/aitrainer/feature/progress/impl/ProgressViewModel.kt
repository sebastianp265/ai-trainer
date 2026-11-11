package com.github.sebastianp265.aitrainer.feature.progress.impl

import com.github.sebastianp265.aitrainer.integration.nutrition.NutritionTarget
import com.github.sebastianp265.aitrainer.integration.nutrition.NutritionService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
import com.github.sebastianp265.aitrainer.integration.exercise.ExerciseService
import com.github.sebastianp265.aitrainer.integration.health.HealthService
import com.github.sebastianp265.aitrainer.integration.health.HealthDay
import com.github.sebastianp265.aitrainer.integration.settings.SettingsService
import com.github.sebastianp265.aitrainer.integration.volume.VolumeService
import com.github.sebastianp265.aitrainer.integration.workout.ExerciseStrength
import com.github.sebastianp265.aitrainer.integration.workout.WorkoutService
import com.github.sebastianp265.aitrainer.shared.volume.VolumeRow
import com.github.sebastianp265.aitrainer.shared.volume.averageSetsPerWeek
import com.github.sebastianp265.aitrainer.shared.volume.volumeRows
import com.github.sebastianp265.aitrainer.shared.state.WhileUiSubscribed
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

internal data class ProgressUiState(
    val range: ProgressRange = ProgressRange.FOUR_WEEKS,
    val cards: ProgressCards? = null,
    val strength: List<StrengthRow> = emptyList(),
    val volumeRows: List<VolumeRow> = emptyList(),
    val showTargets: Boolean = true,
    val nutritionTarget: NutritionTarget? = null,
)

private class StoredProgress(
    val days: List<HealthDay>,
    val sessionCount: Int,
    val strength: List<ExerciseStrength>,
    val exercises: Map<String, Exercise>,
)

internal data class StrengthRow(val exercise: Exercise, val estimatedOneRepMaxKg: Double, val isRecord: Boolean)

internal class ProgressViewModel(
    private val healthService: HealthService,
    private val workoutService: WorkoutService,
    private val volumeService: VolumeService,
    private val nutritionService: NutritionService,
    settingsService: SettingsService,
    exerciseService: ExerciseService,
    private val clock: Clock,
) : ViewModel() {

    private val range = MutableStateFlow(ProgressRange.FOUR_WEEKS)

    private val exercisesByKey = exerciseService.observeExercisesByKey()

    private val targetsAndGroups = combine(
        volumeService.observeTargets(),
        exerciseService.observeMuscleGroups(),
        settingsService.observeShowTargets(),
    ) { targets, groups, showTargets -> Triple(targets, groups, showTargets) }


    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ProgressUiState> = range.flatMapLatest { selected ->
        val today = clock.todayIn(TimeZone.currentSystemDefault())
        val firstDay = today.minus(DatePeriod(days = selected.days - 1))
        val stored = combine(
            healthService.observeDays(firstDay, today),
            workoutService.observeFinishedSessions(firstDay, today),
            workoutService.observeStrength(firstDay, today),
            exercisesByKey,
        ) { days, sessions, strength, exercises -> StoredProgress(days, sessions.size, strength, exercises) }
        val volume = combine(volumeService.observeWeeklySets(firstDay, today), targetsAndGroups) { weeks, (targets, groups, showTargets) ->
            volumeRows(groups, targets, averageSetsPerWeek(weeks, selected.days)) to showTargets
        }
        combine(stored, volume, nutritionService.observeTarget()) { data, (rows, showTargets), nutritionTarget ->
            ProgressUiState(
                range = selected,
                cards = ProgressCalculator.calculate(data.days, data.sessionCount, selected, today),
                strength = data.strength.take(STRENGTH_ROWS).map { StrengthRow(data.exercises.getValue(it.exerciseKey), it.estimatedOneRepMaxKg, it.isRecord) },
                volumeRows = rows.takeIf { list -> list.any { it.done > 0 } }.orEmpty(),
                showTargets = showTargets,
                nutritionTarget = nutritionTarget,
            )
        }
    }.stateIn(viewModelScope, WhileUiSubscribed, ProgressUiState())

    fun selectRange(selected: ProgressRange) {
        range.value = selected
    }

    fun syncAfterConnecting() {
        viewModelScope.launch { healthService.sync() }
    }

    private companion object {
        const val STRENGTH_ROWS = 4
    }
}
