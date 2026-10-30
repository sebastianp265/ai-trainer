package com.github.sebastianp265.aitrainer.domain.model

import kotlinx.datetime.LocalDate

// TODO: this a duplicate, will address it in other branch/PR, we will restructure the packages
data class HealthDay(
    val date: LocalDate,
    val kcalEaten: Int?,
    val proteinG: Double?,
    val carbsG: Double?,
    val fatG: Double?,
    val sourcePackages: List<String>,
)
