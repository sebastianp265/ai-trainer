package com.github.sebastianp265.aitrainer.feature.workout.impl

import com.github.sebastianp265.aitrainer.shared.workout.WorkingSetRow
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StrengthRulesUTest {

    private val from = LocalDate(2026, 10, 14)
    private val to = LocalDate(2026, 11, 10)

    private fun set(date: LocalDate, weightKg: Double, reps: Int, exercise: String = "bench") =
        WorkingSetRow(date, exercise, weightKg, reps)

    private fun strength(vararg sets: WorkingSetRow) = strengthInRange(sets.toList(), from, to)

    @Test
    fun `estimated one rep max follows the Epley formula`() {
        assertEquals(110.0, estimatedOneRepMax(100.0, 3), 0.0001)
        assertEquals(100.0 * (1 + 10 / 30.0), estimatedOneRepMax(100.0, 10), 0.0001)
    }

    @Test
    fun `the exercise estimate is the best set of its latest day in the range`() {
        val result = strength(
            set(LocalDate(2026, 11, 3), 100.0, 5),
            set(LocalDate(2026, 11, 8), 80.0, 5),
            set(LocalDate(2026, 11, 8), 90.0, 5),
        )

        assertEquals(estimatedOneRepMax(90.0, 5), result.single().estimatedOneRepMaxKg)
    }

    @Test
    fun `sets above twelve reps and without weight are not used`() {
        val result = strength(set(LocalDate(2026, 11, 8), 40.0, 15), set(LocalDate(2026, 11, 8), 0.0, 8))

        assertTrue(result.isEmpty())
    }

    @Test
    fun `an exercise without sets in the range is left out`() {
        val result = strength(set(LocalDate(2026, 6, 1), 100.0, 5))

        assertTrue(result.isEmpty())
    }

    @Test
    fun `the latest estimate is a record when no earlier day was better`() {
        val result = strength(set(LocalDate(2026, 9, 1), 90.0, 5), set(LocalDate(2026, 11, 8), 100.0, 5))

        assertTrue(result.single().isRecord)
    }

    @Test
    fun `the latest estimate is not a record when an earlier day was better`() {
        val result = strength(set(LocalDate(2026, 9, 1), 110.0, 5), set(LocalDate(2026, 11, 8), 100.0, 5))

        assertFalse(result.single().isRecord)
    }

    @Test
    fun `sets after the end of the range are ignored`() {
        val result = strength(set(LocalDate(2026, 11, 8), 100.0, 5), set(LocalDate(2026, 11, 20), 200.0, 5))

        assertEquals(estimatedOneRepMax(100.0, 5), result.single().estimatedOneRepMaxKg)
    }

    @Test
    fun `exercises with more working sets come first`() {
        val result = strength(
            set(LocalDate(2026, 11, 8), 100.0, 5, "squat"),
            set(LocalDate(2026, 11, 8), 60.0, 5, "bench"),
            set(LocalDate(2026, 11, 7), 60.0, 5, "bench"),
        )

        assertEquals(listOf("bench", "squat"), result.map { it.exerciseKey })
    }
}
