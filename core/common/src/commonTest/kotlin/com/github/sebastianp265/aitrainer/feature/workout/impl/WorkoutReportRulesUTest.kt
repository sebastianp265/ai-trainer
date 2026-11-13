package com.github.sebastianp265.aitrainer.feature.workout.impl

import com.github.sebastianp265.aitrainer.integration.plan.PlannedSet
import com.github.sebastianp265.aitrainer.integration.workout.RecordKind
import com.github.sebastianp265.aitrainer.integration.workout.SetEntry
import com.github.sebastianp265.aitrainer.shared.workout.WorkingSetRow
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WorkoutReportRulesUTest {

    private fun done(reps: Int, weight: Double, rpe: Double? = null, warmup: Boolean = false, exercise: String = "bench") =
        SetEntry(0, 1, exercise, 0, reps, weight, rpe, warmup)

    private fun planned(reps: Int, weight: Double, warmup: Boolean = false, exercise: String = "bench") =
        PlannedSet(exercise, reps, weight, null, warmup)

    private fun before(reps: Int, weight: Double, exercise: String = "bench") =
        WorkingSetRow(LocalDate(2026, 11, 1), exercise, weight, reps)

    @Test
    fun `a workout that does what the plan says has no difference`() {
        val difference = planDifference(listOf(planned(8, 80.0), planned(8, 80.0)), listOf(done(8, 80.0), done(8, 80.0)))

        assertEquals(0, difference.setCountDelta)
        assertEquals(0.0, difference.weightDeltaKg)
        assertEquals(0, difference.repsDelta)
    }

    @Test
    fun `missing sets, heavier weight and extra reps are counted against the plan`() {
        val missing = planDifference(listOf(planned(12, 25.0), planned(12, 25.0), planned(12, 25.0)), listOf(done(12, 25.0), done(12, 25.0)))
        val heavier = planDifference(listOf(planned(8, 70.0), planned(8, 70.0)), listOf(done(8, 72.5), done(8, 72.5)))
        val moreReps = planDifference(listOf(planned(6, 50.0), planned(6, 50.0)), listOf(done(7, 50.0), done(6, 50.0)))

        assertEquals(-1, missing.setCountDelta)
        assertEquals(2.5, heavier.weightDeltaKg, 0.0001)
        assertEquals(1, moreReps.repsDelta)
    }

    @Test
    fun `warm-up sets are left out of the comparison`() {
        val difference = planDifference(listOf(planned(10, 40.0, warmup = true), planned(8, 80.0)), listOf(done(8, 80.0)))

        assertEquals(0, difference.setCountDelta)
    }

    @Test
    fun `an exercise that is not in the plan is marked as added`() {
        assertTrue(planDifference(emptyList(), listOf(done(10, 20.0))).addedToPlan)
    }

    @Test
    fun `average RPE counts working sets with an RPE only`() {
        assertEquals(8.0, averageRpe(listOf(done(8, 80.0, 7.5), done(8, 80.0, 8.5), done(8, 80.0), done(10, 40.0, 5.0, warmup = true))))
        assertNull(averageRpe(listOf(done(8, 80.0))))
    }

    @Test
    fun `a higher estimated one rep max than ever before is a record with its improvement`() {
        val records = workoutRecords(listOf(done(8, 82.5)), listOf(before(8, 80.0)))

        val record = records.single { it.kind == RecordKind.ONE_REP_MAX }
        assertEquals(estimatedOneRepMax(82.5, 8) - estimatedOneRepMax(80.0, 8), record.improvement, 0.0001)
    }

    @Test
    fun `more reps at a weight than ever before is a record`() {
        val records = workoutRecords(listOf(done(7, 50.0)), listOf(before(6, 50.0), before(6, 50.0)))

        val record = records.single { it.kind == RecordKind.REPS_AT_WEIGHT }
        assertEquals(7, record.reps)
        assertEquals(1.0, record.improvement)
    }

    @Test
    fun `nothing is a record without earlier workouts or without beating them`() {
        assertEquals(emptyList(), workoutRecords(listOf(done(8, 100.0)), emptyList()))
        assertEquals(emptyList(), workoutRecords(listOf(done(8, 70.0)), listOf(before(8, 80.0))))
    }

    @Test
    fun `warm-up sets never make a record`() {
        assertEquals(emptyList(), workoutRecords(listOf(done(8, 120.0, warmup = true)), listOf(before(8, 80.0))))
    }
}
