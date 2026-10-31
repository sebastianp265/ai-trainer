package com.github.sebastianp265.aitrainer.feature.workout.domain.usecase

import com.github.sebastianp265.aitrainer.feature.workout.domain.DEFAULT_REPS
import com.github.sebastianp265.aitrainer.feature.workout.domain.DEFAULT_WEIGHT_KG
import com.github.sebastianp265.aitrainer.feature.workout.domain.WorkoutRepository

class AddExerciseToWorkoutUseCase(private val workoutRepository: WorkoutRepository) {

    suspend operator fun invoke(sessionId: Long, exerciseId: Long) {
        val lastWorkingSet = workoutRepository.lastSetsFor(exerciseId, excludingSessionId = sessionId).firstOrNull { !it.isWarmup }
        workoutRepository.addSet(
            sessionId = sessionId,
            exerciseId = exerciseId,
            reps = lastWorkingSet?.reps ?: DEFAULT_REPS,
            weightKg = lastWorkingSet?.weightKg ?: DEFAULT_WEIGHT_KG,
        )
    }
}
