package com.github.sebastianp265.aitrainer.integration.workout

const val RPE_MIN = 6.0
const val RPE_MAX = 10.0
const val RPE_STEP = 0.5

data class LastTimeGroup(
    val setCount: Int,
    val reps: Int,
    val weightKg: Double,
    val rpeFrom: Double?,
    val rpeTo: Double?,
)
