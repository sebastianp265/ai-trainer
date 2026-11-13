package com.github.sebastianp265.aitrainer.integration.exercise

data class NewExercise(
    val name: String,
    val primaryGroupKey: String,
    val secondaryGroupKeys: List<String>,
)
