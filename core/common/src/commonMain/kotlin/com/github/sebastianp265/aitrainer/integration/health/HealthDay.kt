package com.github.sebastianp265.aitrainer.integration.health

import kotlinx.datetime.LocalDate

data class HealthDay(
    val date: LocalDate,
    val kcalEaten: Int?,
    val proteinG: Double?,
    val carbsG: Double?,
    val fatG: Double?,
    val sourcePackages: List<String>,
)
