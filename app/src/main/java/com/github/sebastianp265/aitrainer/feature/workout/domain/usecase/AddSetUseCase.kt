package com.github.sebastianp265.aitrainer.feature.workout.domain.usecase

import com.github.sebastianp265.aitrainer.feature.workout.domain.DEFAULT_REPS
import com.github.sebastianp265.aitrainer.feature.workout.domain.DEFAULT_WEIGHT_KG
import com.github.sebastianp265.aitrainer.feature.workout.domain.ExerciseBlock
import com.github.sebastianp265.aitrainer.feature.workout.domain.WorkoutRepository

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
