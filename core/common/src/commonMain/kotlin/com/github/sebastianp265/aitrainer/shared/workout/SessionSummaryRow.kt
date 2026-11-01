package com.github.sebastianp265.aitrainer.shared.workout

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

internal data class SessionSummaryRow(
    val id: Long,
    val date: LocalDate,
    val startedAt: Instant,
    val finishedAt: Instant?,
    val exerciseCount: Int,
    val setCount: Int,
)
