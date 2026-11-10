package com.github.sebastianp265.aitrainer.integration.workout

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

data class WorkoutSession(
    val id: Long,
    val date: LocalDate,
    val startedAt: Instant,
    val finishedAt: Instant?,
    val planId: Long?,
    val notes: String?,
)
