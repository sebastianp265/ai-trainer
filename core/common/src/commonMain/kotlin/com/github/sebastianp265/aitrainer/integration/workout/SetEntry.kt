package com.github.sebastianp265.aitrainer.integration.workout

data class SetEntry(
    val id: Long,
    val sessionId: Long,
    val exerciseKey: String,
    val positionInSession: Int,
    val reps: Int,
    val weightKg: Double,
    val rpe: Double?,
    val isWarmup: Boolean,
)
