package com.github.sebastianp265.aitrainer.feature.workout.impl

import com.github.sebastianp265.aitrainer.integration.workout.SessionSummary
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class HistoryMonthsUTest {

    @Test
    fun `workouts are grouped by month, newest first, with the year only outside the current year`() {
        val today = LocalDate(2026, 11, 12)
        val sessions = listOf(
            session(1, LocalDate(2025, 12, 18)),
            session(2, LocalDate(2026, 11, 10)),
            session(3, LocalDate(2026, 10, 31)),
            session(4, LocalDate(2026, 11, 8)),
        )

        val months = historyMonths(sessions, today)

        assertEquals(
            listOf(
                Triple(Month.NOVEMBER, true, listOf(2L, 4L)),
                Triple(Month.OCTOBER, true, listOf(3L)),
                Triple(Month.DECEMBER, false, listOf(1L)),
            ),
            months.map { Triple(it.month, it.isCurrentYear, it.sessions.map(SessionSummary::id)) },
        )
    }

    private fun session(id: Long, date: LocalDate): SessionSummary {
        val start = Instant.parse("${date}T17:00:00Z")
        return SessionSummary(id, date, start, start + 1.hours, 3, 12, null, null)
    }
}
