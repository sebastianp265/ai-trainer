package com.github.sebastianp265.aitrainer.feature.coach.impl

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DefaultCoachMemoryServiceUTest {

    private val f = CoachFixture()

    @AfterTest
    fun close() = f.database.close()

    @Test
    fun `notes can be added, changed and deleted`() = runBlocking {
        val id = f.memory.addNote("  Prefers mornings ")
        assertEquals("Prefers mornings", f.memory.observeNotes().first().single().text)

        assertTrue(f.memory.updateNote(id, "Prefers early mornings"))
        assertEquals("Prefers early mornings", f.memory.observeNotes().first().single().text)

        assertTrue(f.memory.deleteNote(id))
        assertEquals(emptyList(), f.memory.observeNotes().first())
        assertTrue(!f.memory.deleteNote(id))
        assertTrue(!f.memory.updateNote(id, "x"))
    }

    @Test
    fun `a note needs text`() = runBlocking<Unit> {
        assertFailsWith<IllegalArgumentException> { f.memory.addNote("  ") }
    }

    @Test
    fun `the profile keeps the fields that were filled in and drops the ones that were emptied`() = runBlocking {
        f.memory.setProfileField("equipment", "full gym")
        f.memory.setProfileField("time", "45 minutes")
        assertEquals(mapOf("equipment" to "full gym", "time" to "45 minutes"), f.memory.observeProfile().first())

        f.memory.setProfileField("time", " ")

        assertEquals(mapOf("equipment" to "full gym"), f.memory.observeProfile().first())
    }

    @Test
    fun `an unknown profile field is refused`() = runBlocking<Unit> {
        assertFailsWith<IllegalArgumentException> { f.memory.setProfileField("mood", "x") }
    }
}
