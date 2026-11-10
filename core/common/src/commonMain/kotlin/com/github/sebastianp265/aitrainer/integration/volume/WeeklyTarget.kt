package com.github.sebastianp265.aitrainer.integration.volume

import kotlinx.datetime.LocalDate

const val DEFAULT_MIN_WEEKLY_SETS = 10
const val DEFAULT_MAX_WEEKLY_SETS = 20

enum class TargetSource { DEFAULT, COACH, USER }

data class WeeklyTarget(
    val groupKey: String,
    val minSets: Int,
    val maxSets: Int,
    val source: TargetSource,
    val date: LocalDate?,
    val reason: String?,
)
