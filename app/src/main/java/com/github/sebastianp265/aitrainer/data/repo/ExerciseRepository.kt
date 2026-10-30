package com.github.sebastianp265.aitrainer.data.repo

import com.github.sebastianp265.aitrainer.domain.model.Exercise
import com.github.sebastianp265.aitrainer.domain.model.ExercisePreference
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    fun observeExercises(): Flow<List<Exercise>>

    suspend fun setPreference(exerciseId: Long, preference: ExercisePreference)
}
