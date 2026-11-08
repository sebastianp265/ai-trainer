package com.github.sebastianp265.aitrainer.integration.exercise

const val DEFAULT_EXERCISE_LANGUAGE = "en"

data class Exercise(
    val key: String,
    val namesByLanguage: Map<String, String>,
    val equipment: Equipment,
    val primaryMuscles: List<MuscleGroup>,
    val secondaryMuscles: List<MuscleGroup>,
)

data class MuscleGroup(val key: String, val namesByLanguage: Map<String, String>)

data class Equipment(val key: String, val namesByLanguage: Map<String, String>)
