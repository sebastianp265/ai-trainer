package com.github.sebastianp265.aitrainer.integration.workout

sealed interface SetChange {
    data class Reps(val delta: Int) : SetChange
    data class RepsTo(val reps: Int) : SetChange
    data class WeightTo(val kg: Double) : SetChange
    data class RpeTo(val rpe: Double?) : SetChange
    data object ToggleWarmup : SetChange
}
