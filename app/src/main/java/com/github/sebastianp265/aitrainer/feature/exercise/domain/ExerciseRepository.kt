package com.github.sebastianp265.aitrainer.feature.exercise.domain

import com.github.sebastianp265.aitrainer.feature.exercise.domain.model.Exercise
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    fun observeExercises(): Flow<List<Exercise>>
}
