package com.github.sebastianp265.aitrainer.shared.volume

import kotlinx.datetime.LocalDate

internal data class MuscleSetRow(
    val date: LocalDate,
    val groupKey: String,
    val isPrimary: Boolean,
)
