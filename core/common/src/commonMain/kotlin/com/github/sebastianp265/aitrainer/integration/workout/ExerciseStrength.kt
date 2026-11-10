package com.github.sebastianp265.aitrainer.integration.workout

data class ExerciseStrength(
    val exerciseKey: String,
    val estimatedOneRepMaxKg: Double,
    val isRecord: Boolean,
    val workingSetCount: Int,
)
