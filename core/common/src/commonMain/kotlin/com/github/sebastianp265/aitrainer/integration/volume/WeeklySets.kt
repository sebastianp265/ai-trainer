package com.github.sebastianp265.aitrainer.integration.volume

import kotlinx.datetime.LocalDate

data class WeeklySets(
    val weekStart: LocalDate,
    val setsByGroup: Map<String, Double>,
)
