package com.github.sebastianp265.aitrainer.domain.usecase

import com.github.sebastianp265.aitrainer.data.repo.WorkoutRepository
import com.github.sebastianp265.aitrainer.domain.model.SetEntry
import com.github.sebastianp265.aitrainer.domain.workout.adjustReps
import com.github.sebastianp265.aitrainer.domain.workout.adjustWeight

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
