package com.github.sebastianp265.aitrainer.feature.coach.impl

import com.github.sebastianp265.aitrainer.integration.plan.PlannedSet
import kotlin.test.Test
import kotlin.test.assertEquals

class PlanChangesUTest {

    private fun set(exercise: String, weight: Double = 50.0, warmup: Boolean = false) = PlannedSet(exercise, 8, weight, null, warmup)

    @Test
    fun `a removed exercise is reported with the sets it had`() {
        val changes = planChanges(listOf(set("raise"), set("raise"), set("bench")), listOf(set("bench")))

        assertEquals(listOf(PlanChange.Removed("raise", 2)), changes)
    }

    @Test
    fun `fewer sets and a heavier weight are separate changes`() {
        val changes = planChanges(listOf(set("incline"), set("incline"), set("incline"), set("incline")), listOf(set("incline", 52.5), set("incline", 52.5), set("incline", 52.5)))

        assertEquals(listOf(PlanChange.SetsChanged("incline", 4, 3), PlanChange.WeightChanged("incline", 2.5)), changes)
    }

    @Test
    fun `a new exercise is reported as added`() {
        assertEquals(listOf(PlanChange.Added("curl")), planChanges(listOf(set("bench")), listOf(set("bench"), set("curl"))))
    }

    @Test
    fun `warm-up sets and an identical plan give no changes`() {
        val current = listOf(set("bench", 40.0, warmup = true), set("bench", 80.0))
        val proposed = listOf(set("bench", 60.0, warmup = true), set("bench", 80.0))

        assertEquals(emptyList(), planChanges(current, proposed))
    }
}
