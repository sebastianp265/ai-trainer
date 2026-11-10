package com.github.sebastianp265.aitrainer.feature.workout.impl

import com.github.sebastianp265.aitrainer.integration.exercise.Equipment
import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
import com.github.sebastianp265.aitrainer.integration.workout.SetChange
import com.github.sebastianp265.aitrainer.shared.database.impl.inMemoryDatabase
import com.github.sebastianp265.aitrainer.shared.database.impl.seedExercises
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class DefaultWorkoutServiceUTest {

    private var now = Instant.parse("2026-11-10T10:00:00Z")
    private val clock = object : Clock {
        override fun now() = now
    }
    private val database = inMemoryDatabase().also { it.seedExercises("bench", "squat") }
    private val service = DefaultWorkoutService(database.workoutDao(), clock)

    @AfterTest
    fun close() = database.close()

    private suspend fun startSession(): Long {
        service.startSession()
        return service.observeActiveSession().first()!!.id
    }

    private suspend fun setsOf(sessionId: Long) = service.observeSets(sessionId).first()

    private suspend fun addSetToBench(sessionId: Long) =
        service.addSet(sessionId, service.groupSetsByExercise(setsOf(sessionId), exercises()).first { it.exercise.key == "bench" })

    @Test
    fun `typed weight, reps and RPE are stored on the set`() = runBlocking {
        val session = startSession()
        service.addExercise(session, "bench")
        val set = setsOf(session).single()

        service.changeSet(set.id, SetChange.WeightTo(82.5))
        service.changeSet(set.id, SetChange.RepsTo(8))
        service.changeSet(set.id, SetChange.RpeTo(7.5))

        val changed = setsOf(session).single()
        assertEquals(82.5, changed.weightKg)
        assertEquals(8, changed.reps)
        assertEquals(7.5, changed.rpe)
    }

    @Test
    fun `RPE can be cleared again`() = runBlocking {
        val session = startSession()
        service.addExercise(session, "bench")
        val set = setsOf(session).single()
        service.changeSet(set.id, SetChange.RpeTo(9.0))

        service.changeSet(set.id, SetChange.RpeTo(null))

        assertEquals(null, setsOf(session).single().rpe)
    }

    @Test
    fun `an exercise can have several warm-up sets`() = runBlocking {
        val session = startSession()
        service.addExercise(session, "bench")
        addSetToBench(session)
        addSetToBench(session)

        setsOf(session).take(2).forEach { service.changeSet(it.id, SetChange.ToggleWarmup) }

        assertEquals(listOf(true, true, false), setsOf(session).map { it.isWarmup })
    }

    @Test
    fun `removing an exercise removes all its sets and keeps the others`() = runBlocking {
        val session = startSession()
        service.addExercise(session, "bench")
        service.addExercise(session, "squat")
        addSetToBench(session)

        service.removeExercise(session, "bench")

        assertEquals(listOf("squat"), setsOf(session).map { it.exerciseKey })
    }

    @Test
    fun `last working sets come from the latest finished workout and skip warm-ups`() = runBlocking {
        val first = startSession()
        service.addExercise(first, "bench")
        service.changeSet(setsOf(first).single().id, SetChange.WeightTo(60.0))
        service.finishSession(first)
        now += 1.days
        val second = startSession()
        service.addExercise(second, "bench")
        service.changeSet(setsOf(second).single().id, SetChange.ToggleWarmup)
        addSetToBench(second)
        service.changeSet(setsOf(second).last().id, SetChange.WeightTo(80.0))
        service.finishSession(second)
        now += 1.days
        val third = startSession()

        val last = service.lastWorkingSets("bench", excludingSessionId = third)

        assertEquals(listOf(80.0), last.map { it.weightKg })
    }

    @Test
    fun `an unfinished workout is not the last time`() = runBlocking {
        val unfinished = startSession()
        service.addExercise(unfinished, "bench")
        service.discardSession(unfinished)
        val current = startSession()

        assertEquals(emptyList(), service.lastWorkingSets("bench", excludingSessionId = current))
    }

    private fun exercises() = listOf("bench", "squat").associateWith {
        Exercise(it, emptyMap(), Equipment("BARBELL", emptyMap()), emptyList(), emptyList())
    }
}
