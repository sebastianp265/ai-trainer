package com.github.sebastianp265.aitrainer.feature.workout.domain.usecase

import com.github.sebastianp265.aitrainer.feature.workout.domain.WorkoutRepository
import com.github.sebastianp265.aitrainer.feature.workout.domain.adjustReps
import com.github.sebastianp265.aitrainer.feature.workout.domain.adjustWeight
import com.github.sebastianp265.aitrainer.feature.workout.domain.model.SetEntry

sealed interface SetChange {
    data class Reps(val delta: Int) : SetChange
    data class Weight(val deltaKg: Double) : SetChange
    data object ToggleWarmup : SetChange
}

class ChangeSetUseCase(private val workoutRepository: WorkoutRepository) {

    suspend operator fun invoke(setId: Long, change: SetChange) {
        workoutRepository.modifySet(setId) { it.apply(change) }
    }

    private fun SetEntry.apply(change: SetChange): SetEntry = when (change) {
        is SetChange.Reps -> copy(reps = adjustReps(reps, change.delta))
        is SetChange.Weight -> copy(weightKg = adjustWeight(weightKg, change.deltaKg))
        SetChange.ToggleWarmup -> copy(isWarmup = !isWarmup)
    }
}
