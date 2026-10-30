package com.github.sebastianp265.aitrainer.domain.usecase

import com.github.sebastianp265.aitrainer.data.repo.WorkoutRepository

class CopySetsFromLastTimeUseCase(private val workoutRepository: WorkoutRepository) {

    suspend operator fun invoke(sessionId: Long, exerciseId: Long): CopyResult {
        val previousSets = workoutRepository.lastSetsFor(exerciseId, excludingSessionId = sessionId)
        if (previousSets.isEmpty()) return CopyResult.NO_PREVIOUS_SETS
        previousSets.forEach { workoutRepository.addSet(sessionId, it.exerciseId, it.reps, it.weightKg, it.isWarmup) }
        return CopyResult.COPIED
    }

    enum class CopyResult { COPIED, NO_PREVIOUS_SETS }
}
