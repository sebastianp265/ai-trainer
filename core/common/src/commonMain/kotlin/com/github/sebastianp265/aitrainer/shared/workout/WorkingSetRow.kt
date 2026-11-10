package com.github.sebastianp265.aitrainer.shared.workout

import kotlinx.datetime.LocalDate

internal data class WorkingSetRow(
    val date: LocalDate,
    val exerciseKey: String,
    val weightKg: Double,
    val reps: Int,
)
