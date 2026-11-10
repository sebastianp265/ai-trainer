package com.github.sebastianp265.aitrainer.feature.volume.impl

import com.github.sebastianp265.aitrainer.integration.volume.WeeklySets
import com.github.sebastianp265.aitrainer.shared.volume.MuscleSetRow
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

internal const val PRIMARY_SET_WEIGHT = 1.0
internal const val SECONDARY_SET_WEIGHT = 0.5

internal fun mondayOf(date: LocalDate): LocalDate = date.minus(DatePeriod(days = date.dayOfWeek.ordinal))

internal fun weeklySets(rows: List<MuscleSetRow>): List<WeeklySets> =
    rows.groupBy { mondayOf(it.date) }
        .toSortedMap()
        .map { (weekStart, weekRows) ->
            WeeklySets(
                weekStart,
                weekRows.groupBy({ it.groupKey }, { if (it.isPrimary) PRIMARY_SET_WEIGHT else SECONDARY_SET_WEIGHT })
                    .mapValues { it.value.sum() },
            )
        }
