package com.github.sebastianp265.aitrainer.domain.usecase

import com.github.sebastianp265.aitrainer.data.repo.WorkoutRepository
import com.github.sebastianp265.aitrainer.domain.workout.DEFAULT_REPS
import com.github.sebastianp265.aitrainer.domain.workout.DEFAULT_WEIGHT_KG
import com.github.sebastianp265.aitrainer.domain.workout.ExerciseBlock

class AddSetUseCase(private val workoutRepository: WorkoutRepository) {

    suspend operator fun invoke(sessionId: Long, block: ExerciseBlock) {
        val previousSet = block.sets.lastOrNull()
        workoutRepository.addSet(
            sessionId = sessionId,
            exerciseId = block.exercise.id,
            reps = previousSet?.reps ?: DEFAULT_REPS,
            weightKg = previousSet?.weightKg ?: DEFAULT_WEIGHT_KG,
        )
    }
}
