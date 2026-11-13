package com.github.sebastianp265.aitrainer.feature.exercise.impl

import com.github.sebastianp265.aitrainer.integration.exercise.NewExercise
import com.github.sebastianp265.aitrainer.shared.database.impl.inMemoryDatabase
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

class DefaultExerciseServiceUTest {

    private val clock = object : Clock {
        override fun now() = Instant.fromEpochMilliseconds(1_000)
    }
    private val database = inMemoryDatabase()
    private val service = DefaultExerciseService(database.exerciseDao(), clock)

    @AfterTest
    fun close() = database.close()

    @Test
    fun `a created exercise has its name, one primary group and its secondary groups`() = runTest {
        val key = service.createExercise(NewExercise("  Landmine press ", "SHOULDERS", listOf("CHEST", "ARMS")))

        val created = service.exercises().single { it.key == key }
        assertEquals("Landmine press", created.namesByLanguage.getValue("en"))
        assertEquals(listOf("SHOULDERS"), created.primaryMuscles.map { it.key })
        assertEquals(setOf("CHEST", "ARMS"), created.secondaryMuscles.map { it.key }.toSet())
    }

    @Test
    fun `the primary group is not repeated as a secondary group`() = runTest {
        val key = service.createExercise(NewExercise("Hip drive", "LEGS", listOf("LEGS", "ABS")))

        val created = service.exercises().single { it.key == key }
        assertEquals(listOf("ABS"), created.secondaryMuscles.map { it.key })
    }
}
