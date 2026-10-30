package com.github.sebastianp265.aitrainer.domain.model

data class Exercise(
    val id: Long,
    val key: ExerciseKey,
    val primaryMuscles: List<MuscleGroup>,
    val secondaryMuscles: List<MuscleGroup>,
    val equipment: Equipment,
    val preference: ExercisePreference,
)
