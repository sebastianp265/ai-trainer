package com.github.sebastianp265.aitrainer.integration.workout

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

data class SessionSummary(
    val id: Long,
    val date: LocalDate,
    val startedAt: Instant,
    val finishedAt: Instant?,
    val exerciseCount: Int,
    val setCount: Int,
    val planId: Long?,
    val planName: String?,
)
