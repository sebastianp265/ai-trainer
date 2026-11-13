package com.github.sebastianp265.aitrainer.feature.volume.impl

import com.github.sebastianp265.aitrainer.feature.plan.impl.DefaultPlanService
import com.github.sebastianp265.aitrainer.feature.workout.impl.DefaultWorkoutService
import com.github.sebastianp265.aitrainer.integration.volume.TargetSource
import com.github.sebastianp265.aitrainer.integration.workout.SetChange
import com.github.sebastianp265.aitrainer.shared.database.impl.inMemoryDatabase
import com.github.sebastianp265.aitrainer.shared.database.impl.seedExerciseMuscles
import com.github.sebastianp265.aitrainer.shared.database.impl.seedExercises
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class DefaultVolumeServiceUTest {

    private val clock = object : Clock {
        override fun now() = Instant.parse("2026-11-10T10:00:00Z")
    }
    private val database = inMemoryDatabase().also {
        it.seedExercises("bench")
        it.seedExerciseMuscles("bench", "CHEST", "ARMS", "SHOULDERS")
    }
    private val workouts = DefaultWorkoutService(database.workoutDao(), database.planDao(), DefaultPlanService(database.planDao(), clock), clock)
    private val volume = DefaultVolumeService(database.volumeDao(), clock)
    private val week = LocalDate(2026, 11, 9) to LocalDate(2026, 11, 15)

    @AfterTest
    fun close() = database.close()

    private suspend fun observed() = volume.observeWeeklySets(week.first, week.second).first()

    @Test
    fun `finished working sets are counted for their muscle groups`() = runTest {
        workouts.startSession()
        val session = workouts.observeActiveSession().first()!!.id
        workouts.addExercise(session, "bench")
        workouts.finishSession(session)

        assertEquals(mapOf("CHEST" to 1.0, "ARMS" to 0.5, "SHOULDERS" to 0.5), observed().single().setsByGroup)
    }

    @Test
    fun `warm-up sets are not counted`() = runTest {
        workouts.startSession()
        val session = workouts.observeActiveSession().first()!!.id
        workouts.addExercise(session, "bench")
        workouts.changeSet(workouts.observeSets(session).first().single().id, SetChange.ToggleWarmup)
        workouts.finishSession(session)

        assertEquals(emptyList(), observed())
    }

    @Test
    fun `a workout that is still running is not counted`() = runTest {
        workouts.startSession()
        val session = workouts.observeActiveSession().first()!!.id
        workouts.addExercise(session, "bench")

        assertEquals(emptyList(), observed())
    }

    @Test
    fun `every muscle group starts with the default target`() = runTest {
        val targets = volume.observeTargets().first()

        assertEquals(listOf("CHEST", "BACK", "LEGS", "SHOULDERS", "ARMS", "ABS"), targets.map { it.groupKey })
        assertEquals(setOf(10 to 20), targets.map { it.minSets to it.maxSets }.toSet())
        assertEquals(setOf(TargetSource.DEFAULT), targets.map { it.source }.toSet())
    }

    @Test
    fun `a target changed by hand is remembered with its source and date`() = runTest {
        volume.setTarget("ABS", 6, 10)

        val abs = volume.observeTargets().first().single { it.groupKey == "ABS" }
        assertEquals(6 to 10, abs.minSets to abs.maxSets)
        assertEquals(TargetSource.USER, abs.source)
        assertTrue(abs.date in listOf(LocalDate(2026, 11, 10), LocalDate(2026, 11, 11)))
    }

    @Test
    fun `restoring defaults brings back 10 to 20 for every group`() = runTest {
        volume.setTarget("ABS", 6, 10)
        volume.setTarget("CHEST", 12, 18)

        volume.restoreDefaultTargets()

        assertEquals(setOf(10 to 20), volume.observeTargets().first().map { it.minSets to it.maxSets }.toSet())
    }

    @Test
    fun `a target needs a range from zero upwards`() = runTest {
        assertFailsWith<IllegalArgumentException> { volume.setTarget("ABS", 12, 8) }
    }
}
