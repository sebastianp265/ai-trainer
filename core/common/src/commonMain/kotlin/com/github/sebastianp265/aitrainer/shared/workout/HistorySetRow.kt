package com.github.sebastianp265.aitrainer.shared.workout

import kotlinx.datetime.LocalDate

internal data class HistorySetRow(
    val sessionId: Long,
    val date: LocalDate,
    val planName: String?,
    val positionInSession: Int,
    val reps: Int,
    val weightKg: Double,
    val rpe: Double?,
)
