package com.github.sebastianp265.aitrainer.domain.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

data class WorkoutSession(
    val id: Long,
    val date: LocalDate,
    val startedAt: Instant,
    val finishedAt: Instant?,
    val planSessionRef: String?,
    val notes: String?,
) {
    val isActive: Boolean get() = finishedAt == null
}
