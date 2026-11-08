package com.github.sebastianp265.aitrainer.integration.workout

sealed interface SetChange {
    data class Reps(val delta: Int) : SetChange
    data class Weight(val deltaKg: Double) : SetChange
    data object ToggleWarmup : SetChange
}
