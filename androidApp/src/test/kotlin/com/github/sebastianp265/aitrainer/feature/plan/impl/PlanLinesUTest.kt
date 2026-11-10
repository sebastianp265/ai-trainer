package com.github.sebastianp265.aitrainer.feature.plan.impl

import com.github.sebastianp265.aitrainer.integration.plan.PlannedSet
import kotlin.test.Test
import kotlin.test.assertEquals

class PlanLinesUTest {

    private fun set(exercise: String, reps: Int, weight: Double, rpe: Double? = null, warmup: Boolean = false) =
        PlannedSet(exercise, reps, weight, rpe, warmup)

    @Test
    fun `equal working sets become one numbered line`() {
        val lines = planExercises(listOf(set("bench", 8, 82.5, 8.0), set("bench", 8, 82.5, 8.0), set("bench", 8, 82.5, 8.0))).single().lines

        assertEquals(listOf(PlanLine.Working(1, 3, 3, 8, 82.5, 8.0)), lines)
    }

    @Test
    fun `warm-up sets are separate lines and do not take a number`() {
        val lines = planExercises(listOf(set("bench", 10, 40.0, warmup = true), set("bench", 5, 60.0, warmup = true), set("bench", 8, 82.5))).single().lines

        assertEquals(listOf(PlanLine.Warmup(10, 40.0), PlanLine.Warmup(5, 60.0), PlanLine.Working(1, 1, 1, 8, 82.5, null)), lines)
    }

    @Test
    fun `a different weight or RPE starts a new line and numbering continues`() {
        val lines = planExercises(listOf(set("bench", 5, 100.0, 8.0), set("bench", 5, 100.0, 9.0), set("bench", 5, 105.0, 9.0))).single().lines

        assertEquals(
            listOf(PlanLine.Working(1, 1, 1, 5, 100.0, 8.0), PlanLine.Working(2, 2, 1, 5, 100.0, 9.0), PlanLine.Working(3, 3, 1, 5, 105.0, 9.0)),
            lines,
        )
    }

    @Test
    fun `exercises keep the order of their first set`() {
        val exercises = planExercises(listOf(set("squat", 5, 100.0), set("bench", 8, 80.0), set("squat", 5, 100.0)))

        assertEquals(listOf("squat", "bench"), exercises.map { it.exerciseKey })
        assertEquals(2, (exercises.first().lines.single() as PlanLine.Working).setCount)
    }

    @Test
    fun `counts leave warm-up sets out of the sets and count each exercise once`() {
        val sets = listOf(set("bench", 10, 40.0, warmup = true), set("bench", 8, 80.0), set("squat", 5, 100.0))

        assertEquals(2, workingSetCount(sets))
        assertEquals(2, exerciseCount(sets))
    }
}
