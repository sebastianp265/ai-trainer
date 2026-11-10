package com.github.sebastianp265.aitrainer.integration.plan

import kotlinx.datetime.LocalDate

data class PlannedSet(
    val exerciseKey: String,
    val reps: Int,
    val weightKg: Double,
    val rpe: Double?,
    val isWarmup: Boolean,
)

data class Plan(
    val id: Long,
    val name: String,
    val date: LocalDate?,
    val author: PlanAuthor,
    val sets: List<PlannedSet>,
)

data class PlanDraft(
    val name: String,
    val date: LocalDate?,
    val sets: List<PlannedSet>,
)
