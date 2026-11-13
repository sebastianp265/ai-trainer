package com.github.sebastianp265.aitrainer.integration.exercise

import kotlinx.coroutines.flow.Flow

interface ExerciseService {
    fun observeExercises(): Flow<List<Exercise>>

    fun observeMuscleGroups(): Flow<List<MuscleGroup>>

    fun observeExercisesByKey(): Flow<Map<String, Exercise>>

    suspend fun exercises(): List<Exercise>

    suspend fun createExercise(newExercise: NewExercise): String
}
