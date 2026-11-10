package com.github.sebastianp265.aitrainer.feature.workout.impl

import com.github.sebastianp265.aitrainer.shared.workout.HistorySetRow
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class ExerciseHistoryRulesUTest {

    private val today = LocalDate(2026, 11, 10)

    private fun row(session: Long, date: LocalDate, weight: Double, reps: Int, rpe: Double? = null, position: Int = 0) =
        HistorySetRow(session, date, position, reps, weight, rpe)

    private fun history(vararg rows: HistorySetRow) = exerciseHistory("bench", rows.toList(), today)

    @Test
    fun `sessions are listed newest first with their grouped sets`() {
        val result = history(
            row(1, LocalDate(2026, 10, 6), 75.0, 8, 7.5, 0),
            row(1, LocalDate(2026, 10, 6), 75.0, 8, 7.5, 1),
            row(2, LocalDate(2026, 11, 3), 80.0, 8, 8.0, 0),
        )

        assertEquals(listOf(2L, 1L), result.sessions.map { it.sessionId })
        assertEquals(2, result.sessions.last().groups.single().setCount)
        assertEquals(2, result.sessionCount)
    }

    @Test
    fun `the current estimate is the best set of the latest session`() {
        val result = history(
            row(1, LocalDate(2026, 10, 6), 100.0, 5),
            row(2, LocalDate(2026, 11, 3), 80.0, 8),
            row(2, LocalDate(2026, 11, 3), 70.0, 8, position = 1),
        )

        assertEquals(estimatedOneRepMax(80.0, 8), result.estimatedOneRepMaxKg)
    }

    @Test
    fun `the best set is the one with the highest estimate of all sessions`() {
        val result = history(row(1, LocalDate(2026, 10, 6), 100.0, 5), row(2, LocalDate(2026, 11, 3), 80.0, 8))

        assertEquals(100.0, result.bestSet?.weightKg)
        assertEquals(5, result.bestSet?.reps)
    }

    @Test
    fun `a session is a record when its estimate beats all earlier ones`() {
        val result = history(
            row(1, LocalDate(2026, 10, 6), 75.0, 8),
            row(2, LocalDate(2026, 10, 13), 70.0, 8),
            row(3, LocalDate(2026, 11, 3), 80.0, 8),
        )

        assertEquals(listOf(true, false, false), result.sessions.map { it.isRecord })
    }

    @Test
    fun `the first session is not called a record`() {
        assertFalse(history(row(1, LocalDate(2026, 10, 6), 75.0, 8)).sessions.single().isRecord)
    }

    @Test
    fun `change is measured over the last three months and needs two sessions there`() {
        val result = history(
            row(1, LocalDate(2026, 5, 6), 60.0, 8),
            row(2, LocalDate(2026, 9, 1), 70.0, 8),
            row(3, LocalDate(2026, 11, 3), 80.0, 8),
        )

        assertEquals(estimatedOneRepMax(80.0, 8) - estimatedOneRepMax(70.0, 8), result.oneRepMaxChangeKg!!, 0.0001)
        assertNull(history(row(1, LocalDate(2026, 11, 3), 80.0, 8)).oneRepMaxChangeKg)
    }

    @Test
    fun `average RPE counts only sets with an RPE`() {
        val day = LocalDate(2026, 11, 3)
        val result = history(row(1, day, 80.0, 8, 7.0), row(1, day, 80.0, 8, null, 1), row(1, day, 80.0, 8, 8.0, 2))

        assertEquals(7.5, result.sessions.single().averageRpe)
        assertNull(history(row(1, day, 80.0, 8)).sessions.single().averageRpe)
    }
}
