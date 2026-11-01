package com.github.sebastianp265.aitrainer.integration.exercise

interface ExerciseService {
    suspend fun exercises(): List<Exercise>
}
