package com.github.sebastianp265.aitrainer.feature.workout.impl

import com.github.sebastianp265.aitrainer.integration.workout.LastTimeGroup
import com.github.sebastianp265.aitrainer.integration.workout.SetEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LastTimeRulesUTest {

    private fun set(position: Int, reps: Int, weightKg: Double, rpe: Double? = null, warmup: Boolean = false) =
        SetEntry(position.toLong(), 1, "bench", position, reps, weightKg, rpe, warmup)

    @Test
    fun `sets with the same weight and reps become one group`() {
        val groups = lastTimeGroups(listOf(set(1, 8, 80.0, 7.5), set(2, 8, 80.0, 7.5), set(3, 8, 80.0, 7.5)))

        assertEquals(listOf(LastTimeGroup(3, 8, 80.0, 7.5, 7.5)), groups)
    }

    @Test
    fun `sets with different weight or reps stay separate groups`() {
        val groups = lastTimeGroups(listOf(set(1, 10, 26.0, 8.0), set(2, 10, 26.0, 8.0), set(3, 8, 26.0, 9.0)))

        assertEquals(listOf(LastTimeGroup(2, 10, 26.0, 8.0, 8.0), LastTimeGroup(1, 8, 26.0, 9.0, 9.0)), groups)
    }

    @Test
    fun `different RPE in one group gives a range`() {
        val groups = lastTimeGroups(listOf(set(1, 6, 50.0, 9.0), set(2, 6, 50.0, 8.0)))

        assertEquals(listOf(LastTimeGroup(2, 6, 50.0, 8.0, 9.0)), groups)
    }

    @Test
    fun `sets without RPE give no RPE`() {
        val groups = lastTimeGroups(listOf(set(1, 12, 15.0), set(2, 12, 15.0)))

        assertEquals(listOf(LastTimeGroup(2, 12, 15.0, null, null)), groups)
    }

    @Test
    fun `a set without RPE does not hide the RPE of the others in its group`() {
        val groups = lastTimeGroups(listOf(set(1, 12, 15.0, 7.0), set(2, 12, 15.0)))

        assertEquals(listOf(LastTimeGroup(2, 12, 15.0, 7.0, 7.0)), groups)
    }

    @Test
    fun `warm-up sets are left out`() {
        val groups = lastTimeGroups(listOf(set(1, 10, 40.0, warmup = true), set(2, 8, 80.0)))

        assertEquals(listOf(LastTimeGroup(1, 8, 80.0, null, null)), groups)
    }

    @Test
    fun `equal sets separated by another set are not merged`() {
        val groups = lastTimeGroups(listOf(set(1, 8, 80.0), set(2, 6, 85.0), set(3, 8, 80.0)))

        assertEquals(3, groups.size)
    }

    @Test
    fun `no previous sets give no groups`() {
        assertTrue(lastTimeGroups(emptyList()).isEmpty())
    }
}
