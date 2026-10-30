package com.github.sebastianp265.aitrainer.domain.usecase

import com.github.sebastianp265.aitrainer.data.repo.WorkoutRepository
import com.github.sebastianp265.aitrainer.domain.workout.DEFAULT_REPS
import com.github.sebastianp265.aitrainer.domain.workout.DEFAULT_WEIGHT_KG

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
