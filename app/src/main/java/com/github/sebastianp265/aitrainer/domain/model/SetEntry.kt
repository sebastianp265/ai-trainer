package com.github.sebastianp265.aitrainer.domain.model

data class SetEntry(
    val id: Long,
    val sessionId: Long,
    val exerciseId: Long,
    val positionInSession: Int,
    val reps: Int,
    val weightKg: Double,
    val rpe: Double?,
    val isWarmup: Boolean,
)
